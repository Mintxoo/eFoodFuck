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
        loadSampleRestaurants();
    }

    public void start(String masterHost, int masterPort) throws Exception {
        // 1) Registrar en Master y esperar confirmación
        try (Socket sock = new Socket(masterHost, masterPort);
             ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(sock.getInputStream())) {
            // Enviamos REGISTER
            oos.writeObject(new Message(Message.MessageType.REGISTER, info));
            oos.flush();
            // Leemos la respuesta del Master
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

    /**
     * Inicializa la lista con 5 restaurantes de muestra.
     */
    private void loadSampleRestaurants() {
        // Restaurantes de muestra
        // Restaurantes de muestra (coordenadas en un radio de ~5 km desde DEFAULT_LAT, DEFAULT_LON)
        Restaurant r1 = new Restaurant("PizzaFun", 40.01, 23.01, "pizza", 4.5, PriceCategory.TWO_DOLLARS); // Original (centro)
        r1.addProduct("Margherita", 8.99);
        r1.addProduct("Coke", 1.50);
        r1.addRating(5);
        r1.addRating(4);
        restaurants.add(r1);

        Restaurant r2 = new Restaurant("SushiPlace", 40.01 + 0.02, 23.01 + 0.03, "sushi", 4.0, PriceCategory.THREE_DOLLARS); // ~3 km NE
        r2.addProduct("California Roll", 6.50);
        r2.addProduct("Green Tea", 2.00);
        r2.addRating(4);
        r2.addRating(5);
        restaurants.add(r2);

        Restaurant r3 = new Restaurant("BurgerSpot", 40.01 - 0.025, 23.01 + 0.015, "burger", 3.5, PriceCategory.ONE_DOLLAR); // ~2 km SE
        r3.addProduct("Cheeseburger", 5.99);
        r3.addProduct("Fries", 2.50);
        r3.addRating(3);
        r3.addRating(4);
        restaurants.add(r3);

        Restaurant r4 = new Restaurant("PastaHouse", 40.01 - 0.03, 23.01 - 0.02, "pasta", 4.2, PriceCategory.TWO_DOLLARS); // ~3.6 km SW
        r4.addProduct("Spaghetti Bolognese", 9.99);
        r4.addProduct("Garlic Bread", 2.99);
        r4.addRating(4);
        r4.addRating(5);
        restaurants.add(r4);

        Restaurant r5 = new Restaurant("TacoCorner", 40.01 + 0.015, 23.01 - 0.035, "tacos", 4.8, PriceCategory.ONE_DOLLAR); // ~3.8 km NW
        r5.addProduct("Beef Taco", 3.99);
        r5.addProduct("Salsa", 0.99);
        r5.addRating(5);
        r5.addRating(5);
        restaurants.add(r5);
    }


    public synchronized void addRestaurant(Restaurant r) {
        System.out.println("Worker " + info.getId() + ": addRestaurant invoked para " + r);
        restaurants.add(r);
        System.out.println("Worker " + info.getId() + ": current restaurants = " + restaurants);
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
        // Si no se ponen lat y lon (0.0), saltamos distancia
        boolean skipDistance = fs.getLatitude() == 0.0 && fs.getLongitude() == 0.0;
        // Si no se pone priceCategory (null), saltamos precio
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
                            System.out.println("Producto no válido: '" + productName + "' no existe en " + r.getName());
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

    /**
     * Haversine simplificado in‑place
     */
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
        // args: id host port masterHost masterPort
        WorkerInfo info = new WorkerInfo(args[0], args[1], Integer.parseInt(args[2]));
        String masterHost = args[3];
        int masterPort = Integer.parseInt(args[4]);
        new WorkerNode(info).start(masterHost, masterPort);
    }
}
