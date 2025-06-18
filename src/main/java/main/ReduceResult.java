package main;

import java.io.Serializable;
import java.util.*;

/**
 * Resultado final del Reduce: lista de restaurantes y mapa de ventas agregadas.
 */
public class ReduceResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<Restaurant> restaurants;
    private final Map<String, Integer> ventasPorKey;

    public ReduceResult(List<Restaurant> restaurants, Map<String, Integer> ventasPorKey) {
        this.restaurants = new ArrayList<>(restaurants);
        this.ventasPorKey = new HashMap<>(ventasPorKey);
        // suma total
        int total = this.ventasPorKey.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
        this.ventasPorKey.put("total", total);
    }

    public List<Restaurant> getRestaurants() {
        return Collections.unmodifiableList(restaurants);
    }

    public Map<String, Integer> getVentasPorKey() {
        return Collections.unmodifiableMap(ventasPorKey);
    }

    @Override
    public String toString() {
        return "ReduceResult{restaurants=" + restaurants + ", ventasPorKey=" + ventasPorKey + '}';
    }
}
