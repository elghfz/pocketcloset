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
    private RecyclerView recyclerView;
    private ClothingAdapter adapter;
    private DatabaseHelper dbHelper;
    private int collectionId;
    private String collectionName;
    private Button renameButton, addClothesButton, removeFromCollectionButton, closeButton;
    private boolean isSelectionMode = false;
    private final Set<ClothingItem> selectedItems = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_collection_detail, container, false);

        try {
            // Get collection details from arguments
            collectionId = requireArguments().getInt("collection_id", -1);
            collectionName = requireArguments().getString("collection_name", "Default Collection");

            dbHelper = new DatabaseHelper(requireContext());

            // Initialize UI elements
            recyclerView = view.findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2)); // Grid view with 2 columns

            TextView titleTextView = view.findViewById(R.id.titleTextView);
            titleTextView.setText(collectionName); // Set collection name dynamically

            renameButton = view.findViewById(R.id.button_rename_collection);
            addClothesButton = view.findViewById(R.id.button_add_clothes_to_collection);
            removeFromCollectionButton = view.findViewById(R.id.removeFromCollectionButton);
            closeButton = view.findViewById(R.id.button_close_collection);

            renameButton.setOnClickListener(v -> showRenameDialog());
            addClothesButton.setOnClickListener(v -> showAddClothesDialog());
            removeFromCollectionButton.setOnClickListener(v -> removeSelectedFromCollection());
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
                    true, // Show "Remove from Collection" button
                    collectionId,
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
        input.setText(collectionName); // Pre-fill with the current name
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

            collectionName = newName;
            TextView titleTextView = getView().findViewById(R.id.titleTextView);
            titleTextView.setText(newName);

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

    private void closeCollection() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).closeCollectionDetail();
        }
    }
}
