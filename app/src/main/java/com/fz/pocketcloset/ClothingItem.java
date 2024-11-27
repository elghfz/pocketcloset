package com.fz.pocketcloset;

public class ClothingItem {
    private final int id;
    private final String imagePath;
    private final String tags;

    public ClothingItem(int id, String imagePath, String tags) {
        this.id = id;
        this.imagePath = imagePath;
        this.tags = tags;
    }

    public int getId() {
        return id;
    }


    public String getImagePath() {
        return imagePath;
    }

    public String getTags() {
        return tags;
    }
}
