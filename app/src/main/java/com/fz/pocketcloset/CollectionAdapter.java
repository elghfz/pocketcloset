package com.fz.pocketcloset;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollectionAdapter extends RecyclerView.Adapter<CollectionAdapter.CollectionViewHolder> {

    private final List<Collection> collectionList;
    private final OnItemClickListener onItemClickListener;
    private final OnItemLongClickListener onItemLongClickListener;
    private boolean selectionMode;
    private final Set<Collection> selectedItems = new HashSet<>();

    public interface OnItemClickListener {
        void onItemClick(Collection collection);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(Collection collection);
    }

    public CollectionAdapter(
            List<Collection> collectionList,
            OnItemClickListener onItemClickListener,
            OnItemLongClickListener onItemLongClickListener,
            boolean selectionMode
    ) {
        this.collectionList = collectionList;
        this.onItemClickListener = onItemClickListener;
        this.onItemLongClickListener = onItemLongClickListener;
        this.selectionMode = selectionMode;
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

        holder.collectionNameTextView.setText(collection.getName());

        holder.itemView.setOnClickListener(v -> {
            if (!selectionMode) {
                onItemClickListener.onItemClick(collection);
            } else {
                toggleSelection(collection, holder);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!selectionMode) {
                selectionMode = true;
                setSelectionMode(true);
                toggleSelection(collection, holder);
                onItemLongClickListener.onItemLongClick(collection);
            }
            return true;
        });

        holder.selectCheckbox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
        holder.selectCheckbox.setChecked(selectedItems.contains(collection));
        holder.selectCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedItems.add(collection);
            } else {
                selectedItems.remove(collection);
            }

            // Notify fragment to exit selection mode if no items are selected
            if (selectedItems.isEmpty()) {
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

    @Override
    public int getItemCount() {
        return collectionList.size();
    }

    static class CollectionViewHolder extends RecyclerView.ViewHolder {
        TextView collectionNameTextView;
        CheckBox selectCheckbox;

        public CollectionViewHolder(@NonNull View itemView) {
            super(itemView);
            collectionNameTextView = itemView.findViewById(R.id.textViewCollectionName);
            selectCheckbox = itemView.findViewById(R.id.checkbox);
        }
    }
}
