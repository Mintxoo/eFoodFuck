package main;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Información de un restaurante, con productos, ventas y valoraciones.
 */
public class Restaurant implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final double latitude;
    private final double longitude;
    private final String foodCategory;
    private final PriceCategory priceCategory;

    private final Map<String, Integer> sales = new HashMap<>();
    private int noOfVotes;
    private double averageRating;
    private final Map<String, Product> products = new HashMap<>();

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
        this.averageRating = initialRating;
        this.noOfVotes = 1;
        this.priceCategory = priceCategory;
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getFoodCategory() {
        return foodCategory;
    }

    public PriceCategory getPriceCategory() {
        return priceCategory;
    }

    public synchronized Map<String, Integer> getSales() {
        return Collections.unmodifiableMap(sales);
    }

    public synchronized void addSale(String productName, int quantity) {
        sales.merge(productName, quantity, Integer::sum);
    }

    public synchronized void addProduct(String productName, double price) {
        products.put(productName, new Product(productName, price));
    }

    public synchronized void removeProduct(String productName) {
        products.remove(productName);
    }

    public synchronized Map<String, Product> getProducts() {
        return Collections.unmodifiableMap(products);
    }

    public synchronized void addRating(int rating) {
        averageRating = (averageRating * noOfVotes + rating) / (++noOfVotes);
    }

    public synchronized int getNoOfVotes() {
        return noOfVotes;
    }

    public synchronized double getAverageRating() {
        return averageRating;
    }

    @Override
    public String toString() {
        return "Restaurant{" +
                "name='" + name + '\'' +
                ", lat=" + latitude +
                ", lon=" + longitude +
                ", category='" + foodCategory + '\'' +
                ", avgRating=" + averageRating +
                ", priceCategory=" + priceCategory +
                ", products=" + products.keySet() +
                '}';
    }
}