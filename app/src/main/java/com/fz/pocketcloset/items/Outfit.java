package com.fz.pocketcloset.items;

public class Outfit {
    private int id;
    private String name;
    private String combinedImagePath;

    // Constructor
    public Outfit(int id, String name, String combinedImagePath) {
        this.id = id;
        this.name = name;
        this.combinedImagePath = combinedImagePath;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCombinedImagePath() {
        return combinedImagePath;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCombinedImagePath(String combinedImagePath) {
        this.combinedImagePath = combinedImagePath;
    }
}
