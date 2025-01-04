package com.fz.pocketcloset;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollectionDetailFragment extends Fragment {

    private static final String TAG = "CollectionDetailFragment";
    private TextView collectionNameTextView;
    private TextView emojiTextView;
    private RecyclerView recyclerView;
    private ClothingAdapter adapter;
    private DatabaseHelper dbHelper;
    private int collectionId;
    private String collectionName;
    private String collectionEmoji;
    private ImageButton renameButton, addClothesButton, removeFromCollectionButton, closeButton, deleteButton;

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

                collectionNameTextView.setText(collectionName);
                emojiTextView.setText(collectionEmoji);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading collection details: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error loading collection details.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_collection_detail, container, false);

        try {
            collectionId = requireArguments().getInt("collection_id", -1);

            dbHelper = new DatabaseHelper(requireContext());

            // Initialize UI elements
            recyclerView = view.findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 4));

            collectionNameTextView = view.findViewById(R.id.textViewCollectionName);
            emojiTextView = view.findViewById(R.id.emojiView);

            renameButton = view.findViewById(R.id.button_rename_collection);
            addClothesButton = view.findViewById(R.id.button_add_clothes_to_collection);
            removeFromCollectionButton = view.findViewById(R.id.button_remove_from_collection);
            deleteButton = view.findViewById(R.id.deleteButton);
            closeButton = view.findViewById(R.id.button_close_collection);

            renameButton.setOnClickListener(v -> showRenameDialog());
            addClothesButton.setOnClickListener(v -> showAddClothesDialog());
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
            renameButton.setVisibility(View.GONE);
            addClothesButton.setVisibility(View.GONE);
            deleteButton.setVisibility(View.GONE);
            removeFromCollectionButton.setVisibility(View.VISIBLE);
        } else {
            renameButton.setVisibility(View.VISIBLE);
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


    private void showRenameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Rename Collection");

        final EditText input = new EditText(requireContext());
        input.setText(collectionName); // Pre-fill with the current name
        input.setHint("Enter a new name");
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
            int rowsAffected = db.update("Collections", values, "id = ?", new String[]{String.valueOf(collectionId)});
            db.close();

            if (rowsAffected > 0) {
                collectionName = newName;
                collectionNameTextView.setText(newName);

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








}