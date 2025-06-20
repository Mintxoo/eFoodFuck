package main;

import java.io.Serializable;
import java.util.Set;


public class FilterSpec implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final double RADIUS_KM = 5.0;

    private final double latitude;
    private final double longitude;
    private final Set<String> foodCategories;
    private final int minStars;
    private final PriceCategory priceCategory;

    public FilterSpec(double latitude,
                      double longitude,
                      Set<String> foodCategories,
                      int minStars,
                      PriceCategory priceCategory) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.foodCategories = foodCategories;
        this.minStars = minStars;
        this.priceCategory = priceCategory;
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public Set<String> getFoodCategories() { return foodCategories; }
    public int getMinStars() { return minStars; }
    public PriceCategory getPriceCategory() { return priceCategory; }
    public double getRadiusKm() { return RADIUS_KM; }

    @Override
    public String toString() {
        return "FilterSpec{" +
                "lat=" + latitude +
                ", lon=" + longitude +
                ", cats=" + foodCategories +
                ", minStars=" + minStars +
                ", priceCat=" + priceCategory +
                ", radiusKm=" + RADIUS_KM +
                '}';
    }
}
