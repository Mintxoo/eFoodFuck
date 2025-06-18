package main;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * Worker que almacena restaurantes, productos y ventas en memoria.
 */
public class WorkerNode {
    private final WorkerInfo info;
    private final List<Restaurant> restaurants = new ArrayList<>();

    public WorkerNode(WorkerInfo info) {
        this.info = info;
    }

    public void start(String masterHost, int masterPort) throws Exception {
        // 1) Registrar en Master y esperar confirmación
        try (Socket sock = new Socket(masterHost, masterPort);
             ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(sock.getInputStream())) {
            oos.writeObject(new Message(Message.MessageType.REGISTER, info));
            oos.flush();
            Message resp = (Message) ois.readObject();
            System.out.println("Register response from Master: " + resp.getPayload());
        }
        // 2) Escuchar peticiones
        try (ServerSocket server = new ServerSocket(info.getPort())) {
            System.out.println("Worker " + info.getId() + " escuchando en " + info.getPort());
            while (true) {
                Socket sock = server.accept();
                new Thread(new WorkerHandler(sock, this)).start();
            }
        }
    }

    public synchronized void addRestaurant(Restaurant r) {
        restaurants.add(r);
        System.out.println("Worker " + info.getId() + ": added restaurant " + r.getName());
    }
    public synchronized void addProduct(String store, String product, double price) {
        restaurants.stream()
                .filter(r -> r.getName().equals(store))
                .findFirst()
                .ifPresent(r -> r.addProduct(product, price));
    }
    public synchronized void removeProduct(String store, String product) {
        restaurants.stream()
                .filter(r -> r.getName().equals(store))
                .findFirst()
                .ifPresent(r -> r.removeProduct(product));
    }
    public synchronized void rate(String store, int stars) {
        restaurants.stream()
                .filter(r -> r.getName().equals(store))
                .findFirst()
                .ifPresent(r -> r.addRating(stars));
    }
    public synchronized MapResult handleSearch(FilterSpec fs) {
        MapResult mr = new MapResult();
        for (Restaurant r : restaurants) {
            double dist = haversine(fs.getLatitude(), fs.getLongitude(),
                    r.getLatitude(), r.getLongitude());
            if (dist <= fs.getRadiusKm()
                    && fs.getFoodCategories().contains(r.getFoodCategory())
                    && r.getAverageRating() >= fs.getMinStars()
                    && r.getPriceCategory() == fs.getPriceCategory()) {
                mr.addRestaurant(r);
            }
        }
        return mr;
    }
    public synchronized void handleSale(Sale sale) {
        restaurants.stream()
                .filter(r -> r.getName().equals(sale.getStoreName()))
                .findFirst()
                .ifPresent(r -> sale.getItems().forEach(r::addSale));
    }
    public synchronized MapResult handleReport(String type) {
        MapResult mr = new MapResult();
        if ("food".equals(type)) {
            for (Restaurant r : restaurants) {
                mr.addVenta(r.getFoodCategory(),
                        r.getSales().values().stream().mapToInt(Integer::intValue).sum());
            }
        } else {
            for (Restaurant r : restaurants) {
                r.getSales().forEach(mr::addVenta);
            }
        }
        return mr;
    }

    /** Haversine simplificado in‑place */
    private static double haversine(double lat1, double lon1,
                                    double lat2, double lon2) {
        double R = 6371.0088; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2)*Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }

    public static void main(String[] args) throws Exception {
        WorkerInfo info = new WorkerInfo(args[0], args[1], Integer.parseInt(args[2]));
        String masterHost = args[3];
        int masterPort = Integer.parseInt(args[4]);
        new WorkerNode(info).start(masterHost, masterPort);
    }
}