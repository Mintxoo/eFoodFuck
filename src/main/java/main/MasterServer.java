// src/main/java/main/MasterServer.java
package main;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class MasterServer {
    private final int port;
    private final List<WorkerInfo> workers = new ArrayList<>();

    public MasterServer(int port) {
        this.port = port;
    }

    public void start() throws IOException, ClassNotFoundException {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("MasterServer escuchando en " + port);
            while (true) {
                Socket sock = server.accept();
                // atender cada cliente/manager en un hilo:
                new Thread(() -> handleConnection(sock)).start();
            }
        }
    }

    private void handleConnection(Socket sock) {
        try (sock;
             ObjectInputStream  ois = new ObjectInputStream(sock.getInputStream());
             ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream())) {

            Message msg;
            while ((msg = (Message) ois.readObject()) != null) {
                switch (msg.getType()) {
                    case REGISTER -> {
                        WorkerInfo w = (WorkerInfo) msg.getPayload();
                        synchronized (workers) { workers.add(w); }
                        oos.writeObject(new Message(Message.MessageType.RESULT, "OK"));
                    }
                    case TASK -> {
                        FilterSpec fs = (FilterSpec) msg.getPayload();
                        List<MapResult> partials = dispatchMapTasks(fs);
                        ReduceResult rr = new ReduceTask().combine(partials);
                        oos.writeObject(new Message(Message.MessageType.RESULT, rr));
                    }
                    case SALE -> {
                        Object sale = msg.getPayload();
                        broadcast(new Message(Message.MessageType.SALE, sale));
                        oos.writeObject(new Message(Message.MessageType.RESULT, "OK"));
                    }
                    case REPORT -> {
                        String type = (String) msg.getPayload();
                        List<MapResult> partials = dispatchSalesReports(type);
                        ReduceResult rr = new ReduceTask().combine(partials);
                        oos.writeObject(new Message(Message.MessageType.RESULT, rr.getVentasPorKey()));
                    }
                    case PING -> {
                        oos.writeObject(new Message(Message.MessageType.PONG, "OK"));
                    }
                    default -> {
                        oos.writeObject(new Message(Message.MessageType.RESULT, "UNKNOWN"));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error en conexión: " + e.getMessage());
        }
    }

    public void registerWorker(WorkerInfo w) {
        synchronized (workers) {
            workers.add(w);
        }
        System.out.println("Worker registrado: " + w);
    }

    public List<MapResult> dispatchMapTasks(FilterSpec fs) {
        List<MapResult> res = new ArrayList<>();
        for (WorkerInfo w : workers) {
            try { res.add(new MapTask(fs, w).execute()); }
            catch (Exception e) { System.err.println("MapTask falló en " + w + ": " + e); }
        }
        return res;
    }

    public List<MapResult> dispatchSalesReports(String type) {
        List<MapResult> res = new ArrayList<>();
        for (WorkerInfo w : workers) {
            try { res.add(new ReportTask(type, w).execute()); }
            catch (Exception e) { System.err.println("ReportTask falló en " + w + ": " + e); }
        }
        return res;
    }

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
