package com.fz.pocketcloset;

import android.app.AlertDialog;
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
    private ClothingManager clothingManager;
    private Parcelable recyclerViewState;
    private boolean isSelectionMode = false;
    private final Set<ClothingItem> selectedItems = new HashSet<>();
    private Button deleteButton, addToCollectionButton, addClothesButton;
    private ImagePickerHelper imagePickerHelper;
    private DatabaseHelper dbHelper;


    @Override
    public void onPause() {
        super.onPause();
        if (recyclerView != null && recyclerView.getLayoutManager() != null) {
            recyclerViewState = recyclerView.getLayoutManager().onSaveInstanceState();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadClothingItems();
        if (recyclerViewState != null && recyclerView.getLayoutManager() != null) {
            recyclerView.post(() -> recyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_clothes, container, false);

        try {
            clothingManager = new ClothingManager(requireContext());

            recyclerView = view.findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            dbHelper = new DatabaseHelper(requireContext());

            imagePickerHelper = new ImagePickerHelper(
                    this, // Pass the current fragment
                    dbHelper,
                    unused -> loadClothingItems() // Callback to refresh the list after adding
            );

            deleteButton = view.findViewById(R.id.deleteButton);
            addToCollectionButton = view.findViewById(R.id.addToCollectionButton);
            addClothesButton = view.findViewById(R.id.button_add_clothes);

            deleteButton.setOnClickListener(v -> deleteSelectedItems());
            addToCollectionButton.setOnClickListener(v -> showCollectionSelectionDialog());

            addClothesButton.setOnClickListener(v -> showAddClothesDialog());

            updateButtonVisibility();
            loadClothingItems();

        } catch (Exception e) {
            Log.e(TAG, "Error initializing ClothesFragment: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error initializing ClothesFragment.", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void loadClothingItems() {
        try {
            List<ClothingItem> clothingList = clothingManager.getAllClothingItems();

            adapter = new ClothingAdapter(
                    clothingList,
                    this::handleItemClick,
                    this::handleItemLongClick,
                    isSelectionMode,
                    false, // showRemoveFromCollectionButton is false for this context
                    -1, // No specific collection
                    clothingItemId -> {
                        // No "Remove from Collection" functionality here
                    }
            );

            recyclerView.setAdapter(adapter);
        } catch (Exception e) {
            Log.e(TAG, "Error loading clothing items: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error loading clothing items.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleItemClick(ClothingItem item) {
        if (isSelectionMode) {
            toggleItemSelection(item);
        } else {
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.openClothingDetail(item.getId());
            }
        }
    }




    private void handleItemLongClick(ClothingItem item) {
        if (item == null) {
            exitSelectionMode();
            return;
        }
        if (!isSelectionMode) {
            isSelectionMode = true;
            updateButtonVisibility();
        }
        toggleItemSelection(item);
    }

    private void toggleItemSelection(ClothingItem item) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item);
        } else {
            selectedItems.add(item);
        }

        // Check if all items are deselected
        if (selectedItems.isEmpty() && isSelectionMode) {
            exitSelectionMode();
            Log.d(TAG, "Exiting selection mode as no items are selected.");
        }

        adapter.notifyDataSetChanged();
    }


    private void exitSelectionMode() {
        isSelectionMode = false;
        selectedItems.clear();
        adapter.setSelectionMode(false);
        updateButtonVisibility();
    }

    private void deleteSelectedItems() {
        try {
            List<Integer> idsToDelete = new ArrayList<>();
            for (ClothingItem item : selectedItems) {
                idsToDelete.add(item.getId());
            }
            clothingManager.deleteMultipleItems(idsToDelete);
            Toast.makeText(requireContext(), "Items deleted!", Toast.LENGTH_SHORT).show();
            exitSelectionMode();
            loadClothingItems();
        } catch (Exception e) {
            Log.e(TAG, "Error deleting selected items: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error deleting items.", Toast.LENGTH_SHORT).show();
        }
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
                    clothingManager.updateClothingItem(item.getId(), newTags);
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

    private void showAddClothesDialog() {
        try {
            // Use the ImagePickerHelper to open the image picker
            if (imagePickerHelper != null) {
                imagePickerHelper.openImagePicker();
            } else {
                Log.e(TAG, "ImagePickerHelper is null. Cannot open image picker.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing Add Clothes dialog: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error adding clothing item.", Toast.LENGTH_SHORT).show();
        }
    }



    private void updateButtonVisibility() {
        if (isSelectionMode) {
            deleteButton.setVisibility(View.VISIBLE);
            addToCollectionButton.setVisibility(View.VISIBLE);
            addClothesButton.setVisibility(View.GONE);
        } else {
            deleteButton.setVisibility(View.GONE);
            addToCollectionButton.setVisibility(View.GONE);
            addClothesButton.setVisibility(View.VISIBLE);
        }
    }
}

