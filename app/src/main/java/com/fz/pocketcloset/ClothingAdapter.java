package com.fz.pocketcloset;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ClothingAdapter extends RecyclerView.Adapter<ClothingAdapter.ClothingViewHolder> {

    private final List<ClothingItem> clothingList;
    private final OnEditClickListener onEditClickListener;
    private final OnDeleteClickListener onDeleteClickListener;
    private final OnRemoveFromCollectionListener onRemoveFromCollectionListener;
    private final boolean showRemoveFromCollectionButton;
    private final int currentCollectionId;

    public interface OnEditClickListener {
        void onEditClick(ClothingItem item);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(ClothingItem item);
    }

    public interface OnRemoveFromCollectionListener {
        void onRemoveFromCollection(int clothingItemId);
    }

    public ClothingAdapter(List<ClothingItem> clothingList, OnEditClickListener onEditClickListener, OnDeleteClickListener onDeleteClickListener, boolean showRemoveFromCollectionButton, int currentCollectionId, OnRemoveFromCollectionListener onRemoveFromCollectionListener) {
        this.clothingList = clothingList;
        this.onEditClickListener = onEditClickListener;
        this.onDeleteClickListener = onDeleteClickListener;
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

        // Bind the clothing tags
        holder.tagsTextView.setText(item.getTags());

        // Handle edit button click
        holder.editButton.setOnClickListener(v -> onEditClickListener.onEditClick(item));

        // Handle delete button click
        holder.deleteButton.setOnClickListener(v -> onDeleteClickListener.onDeleteClick(item));

        // Conditionally show the "Remove from Collection" button
        if (showRemoveFromCollectionButton) {
            holder.removeFromCollectionButton.setVisibility(View.VISIBLE);
            // Remove from collection button click listener (ONLY in the Collection Detail Fragment)
            holder.removeFromCollectionButton.setOnClickListener(v -> {
                if (currentCollectionId != -1) {
                    onRemoveFromCollectionListener.onRemoveFromCollection(item.getId());
                }
            });
        } else {
            holder.removeFromCollectionButton.setVisibility(View.GONE);
        }



    }


    @Override
    public int getItemCount() {
        return clothingList.size();
    }

    static class ClothingViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView tagsTextView;
        Button editButton, deleteButton, removeFromCollectionButton;

        public ClothingViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewClothing);
            tagsTextView = itemView.findViewById(R.id.textViewClothingTags);
            editButton = itemView.findViewById(R.id.buttonEditClothing);
            deleteButton = itemView.findViewById(R.id.buttonDeleteClothing);
            removeFromCollectionButton = itemView.findViewById(R.id.buttonRemoveFromCollection);
        }
    }
}
