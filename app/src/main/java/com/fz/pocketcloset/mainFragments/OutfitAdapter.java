package com.fz.pocketcloset.mainFragments;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fz.pocketcloset.R;
import com.fz.pocketcloset.items.Outfit;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OutfitAdapter extends RecyclerView.Adapter<OutfitAdapter.OutfitViewHolder> {

    private final List<Outfit> outfits;
    private final Set<Outfit> selectedItems = new HashSet<>();

    private final OnItemClickListener clickListener;
    private final OnItemLongClickListener longClickListener;

    public interface OnItemClickListener {
        void onItemClick(Outfit outfit);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(Outfit outfit);
    }

    public OutfitAdapter(List<Outfit> outfits, OnItemClickListener clickListener, OnItemLongClickListener longClickListener) {
        this.outfits = outfits;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public OutfitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_outfit, parent, false);
        return new OutfitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OutfitViewHolder holder, int position) {
        Outfit outfit = outfits.get(position);
        holder.bind(outfit, clickListener, longClickListener, selectedItems);
    }

    @Override
    public int getItemCount() {
        return outfits.size();
    }

    public void updateData(List<Outfit> newOutfits) {
        outfits.clear();
        outfits.addAll(newOutfits);
        notifyDataSetChanged();
    }

    public void toggleItem(Outfit outfit) {
        if (selectedItems.contains(outfit)) {
            selectedItems.remove(outfit);
        } else {
            selectedItems.add(outfit);
        }
        notifyDataSetChanged(); // Refresh the UI
    }

    public Set<Outfit> getSelectedItems() {
        return selectedItems;
    }

    static class OutfitViewHolder extends RecyclerView.ViewHolder {
        private final ImageView outfitImage;
        private final CheckBox checkBoxOutfit;

        OutfitViewHolder(@NonNull View itemView) {
            super(itemView);
            outfitImage = itemView.findViewById(R.id.imageViewOutfit);
            checkBoxOutfit = itemView.findViewById(R.id.checkboxOutfit);
        }

        void bind(Outfit outfit, OnItemClickListener clickListener, OnItemLongClickListener longClickListener, Set<Outfit> selectedItems) {
            // Load outfit image
            if (outfit.getCombinedImagePath() != null) {
                outfitImage.setImageURI(Uri.parse(outfit.getCombinedImagePath()));
            } else {
                outfitImage.setImageResource(R.drawable.placeholder_clothing_item);
            }

            // Update checkbox visibility and state based on selection
            checkBoxOutfit.setVisibility(selectedItems.isEmpty() ? View.GONE : View.VISIBLE);
            checkBoxOutfit.setChecked(selectedItems.contains(outfit));

            // Handle checkbox changes
            checkBoxOutfit.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedItems.add(outfit); // Add the item to selected items
                } else {
                    selectedItems.remove(outfit); // Remove the item from selected items

                    // Notify fragment to exit selection mode if no items are selected
                    if (selectedItems.isEmpty()) {
                        longClickListener.onItemLongClick(null); // Exit selection mode
                    }
                }
            });

            // Handle item click
            itemView.setOnClickListener(v -> {
                if (selectedItems.isEmpty()) {
                    clickListener.onItemClick(outfit); // Normal click behavior
                } else {
                    if (selectedItems.contains(outfit)) {
                        selectedItems.remove(outfit);
                        checkBoxOutfit.setChecked(false);
                    } else {
                        selectedItems.add(outfit);
                        checkBoxOutfit.setChecked(true);
                    }

                    // Exit selection mode if no items are selected
                    if (selectedItems.isEmpty()) {
                        longClickListener.onItemLongClick(null);
                    }
                }
            });

            // Handle long click
            itemView.setOnLongClickListener(v -> {
                selectedItems.add(outfit); // Add the long-clicked item
                checkBoxOutfit.setChecked(true); // Check the checkbox
                longClickListener.onItemLongClick(outfit); // Notify fragment
                return true;
            });
        }

    }
}
