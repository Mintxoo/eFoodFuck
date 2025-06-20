// src/main/java/main/MapResult.java
package main;

import java.io.Serializable;
import java.util.*;


public class MapResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<Restaurant> restaurants = new ArrayList<>();
    private final Map<String, Integer> ventasPorKey = new HashMap<>();

    public MapResult() {
    }

    public void addRestaurant(Restaurant r) {

        boolean exists = restaurants.stream()
                .anyMatch(existing -> existing.getName().equals(r.getName()));
        if (!exists) {
            restaurants.add(r);
        }
    }


    public List<Restaurant> getRestaurants() {
        return Collections.unmodifiableList(restaurants);
    }

    public void addVenta(String key, int qty) {
        ventasPorKey.merge(key, qty, Integer::sum);
    }

    public Map<String, Integer> getVentasPorKey() {
        return Collections.unmodifiableMap(ventasPorKey);
    }


    public void merge(MapResult other) {
        other.restaurants.forEach(this.restaurants::add);
        other.ventasPorKey.forEach(
                (k, v) -> this.ventasPorKey.merge(k, v, Integer::sum)
        );
    }

    @Override
    public String toString() {
        return "MapResult{restaurants=" + restaurants + ", ventas=" + ventasPorKey + '}';
    }
}
