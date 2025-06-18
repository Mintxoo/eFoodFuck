package main;

import java.util.*;

public class ReduceTask {

    public ReduceResult combine(List<MapResult> partials) {
        List<Restaurant> all = new ArrayList<>();
        Map<String,Integer> ventas = new HashMap<>();

        for (MapResult mr : partials) {
            all.addAll(mr.getRestaurants());
            mr.getVentasPorKey().forEach(
                (k,v) -> ventas.merge(k, v, Integer::sum)
            );
        }
        return new ReduceResult(all, ventas);
    }
}
