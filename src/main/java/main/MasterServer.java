package main;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class MasterServer {
    private final int port;
    private List<Restaurant> allRestaurants = new ArrayList<>();
    private List<WorkerInfo> workers = new ArrayList<>();
    private Map<String,WorkerInfo> assignmentMap = new HashMap<>();

    public MasterServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("MasterServer listening on " + port);
            while (true) {
                Socket sock = server.accept();
                new Thread(() -> handleConnection(sock)).start();
            }
        }
    }

    private void loadRestaurantsFromJson(String path) throws IOException {
        String json = Files.readString(Paths.get(path), StandardCharsets.UTF_8)
                .replaceAll("[\\n\\r]", " ")
                .trim();

        int b0 = json.indexOf('[');
        int b1 = json.lastIndexOf(']');
        if (b0 < 0 || b1 < 0 || b1 <= b0) {
            throw new IOException("JSON format invalid");
        }
        String body = json.substring(b0 + 1, b1);

        List<String> objs = new ArrayList<>();
        int depth = 0, start = -1;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    objs.add(body.substring(start, i + 1));
                    start = -1;
                }
            }
        }

        for (String obj : objs) {
            String name   = extractString(obj, "name");
            double lat    = extractDouble(obj, "latitude");
            double lon    = extractDouble(obj, "longitude");
            String cat    = extractString(obj, "category");
            double rating = extractDouble(obj, "averageRating");
            PriceCategory pc = PriceCategory.valueOf(
                    extractString(obj, "priceCategory")
            );

            Restaurant r = new Restaurant(name, lat, lon, cat, rating, pc);

            String prodsBlock = extractArrayBlock(obj, "products");
            if (!prodsBlock.isEmpty()) {
                int pd = 0, ps = -1;
                for (int i = 0; i < prodsBlock.length(); i++) {
                    char ch = prodsBlock.charAt(i);
                    if (ch == '{') {
                        if (pd == 0) ps = i;
                        pd++;
                    } else if (ch == '}') {
                        pd--;
                        if (pd == 0 && ps >= 0) {
                            String pObj = prodsBlock.substring(ps, i + 1);
                            String prodName = extractString(pObj, "name");
                            double price    = extractDouble(pObj, "price");
                            r.addProduct(prodName, price);
                            ps = -1;
                        }
                    }
                }
            }

            String ratesBlock = extractArrayBlock(obj, "ratings");
            if (!ratesBlock.isEmpty()) {
                for (String num : ratesBlock.split(",")) {
                    try {
                        r.addRating(Integer.parseInt(num.trim()));
                    } catch (NumberFormatException ignored) {}
                }
            }

            allRestaurants.add(r);
        }

        System.out.println("Master: saved "
                + allRestaurants.size()
                + " restaurants from JSON");
    }

    private static String extractString(String src, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(src);
        return m.find() ? m.group(1) : "";
    }

    private static double extractDouble(String src, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*([-+]?\\d*\\.?\\d+)");
        Matcher m = p.matcher(src);
        return m.find() ? Double.parseDouble(m.group(1)) : 0;
    }

    private static String extractArrayBlock(String src, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
        Matcher m = p.matcher(src);
        return m.find() ? m.group(1) : "";
    }

    private void handleConnection(Socket sock) {
        try (
                sock;
                ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
                ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream())
        ) {

            Message msg = (Message) ois.readObject();

            if (msg.getType() == Message.MessageType.REGISTER) {
                WorkerInfo w = (WorkerInfo) msg.getPayload();
                registerWorker(w);
                oos.writeObject(new Message(Message.MessageType.RESULT, "OK"));
                oos.flush();
                return;
            }

            do {
                switch (msg.getType()) {
                    case ADD_RESTAURANT -> {
                        Restaurant r = (Restaurant) msg.getPayload();
                        addRestaurant(r);
                        oos.writeObject(new Message(Message.MessageType.RESULT, "OK"));
                    }
                    case CREATE_RESTAURANT -> {
                        Restaurant r = (Restaurant) msg.getPayload();
                        createRestaurant(r);
                        oos.writeObject(new Message(Message.MessageType.RESULT, "OK"));
                    }
                    case ADD_PRODUCT -> {
                        ProductAction pa = (ProductAction) msg.getPayload();
                        addProduct(pa);
                        oos.writeObject(new Message(Message.MessageType.RESULT, "OK"));
                    }
                    case REMOVE_PRODUCT -> {
                        ProductAction pa2 = (ProductAction) msg.getPayload();
                        removeProduct(pa2);
                        oos.writeObject(new Message(Message.MessageType.RESULT, "OK"));
                    }
                    case RATE -> {
                        Rating rt = (Rating) msg.getPayload();
                        rateRestaurant(rt);
                        oos.writeObject(new Message(Message.MessageType.RESULT, "OK"));
                    }
                    case TASK -> {
                        FilterSpec fs = (FilterSpec) msg.getPayload();
                        List<MapResult> partials = dispatchMapTasks(fs);
                        ReduceResult rr = new ReduceTask().combine(partials);
                        oos.writeObject(new Message(Message.MessageType.RESULT, rr));
                    }
                    case SALE -> {
                        Sale sale = (Sale) msg.getPayload();
                        broadcast(new Message(Message.MessageType.SALE, sale));
                        oos.writeObject(new Message(Message.MessageType.RESULT, "OK"));
                    }
                    case REPORT -> {
                        String type = (String) msg.getPayload();
                        List<MapResult> partials = dispatchSalesReports(type);
                        ReduceResult rr = reduce(partials);
                        oos.writeObject(new Message(Message.MessageType.RESULT, rr.getVentasPorKey()));
                    }
                    case PING -> {
                        oos.writeObject(new Message(Message.MessageType.PONG, "OK"));
                    }
                    default -> {
                        oos.writeObject(new Message(Message.MessageType.RESULT, "UNKNOWN"));
                    }
                }
                oos.flush();

            } while ((msg = (Message) ois.readObject()) != null);

        } catch (EOFException eof) {
            // Manager or Client closed connection
        } catch (Exception e) {
            System.err.println("Error en conexión: " + e.getMessage());
        }
    }

    public void registerWorker(WorkerInfo w) {
        synchronized (workers) {
            workers.add(w);
        }
        System.out.println("Worker registrado: " + w);
        rebalanceAssignments();
    }

    private void rebalanceAssignments() {
        int n = workers.size();
        for (Restaurant r : allRestaurants) {
            int slot = Math.abs(r.getName().hashCode()) % n;
            WorkerInfo target = workers.get(slot);
            WorkerInfo current = assignmentMap.get(r.getName());

            if (current == null) {

                sendToWorker(target, new Message(Message.MessageType.ADD_RESTAURANT, r));
                assignmentMap.put(r.getName(), target);
                System.out.println("Master → ADD " + r.getName() + " to Worker " + slot);
            }
            else if (!current.equals(target)) {

                sendToWorker(current, new Message(Message.MessageType.REMOVE_RESTAURANT, r));
                sendToWorker(target, new Message(Message.MessageType.ADD_RESTAURANT, r));
                assignmentMap.put(r.getName(), target);
                System.out.println("Master → MOVE " + r.getName() +
                        " from Worker " + workers.indexOf(current) +
                        " to Worker " + slot);
            }
            // if current == target, it already has so we do nothing
        }
    }

    public void addRestaurant(Restaurant r) {
        int idx = Math.abs(r.getName().hashCode()) % workers.size();
        WorkerInfo w = workers.get(idx);
        sendToWorker(w, new Message(Message.MessageType.ADD_RESTAURANT, r));
    }

    private synchronized void createRestaurant(Restaurant r) {
        allRestaurants.add(r);
        int n = workers.size();
        if (n == 0) {
            System.out.println("Master: no workers registered; " +
                    "restaurant " + r.getName());
            return;
        }

        int slot = Math.abs(r.getName().hashCode()) % n;
        WorkerInfo target = workers.get(slot);

        assignmentMap.put(r.getName(), target);

        sendToWorker(target, new Message(Message.MessageType.ADD_RESTAURANT, r));
        System.out.println("Master: new restaurant assigned"
                + r.getName() + " to Worker " + slot);
    }

    public void addProduct(ProductAction pa) {
        int idx = Math.abs(pa.getStoreName().hashCode()) % workers.size();
        WorkerInfo w = workers.get(idx);
        sendToWorker(w, new Message(Message.MessageType.ADD_PRODUCT, pa));
    }

    public void removeProduct(ProductAction pa) {
        int idx = Math.abs(pa.getStoreName().hashCode()) % workers.size();
        WorkerInfo w = workers.get(idx);
        sendToWorker(w, new Message(Message.MessageType.REMOVE_PRODUCT, pa));
    }

    public void rateRestaurant(Rating rt) {
        int idx = Math.abs(rt.getStoreName().hashCode()) % workers.size();
        WorkerInfo w = workers.get(idx);
        sendToWorker(w, new Message(Message.MessageType.RATE, rt));
    }

    private void sendToWorker(WorkerInfo w, Message msg) {
        System.out.println("Master->Worker " + w.getId() + ": send " + msg.getType());
        try (Socket s = new Socket(w.getHost(), w.getPort());
             ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(s.getInputStream())) {

            oos.writeObject(msg);
            oos.flush();

            Message resp = (Message) ois.readObject();
            System.out.println("Response from worker " + w.getId() + ": " + resp.getPayload());

        } catch (IOException | ClassNotFoundException e) {
            System.err.println(msg.getType() + " Error in " + w + ": " + e);
        }
    }

    public List<MapResult> dispatchMapTasks(FilterSpec fs) {
        List<MapResult> res = new ArrayList<>();
        for (WorkerInfo w : workers) {
            try { res.add(new MapTask(fs, w).execute()); }
            catch (Exception e) { System.err.println("MapTask failed in " + w + ": " + e); }
        }
        return res;
    }

    public List<MapResult> dispatchSalesReports(String type) {
        List<MapResult> res = new ArrayList<>();
        for (WorkerInfo w : workers) {
            try { res.add(new ReportTask(type, w).execute()); }
            catch (Exception e) { System.err.println("ReportTask failed in " + w + ": " + e); }
        }
        return res;
    }

    public ReduceResult reduce(List<MapResult> partials) {
        return new ReduceTask().combine(partials);
    }

    public void broadcast(Message msg) {
        for (WorkerInfo w : workers) {
            try (Socket s = new Socket(w.getHost(), w.getPort());
                 ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
                 ObjectInputStream ois = new ObjectInputStream(s.getInputStream())) {

                oos.writeObject(msg);
                oos.flush();

                Message resp = (Message) ois.readObject();
                System.out.println("Broadcast response from worker " + w.getId() + ": " + resp.getPayload());

            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Broadcast failed in " + w + ": " + e);
            }
        }
    }


    public static void main(String[] args) throws Exception {
        MasterServer server = new MasterServer(5555);
        server.loadRestaurantsFromJson("restaurants.json");
        server.start();
    }
}
