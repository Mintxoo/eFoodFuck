package main;

import java.io.Serializable;


public enum PriceCategory implements Serializable {
    ONE_DOLLAR, TWO_DOLLARS, THREE_DOLLARS;


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