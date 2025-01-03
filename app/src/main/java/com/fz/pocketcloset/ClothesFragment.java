package com.fz.pocketcloset;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    private ImageButton deleteButton, addToCollectionButton, addClothesButton;
    private ImagePickerHelper imagePickerHelper;
    private DatabaseHelper dbHelper;
    private ImageButton filterButton, clearFilterButton;
    private List<ClothingItem> clothingList; // Original, unfiltered list
    private List<ClothingItem> filteredClothingList; // Filtered list

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

    @SuppressLint("WrongViewCast")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_clothes, container, false);

        try {
            clothingManager = new ClothingManager(requireContext());
            recyclerView = view.findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 4));
            dbHelper = new DatabaseHelper(requireContext());

            imagePickerHelper = new ImagePickerHelper(
                    this,
                    dbHelper,
                    unused -> {
                        clothingList = clothingManager.getAllClothingItems(); // Refresh full list
                        filteredClothingList = new ArrayList<>(clothingList); // Reset filtered list
                        updateAdapterSafely(); // Update adapter
                        updateClearFilterButtonVisibility(Collections.emptySet()); // Reset to no filter
                    }
            );

            deleteButton = view.findViewById(R.id.deleteButton);
            addToCollectionButton = view.findViewById(R.id.addToCollectionButton);
            addClothesButton = view.findViewById(R.id.button_add_clothes);
            filterButton = view.findViewById(R.id.filterButton);
            clearFilterButton = view.findViewById(R.id.clearFilterButton);

            deleteButton.setOnClickListener(v -> deleteSelectedItems());
            addToCollectionButton.setOnClickListener(v -> showCollectionSelectionDialog());
            addClothesButton.setOnClickListener(v -> showAddClothesDialog());
            filterButton.setOnClickListener(v -> showFilterDialog());
            clearFilterButton.setOnClickListener(v -> clearFilter());

            loadClothingItems();
            updateButtonVisibility();

        } catch (Exception e) {
            Log.e(TAG, "Error initializing ClothesFragment: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error initializing ClothesFragment.", Toast.LENGTH_SHORT).show();
        }

        return view;
    }


    private void loadClothingItems() {
        try {
            // Ensure filteredClothingList is initialized
            if (filteredClothingList == null) {
                filteredClothingList = new ArrayList<>();
            }

            adapter = new ClothingAdapter(
                    filteredClothingList,
                    this::handleItemClick,
                    this::handleItemLongClick,
                    false,
                    false,
                    -1,
                    null
            );

            recyclerView.setAdapter(adapter);
            adapter.notifyDataSetChanged();
            updateClearFilterButtonVisibility(Collections.emptySet()); // Reset filter visibility

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

                // Call openClothingDetail with null values for collectionId and collectionName
                mainActivity.openClothingDetail(
                        item.getId(),            // clothingId
                        "ClothesFragment",       // originTag
                        -1,                      // collectionId (not applicable, set to -1)
                        null                     // collectionName (not applicable, set to null)
                );
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

        if (selectedItems.isEmpty() && isSelectionMode) {
            exitSelectionMode();
        }

        recyclerView.post(adapter::notifyDataSetChanged);
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        selectedItems.clear();

        recyclerView.post(() -> {
            adapter.setSelectionMode(false);
            adapter.notifyDataSetChanged();
        });

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

            // Update clothing list and filtered list
            clothingList = clothingManager.getAllClothingItems();
            filteredClothingList = new ArrayList<>(clothingList);

            // Update adapter and reset filter visibility
            recyclerView.post(() -> {
                adapter.updateData(filteredClothingList);
                adapter.notifyDataSetChanged();
                updateClearFilterButtonVisibility(Collections.emptySet()); // Reset filter visibility
            });

        } catch (Exception e) {
            Log.e(TAG, "Error deleting selected items: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error deleting items.", Toast.LENGTH_SHORT).show();
        }
    }


    public void reloadData() {
        clothingList = clothingManager.getAllClothingItems(); // Reload from DB
        filteredClothingList = new ArrayList<>(clothingList); // Reset filtered list
        updateAdapterSafely();
    }


    private void showCollectionSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add to Collection");

        List<Collection> collections = new CollectionsManager(requireContext()).getAllCollections();
        String[] collectionNames = new String[collections.size()];
        boolean[] checkedItems = new boolean[collections.size()];

        for (int i = 0; i < collections.size(); i++) {
            collectionNames[i] = collections.get(i).getName();
            checkedItems[i] = false;
        }

        builder.setSingleChoiceItems(collectionNames, -1, (dialog, which) -> {
            for (int i = 0; i < checkedItems.length; i++) {
                checkedItems[i] = (i == which);
            }
        });
        builder.setPositiveButton("Add", (dialog, which) -> {
            int selectedCollectionId = -1;
            for (int i = 0; i < checkedItems.length; i++) {
                if (checkedItems[i]) {
                    selectedCollectionId = collections.get(i).getId();
                    break;
                }
            }

            if (selectedCollectionId != -1) {
                for (ClothingItem clothingItem : adapter.getSelectedItems()) {
                    new CollectionsManager(requireContext())
                            .assignClothingToCollection(clothingItem.getId(), selectedCollectionId);
                }
                Toast.makeText(requireContext(), "Clothes added to collection!", Toast.LENGTH_SHORT).show();
                exitSelectionMode();
            } else {
                Toast.makeText(requireContext(), "Please select a collection.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showAddClothesDialog() {
        if (imagePickerHelper != null) {
            imagePickerHelper.openImagePicker();
        } else {
            Log.e(TAG, "ImagePickerHelper is null. Cannot open image picker.");
        }
    }

    private void showFilterDialog() {
        try {
            List<String> tags = clothingManager.getAllTags();
            if (tags.isEmpty()) {
                Toast.makeText(requireContext(), "No tags available for filtering.", Toast.LENGTH_SHORT).show();
                return;
            }

            String[] tagArray = tags.toArray(new String[0]);
            boolean[] selectedTags = new boolean[tagArray.length];

            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Filter by Tags")
                    .setMultiChoiceItems(tagArray, selectedTags, (dialog, which, isChecked) -> selectedTags[which] = isChecked)
                    .setPositiveButton("Apply", (dialog, which) -> applyFilter(tagArray, selectedTags))
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing filter dialog: " + e.getMessage(), e);
        }
    }

    private void applyFilter(String[] tags, boolean[] selectedTags) {
        try {
            Set<String> selectedTagSet = new HashSet<>();
            for (int i = 0; i < tags.length; i++) {
                if (selectedTags[i]) {
                    selectedTagSet.add(tags[i].trim()); // Normalize and trim tags
                }
            }

            if (selectedTagSet.isEmpty()) {
                filteredClothingList = new ArrayList<>(clothingList); // No tags selected, show all items
            } else {
                filteredClothingList.clear();
                for (ClothingItem item : clothingList) {
                    if (item.getTags() != null) {
                        Set<String> itemTags = new HashSet<>(Arrays.asList(item.getTags().split(",")));
                        itemTags = normalizeTags(itemTags);

                        // Check if there is any overlap with the selected tags
                        if (!Collections.disjoint(itemTags, selectedTagSet)) {
                            filteredClothingList.add(item);
                        }
                    }
                }
            }

            adapter.updateData(filteredClothingList);
            adapter.notifyDataSetChanged();

            // Hide Add Clothes button while filtering
            updateAddClothesButtonVisibility(false);
            updateClearFilterButtonVisibility(selectedTagSet); // Pass the selected tags

        } catch (Exception e) {
            Log.e(TAG, "Error applying filter: " + e.getMessage(), e);
        }
    }


    private Set<String> normalizeTags(Set<String> tags) {
        Set<String> normalizedTags = new HashSet<>();
        for (String tag : tags) {
            normalizedTags.add(tag.trim().toLowerCase()); // Trim and normalize
        }
        return normalizedTags;
    }



    private void clearFilter() {
        try {
            filteredClothingList = new ArrayList<>(clothingList); // Reset to the full list
            adapter.updateData(filteredClothingList);
            adapter.notifyDataSetChanged();
            // Restore Add Clothes button visibility
            updateAddClothesButtonVisibility(true);
            updateClearFilterButtonVisibility(Collections.emptySet()); // No tags selected
        } catch (Exception e) {
            Log.e(TAG, "Error clearing filter: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Failed to clear filter.", Toast.LENGTH_SHORT).show();
        }
    }


    private void updateAddClothesButtonVisibility(boolean isVisible) {
        addClothesButton.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }


    private void updateAdapterSafely() {
        recyclerView.post(() -> {
            adapter.updateData(filteredClothingList); // Update adapter with new data
            adapter.notifyDataSetChanged(); // Notify adapter to refresh views
        });
    }

    private void updateClearFilterButtonVisibility(Set<String> selectedTagSet) {
        if (selectedTagSet != null && !selectedTagSet.isEmpty()) {
            clearFilterButton.setVisibility(View.VISIBLE); // Show button if filtering is applied
        } else {
            clearFilterButton.setVisibility(View.GONE); // Hide button otherwise
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
