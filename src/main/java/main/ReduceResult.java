package main;

import java.util.*;

/**
 * Resultado final tras Reduce: restaurantes y ventas agregadas (incluye "total").
 */
public class ReduceResult {
    private final List<Restaurant> restaurants;
    private final Map<String, Integer> ventasPorKey;

    public ReduceResult(List<Restaurant> restaurants, Map<String, Integer> ventasPorKey) {
        this.restaurants = new ArrayList<>(restaurants);
        Map<String, Integer> copy = new HashMap<>(ventasPorKey);
        if (!ventasPorKey.isEmpty()) {
            int total = ventasPorKey.values().stream().mapToInt(Integer::intValue).sum();
            copy.put("total", total);
        }
        this.ventasPorKey = Collections.unmodifiableMap(copy);
    }

    public List<Restaurant> getRestaurants() {
        return Collections.unmodifiableList(restaurants);
    }
    public Map<String, Integer> getVentasPorKey() {
        return ventasPorKey;
    }
}

