package com.fz.pocketcloset.mainFragments;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fz.pocketcloset.items.ClothingItem;
import com.fz.pocketcloset.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClothingAdapter extends RecyclerView.Adapter<ClothingAdapter.ClothingViewHolder> {

    private List<ClothingItem> clothingList;
    private final OnItemClickListener onItemClickListener;
    private final OnItemLongClickListener onItemLongClickListener;
    private final OnRemoveFromCollectionListener onRemoveFromCollectionListener;
    private boolean selectionMode;
    private final Set<ClothingItem> selectedItems = new HashSet<>();
    private final boolean showRemoveFromCollectionButton;
    private final int currentCollectionId;


    public interface OnItemClickListener {
        void onItemClick(ClothingItem item);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(ClothingItem item);
    }

    public interface OnRemoveFromCollectionListener {
        void onRemoveFromCollection(int clothingItemId);
    }

    public ClothingAdapter(List<ClothingItem> clothingList,
                           OnItemClickListener onItemClickListener,
                           OnItemLongClickListener onItemLongClickListener,
                           boolean selectionMode,
                           boolean showRemoveFromCollectionButton,
                           int currentCollectionId,
                           OnRemoveFromCollectionListener onRemoveFromCollectionListener) {
        this.clothingList = clothingList != null ? clothingList : new ArrayList<>();
        this.onItemClickListener = onItemClickListener;
        this.onItemLongClickListener = onItemLongClickListener;
        this.selectionMode = selectionMode;
        this.showRemoveFromCollectionButton = showRemoveFromCollectionButton;
        this.currentCollectionId = currentCollectionId;
        this.onRemoveFromCollectionListener = onRemoveFromCollectionListener;
    }

    @NonNull
    @Override
    public ClothingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_clothing, parent, false);
        return new ClothingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClothingViewHolder holder, int position) {
        ClothingItem item = clothingList.get(position);

        // Bind the clothing image
        holder.imageView.setImageURI(item.getImagePath() != null ? Uri.parse(item.getImagePath()) : null);

        // Synchronize the checkbox with selection state
        boolean isSelected = selectedItems.contains(item);
        holder.selectCheckbox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
        holder.selectCheckbox.setChecked(isSelected);

        // Handle item clicks
        holder.itemView.setOnClickListener(v -> {
            if (selectionMode) {
                toggleSelection(item);
            } else {
                onItemClickListener.onItemClick(item);
            }
        });

        // Handle long clicks to enter selection mode
        holder.itemView.setOnLongClickListener(v -> {
            if (!selectionMode) {
                selectionMode = true;
                notifyDataSetChanged(); // Refresh UI to show checkboxes
            }
            toggleSelection(item);
            onItemLongClickListener.onItemLongClick(item);
            return true;
        });

        // Handle checkbox clicks
        holder.selectCheckbox.setOnClickListener(v -> toggleSelection(item));
    }


    public void updateData(List<ClothingItem> newData) {
        Log.d(TAG, "Updating adapter data. New item count: " + newData.size());
        this.clothingList.clear();
        this.clothingList.addAll(newData);
        notifyDataSetChanged();
    }

    public void clearSelections() {
        if (selectedItems != null) {
            selectedItems.clear();
        }
        new android.os.Handler().post(() -> notifyDataSetChanged());
    }


    @Override
    public int getItemCount() {
        return clothingList != null ? clothingList.size() : 0;
    }

    public void setSelectionMode(boolean selectionMode) {
        this.selectionMode = selectionMode;
    }


    private void toggleSelection(ClothingItem item) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item);
        } else {
            selectedItems.add(item);
        }

        // Log the current selection for debugging
        Log.d(TAG, "toggleSelection - Selected Items: " + selectedItems);

        // Ensure the adapter reflects the updated selection
        notifyDataSetChanged();
    }


    // Retrieve selected items
    public Set<ClothingItem> getSelectedItems() {
        return new HashSet<>(selectedItems); // Return a copy of selected items
    }


    // ViewHolder class for binding views
    static class ClothingViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        CheckBox selectCheckbox;

        public ClothingViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewClothing);
            selectCheckbox = itemView.findViewById(R.id.checkboxClothing);
        }
    }
}
