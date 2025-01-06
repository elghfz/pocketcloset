package com.fz.pocketcloset;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class Collection implements SelectableItem {
    private final int id;
    private final String name;
    private String emoji;
    private boolean isSelected; // Selection state
    private ArrayList<Uri> previewImages; // List of URIs for preview

    public Collection(int id, String name) {
        this.id = id;
        this.name = name;
        this.emoji = "📁"; // Default emoji
        this.isSelected = false; // Default selection state
        this.previewImages = new ArrayList<>(); // Default empty preview images
    }

    public Collection(int id, String name, String emoji) {
        this.id = id;
        this.name = name;
        this.emoji = emoji;
        this.isSelected = false; // Default selection state
        this.previewImages = new ArrayList<>(); // Default empty preview images
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

    public void setPreviewImages(List<Uri> images) {
        if (images != null) {
            this.previewImages = new ArrayList<>(images);
        } else {
            this.previewImages = new ArrayList<>();
        }
    }

    /**
     * Returns a list of URIs for preview.
     * If there are fewer than 4 images, placeholders are added to fill the list.
     */
    public List<Uri> getPreviewImages() {
        List<Uri> result = new ArrayList<>();

        // Add existing preview images
        if (previewImages != null) {
            result.addAll(previewImages);
        }

        // Add placeholders if fewer than 4 images
        int placeholderCount = Math.max(0, 4 - result.size());
        for (int i = 0; i < placeholderCount; i++) {
            result.add(Uri.parse("android.resource://com.fz.pocketcloset/drawable/placeholder_image"));
        }

        // Return exactly 4 items
        return result.subList(0, 4);
    }


    @Override
    public String getDisplayName() {
        return name; // The display name is the collection's name
    }

    @Override
    public boolean isSelected() {
        return isSelected;
    }

    @Override
    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }

    // Parcelable implementation
    protected Collection(Parcel in) {
        id = in.readInt();
        name = in.readString();
        emoji = in.readString();
        previewImages = in.createTypedArrayList(Uri.CREATOR);
    }

    public static final Creator<Collection> CREATOR = new Creator<Collection>() {
        @Override
        public Collection createFromParcel(Parcel in) {
            return new Collection(in);
        }

        @Override
        public Collection[] newArray(int size) {
            return new Collection[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(emoji);
        dest.writeTypedList(previewImages);
    }
}
