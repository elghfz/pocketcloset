package com.fz.pocketcloset;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollectionDetailFragment extends Fragment implements SelectionFragment.SelectionListener {

    private static final String TAG = "CollectionDetailFragment";
    private EditText editCollectionName;
    private TextView emojiTextView;
    private RecyclerView recyclerView;
    private ClothingAdapter adapter;
    private DatabaseHelper dbHelper;
    private int collectionId;
    private String collectionName;
    private String collectionEmoji;
    private ImageButton addClothesButton, removeFromCollectionButton, closeButton, deleteButton;

    private boolean isSelectionMode = false;
    private final Set<ClothingItem> selectedItems = new HashSet<>();

    @Override
    public void onResume() {
        super.onResume();
        reloadData(); // Ensure data is refreshed whenever the fragment becomes active
    }


    private void loadCollectionDetails() {
        try {
            // Fetch the latest collection details from the database
            Collection collection = new CollectionsManager(requireContext()).getCollectionById(collectionId);

            if (collection != null) {
                collectionName = collection.getName();
                collectionEmoji = collection.getEmoji();

                editCollectionName.setText(collectionName);
                emojiTextView.setText(collectionEmoji);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading collection details: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error loading collection details.", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_collection_detail, container, false);

        try {
            collectionId = requireArguments().getInt("collection_id", -1);

            dbHelper = new DatabaseHelper(requireContext());

            // Initialize UI elements
            recyclerView = view.findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 4));

            editCollectionName = view.findViewById(R.id.editCollectionName);
            emojiTextView = view.findViewById(R.id.emojiView);

            addClothesButton = view.findViewById(R.id.button_add_clothes_to_collection);
            removeFromCollectionButton = view.findViewById(R.id.button_remove_from_collection);
            deleteButton = view.findViewById(R.id.deleteButton);
            closeButton = view.findViewById(R.id.button_close_collection);

            // Listener to handle renaming when focus changes
            editCollectionName.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    String newName = editCollectionName.getText().toString().trim();
                    if (!newName.isEmpty() && !newName.equals(collectionName)) {
                        renameCollection(newName);
                    }
                    hideKeyboard(editCollectionName);
                }
            });

            // Optional: Handle "Done" action on the keyboard
            editCollectionName.setOnEditorActionListener((v, actionId, event) -> {
                String newName = editCollectionName.getText().toString().trim();
                if (!newName.isEmpty() && !newName.equals(collectionName)) {
                    renameCollection(newName);
                    editCollectionName.clearFocus(); // Dismiss keyboard
                    hideKeyboard(editCollectionName);
                }
                return false; // Let the event propagate
            });

            // Clear focus when touching outside the EditText
            View parentLayout = view.findViewById(R.id.parentLayout);
            parentLayout.setOnTouchListener((v, event) -> {
                editCollectionName.clearFocus();
                hideKeyboard(editCollectionName);
                return false;
            });

            addClothesButton.setOnClickListener(v -> showAddClothesFragment());
            removeFromCollectionButton.setOnClickListener(v -> removeSelectedFromCollection());
            closeButton.setOnClickListener(v -> navigateBack());
            emojiTextView.setOnClickListener(v -> updateEmoji());
            deleteButton.setOnClickListener(v -> deleteCollection());

            // Load the latest collection details
            loadCollectionDetails();
            loadClothesInCollection();

            updateButtonVisibility();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing CollectionDetailFragment: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error loading collection details.", Toast.LENGTH_SHORT).show();
        }

        return view;
    }



    public void reloadData() {
        Log.d(TAG, "reloadData: Reloading collection details...");
        try {
            loadCollectionDetails();
            loadClothesInCollection();
            updateButtonVisibility(); // Ensure buttons are updated
        } catch (Exception e) {
            Log.e(TAG, "Error reloading collection data: " + e.getMessage(), e);
        }
    }




    private void loadClothesInCollection() {
        try {
            Log.d(TAG, "Loading clothes in collection...");
            List<ClothingItem> clothesInCollection = new CollectionsManager(requireContext())
                    .getClothesInCollection(collectionId);

            Log.d(TAG, "Fetched " + clothesInCollection.size() + " clothing items for collection ID: " + collectionId);

            if (adapter == null) {
                adapter = new ClothingAdapter(
                        clothesInCollection,
                        this::handleItemClick,
                        this::handleItemLongClick,
                        isSelectionMode,
                        true,
                        collectionId,
                        null
                );
                recyclerView.setAdapter(adapter);
                Log.d(TAG, "Adapter initialized and set to RecyclerView.");
            } else {
                adapter.updateData(clothesInCollection);
                Log.d(TAG, "Adapter updated with new data. Item count: " + clothesInCollection.size());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading clothes in collection: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error loading clothes in collection.", Toast.LENGTH_SHORT).show();
        }
    }


    private void handleItemClick(ClothingItem item) {
        if (isSelectionMode) {
            toggleItemSelection(item);
        } else {
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();

                // Call openClothingDetail with the required parameters
                mainActivity.openClothingDetail(
                        item.getId(),                        // clothingId
                        "CollectionDetailFragment",          // originTag
                        collectionId,                        // collectionId (retrieved from the fragment's state)
                        collectionName                       // collectionName (retrieved from the fragment's state)
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

        if (selectedItems.isEmpty()) {
            exitSelectionMode();
        }

        adapter.notifyDataSetChanged();
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        selectedItems.clear();
        adapter.setSelectionMode(false);
        adapter.notifyDataSetChanged();
        updateButtonVisibility();
    }

    private void updateButtonVisibility() {
        if (isSelectionMode) {
            addClothesButton.setVisibility(View.GONE);
            deleteButton.setVisibility(View.GONE);
            removeFromCollectionButton.setVisibility(View.VISIBLE);
        } else {
            addClothesButton.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.VISIBLE);
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



    private void renameCollection(String newName) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("name", newName);
            int rowsAffected = db.update("Collections", values, "id = ?", new String[]{String.valueOf(collectionId)});
            db.close();

            if (rowsAffected > 0) {
                collectionName = newName;
                editCollectionName.setText(newName);

                // Notify parent fragment
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).refreshCollections();
                }

                Toast.makeText(requireContext(), "Collection renamed successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Failed to rename collection.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error renaming collection: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error renaming collection.", Toast.LENGTH_SHORT).show();
        }
    }



    private void deleteCollection() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Delete Collection")
                .setMessage("Are you sure you want to delete this collection? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    try {
                        // Delete the collection from the database
                        new CollectionsManager(requireContext()).deleteCollection(collectionId);

                        Toast.makeText(requireContext(), "Collection deleted!", Toast.LENGTH_SHORT).show();

                        if (getActivity() instanceof MainActivity) {
                            MainActivity mainActivity = (MainActivity) getActivity();

                            // Notify MainActivity about the deletion and handle navigation
                            String origin = getArguments() != null ? getArguments().getString("origin", "CollectionsFragment") : "CollectionsFragment";
                            mainActivity.handleCollectionDeletion(collectionId, origin);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error deleting collection: " + e.getMessage(), e);
                        Toast.makeText(requireContext(), "Failed to delete collection.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }


    private void showAddClothesFragment() {
        List<ClothingItem> availableClothes = new CollectionsManager(requireContext())
                .getAvailableClothesForCollection(collectionId);

        if (availableClothes.isEmpty()) {
            Toast.makeText(requireContext(), "No clothes available to add.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert clothing items to SelectableItem and create selection state
        List<SelectableItem> clothingItems = new ArrayList<>(availableClothes);
        boolean[] selectedItems = new boolean[availableClothes.size()];

        SelectionFragment fragment = SelectionFragment.newInstance(
                "Add Clothes to Collection",
                clothingItems,
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

            // Fetch available clothes for the collection
            List<ClothingItem> availableClothes = new CollectionsManager(requireContext())
                    .getAvailableClothesForCollection(collectionId);

            if (availableClothes == null || availableClothes.isEmpty()) {
                Toast.makeText(requireContext(), "No clothes available to add.", Toast.LENGTH_SHORT).show();
                return;
            }

            SQLiteDatabase db = new DatabaseHelper(requireContext()).getWritableDatabase();
            db.beginTransaction(); // Begin transaction for consistency

            try {
                // Iterate over the selected items
                for (int i = 0; i < selectedItems.length; i++) {
                    if (selectedItems[i]) {
                        ClothingItem clothingItem = availableClothes.get(i);

                        // Check if already assigned to avoid duplicates
                        if (!isClothingAlreadyAssigned(clothingItem.getId(), collectionId, db)) {
                            ContentValues values = new ContentValues();
                            values.put("clothes_id", clothingItem.getId());
                            values.put("collection_id", collectionId);
                            db.insert("Clothes_Collections", null, values);

                            Log.d(TAG, "Assigned clothing ID: " + clothingItem.getId() + " to collection ID: " + collectionId);
                        } else {
                            Log.d(TAG, "Skipping duplicate assignment: clothing ID: " + clothingItem.getId());
                        }
                    }
                }

                db.setTransactionSuccessful(); // Mark transaction as successful
            } finally {
                db.endTransaction(); // End the transaction
            }

            // Refresh the UI
            loadClothesInCollection();
            Toast.makeText(requireContext(), "Clothes added successfully!", Toast.LENGTH_SHORT).show();

            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).refreshCollections();
            }

            hideSelectionFragment(); // Restore main content visibility
        } catch (Exception e) {
            Log.e(TAG, "Error adding clothes to collection: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "An error occurred while adding clothes.", Toast.LENGTH_SHORT).show();
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
            Log.e(TAG, "Error checking clothing assignment: " + e.getMessage(), e);
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

        getChildFragmentManager().beginTransaction()
                .remove(getChildFragmentManager().findFragmentByTag("SelectionFragment"))
                .commit();

        // Hide the container
        getView().findViewById(R.id.selection_fragment_container).setVisibility(View.GONE);

        // Notify MainActivity to refresh parent fragments
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).refreshCollections();
        }

    }


    private void updateEmoji() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Pick an Emoji");

        final EditText input = new EditText(requireContext());
        input.setHint("Enter an emoji");
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String emoji = input.getText().toString().trim();
            if (isEmoji(emoji)) {
                new CollectionsManager(requireContext()).updateCollectionEmoji(collectionId, emoji);

                // Reload local details
                loadCollectionDetails();

                // Notify parent fragment
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).refreshCollections();
                }

                Toast.makeText(requireContext(), "Emoji updated!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Invalid emoji. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }


    private boolean isEmoji(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        int codePoint = input.codePointAt(0);
        return Character.isSupplementaryCodePoint(codePoint);
    }


    void navigateBack() {
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();

            String origin = getArguments() != null ? getArguments().getString("origin", "CollectionsFragment") : "CollectionsFragment";

            if ("ClothingDetailFragment".equals(origin)) {
                int clothingId = getArguments() != null ? getArguments().getInt("clothing_id", -1) : -1;
                int collectionId = getArguments() != null ? getArguments().getInt("collection_id", -1) : -1;
                String collectionName = getArguments() != null ? getArguments().getString("collection_name") : null;

                if (clothingId != -1) {
                    Log.d(TAG, "Navigating back to ClothingDetailFragment with clothingId: " + clothingId);
                    mainActivity.openClothingDetail(clothingId, origin, collectionId, collectionName);
                } else {
                    Log.e(TAG, "Missing clothing ID for navigation back to ClothingDetailFragment.");
                }
            } else {
                mainActivity.navigateBackToCollectionsFragment();
            }
        }
    }




    private void hideKeyboard(View view) {
        try {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error hiding keyboard: " + e.getMessage(), e);
        }
    }




}