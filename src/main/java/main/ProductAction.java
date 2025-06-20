package main;

import java.io.Serializable;

public class ProductAction implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String storeName;
    private final String productName;
    private final Double price;

    public ProductAction(String storeName, String productName, Double price) {
        this.storeName = storeName;
        this.productName = productName;
        this.price = price;
    }

    public String getStoreName() { return storeName; }
    public String getProductName() { return productName; }
    public Double getPrice() { return price; }

    @Override
    public String toString() {
        return "ProductAction{" +
                "storeName='" + storeName + '\'' +
                ", productName='" + productName + '\'' +
                ", price=" + price +
                '}';
    }
}