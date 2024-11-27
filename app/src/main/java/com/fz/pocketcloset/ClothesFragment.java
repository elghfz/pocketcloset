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
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ClothesFragment extends Fragment {

    private static final String TAG = "ClothesFragment";
    private RecyclerView recyclerView;
    private ClothingAdapter adapter;
    private DatabaseHelper dbHelper;
    private ImagePickerHelper imagePickerHelper;
    private Parcelable recyclerViewState;

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

            // Set up Add Clothing button
            view.findViewById(R.id.button_add_clothes).setOnClickListener(v -> {
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

            adapter = new ClothingAdapter(
                    clothingList,
                    item -> showEditClothingDialog(item),
                    item -> deleteClothingItem(item)
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
