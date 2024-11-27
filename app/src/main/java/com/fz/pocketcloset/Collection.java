package com.fz.pocketcloset;

public class Collection {
    private final int id;
    private final String name;

    public Collection(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
