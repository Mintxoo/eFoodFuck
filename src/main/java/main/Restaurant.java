package main;

/**
 * Stub simplificado de Restaurant para testing de MapResult/ReduceTask.
 */
public class Restaurant {
    private final String name;
    private final double latitude, longitude;
    private final String foodCategory;
    private final int stars;
    private final PriceCategory priceCategory;

    public Restaurant(String name, double latitude, double longitude,
                      String foodCategory, int stars, PriceCategory priceCategory) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.foodCategory = foodCategory;
        this.stars = stars;
        this.priceCategory = priceCategory;
    }

    @Override
    public String toString() {
        return "Restaurant{" +
               "name='" + name + '\'' +
               ", lat=" + latitude +
               ", lon=" + longitude +
               ", cat='" + foodCategory + '\'' +
               ", stars=" + stars +
               ", price=" + priceCategory +
               '}';
    }
}
