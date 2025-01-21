package com.fz.pocketcloset.items;

import android.os.Parcel;
import android.os.Parcelable;

public class SelectableTag implements SelectableItem {

    private String tag;
    private boolean isSelected;

    // Constructor to create SelectableTag from a String tag
    public SelectableTag(String tag) {
        this.tag = tag;
        this.isSelected = false; // Default selection state
    }

    // Constructor for Parcelable
    protected SelectableTag(Parcel in) {
        tag = in.readString();
        isSelected = in.readByte() != 0;  // Read selected state
    }

    @Override
    public String getDisplayName() {
        return tag;
    }

    @Override
    public boolean isSelected() {
        return isSelected;
    }

    @Override
    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(tag);  // Write tag to Parcel
        dest.writeByte((byte) (isSelected ? 1 : 0));  // Write selection state to Parcel
    }

    @Override
    public int describeContents() {
        return 0;  // No special objects, so we return 0
    }

    // Parcelable.Creator implementation
    public static final Creator<SelectableTag> CREATOR = new Creator<SelectableTag>() {
        @Override
        public SelectableTag createFromParcel(Parcel in) {
            return new SelectableTag(in);
        }

        @Override
        public SelectableTag[] newArray(int size) {
            return new SelectableTag[size];
        }
    };
}
