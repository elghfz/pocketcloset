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

import java.util.List;

public class OutfitAdapter extends RecyclerView.Adapter<OutfitAdapter.OutfitViewHolder> {

    private final List<Outfit> outfits;
    private boolean isSelectionMode;
    private final OnItemClickListener clickListener;
    private final OnItemLongClickListener longClickListener;

    public interface OnItemClickListener {
        void onItemClick(Outfit outfit);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(Outfit outfit);
    }

    public OutfitAdapter(List<Outfit> outfits, OnItemClickListener clickListener, OnItemLongClickListener longClickListener, boolean isSelectionMode) {
        this.outfits = outfits;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
        this.isSelectionMode = isSelectionMode;
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
        holder.bind(outfit, clickListener, longClickListener);
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

    public void setSelectionMode(boolean isSelectionMode) {
        this.isSelectionMode = isSelectionMode;
        notifyDataSetChanged();
    }

    static class OutfitViewHolder extends RecyclerView.ViewHolder {
        private final ImageView outfitImage;
        private final CheckBox checkBoxOutfit;

        OutfitViewHolder(@NonNull View itemView) {
            super(itemView);
            outfitImage = itemView.findViewById(R.id.imageViewOutfit);
            checkBoxOutfit = itemView.findViewById(R.id.checkboxOutfit);
        }

        void bind(Outfit outfit, OnItemClickListener clickListener, OnItemLongClickListener longClickListener) {
            // Load image from outfit's combinedImagePath
            if (outfit.getCombinedImagePath() != null) {
                outfitImage.setImageURI(Uri.parse(outfit.getCombinedImagePath()));
            } else {
                outfitImage.setImageResource(R.drawable.placeholder_clothing_item); // Fallback image
            }

            itemView.setOnClickListener(v -> clickListener.onItemClick(outfit));
            itemView.setOnLongClickListener(v -> {
                longClickListener.onItemLongClick(outfit);
                return true;
            });
        }
    }
}
