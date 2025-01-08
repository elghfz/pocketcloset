package com.fz.pocketcloset.mainFragments;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fz.pocketcloset.R;
import com.fz.pocketcloset.items.ClothingItem;
import com.fz.pocketcloset.items.Collection;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollectionAdapter extends RecyclerView.Adapter<CollectionAdapter.CollectionViewHolder> {

    private final List<Collection> collectionList;
    private final OnItemClickListener onItemClickListener;
    private final OnItemLongClickListener onItemLongClickListener;
    private final EmojiClickListener emojiClickListener;
    private boolean selectionMode;
    private final Set<Collection> selectedItems = new HashSet<>();

    public interface OnItemClickListener {
        void onItemClick(Collection collection);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(Collection collection);
    }

    public interface EmojiClickListener {
        void onEmojiClicked(Collection collection);
    }

    public CollectionAdapter(
            List<Collection> collectionList,
            OnItemClickListener onItemClickListener,
            OnItemLongClickListener onItemLongClickListener,
            boolean selectionMode,
            EmojiClickListener emojiClickListener
    ) {
        this.collectionList = collectionList;
        this.onItemClickListener = onItemClickListener;
        this.onItemLongClickListener = onItemLongClickListener;
        this.selectionMode = selectionMode;
        this.emojiClickListener = emojiClickListener;
    }

    public void setSelectionMode(boolean selectionMode) {
        this.selectionMode = selectionMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CollectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_collection, parent, false);
        return new CollectionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CollectionViewHolder holder, int position) {
        Collection collection = collectionList.get(position);
        Log.d(TAG, "Binding collection: " + collection.getName() + " (" + collection.getEmoji() + ")");

        // Set collection name and emoji
        holder.collectionName.setText(collection.getName());
        holder.emojiView.setText(collection.getEmoji());

        // Populate the preview grid
        List<ClothingItem> previewItems = new CollectionsManager(holder.itemView.getContext())
                .getClothesInCollection(collection.getId());

        for (int i = 0; i < holder.previewImages.length; i++) {
            if (i < previewItems.size()) {
                ClothingItem item = previewItems.get(i);
                holder.previewImages[i].setImageURI(Uri.parse(item.getImagePath()));
                holder.previewImages[i].setVisibility(View.VISIBLE);
            } else {
                holder.previewImages[i].setImageResource(R.drawable.placeholder_clothing_item); // Default placeholder
                holder.previewImages[i].setVisibility(View.VISIBLE);
            }
        }

        // Handle emoji click
        holder.emojiView.setOnClickListener(v -> {
            if (emojiClickListener != null) {
                emojiClickListener.onEmojiClicked(collection);
            }
        });

        // Handle item click
        holder.itemView.setOnClickListener(v -> {
            if (!selectionMode) {
                onItemClickListener.onItemClick(collection);
            } else {
                toggleSelection(collection, holder);
            }
        });

        // Handle long click
        holder.itemView.setOnLongClickListener(v -> {
            if (!selectionMode) {
                selectionMode = true;
                setSelectionMode(true);
                toggleSelection(collection, holder);
                onItemLongClickListener.onItemLongClick(collection);
            }
            return true;
        });

        // Handle checkbox visibility and state
        if (selectionMode) {
            holder.selectCheckbox.setVisibility(View.VISIBLE);
            holder.selectCheckbox.setChecked(selectedItems.contains(collection));
        } else {
            holder.selectCheckbox.setVisibility(View.GONE);
            holder.selectCheckbox.setChecked(false);
        }

        holder.selectCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedItems.add(collection);
            } else {
                selectedItems.remove(collection);
            }

            // Exit selection mode if no items are selected
            if (selectedItems.isEmpty() && selectionMode) {
                onItemLongClickListener.onItemLongClick(null);
            }
        });
    }

    private void toggleSelection(Collection collection, CollectionViewHolder holder) {
        if (selectedItems.contains(collection)) {
            selectedItems.remove(collection);
            holder.selectCheckbox.setChecked(false);
        } else {
            selectedItems.add(collection);
            holder.selectCheckbox.setChecked(true);
        }
    }

    public void updateData(List<Collection> newData) {
        this.collectionList.clear();
        this.collectionList.addAll(newData);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return collectionList.size();
    }

    static class CollectionViewHolder extends RecyclerView.ViewHolder {
        TextView collectionName;
        CheckBox selectCheckbox;
        TextView emojiView;
        ImageView[] previewImages = new ImageView[4];

        public CollectionViewHolder(@NonNull View itemView) {
            super(itemView);
            collectionName = itemView.findViewById(R.id.collectionName);
            selectCheckbox = itemView.findViewById(R.id.collectionCheckbox);
            emojiView = itemView.findViewById(R.id.collectionEmoji);
            previewImages[0] = itemView.findViewById(R.id.previewItem1);
            previewImages[1] = itemView.findViewById(R.id.previewItem2);
            previewImages[2] = itemView.findViewById(R.id.previewItem3);
            previewImages[3] = itemView.findViewById(R.id.previewItem4);
        }
    }
}
