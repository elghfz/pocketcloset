package com.fz.pocketcloset;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SelectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<SelectableItem> items;
    private final boolean[] selectedItems; // Tracks selection state

    public SelectionAdapter(List<SelectableItem> items, boolean[] selectedItems) {
        this.items = items;
        this.selectedItems = selectedItems;
    }

    public void clearSelections() {
        for (int i = 0; i < selectedItems.length; i++) {
            selectedItems[i] = false; // Reset the selection state
        }
        notifyDataSetChanged(); // Refresh the UI
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof Collection ? 1 : 0; // 1 for Collection, 0 for Clothing
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == 1) { // Collection
            View view = inflater.inflate(R.layout.item_collection, parent, false);
            return new CollectionViewHolder(view);
        } else { // Clothing
            View view = inflater.inflate(R.layout.item_clothing, parent, false);
            return new ClothingViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SelectableItem item = items.get(position);
        boolean isSelected = selectedItems[position];

        if (holder instanceof ClothingViewHolder) {
            ((ClothingViewHolder) holder).bind((ClothingItem) item, isSelected);
        } else if (holder instanceof CollectionViewHolder) {
            ((CollectionViewHolder) holder).bind((Collection) item, isSelected);
        }

        // Unified click listener for parent item
        holder.itemView.setOnClickListener(v -> {
            selectedItems[position] = !selectedItems[position];
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class CollectionViewHolder extends RecyclerView.ViewHolder {
        TextView collectionName;
        CheckBox collectionCheckbox;
        ImageView[] previewImages = new ImageView[4];

        public CollectionViewHolder(@NonNull View itemView) {
            super(itemView);
            collectionName = itemView.findViewById(R.id.collectionName);
            collectionCheckbox = itemView.findViewById(R.id.collectionCheckbox);
            previewImages[0] = itemView.findViewById(R.id.previewItem1);
            previewImages[1] = itemView.findViewById(R.id.previewItem2);
            previewImages[2] = itemView.findViewById(R.id.previewItem3);
            previewImages[3] = itemView.findViewById(R.id.previewItem4);

            // Listener for checkbox clicks
            collectionCheckbox.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    selectedItems[position] = collectionCheckbox.isChecked();
                }
            });
        }

        public void bind(Collection collection, boolean isSelected) {
            collectionName.setText(collection.getName());
            collectionCheckbox.setChecked(isSelected);
            collectionCheckbox.setVisibility(View.VISIBLE);

            // Load preview images for collections
            List<ClothingItem> previewItems = new CollectionsManager(itemView.getContext())
                    .getClothesInCollection(collection.getId());

            for (int i = 0; i < previewImages.length; i++) {
                if (i < previewItems.size()) {
                    ClothingItem item = previewItems.get(i);
                    previewImages[i].setImageURI(Uri.parse(item.getImagePath()));
                    previewImages[i].setVisibility(View.VISIBLE);
                } else {
                    previewImages[i].setImageResource(R.drawable.placeholder_clothing_item); // Default placeholder
                    previewImages[i].setVisibility(View.VISIBLE);
                }
            }
        }
    }

    class ClothingViewHolder extends RecyclerView.ViewHolder {
        ImageView clothingImage;
        CheckBox clothingCheckbox;

        public ClothingViewHolder(@NonNull View itemView) {
            super(itemView);
            clothingImage = itemView.findViewById(R.id.imageViewClothing);
            clothingCheckbox = itemView.findViewById(R.id.checkboxClothing);

            // Listener for checkbox clicks
            clothingCheckbox.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    selectedItems[position] = clothingCheckbox.isChecked();
                }
            });
        }

        public void bind(ClothingItem clothingItem, boolean isSelected) {
            if (clothingItem.getImagePath() != null && !clothingItem.getImagePath().isEmpty()) {
                clothingImage.setImageURI(Uri.parse(clothingItem.getImagePath()));
            } else {
                clothingImage.setImageResource(R.drawable.placeholder_clothing_item); // Placeholder
            }
            clothingCheckbox.setChecked(isSelected);
            clothingCheckbox.setVisibility(View.VISIBLE);
        }
    }
}
