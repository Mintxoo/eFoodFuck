package main;

import java.io.Serializable;

public class Rating implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String storeName;
    private final int stars;

    public Rating(String storeName, int stars) {
        this.storeName = storeName;
        this.stars = stars;
    }

    public String getStoreName() { return storeName; }
    public int getStars() { return stars; }

    @Override
    public String toString() {
        return "Rating{" +
                "storeName='" + storeName + '\'' +
                ", stars=" + stars +
                '}';
    }
}
