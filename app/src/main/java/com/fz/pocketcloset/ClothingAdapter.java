package com.fz.pocketcloset;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClothingAdapter extends RecyclerView.Adapter<ClothingAdapter.ClothingViewHolder> {

    private final List<ClothingItem> clothingList;
    private final OnItemClickListener onItemClickListener;
    private final OnItemLongClickListener onItemLongClickListener;
    private boolean selectionMode;
    private final Set<ClothingItem> selectedItems = new HashSet<>();

    public interface OnItemClickListener {
        void onItemClick(ClothingItem item);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(ClothingItem item);
    }

    public ClothingAdapter(List<ClothingItem> clothingList,
                           OnItemClickListener onItemClickListener,
                           OnItemLongClickListener onItemLongClickListener,
                           boolean selectionMode) {
        this.clothingList = clothingList;
        this.onItemClickListener = onItemClickListener;
        this.onItemLongClickListener = onItemLongClickListener;
        this.selectionMode = selectionMode;
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

        // Bind the clothing tags
        holder.tagsTextView.setText(item.getTags());

        // Handle item click for editing (replaces Edit button)
        holder.itemView.setOnClickListener(v -> {
            if (!selectionMode) {
                onItemClickListener.onItemClick(item);
            } else {
                toggleSelection(item, holder);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!selectionMode) {
                selectionMode = true;
                setSelectionMode(true);
                toggleSelection(item, holder);
                onItemLongClickListener.onItemLongClick(item); // Notify fragment about selection mode
            }
            return true;
        });

        holder.selectCheckbox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
        holder.selectCheckbox.setChecked(selectedItems.contains(item));
        holder.selectCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedItems.add(item);
            } else {
                selectedItems.remove(item);
            }
        });
        holder.selectCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedItems.add(item);
            } else {
                selectedItems.remove(item);
            }

            // Check if all items are unchecked
            if (selectedItems.isEmpty()) {
                // Notify the fragment to exit selection mode
                if (onItemLongClickListener != null) {
                    onItemLongClickListener.onItemLongClick(null); // Pass null to indicate exiting selection mode
                }
            }
        });
    }

    public void setSelectionMode(boolean selectionMode) {
        this.selectionMode = selectionMode;
        notifyDataSetChanged(); // Notify adapter of selection mode change
    }

    private void toggleSelection(ClothingItem item, ClothingViewHolder holder) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item);
            holder.selectCheckbox.setChecked(false);
        } else {
            selectedItems.add(item);
            holder.selectCheckbox.setChecked(true);
        }
    }


    @Override
    public int getItemCount() {
        return clothingList.size();
    }

    // ViewHolder class for binding views
    static class ClothingViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView tagsTextView;
        CheckBox selectCheckbox; // Added for item selection
        public ClothingViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewClothing);
            tagsTextView = itemView.findViewById(R.id.textViewClothingTags);
            selectCheckbox = itemView.findViewById(R.id.checkboxSelectClothing);
        }
    }

    // Get selected items
    public Set<ClothingItem> getSelectedItems() {
        return selectedItems;
    }
}

