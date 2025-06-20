package main;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;


public class WorkerNode {
    private final WorkerInfo info;
    private final List<Restaurant> restaurants = new ArrayList<>();

    public WorkerNode(WorkerInfo info) {
        this.info = info;
    }

    public void start(String masterHost, int masterPort) throws Exception {

        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(info.getPort())) {
                System.out.println("Worker " + info.getId() + " listening on " + info.getPort());
                while (true) {
                    Socket s = server.accept();
                    new Thread(new WorkerHandler(s, this)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        try (Socket sock = new Socket(masterHost, masterPort);
             ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
             ObjectInputStream  ois = new ObjectInputStream(sock.getInputStream())) {
            oos.writeObject(new Message(Message.MessageType.REGISTER, info));
            oos.flush();
            Message resp = (Message) ois.readObject();
            System.out.println("Register response from Master: " + resp.getPayload());
        }
    }

    public synchronized void addRestaurant(Restaurant r) {
        System.out.println("Worker " + info.getId() + ": addRestaurant invoked for " + r.getName());
        restaurants.add(r);
    }

    public synchronized void removeRestaurant(Restaurant r) {
        restaurants.removeIf(existing -> existing.getName().equals(r.getName()));
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

        boolean skipDistance = fs.getLatitude() == 0.0 && fs.getLongitude() == 0.0;

        boolean skipPrice    = fs.getPriceCategory() == null;

        for (Restaurant r : restaurants) {
            boolean distMatch = true;
            if (!skipDistance) {
                double dist = haversine(
                        fs.getLatitude(), fs.getLongitude(),
                        r.getLatitude(), r.getLongitude());
                distMatch = dist <= fs.getRadiusKm();
            }

            boolean catMatch   = fs.getFoodCategories().isEmpty()
                    || fs.getFoodCategories().contains(r.getFoodCategory());
            boolean starsMatch = r.getAverageRating() >= fs.getMinStars();
            boolean priceMatch = skipPrice
                    || r.getPriceCategory() == fs.getPriceCategory();

            if (distMatch && catMatch && starsMatch && priceMatch) {
                mr.addRestaurant(r);
            }
        }
        return mr;
    }




    public synchronized void handleSale(Sale sale) {
        restaurants.stream()
                .filter(r -> r.getName().equals(sale.getStoreName()))
                .findFirst()
                .ifPresent(r -> {
                    Map<String, Product> availableProducts = r.getProducts();
                    for (Map.Entry<String, Integer> item : sale.getItems().entrySet()) {
                        String productName = item.getKey();
                        int qty = item.getValue();
                        if (availableProducts.containsKey(productName)) {
                            r.addSale(productName, qty);
                        } else {
                            System.out.println("Not valid product name: '" + productName + "' does not exist on " + r.getName());
                        }
                    }
                });
    }



    public synchronized MapResult handleReport(String type) {
        MapResult mr = new MapResult();

        if ("all".equals(type)) {
            for (Restaurant r : restaurants) {
                r.getSales().forEach(mr::addVenta);
            }
        } else if (type.startsWith("food:")) {
            String foodCat = type.substring("food:".length()).toLowerCase();
            for (Restaurant r : restaurants) {
                if (r.getFoodCategory().equalsIgnoreCase(foodCat)) {
                    int totalSales = r.getSales().values().stream().mapToInt(Integer::intValue).sum();
                    if (totalSales > 0) {
                        mr.addVenta(r.getName(), totalSales);
                    }
                }
            }
        } else if (type.startsWith("product:")) {
            String product = type.substring("product:".length()).toLowerCase();
            for (Restaurant r : restaurants) {
                r.getSales().forEach((productName, quantity) -> {
                    if (productName.equalsIgnoreCase(product)) {
                        mr.addVenta(r.getName(), quantity);
                    }
                });
            }
        }

        return mr;
    }

    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
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
