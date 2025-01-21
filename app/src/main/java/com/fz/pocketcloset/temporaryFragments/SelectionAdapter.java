package com.fz.pocketcloset.temporaryFragments;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fz.pocketcloset.R;
import com.fz.pocketcloset.items.ClothingItem;
import com.fz.pocketcloset.items.Collection;
import com.fz.pocketcloset.items.SelectableItem;
import com.fz.pocketcloset.items.SelectableTag;
import com.fz.pocketcloset.mainFragments.CollectionsManager;
import com.google.android.flexbox.FlexboxLayoutManager;

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
        // Check if the item is a Collection or a Tag (SelectableTag)
        if (items.get(position) instanceof Collection) {
            return 1; // 1 for Collection
        } else if (items.get(position) instanceof SelectableTag) {
            return 2; // 2 for SelectableTag
        } else {
            return 0; // 0 for Clothing
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == 1) { // Collection
            View view = inflater.inflate(R.layout.item_collection, parent, false);
            return new CollectionViewHolder(view);
        } else if (viewType == 2) { // SelectableTag
            View view = inflater.inflate(R.layout.item_tag, parent, false);  // Inflate layout for tag items
            return new TagViewHolder(view);
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
        } else if (holder instanceof TagViewHolder) {
            ((TagViewHolder) holder).bind((SelectableTag) item, isSelected);
        }

        // Unified click listener for parent item
        holder.itemView.setOnClickListener(v -> {
            selectedItems[position] = !selectedItems[position];
            notifyItemChanged(position);  // Refresh this item
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // Collection ViewHolder
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

    // Clothing ViewHolder
    class ClothingViewHolder extends RecyclerView.ViewHolder {
        ImageView clothingImage;
        CheckBox clothingCheckbox;

        public ClothingViewHolder(@NonNull View itemView) {
            super(itemView);
            clothingImage = itemView.findViewById(R.id.imageViewClothing);
            clothingCheckbox = itemView.findViewById(R.id.checkboxClothing);

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

    // Tag ViewHolder for SelectableTag
    class TagViewHolder extends RecyclerView.ViewHolder {
        TextView tagTextView;

        public TagViewHolder(@NonNull View itemView) {
            super(itemView);
            tagTextView = itemView.findViewById(R.id.tagTextView);  // Ensure the TextView exists in item_tag.xml
        }

        public void bind(SelectableTag tag, boolean isSelected) {
            tagTextView.setText(tag.getDisplayName());
            if (isSelected) {
                tagTextView.setBackgroundResource(R.drawable.tag_background);
                tagTextView.setTextColor(itemView.getContext().getColor(R.color.tag_selected_text));
                tagTextView.getBackground().setTint(itemView.getContext().getColor(R.color.tag_selected_background));  // Change the background color to a darker shade
            } else {
                // For unselected tag, use the default background and text color
                tagTextView.setBackgroundResource(R.drawable.tag_background);
                tagTextView.setTextColor(itemView.getContext().getColor(android.R.color.white));

                tagTextView.getBackground().setTintList(null);
            }

            // Apply padding and margin to match the styling of selected tags
            tagTextView.setPadding(16, 8, 16, 8);  // Padding for the content inside the tag

            // Use FlexboxLayoutManager.LayoutParams for RecyclerView items
            FlexboxLayoutManager.LayoutParams params = (FlexboxLayoutManager.LayoutParams) tagTextView.getLayoutParams();
            params.setMargins(8, 8, 8, 8);  // Margins for spacing between tags
            tagTextView.setLayoutParams(params);  // Apply the new LayoutParams
        }
    }


}
