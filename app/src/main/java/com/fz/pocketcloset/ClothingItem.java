package com.fz.pocketcloset;

import android.net.Uri;
import android.os.Parcel;

public class ClothingItem implements SelectableItem {
    private final int id;
    private final String imagePath;
    private final String tags;
    private boolean isSelected; // For selection state

    public ClothingItem(int id, String imagePath, String tags) {
        this.id = id;
        this.imagePath = imagePath;
        this.tags = tags;
        this.isSelected = false; // Default selection state
    }

    public int getId() {
        return id;
    }

    public String getImagePath() {
        return imagePath;
    }

    public Uri getImageUri() {
        return imagePath != null ? Uri.parse(imagePath) : null;
    }

    public String getTags() {
        return tags;
    }

    @Override
    public String getDisplayName() {
        return ""; // Clothing items may not have a display name; return empty string or customize.
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
    protected ClothingItem(Parcel in) {
        id = in.readInt();
        imagePath = in.readString();
        tags = in.readString();
    }

    public static final Creator<ClothingItem> CREATOR = new Creator<ClothingItem>() {
        @Override
        public ClothingItem createFromParcel(Parcel in) {
            return new ClothingItem(in);
        }

        @Override
        public ClothingItem[] newArray(int size) {
            return new ClothingItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(imagePath);
        dest.writeString(tags);
    }
}

