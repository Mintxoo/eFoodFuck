package main;

import java.io.Serializable;
import java.util.Map;


public class Sale implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String storeName;
    private final Map<String, Integer> items;

    public Sale(String storeName, Map<String, Integer> items) {
        this.storeName = storeName;
        this.items = items;
    }

    public String getStoreName() {
        return storeName;
    }

    public Map<String, Integer> getItems() {
        return items;
    }

    @Override
    public String toString() {
        return "Sale{store='" + storeName + "', items=" + items + '}';
    }
}
