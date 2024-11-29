package com.fz.pocketcloset;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollectionDetailFragment extends Fragment {

    private static final String TAG = "CollectionDetailFragment";
    private RecyclerView recyclerView;
    private ClothingAdapter adapter;
    private DatabaseHelper dbHelper;
    private int collectionId;
    private boolean isSelectionMode = false;
    private Set<ClothingItem> selectedItems = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_collection_detail, container, false);

        try {
            // Get collection ID from arguments
            collectionId = requireArguments().getInt("collection_id");

            dbHelper = new DatabaseHelper(requireContext());

            // Set up RecyclerView
            recyclerView = view.findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

            // Load clothes in the collection
            loadClothesInCollection();

            // Set up Add Clothes button
            Button addClothesButton = view.findViewById(R.id.button_add_clothes_to_collection);
            addClothesButton.setOnClickListener(v -> showAddClothesDialog());

        } catch (Exception e) {
            Log.e(TAG, "Error initializing CollectionDetailFragment: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error loading collection details.", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void loadClothesInCollection() {
        try {
            List<ClothingItem> clothesInCollection = new CollectionsManager(requireContext())
                    .getClothesInCollection(collectionId);

            adapter = new ClothingAdapter(
                    clothesInCollection,
                    item -> handleItemClick(item), // Edit the item on click
                    item -> handleItemLongClick(item), // Enter selection mode on long press
                    isSelectionMode // Pass selection mode flag
            );

            recyclerView.setAdapter(adapter);
        } catch (Exception e) {
            Log.e(TAG, "Error loading clothes in collection: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error loading clothes in collection.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddClothesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add Clothes to Collection");

        // Fetch all clothes not already in this collection
        List<ClothingItem> availableClothes = new CollectionsManager(requireContext())
                .getAvailableClothesForCollection(collectionId);

        String[] clothingNames = new String[availableClothes.size()];
        boolean[] checkedItems = new boolean[availableClothes.size()];

        for (int i = 0; i < availableClothes.size(); i++) {
            clothingNames[i] = availableClothes.get(i).getTags();
            checkedItems[i] = false;
        }

        builder.setMultiChoiceItems(clothingNames, checkedItems, (dialog, which, isChecked) -> {
            checkedItems[which] = isChecked;
        });

        builder.setPositiveButton("Add", (dialog, which) -> {
            // Add selected clothes to the collection
            for (int i = 0; i < availableClothes.size(); i++) {
                if (checkedItems[i]) {
                    new CollectionsManager(requireContext())
                            .assignClothingToCollection(availableClothes.get(i).getId(), collectionId);
                }
            }
            loadClothesInCollection(); // Refresh the list
            Toast.makeText(requireContext(), "Clothes added!", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void handleItemClick(ClothingItem item) {
        showEditClothingDialog(item);
    }

    // Handle the item long press to start selection mode
    private void handleItemLongClick(ClothingItem item) {
        isSelectionMode = true;
        selectedItems.add(item);
        adapter.notifyDataSetChanged(); // Notify adapter to update UI
    }

    private void showEditClothingDialog(ClothingItem item) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Edit Clothing Item");

            // Inflate the dialog layout
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_clothes, null);
            EditText inputTags = dialogView.findViewById(R.id.input_tags);

            // Pre-fill the fields with the current item details
            inputTags.setText(item.getTags());

            builder.setView(dialogView);

            builder.setPositiveButton("Save", (dialog, which) -> {
                String newTags = inputTags.getText().toString();

                if (!newTags.isEmpty()) {
                    updateClothingItem(item.getId(), newTags);
                    loadClothesInCollection();
                    Toast.makeText(requireContext(), "Clothing item updated!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Tags cannot be empty.", Toast.LENGTH_SHORT).show();
                }

            });

            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            builder.show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing edit dialog: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error editing clothing item.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateClothingItem(int id, String tags) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("tags", tags);
            db.update("Clothes", values, "id = ?", new String[]{String.valueOf(id)});
            db.close();
        } catch (Exception e) {
            Log.e(TAG, "Error updating clothing item: " + e.getMessage(), e);
        }
    }

    // Call this method to delete selected items
    private void deleteSelectedItems() {
        for (ClothingItem item : selectedItems) {
            // Perform delete operation (delete from DB, remove from list)
            new ClothingManager(requireContext()).deleteClothingItem(item.getId());
        }

        // Refresh the list after deletion
        loadClothesInCollection();
        Toast.makeText(requireContext(), "Items deleted!", Toast.LENGTH_SHORT).show();
    }
    private void deleteClothingItem(ClothingItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Delete Clothing Item")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    try {
                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                        db.delete("Clothes", "id = ?", new String[]{String.valueOf(item.getId())});
                        db.close();
                        loadClothesInCollection(); // Refresh the list
                        Toast.makeText(requireContext(), "Clothing item deleted!", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error deleting clothing item: " + e.getMessage(), e);
                        Toast.makeText(requireContext(), "Error deleting clothing item.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
