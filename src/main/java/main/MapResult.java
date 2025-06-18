// src/main/java/main/MapResult.java
package main;

import java.util.*;

/**
 * Resultado parcial de Map: lista de restaurantes y ventas por clave.
 */
public class MapResult {
    private final List<Restaurant> restaurants = new ArrayList<>();
    private final Map<String, Integer> ventasPorKey = new HashMap<>();

    public void addRestaurant(Restaurant r) {
        restaurants.add(r);
    }

    public void addVenta(String key, int unidades) {
        ventasPorKey.merge(key, unidades, Integer::sum);
    }

    public void merge(MapResult other) {
        this.restaurants.addAll(other.restaurants);
        other.ventasPorKey.forEach(
            (k,v) -> this.ventasPorKey.merge(k, v, Integer::sum)
        );
    }

    public List<Restaurant> getRestaurants() {
        return Collections.unmodifiableList(restaurants);
    }
    public Map<String, Integer> getVentasPorKey() {
        return Collections.unmodifiableMap(ventasPorKey);
    }
}

