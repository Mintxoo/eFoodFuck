package main;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Información de restaurante, con productos y valoraciones.
 */
public class Restaurant implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final double latitude;
    private final double longitude;
    private final String foodCategory;
    private final PriceCategory priceCategory;
    private int noOfVotes;
    private double averageRating;
    private final Map<String, Product> products = new HashMap<>();
    private final Map<String, Integer> sales = new HashMap<>();

    public Restaurant(String name,
                      double latitude,
                      double longitude,
                      String foodCategory,
                      double initialRating,
                      PriceCategory priceCategory) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.foodCategory = foodCategory;
        this.noOfVotes = 1;
        this.averageRating = initialRating;
        this.priceCategory = priceCategory;
    }

    public String getName() { return name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getFoodCategory() { return foodCategory; }
    public PriceCategory getPriceCategory() { return priceCategory; }
    public int getNoOfVotes() { return noOfVotes; }
    public double getAverageRating() { return averageRating; }

    public Map<String, Product> getProducts() {
        return Collections.unmodifiableMap(products);
    }
    public Map<String, Integer> getSales() {
        return Collections.unmodifiableMap(sales);
    }

    public synchronized void addProduct(String productName, double price) {
        products.put(productName, new Product(productName, price));
    }
    public synchronized void removeProduct(String productName) {
        products.remove(productName);
    }

    public synchronized void addSale(String productName, int qty) {
        sales.merge(productName, qty, Integer::sum);
    }

    public synchronized void addRating(int rating) {
        averageRating = (averageRating * noOfVotes + rating) / (++noOfVotes);
        System.out.println("Updated rating for " + name + ": " + averageRating);
    }


    @Override
    public String toString() {
        return "Restaurant{name='" + name + "', lat=" + latitude + ", lon=" + longitude +
                ", category='" + foodCategory + "', avgRating=" + averageRating +
                ", priceCat=" + priceCategory + ", products=" + products.keySet() +
                "}";
    }
}