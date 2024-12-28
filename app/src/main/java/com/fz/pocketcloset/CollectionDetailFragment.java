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
    private Button renameButton, addClothesButton, removeFromCollectionButton;
    private boolean isSelectionMode = false;
    private final Set<ClothingItem> selectedItems = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_collection_detail, container, false);

        try {
            collectionId = requireArguments().getInt("collection_id");
            dbHelper = new DatabaseHelper(requireContext());

            recyclerView = view.findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

            renameButton = view.findViewById(R.id.button_rename_collection);
            addClothesButton = view.findViewById(R.id.button_add_clothes_to_collection);
            removeFromCollectionButton = view.findViewById(R.id.removeFromCollectionButton);

            renameButton.setOnClickListener(v -> showRenameDialog());
            addClothesButton.setOnClickListener(v -> showAddClothesDialog());
            removeFromCollectionButton.setOnClickListener(v -> removeSelectedFromCollection());

            Button closeButton = view.findViewById(R.id.button_close_collection);
            closeButton.setOnClickListener(v -> closeCollection());

            updateButtonVisibility();
            loadClothesInCollection();

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
                    this::handleItemClick,
                    this::handleItemLongClick,
                    isSelectionMode,
                    true, collectionId,
                    null
            );

            recyclerView.setAdapter(adapter);
        } catch (Exception e) {
            Log.e(TAG, "Error loading clothes in collection: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error loading clothes in collection.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleItemClick(ClothingItem item) {
        if (isSelectionMode) {
            toggleItemSelection(item);
        } else {
            showEditClothingDialog(item);
        }
    }

    private void handleItemLongClick(ClothingItem item) {
        if (item == null) {
            exitSelectionMode(); // Exit when fragment is notified by adapter
            return;
        }
        if (!isSelectionMode) {
            isSelectionMode = true;
            updateButtonVisibility(); // Show Remove button
        }
        toggleItemSelection(item);
    }


    private void toggleItemSelection(ClothingItem item) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item);
        } else {
            selectedItems.add(item);
        }

        // Exit selection mode if no items are selected
        if (selectedItems.isEmpty()) {
            exitSelectionMode();
        }

        adapter.notifyDataSetChanged();
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        selectedItems.clear();
        adapter.setSelectionMode(false);
        updateButtonVisibility();
    }

    private void updateButtonVisibility() {
        if (isSelectionMode) {
            renameButton.setVisibility(View.GONE);
            addClothesButton.setVisibility(View.GONE);
            removeFromCollectionButton.setVisibility(View.VISIBLE);
        } else {
            renameButton.setVisibility(View.VISIBLE);
            addClothesButton.setVisibility(View.VISIBLE);
            removeFromCollectionButton.setVisibility(View.GONE);
        }
    }

    private void removeSelectedFromCollection() {
        for (ClothingItem item : selectedItems) {
            new CollectionsManager(requireContext())
                    .removeClothingFromCollection(item.getId(), collectionId);
        }
        Toast.makeText(requireContext(), "Items removed from collection!", Toast.LENGTH_SHORT).show();
        exitSelectionMode();
        loadClothesInCollection();
    }

    private void showRenameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Rename Collection");

        final EditText input = new EditText(requireContext());
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                renameCollection(newName);
            } else {
                Toast.makeText(requireContext(), "Name cannot be empty!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void renameCollection(String newName) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("name", newName);
            db.update("Collections", values, "id = ?", new String[]{String.valueOf(collectionId)});
            db.close();
            Toast.makeText(requireContext(), "Collection renamed!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error renaming collection: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error renaming collection.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddClothesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add Clothes to Collection");

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
            for (int i = 0; i < availableClothes.size(); i++) {
                if (checkedItems[i]) {
                    new CollectionsManager(requireContext())
                            .assignClothingToCollection(availableClothes.get(i).getId(), collectionId);
                }
            }
            loadClothesInCollection();
            Toast.makeText(requireContext(), "Clothes added!", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showEditClothingDialog(ClothingItem item) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Edit Clothing Item");

            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_clothes, null);
            EditText inputTags = dialogView.findViewById(R.id.input_tags);
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
            Toast.makeText(requireContext(), "Clothing item updated!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error updating clothing item: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error updating clothing item.", Toast.LENGTH_SHORT).show();
        }
    }

    private void closeCollection() {
        // Call the MainActivity's closeCollectionDetail method
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).closeCollectionDetail();
        }
    }
}
