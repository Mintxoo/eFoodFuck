package main;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servidor maestro que coordina Manager, Clientes y Workers.
 */
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
            System.out.println("MasterServer escuchando en " + port);
            while (true) {
                Socket sock = server.accept();
                new Thread(() -> handleConnection(sock)).start();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadRestaurantsFromJson(String path) throws IOException, ScriptException {
        String json = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");
        Object parsed = engine.eval("Java.asJSONCompatible(" + json + ")");

        List<Object> arr = (List<Object>) parsed;
        for (Object entry : arr) {
            Map<String,Object> obj = (Map<String,Object>) entry;
            String name    = (String) obj.get("name");
            double lat     = ((Number)obj.get("latitude")).doubleValue();
            double lon     = ((Number)obj.get("longitude")).doubleValue();
            String cat     = (String) obj.get("category");
            double rating  = ((Number)obj.get("averageRating")).doubleValue();
            PriceCategory pc = PriceCategory.valueOf((String) obj.get("priceCategory"));

            Restaurant r = new Restaurant(name, lat, lon, cat, rating, pc);

            List<Object> prods = (List<Object>) obj.get("products");
            for (Object p0 : prods) {
                Map<String,Object> p = (Map<String,Object>) p0;
                String prodName = (String) p.get("name");
                double price    = ((Number)p.get("price")).doubleValue();
                r.addProduct(prodName, price);
            }
            if (obj.containsKey("ratings")) {
                List<Object> rates = (List<Object>) obj.get("ratings");
                for (Object v : rates) {
                    r.addRating(((Number)v).intValue());
                }
            }

            allRestaurants.add(r);
        }

        System.out.println("Master: cargados " + allRestaurants.size() + " restaurantes desde JSON");
    }

    private void handleConnection(Socket sock) {
        try (
                sock;
                ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
                ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream())
        ) {
            // Leemos un único mensaje al abrir la conexión
            Message msg = (Message) ois.readObject();

            // Si es un registro de Worker, lo procesamos y cerramos
            if (msg.getType() == Message.MessageType.REGISTER) {
                WorkerInfo w = (WorkerInfo) msg.getPayload();
                registerWorker(w);
                oos.writeObject(new Message(Message.MessageType.RESULT, "OK"));
                oos.flush();
                return;
            }

            // En otro caso, atendemos a Manager/Client en bucle
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
                // Leemos siguiente mensaje; EOFException finalizará el bucle
            } while ((msg = (Message) ois.readObject()) != null);

        } catch (EOFException eof) {
            // Manager o Client cerró la conexión (esperado)
        } catch (Exception e) {
            System.err.println("Error en conexión: " + e.getMessage());
        }
    }


    /** Registra un Worker. */
    public void registerWorker(WorkerInfo w) {
        synchronized (workers) {
            workers.add(w);
        }
        System.out.println("Worker registrado: " + w);
        rebalanceAssignments();
    }

    /** Reasigna TODOS los restaurantes según hash(name)%nWorkers,
     enviando ADD y REMOVE *solo* cuando cambie la asignación. */
    private void rebalanceAssignments() {
        int n = workers.size();
        for (Restaurant r : allRestaurants) {
            int slot = Math.abs(r.getName().hashCode()) % n;
            WorkerInfo target = workers.get(slot);
            WorkerInfo current = assignmentMap.get(r.getName());

            if (current == null) {
                // nunca enviado: lo mando al target
                sendToWorker(target, new Message(Message.MessageType.ADD_RESTAURANT, r));
                assignmentMap.put(r.getName(), target);
                System.out.println("Master → ADD " + r.getName() + " a Worker " + slot);
            }
            else if (!current.equals(target)) {
                // había en otro worker: lo quito y lo muevo
                sendToWorker(current, new Message(Message.MessageType.REMOVE_RESTAURANT, r));
                sendToWorker(target, new Message(Message.MessageType.ADD_RESTAURANT, r));
                assignmentMap.put(r.getName(), target);
                System.out.println("Master → MOVE " + r.getName() +
                        " de Worker " + workers.indexOf(current) +
                        " a Worker " + slot);
            }
            // si current == target, ya lo tiene y no hacemos nada
        }
    }

    /** Manager Mode: añade un restaurante al worker correspondiente. */
    public void addRestaurant(Restaurant r) {
        int idx = Math.abs(r.getName().hashCode()) % workers.size();
        WorkerInfo w = workers.get(idx);
        sendToWorker(w, new Message(Message.MessageType.ADD_RESTAURANT, r));
    }

    private synchronized void createRestaurant(Restaurant r) {
        allRestaurants.add(r);
        int n = workers.size();
        if (n == 0) {
            System.out.println("Master: no hay workers registrados; " +
                    "restaurante " + r.getName() + " pendiente.");
            // Podrías guardar pendientes y enviarlos al primer worker que llegue
            return;
        }

        int slot = Math.abs(r.getName().hashCode()) % n;
        WorkerInfo target = workers.get(slot);

        // Registra la asignación
        assignmentMap.put(r.getName(), target);

        // Envía sólo a ese Worker
        sendToWorker(target, new Message(Message.MessageType.ADD_RESTAURANT, r));
        System.out.println("Master: asignado nuevo restaurante "
                + r.getName() + " al Worker " + slot);
    }

    /** Manager Mode: añade un producto. */
    public void addProduct(ProductAction pa) {
        int idx = Math.abs(pa.getStoreName().hashCode()) % workers.size();
        WorkerInfo w = workers.get(idx);
        sendToWorker(w, new Message(Message.MessageType.ADD_PRODUCT, pa));
    }

    /** Manager Mode: remueve un producto. */
    public void removeProduct(ProductAction pa) {
        int idx = Math.abs(pa.getStoreName().hashCode()) % workers.size();
        WorkerInfo w = workers.get(idx);
        sendToWorker(w, new Message(Message.MessageType.REMOVE_PRODUCT, pa));
    }

    /** Manager Mode: valora un restaurante. */
    public void rateRestaurant(Rating rt) {
        int idx = Math.abs(rt.getStoreName().hashCode()) % workers.size();
        WorkerInfo w = workers.get(idx);
        sendToWorker(w, new Message(Message.MessageType.RATE, rt));
    }

    // En MasterServer.java, justo al inicio de sendToWorker():
    private void sendToWorker(WorkerInfo w, Message msg) {
        System.out.println("Master->Worker " + w.getId() + ": enviando " + msg.getType());
        try (Socket s = new Socket(w.getHost(), w.getPort());
             ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(s.getInputStream())) {

            oos.writeObject(msg);
            oos.flush();

            // Esperar y leer respuesta
            Message resp = (Message) ois.readObject();
            System.out.println("Respuesta del worker " + w.getId() + ": " + resp.getPayload());

        } catch (IOException | ClassNotFoundException e) {
            System.err.println(msg.getType() + " fallo en " + w + ": " + e);
        }
    }


    /** Envía FilterSpec y recoge MapResults. */
    public List<MapResult> dispatchMapTasks(FilterSpec fs) {
        List<MapResult> res = new ArrayList<>();
        for (WorkerInfo w : workers) {
            try { res.add(new MapTask(fs, w).execute()); }
            catch (Exception e) { System.err.println("MapTask fallo en " + w + ": " + e); }
        }
        return res;
    }

    /** Envía petición de reporte y recoge MapResults de ventas. */
    public List<MapResult> dispatchSalesReports(String type) {
        List<MapResult> res = new ArrayList<>();
        for (WorkerInfo w : workers) {
            try { res.add(new ReportTask(type, w).execute()); }
            catch (Exception e) { System.err.println("ReportTask fallo en " + w + ": " + e); }
        }
        return res;
    }

    /** Combina parciales usando ReduceTask. */
    public ReduceResult reduce(List<MapResult> partials) {
        return new ReduceTask().combine(partials);
    }

    /** Reenvía un mensaje a todos los workers (sale, purchase). */
    public void broadcast(Message msg) {
        for (WorkerInfo w : workers) {
            try (Socket s = new Socket(w.getHost(), w.getPort());
                 ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
                 ObjectInputStream ois = new ObjectInputStream(s.getInputStream())) {

                oos.writeObject(msg);
                oos.flush();

                // Esperar una respuesta simple
                Message resp = (Message) ois.readObject();
                System.out.println("Broadcast respuesta del worker " + w.getId() + ": " + resp.getPayload());

            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Broadcast fallo en " + w + ": " + e);
            }
        }
    }


    public static void main(String[] args) throws Exception {
        MasterServer server = new MasterServer(5555);
        server.loadRestaurantsFromJson("../../../../restaurants.json");
        server.start();
    }
}
