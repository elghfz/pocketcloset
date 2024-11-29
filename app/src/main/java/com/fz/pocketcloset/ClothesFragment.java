package com.fz.pocketcloset;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Parcelable;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClothesFragment extends Fragment {

    private static final String TAG = "ClothesFragment";
    private RecyclerView recyclerView;
    private ClothingAdapter adapter;
    private DatabaseHelper dbHelper;
    private Button deleteButton;
    private Button addToCollectionButton;
    private Button addClothesButton;
    private ImagePickerHelper imagePickerHelper;
    private Parcelable recyclerViewState;
    private boolean isSelectionMode = false;
    private Set<ClothingItem> selectedItems = new HashSet<>();

    @Override
    public void onPause() {
        super.onPause();
        // Save RecyclerView state
        if (recyclerView != null && recyclerView.getLayoutManager() != null) {
            recyclerViewState = recyclerView.getLayoutManager().onSaveInstanceState();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Restore RecyclerView state
        if (recyclerViewState != null && recyclerView.getLayoutManager() != null) {
            recyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);
        }
        loadClothingItems(); // Ensure the list data is refreshed
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_clothes, container, false);

        try {
            // Initialize DatabaseHelper
            dbHelper = new DatabaseHelper(requireContext());

            // Initialize ImagePickerHelper in ClothesFragment
            imagePickerHelper = new ImagePickerHelper(
                    this, // Pass the fragment itself
                    dbHelper,
                    unused -> loadClothingItems() // Callback to refresh clothing items after adding
            );


            // Initialize RecyclerView
            recyclerView = view.findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

            deleteButton = view.findViewById(R.id.deleteButton);
            deleteButton.setOnClickListener(v -> deleteSelectedItems());

            addToCollectionButton = view.findViewById(R.id.addToCollectionButton);
            addToCollectionButton.setOnClickListener(v -> showCollectionSelectionDialog());

            // Set up Add Clothing button
            addClothesButton = view.findViewById(R.id.button_add_clothes);
            addClothesButton.setOnClickListener(v -> {
                if (imagePickerHelper != null) {
                    imagePickerHelper.openImagePicker();
                } else {
                    Log.e(TAG, "ImagePickerHelper is null. Cannot open image picker.");
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error initializing ClothesFragment: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error initializing ClothesFragment.", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void showCollectionSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add to Collection");

        // Fetch all collections
        List<Collection> collections = new CollectionsManager(requireContext()).getAllCollections();

        String[] collectionNames = new String[collections.size()];
        boolean[] checkedItems = new boolean[collections.size()]; // Initially, no collection is selected

        for (int i = 0; i < collections.size(); i++) {
            collectionNames[i] = collections.get(i).getName();
            checkedItems[i] = false;
        }

        builder.setSingleChoiceItems(collectionNames, -1, (dialog, which) -> {
            // Handle single collection selection
            for (int i = 0; i < checkedItems.length; i++) {
                checkedItems[i] = (i == which); // Only one collection can be selected
            }
        });

        builder.setPositiveButton("Add", (dialog, which) -> {
            // Add selected clothes to the chosen collection
            int selectedCollectionId = -1;
            for (int i = 0; i < checkedItems.length; i++) {
                if (checkedItems[i]) {
                    selectedCollectionId = collections.get(i).getId();
                    break; // We only need one selected collection
                }
            }

            if (selectedCollectionId != -1) {
                for (ClothingItem clothingItem : adapter.getSelectedItems()) {
                    new CollectionsManager(requireContext())
                            .assignClothingToCollection(clothingItem.getId(), selectedCollectionId);
                }
                Toast.makeText(requireContext(), "Clothes added to collection!", Toast.LENGTH_SHORT).show();
                exitSelectionMode(); // Exit selection mode after adding
            } else {
                Toast.makeText(requireContext(), "Please select a collection.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadClothingItems(); // Load data after the view is created
    }

    void loadClothingItems() {
        try {
            if (recyclerView == null) {
                Log.e(TAG, "RecyclerView is null. Cannot load clothing items.");
                return;
            }

            Log.d(TAG, "Starting to load clothing items...");
            List<ClothingItem> clothingList = fetchClothingItems();

            // Set adapter with selectionMode flag
            adapter = new ClothingAdapter(
                    clothingList,
                    item -> handleItemClick(item), // Edit the item on click
                    item -> handleItemLongClick(item), // Enter selection mode on long press
                    isSelectionMode // Pass selection mode flag
            );

            recyclerView.setAdapter(adapter);
            Log.d(TAG, "Clothing items loaded successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Error loading clothing items: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error loading clothing items.", Toast.LENGTH_SHORT).show();
        }
    }

    private List<ClothingItem> fetchClothingItems() {
        List<ClothingItem> clothingItems = new ArrayList<>();
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.rawQuery("SELECT * FROM Clothes", null)) {

            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String imagePath = cursor.getString(cursor.getColumnIndexOrThrow("imagePath"));
                String tags = cursor.getString(cursor.getColumnIndexOrThrow("tags"));

                clothingItems.add(new ClothingItem(id, imagePath, tags));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching clothing items: " + e.getMessage(), e);
        }
        return clothingItems;
    }

    // Handle the item click to open the edit dialog
    private void handleItemClick(ClothingItem item) {
        showEditClothingDialog(item);
    }

    // Handle the item long press to start selection mode
    private void handleItemLongClick(ClothingItem item) {
        if (item == null) { // Check if null is passed, indicating exiting selection mode
            exitSelectionMode();
            return;
        }
        isSelectionMode = !isSelectionMode; // Toggle selection mode
        adapter.setSelectionMode(isSelectionMode);
        if (isSelectionMode) {
            selectedItems.add(item);
        } else {
            selectedItems.clear(); // Clear selected items if exiting selection mode
        }
        deleteButton.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
        addToCollectionButton.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
        addClothesButton.setVisibility(isSelectionMode ? View.GONE : View.VISIBLE);
        adapter.notifyDataSetChanged();
    }
    private void exitSelectionMode() {
        isSelectionMode = false;
        adapter.setSelectionMode(false);
        selectedItems.clear();
        adapter.notifyDataSetChanged();
        deleteButton.setVisibility(View.GONE);
        addToCollectionButton.setVisibility(View.GONE);
        addClothesButton.setVisibility(View.VISIBLE);
        // Hide delete button, etc.
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
                    loadClothingItems();
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
        loadClothingItems();
        Toast.makeText(requireContext(), "Items deleted!", Toast.LENGTH_SHORT).show();
        exitSelectionMode();
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
                        loadClothingItems(); // Refresh the list
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
