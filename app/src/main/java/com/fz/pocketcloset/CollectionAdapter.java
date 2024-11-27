package com.fz.pocketcloset;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CollectionAdapter extends RecyclerView.Adapter<CollectionAdapter.CollectionViewHolder> {

    private static final String TAG = "CollectionAdapter";
    private final List<Collection> collectionList;
    private final Context context;
    private final DatabaseHelper dbHelper;

    private final OnEditClickListener onEditClickListener;
    private final OnDeleteClickListener onDeleteClickListener;

    public interface OnEditClickListener {
        void onEditClick(Collection collection);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(Collection collection);
    }

    public CollectionAdapter(
            List<Collection> collectionList,
            Context context,
            DatabaseHelper dbHelper,
            OnEditClickListener onEditClickListener,
            OnDeleteClickListener onDeleteClickListener
    ) {
        this.collectionList = collectionList;
        this.context = context;
        this.dbHelper = dbHelper;
        this.onEditClickListener = onEditClickListener;
        this.onDeleteClickListener = onDeleteClickListener;
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

        // Set the collection name
        holder.collectionNameTextView.setText(collection.getName());

        // Handle the edit button click
        holder.editButton.setOnClickListener(v -> {
            try {
                onEditClickListener.onEditClick(collection);
            } catch (Exception e) {
                Log.e(TAG, "Error handling edit button click: " + e.getMessage(), e);
            }
        });

        // Handle the delete button click
        holder.deleteButton.setOnClickListener(v -> {
            try {
                onDeleteClickListener.onDeleteClick(collection);
            } catch (Exception e) {
                Log.e(TAG, "Error handling delete button click: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public int getItemCount() {
        return collectionList.size();
    }

    /**
     * Handles adding a new collection via dynamically created input.
     */
    public void showAddCollectionDialog(Fragment fragment) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle("Add New Collection");

        // Dynamically create an EditText for user input
        final EditText input = new EditText(context);
        input.setHint("Collection Name");
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString();
            if (!newName.isEmpty()) {
                addCollectionToDatabase(newName);
                if (fragment instanceof CollectionsFragment) {
                    ((CollectionsFragment) fragment).loadCollections();
                }
                Toast.makeText(context, "Collection added!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Name cannot be empty!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void addCollectionToDatabase(String name) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("name", name);
            db.insert("Collections", null, values);
            db.close();
        } catch (Exception e) {
            Log.e(TAG, "Error adding collection to database: " + e.getMessage(), e);
        }
    }

    /**
     * Refreshes the collection list in the RecyclerView.
     */
    private void refreshCollectionsList() {
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            Fragment currentFragment = activity.getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragment instanceof CollectionsFragment) {
                ((CollectionsFragment) currentFragment).loadCollections();
            }
        }
    }

    // ViewHolder class for binding views
    static class CollectionViewHolder extends RecyclerView.ViewHolder {
        TextView collectionNameTextView;
        Button editButton, deleteButton;

        public CollectionViewHolder(@NonNull View itemView) {
            super(itemView);

            collectionNameTextView = itemView.findViewById(R.id.textViewCollectionName);
            editButton = itemView.findViewById(R.id.buttonEditCollection);
            deleteButton = itemView.findViewById(R.id.buttonDeleteCollection);
        }
    }
}
