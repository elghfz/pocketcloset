package com.fz.pocketcloset;

import android.os.Parcelable;

public interface SelectableItem extends Parcelable {
    // Optionally, you can add methods here that are common to both ClothingItem and CollectionItem.
    String getDisplayName(); // Common display name for the item (can return collection name or clothing name if available).
    boolean isSelected();    // Used to get the selected state of the item.
    void setSelected(boolean selected); // Sets the selected state of the item.
}
