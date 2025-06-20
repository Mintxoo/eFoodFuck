package main;

import java.io.Serializable;

/**
 * Categoría de precio según precio medio de productos.
 */
public enum PriceCategory implements Serializable {
    ONE_DOLLAR, TWO_DOLLARS, THREE_DOLLARS;

    /**
     * Calcula la categoría de precio en base al precio medio.
     * < 5 → ONE_DOLLAR, <10 → TWO_DOLLARS, ≥10 → THREE_DOLLARS
     */
    public static PriceCategory fromAverage(double avgPrice) {
        if (avgPrice < 5) {
            return ONE_DOLLAR;
        } else if (avgPrice < 15) {
            return TWO_DOLLARS;
        } else {
            return THREE_DOLLARS;
        }
    }
}