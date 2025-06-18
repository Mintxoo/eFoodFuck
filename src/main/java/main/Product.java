package main;

import java.io.Serializable;

/**
 * Producto con nombre y precio.
 */
public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final double price;

    public Product(String name, double price) {
        this.name = name;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return "Product{name='" + name + "', price=" + price + '}';
    }
}