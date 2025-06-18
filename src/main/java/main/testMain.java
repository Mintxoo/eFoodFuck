package main;

import java.util.*;

public class TestMain {
    public static void main(String[] args) {
        System.out.println("=== Testing FilterSpec ===");
        FilterSpec spec = new FilterSpec(
            40.0, 23.0,
            new HashSet<>(Arrays.asList("pizza", "sushi")),
            4,
            PriceCategory.TWO_DOLLARS
        );
        System.out.println(spec);

        System.out.println("\n=== Testing MapResult ===");
        MapResult mr1 = new MapResult();
        Restaurant r1 = new Restaurant(
            "PizzaFun", 40.01, 23.01,
            "pizza", 5, PriceCategory.TWO_DOLLARS
        );
        mr1.addRestaurant(r1);
        mr1.addVenta("PizzaFun", 10);
        System.out.println("mr1: restaurants=" + mr1.getRestaurants()
                           + ", ventas=" + mr1.getVentasPorKey());

        MapResult mr2 = new MapResult();
        Restaurant r2 = new Restaurant(
            "SushiPlace", 39.99, 22.99,
            "sushi", 4, PriceCategory.THREE_DOLLARS
        );
        mr2.addRestaurant(r2);
        mr2.addVenta("SushiPlace", 5);
        System.out.println("mr2: restaurants=" + mr2.getRestaurants()
                           + ", ventas=" + mr2.getVentasPorKey());

        System.out.println("\n=== Testing merge() ===");
        mr1.merge(mr2);
        System.out.println("merged mr1: restaurants=" + mr1.getRestaurants()
                           + ", ventas=" + mr1.getVentasPorKey());

        System.out.println("\n=== Testing ReduceTask ===");
        ReduceResult rr = new ReduceResult(
            Arrays.asList(r1, r2),
            Map.of("PizzaFun", 10, "SushiPlace", 5)
        );
        System.out.println("reduce result restaurants=" + rr.getRestaurants());
        System.out.println("reduce result ventas=" + rr.getVentasPorKey());
    }
}
