package com.fz.pocketcloset.mainFragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fz.pocketcloset.items.ClothingItem;
import com.fz.pocketcloset.items.Collection;
import com.fz.pocketcloset.helpers.DatabaseHelper;
import com.fz.pocketcloset.helpers.ImagePickerHelper;
import com.fz.pocketcloset.MainActivity;
import com.fz.pocketcloset.R;
import com.fz.pocketcloset.items.SelectableItem;
import com.fz.pocketcloset.items.SelectableTag;
import com.fz.pocketcloset.temporaryFragments.SelectionAdapter;
import com.fz.pocketcloset.temporaryFragments.SelectionFragment;
import com.google.android.flexbox.FlexboxLayoutManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClothingFragment extends Fragment implements SelectionFragment.SelectionListener {

    private static final String TAG = "ClothesFragment";
    private RecyclerView recyclerView;
    private ClothingAdapter adapter;
    private ClothingManager clothingManager;
    private Parcelable recyclerViewState;
    private boolean isSelectionMode = false;
    private final Set<ClothingItem> selectedItems = new HashSet<>();
    private ImageButton deleteButton, addToCollectionButton, addClothesButton;
    private ImagePickerHelper imagePickerHelper;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private DatabaseHelper dbHelper;
    private ImageButton filterButton, clearFilterButton;
    private List<ClothingItem> clothingList; // Original, unfiltered list
    private List<ClothingItem> filteredClothingList; // Filtered list


    @Override
    public void onPause() {
        super.onPause();
        // Save the state of the clothing lists
        if (recyclerView != null && recyclerView.getLayoutManager() != null) {
            recyclerViewState = recyclerView.getLayoutManager().onSaveInstanceState();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Always reload data when the fragment is resumed
        reloadData();

        // Restore RecyclerView state
        if (recyclerViewState != null && recyclerView.getLayoutManager() != null) {
            recyclerView.post(() -> recyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState));
        }
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register ActivityResultLauncher
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                        imagePickerHelper.handleImageResult(result.getData());
                    }
                }
        );

        // Initialize ImagePickerHelper
        imagePickerHelper = new ImagePickerHelper(
                requireContext(),
                new DatabaseHelper(requireContext()),
                unused -> reloadData(), // Refresh RecyclerView after adding clothes
                -1, // -1 indicates new clothing items
                pickImageLauncher
        );
    }



    @SuppressLint("WrongViewCast")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the fragment layout
        return inflater.inflate(R.layout.fragment_clothes, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            // Initialize variables that depend on the view
            filteredClothingList = new ArrayList<>();
            clothingList = new ArrayList<>();
            clothingManager = new ClothingManager(requireContext());
            dbHelper = new DatabaseHelper(requireContext());

            recyclerView = view.findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));

            // Initialize ImagePickerHelper
            imagePickerHelper = new ImagePickerHelper(
                    requireContext(),
                    dbHelper,
                    unused -> {
                        clothingList = clothingManager.getAllClothingItems(); // Refresh full list
                        filteredClothingList = new ArrayList<>(clothingList); // Reset filtered list
                        updateAdapterSafely(); // Update adapter
                        updateClearFilterButtonVisibility(Collections.emptySet()); // Reset to no filter
                    },
                    -1, // Pass -1 for creating a new item
                    pickImageLauncher // Pass the registered launcher
            );

            deleteButton = view.findViewById(R.id.deleteButton);
            addToCollectionButton = view.findViewById(R.id.addToCollectionButton);
            addClothesButton = view.findViewById(R.id.button_add_clothes);
            filterButton = view.findViewById(R.id.filterButton);
            clearFilterButton = view.findViewById(R.id.clearFilterButton);
            LinearLayout selectedTagsContainer = view.findViewById(R.id.selectedTagsContainer);


            // Set up button listeners
            deleteButton.setOnClickListener(v -> deleteSelectedItems());
            addToCollectionButton.setOnClickListener(v -> showAddClothesToCollectionFragment());
            addClothesButton.setOnClickListener(v -> imagePickerHelper.openImagePicker());
            filterButton.setOnClickListener(v -> showFilterDialog());
            clearFilterButton.setOnClickListener(v -> clearFilter());

            // Load clothing items into the RecyclerView
            loadClothingItems();

            // Update button visibility based on the current state
            updateButtonVisibility();

        } catch (Exception e) {
            Log.e(TAG, "Error in onViewCreated: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error initializing ClothesFragment.", Toast.LENGTH_SHORT).show();
        }
    }



    private void loadClothingItems() {
        try {
            // Fetch the clothing items from the database
            clothingList = clothingManager.getAllClothingItems();

            // Initialize or reset the filtered list
            if (filteredClothingList == null || filteredClothingList.isEmpty()) {
                filteredClothingList = new ArrayList<>(clothingList);
            }

            // Update the adapter with the filtered list
            if (adapter == null) {
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
            } else {
                adapter.updateData(filteredClothingList);
            }

            adapter.notifyDataSetChanged();

            // Reset filter visibility
            updateClearFilterButtonVisibility(Collections.emptySet());

        } catch (Exception e) {
            Log.e(TAG, "Error loading clothing items: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error loading clothing items.", Toast.LENGTH_SHORT).show();
        }
    }




    private void handleItemClick(ClothingItem item) {
        Log.d(TAG, "handleItemClick: isSelectionMode=" + isSelectionMode + ", item=" + item);
        if (isSelectionMode) {
            toggleItemSelection(item);
        } else {
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.openClothingDetail(
                        item.getId(),
                        "ClothesFragment",
                        -1,
                        null
                );
            }
        }
    }


    private void handleItemLongClick(ClothingItem item) {
        if (!isSelectionMode) {
            isSelectionMode = true;
            adapter.setSelectionMode(true); // Enable selection mode in the adapter
            adapter.notifyDataSetChanged(); // Refresh UI to show checkboxes
            updateButtonVisibility();
        }

        toggleItemSelection(item); // Add or remove the item from the selection
        Log.d(TAG, "handleItemLongClick - Current Selected Items: " + selectedItems);
    }


    private void toggleItemSelection(ClothingItem item) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item);
        } else {
            selectedItems.add(item);
        }

        Log.d(TAG, "toggleItemSelection - Current Selected Items: " + selectedItems);

        // Notify adapter to update the UI
        adapter.notifyDataSetChanged();
    }


    private void exitSelectionMode() {
        isSelectionMode = false;
        selectedItems.clear();
        adapter.setSelectionMode(false);
        adapter.clearSelections(); // Clear selection state in the adapter

        updateButtonVisibility();
    }

    private void deleteSelectedItems() {
        try {
            // Use the adapter's selected items
            Set<ClothingItem> selectedItems = adapter.getSelectedItems();
            if (selectedItems.isEmpty()) {
                Toast.makeText(requireContext(), "No items selected for deletion.", Toast.LENGTH_SHORT).show();
                return;
            }

            List<Integer> idsToDelete = new ArrayList<>();
            List<Integer> linkedOutfitIds = new ArrayList<>();
            SQLiteDatabase db = new DatabaseHelper(requireContext()).getReadableDatabase();

            // Collect IDs to delete
            for (ClothingItem item : selectedItems) {
                idsToDelete.add(item.getId());

                // Fetch linked outfits
                String query = "SELECT outfit_id FROM Clothes_Outfits WHERE clothes_id = ?";
                Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(item.getId())});
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        int outfitId = cursor.getInt(0);
                        if (!linkedOutfitIds.contains(outfitId)) {
                            linkedOutfitIds.add(outfitId);
                        }
                    }
                    cursor.close();
                }
            }

            // Delete clothing items
            clothingManager.deleteMultipleItems(idsToDelete);

            // Delete linked outfits
            for (int outfitId : linkedOutfitIds) {
                new OutfitManager(requireContext()).deleteOutfit(outfitId);
            }

            Toast.makeText(requireContext(), "Items and associated outfits deleted!", Toast.LENGTH_SHORT).show();

            // Exit selection mode
            exitSelectionMode();

            // Reload data and update adapter
            reloadData();

            // Refresh related fragments
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.refreshCollections();
                mainActivity.refreshOutfitsFragment();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error deleting selected items: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error deleting items.", Toast.LENGTH_SHORT).show();
        }
    }




    public void reloadData() {
        try {
            // Reload the full clothing list from the database
            clothingList = clothingManager.getAllClothingItems();

            // Reset the filtered list to match the full clothing list
            filteredClothingList = new ArrayList<>(clothingList);

            // Safely update the adapter with the new data
            updateAdapterSafely();

            Log.d(TAG, "ClothesFragment data reloaded. Total items: " + clothingList.size());
        } catch (Exception e) {
            Log.e(TAG, "Error reloading data in ClothesFragment: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error reloading data.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddClothesToCollectionFragment() {
        List<Collection> collections = new CollectionsManager(requireContext()).getAllCollections();

        if (collections.isEmpty()) {
            Toast.makeText(requireContext(), "No collections available.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert collections to SelectableItem and create selection state
        List<SelectableItem> collectionItems = new ArrayList<>(collections);
        boolean[] selectedItems = new boolean[collections.size()];

        SelectionFragment fragment = SelectionFragment.newInstance(
                "Add Clothes to Collections",
                collectionItems,
                selectedItems
        );

        getChildFragmentManager().beginTransaction()
                .replace(R.id.selection_fragment_container, fragment, "SelectionFragment")
                .addToBackStack(null)
                .commit();

        if (getView() != null) {
            getView().findViewById(R.id.selection_fragment_container).setVisibility(View.VISIBLE);
        }
    }
    @Override
    public void onSelectionSaved(boolean[] selectedItems) {
        try {

            boolean anySelected = false;

            // Check if any item is selected
            for (boolean isSelected : selectedItems) {
                if (isSelected) {
                    anySelected = true;
                    break;
                }
            }

            // Show toast and exit if no item is selected
            if (!anySelected) {
                Toast.makeText(requireContext(), "No items selected.", Toast.LENGTH_SHORT).show();
                return;
            }

            List<Collection> collections = new CollectionsManager(requireContext()).getAllCollections();

            if (collections == null || collections.isEmpty()) {
                Toast.makeText(requireContext(), "No collections available.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Log selected collections and items for debugging
            Log.d(TAG, "Selected items array: " + java.util.Arrays.toString(selectedItems));
            Log.d(TAG, "Adapter selected items count: " + adapter.getSelectedItems().size());

            SQLiteDatabase db = new DatabaseHelper(requireContext()).getWritableDatabase();
            db.beginTransaction(); // Begin transaction for consistency

            try {
                // Iterate through selected collections
                for (int i = 0; i < selectedItems.length; i++) {
                    if (selectedItems[i]) {
                        Collection selectedCollection = collections.get(i);

                        // Assign each selected clothing item to this collection
                        for (ClothingItem clothingItem : adapter.getSelectedItems()) {
                            if (!isClothingAlreadyAssigned(clothingItem.getId(), selectedCollection.getId(), db)) {
                                ContentValues values = new ContentValues();
                                values.put("clothes_id", clothingItem.getId());
                                values.put("collection_id", selectedCollection.getId());
                                db.insert("Clothes_Collections", null, values);
                                Log.d(TAG, "Assigned clothingId=" + clothingItem.getId() + " to collectionId=" + selectedCollection.getId());
                            } else {
                                Log.d(TAG, "Skipping duplicate: clothingId=" + clothingItem.getId() + ", collectionId=" + selectedCollection.getId());
                            }
                        }
                    }
                }

                db.setTransactionSuccessful(); // Commit transaction
            } finally {
                db.endTransaction(); // Ensure transaction is ended properly
            }

            Toast.makeText(requireContext(), "Clothes added to selected collections!", Toast.LENGTH_SHORT).show();

            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).refreshCollections();
            }

            hideSelectionFragment();
        } catch (Exception e) {
            Log.e(TAG, "Error saving selections: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "An error occurred while adding clothes to collections.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isClothingAlreadyAssigned(int clothingId, int collectionId, SQLiteDatabase db) {
        boolean exists = false;
        try {
            String query = "SELECT COUNT(*) FROM Clothes_Collections WHERE clothes_id = ? AND collection_id = ?";
            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(clothingId), String.valueOf(collectionId)});

            if (cursor.moveToFirst()) {
                exists = cursor.getInt(0) > 0;
            }

            cursor.close();
        } catch (Exception e) {
            Log.e(TAG, "Error checking assignment: " + e.getMessage(), e);
        }
        return exists;
    }

    @Override
    public void onSelectionCancelled() {
        Toast.makeText(requireContext(), "Selection cancelled.", Toast.LENGTH_SHORT).show();
        hideSelectionFragment();
    }

    private void hideSelectionFragment() {
        // Attempt to clear selections if the adapter is available
        Fragment fragment = getChildFragmentManager().findFragmentByTag("SelectionFragment");
        if (fragment != null && fragment.getView() != null) {
            RecyclerView recyclerView = fragment.getView().findViewById(R.id.selectionRecyclerView);
            if (recyclerView != null && recyclerView.getAdapter() instanceof SelectionAdapter) {
                SelectionAdapter adapter = (SelectionAdapter) recyclerView.getAdapter();
                adapter.clearSelections(); // Clear all selected items
            }
        }


        View container = requireView().findViewById(R.id.selection_fragment_container);
        if (container != null) {
            container.setVisibility(View.GONE);
        }

        getParentFragmentManager().popBackStack(); // Remove SelectionFragment from back stack
        exitSelectionMode();
    }
    private void showFilterDialog() {
        try {
            List<String> tags = clothingManager.getAllTags();
            if (tags.isEmpty()) {
                Toast.makeText(requireContext(), "No tags available for filtering.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Convert tags into SelectableTag objects
            List<SelectableItem> selectableTags = new ArrayList<>();
            for (String tag : tags) {
                selectableTags.add(new SelectableTag(tag));
            }

            boolean[] selectedTags = new boolean[selectableTags.size()];

            // Set up the filter overlay to show the tags
            FrameLayout filterOverlayContainer = requireView().findViewById(R.id.filterOverlayContainer);
            filterOverlayContainer.setVisibility(View.VISIBLE);

            // RecyclerView for displaying tags
            RecyclerView tagsRecyclerView = requireView().findViewById(R.id.tagsRecyclerView);
            FlexboxLayoutManager flexboxLayoutManager = new FlexboxLayoutManager(requireContext());
            tagsRecyclerView.setLayoutManager(flexboxLayoutManager);


            // Set adapter
            SelectionAdapter tagAdapter = new SelectionAdapter(selectableTags, selectedTags);
            tagsRecyclerView.setAdapter(tagAdapter);

            // Handle Save button click
            ImageButton saveFilterButton = requireView().findViewById(R.id.saveFilterButton);
            saveFilterButton.setOnClickListener(v -> {
                Set<String> selectedTagSet = new HashSet<>();
                for (int i = 0; i < selectableTags.size(); i++) {
                    if (selectedTags[i]) {
                        selectedTagSet.add(selectableTags.get(i).getDisplayName().toLowerCase().trim());
                    }
                }
                applyFilter(selectedTagSet);
                filterOverlayContainer.setVisibility(View.GONE);
            });

            // Handle Cancel button click
            ImageButton cancelFilterButton = requireView().findViewById(R.id.cancelFilterButton);
            cancelFilterButton.setOnClickListener(v -> filterOverlayContainer.setVisibility(View.GONE));

        } catch (Exception e) {
            Log.e(TAG, "Error showing filter dialog: " + e.getMessage(), e);
        }
    }



    private void applyFilter(Set<String> selectedTags) {
        try {
            // Normalize the selected tags
            Set<String> normalizedSelectedTags = normalizeTags(selectedTags);

            // Reset the filtered list
            filteredClothingList.clear();

            // Apply filter logic to each clothing item
            for (ClothingItem item : clothingList) {
                if (item.getTags() != null) {
                    Set<String> itemTags = new HashSet<>(Arrays.asList(item.getTags().split(",")));
                    itemTags = normalizeTags(itemTags);  // Normalize item tags

                    // If there is any overlap between the item's tags and selected tags, add it to the filtered list
                    if (!Collections.disjoint(itemTags, normalizedSelectedTags)) {
                        filteredClothingList.add(item);
                    }
                }
            }

            // Update RecyclerView with the filtered data
            adapter.updateData(filteredClothingList);
            adapter.notifyDataSetChanged();

            // Update the UI based on the filter state (show/hide clear filter button)
            updateClearFilterButtonVisibility(normalizedSelectedTags);

            // Update the selected tags container
            updateSelectedTagsDisplay(normalizedSelectedTags);  // This will rebuild the selected tags container with the selected tags

        } catch (Exception e) {
            Log.e(TAG, "Error applying filter: " + e.getMessage(), e);
        }
    }

    private Set<String> normalizeTags(Set<String> tags) {
        Set<String> normalizedTags = new HashSet<>();
        for (String tag : tags) {
            normalizedTags.add(tag.trim().toLowerCase()); // Trim and normalize each tag
        }
        return normalizedTags;
    }

    private void updateClearFilterButtonVisibility(Set<String> selectedTagSet) {
        // Show or hide the clear filter button based on the filter state
        if (selectedTagSet != null && !selectedTagSet.isEmpty()) {
            clearFilterButton.setVisibility(View.VISIBLE); // Show button if filtering is applied
        } else {
            clearFilterButton.setVisibility(View.GONE); // Hide button otherwise
        }
    }


    private void updateSelectedTagsDisplay(Set<String> selectedTags) {
        LinearLayout selectedTagsContainer = requireView().findViewById(R.id.selectedTagsContainer);

        selectedTagsContainer.removeAllViews(); // Clear any previous tags

        if (selectedTags == null || selectedTags.isEmpty()) {
            selectedTagsContainer.setVisibility(View.GONE); // Hide if no tags are selected
            return;
        }

        selectedTagsContainer.setVisibility(View.VISIBLE); // Show the container

        for (String tag : selectedTags) {
            TextView tagView = new TextView(requireContext());
            tagView.setText(tag);
            tagView.setPadding(16, 8, 16, 8);
            tagView.setTextSize(14);
            tagView.setBackgroundResource(R.drawable.tag_background); // Add a drawable background for tags
            tagView.setTextColor(requireContext().getColor(android.R.color.white));
            selectedTagsContainer.addView(tagView);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) tagView.getLayoutParams();
            params.setMargins(8, 8, 8, 8);
            tagView.setLayoutParams(params);
        }
    }


    private void clearFilter() {
        try {
            // Reset the filtered list to show all items
            filteredClothingList = new ArrayList<>(clothingList);
            adapter.updateData(filteredClothingList);
            adapter.notifyDataSetChanged();

            // Reset the UI
            updateAddClothesButtonVisibility(true);
            updateClearFilterButtonVisibility(Collections.emptySet());

            // Clear the selected tags display
            updateSelectedTagsDisplay(Collections.emptySet());

            // Explicitly remove the tint from all tags when filter is cleared
            removeTintFromAllTags();

        } catch (Exception e) {
            Log.e(TAG, "Error clearing filter: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Failed to clear filter.", Toast.LENGTH_SHORT).show();
        }
    }

    private void removeTintFromAllTags() {
        // Get the FlexboxLayout or RecyclerView for the tags
        RecyclerView tagsRecyclerView = requireView().findViewById(R.id.tagsRecyclerView);
        if (tagsRecyclerView != null) {
            // Loop through all the tag items and remove the tint
            for (int i = 0; i < tagsRecyclerView.getChildCount(); i++) {
                View tagView = tagsRecyclerView.getChildAt(i);
                if (tagView instanceof TextView) {
                    // Remove background tint if it exists
                    tagView.getBackground().setTintList(null);  // Remove the tint
                    // Optionally reset the background color (if needed)
                    tagView.setBackgroundResource(R.drawable.tag_background); // Ensure the background stays consistent
                }
            }
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


    private void updateButtonVisibility() {
        Log.d(TAG, "updateButtonVisibility: isSelectionMode=" + isSelectionMode);
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
