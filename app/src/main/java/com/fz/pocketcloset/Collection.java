package com.fz.pocketcloset;

public class Collection {
    private final int id;
    private final String name;

    private String emoji;

    public Collection(int id, String name) {
        this.id = id;
        this.name = name;
        this.emoji = "📁";
    }

    public Collection(int id, String name, String emoji) {
        this.id = id;
        this.name = name;
        this.emoji = emoji;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }
}
