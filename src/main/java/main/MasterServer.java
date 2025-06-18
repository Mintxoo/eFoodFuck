package main;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * Servidor maestro que coordina Manager, Clientes y Workers.
 */
public class MasterServer {
    private final int port;
    private final List<WorkerInfo> workers = new ArrayList<>();

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

    // dentro de MasterServer.java

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
    }

    /** Manager Mode: añade un restaurante al worker correspondiente. */
    public void addRestaurant(Restaurant r) {
        int idx = Math.abs(r.getName().hashCode()) % workers.size();
        WorkerInfo w = workers.get(idx);
        sendToWorker(w, new Message(Message.MessageType.ADD_RESTAURANT, r));
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

    private void sendToWorker(WorkerInfo w, Message msg) {
        try (Socket s = new Socket(w.getHost(), w.getPort());
             ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream())) {
            oos.writeObject(msg);
            oos.flush();
        } catch (IOException e) {
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
                 ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream())) {
                oos.writeObject(msg);
            } catch (IOException e) {
                System.err.println("Broadcast fallo en " + w + ": " + e);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new MasterServer(5555).start();
    }
}
