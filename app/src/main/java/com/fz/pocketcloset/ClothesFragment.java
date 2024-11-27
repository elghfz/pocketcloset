package com.fz.pocketcloset;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

public class ClothesFragment extends Fragment {

    private static final String TAG = "ClothesFragment";
    private RecyclerView recyclerView;
    private ClothingAdapter adapter;
    private DatabaseHelper dbHelper;
    private ImagePickerHelper imagePickerHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            View view = inflater.inflate(R.layout.fragment_clothes, container, false);

            // Initialize DatabaseHelper
            dbHelper = new DatabaseHelper(requireContext());

            // Initialize ImagePickerHelper
            imagePickerHelper = new ImagePickerHelper(requireContext(), dbHelper, unused -> loadClothingItems());

            // Set up RecyclerView
            recyclerView = view.findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

            // Add Clothing Button
            view.findViewById(R.id.button_add_clothes).setOnClickListener(v -> imagePickerHelper.openImagePicker());

            // Load Clothing Items
            loadClothingItems();

            return view;
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Load clothing items from the database and display them in the RecyclerView.
     */
    void loadClothingItems() {
        try {
            List<ClothingItem> clothingList = fetchClothingItems();

            adapter = new ClothingAdapter(clothingList, this::showEditClothingDialog, this::deleteClothingItem);

            recyclerView.setAdapter(adapter);
        } catch (Exception e) {
            Log.e(TAG, "Error loading clothing items: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error loading clothing items.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Fetch clothing items from the database.
     */
    private List<ClothingItem> fetchClothingItems() {
        try {
            return new ClothingManager(requireContext()).getAllClothingItems();
        } catch (Exception e) {
            Log.e(TAG, "Error fetching clothing items: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Show a dialog to edit the selected clothing item.
     */
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

    /**
     * Update a clothing item in the database.
     */
    private void updateClothingItem(int id, String tags) {
        try {
            ClothingManager clothingManager = new ClothingManager(requireContext());
            clothingManager.updateClothingItem(id, tags);
        } catch (Exception e) {
            Log.e(TAG, "Error updating clothing item: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error updating clothing item.", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteClothingItem(ClothingItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Delete Clothing Item")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    new ClothingManager(requireContext()).deleteClothingItem(item.getId());
                    loadClothingItems(); // Refresh the list
                    Toast.makeText(requireContext(), "Clothing item deleted!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

}
