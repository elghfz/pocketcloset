package com.fz.pocketcloset;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollectionsFragment extends Fragment {

    private static final String TAG = "CollectionsFragment";
    private RecyclerView recyclerView;
    private CollectionAdapter adapter;

    private ImageButton addCollectionButton, deleteButton;
    private boolean isSelectionMode = false;
    private final Set<Collection> selectedItems = new HashSet<>();


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_collections, container, false);

        try {
            DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
            recyclerView = view.findViewById(R.id.collectionRecyclerView);
            recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));


            addCollectionButton = view.findViewById(R.id.addCollectionButton);
            deleteButton = view.findViewById(R.id.deleteButton);

            addCollectionButton.setOnClickListener(v -> showAddCollectionDialog());
            deleteButton.setOnClickListener(v -> deleteSelectedCollections());

            updateButtonVisibility();
            loadCollections();

        } catch (Exception e) {
            Log.e(TAG, "Error initializing CollectionsFragment: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error loading collections.", Toast.LENGTH_SHORT).show();
        }

        return view;
    }


    private void loadCollections() {
        try {
            List<Collection> collections = new CollectionsManager(requireContext()).getAllCollections();

            if (adapter == null) {
                adapter = new CollectionAdapter(
                        collections,
                        this::handleItemClick,
                        this::handleItemLongClick,
                        isSelectionMode,
                        null
                );
                recyclerView.setAdapter(adapter);
            } else {
                adapter.updateData(collections); // Dynamically update the data
                adapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading collections: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error loading collections.", Toast.LENGTH_SHORT).show();
        }
    }


    private void handleItemClick(Collection collection) {
        if (isSelectionMode) {
            toggleItemSelection(collection);
        } else {
            openCollection(collection);
        }
    }

    private void handleItemLongClick(Collection collection) {
        if (collection == null) {
            exitSelectionMode();
            return;
        }
        if (!isSelectionMode) {
            isSelectionMode = true;
            updateButtonVisibility(); // Show Delete Button
        }
        toggleItemSelection(collection);
    }


    private void toggleItemSelection(Collection collection) {
        if (selectedItems.contains(collection)) {
            selectedItems.remove(collection);
        } else {
            selectedItems.add(collection);
        }

        // Exit selection mode if all items are deselected
        if (selectedItems.isEmpty() && isSelectionMode) {
            exitSelectionMode();
        }

        adapter.notifyDataSetChanged();
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        selectedItems.clear();
        adapter.setSelectionMode(false);
        updateButtonVisibility(); // This hides the Delete Button
    }


    private void deleteSelectedCollections() {
        try {
            for (Collection collection : selectedItems) {
                new CollectionsManager(requireContext()).deleteCollection(collection.getId());
            }
            Toast.makeText(requireContext(), "Selected collections deleted!", Toast.LENGTH_SHORT).show();
            exitSelectionMode();
            loadCollections();
        } catch (Exception e) {
            Log.e(TAG, "Error deleting selected collections: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error deleting collections.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddCollectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add New Collection");

        // Use an EditText for the input
        final EditText input = new EditText(requireContext());
        input.setHint("Collection Name");
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString();
            if (!newName.isEmpty()) {
                new CollectionsManager(requireContext()).addCollection(newName);
                loadCollections(); // Refresh list
                Toast.makeText(requireContext(), "Collection added!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Name cannot be empty.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }


    public void reloadData() {
        loadCollections();
    }


    private void openCollection(Collection collection) {
        if (!isSelectionMode) {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openCollectionDetail(collection.getId(), collection.getName(), "CollectionsFragment");
            }
        }
    }

    private void updateButtonVisibility() {
        if (isSelectionMode) {
            addCollectionButton.setVisibility(View.GONE); // Hide "Add Collection" button
            deleteButton.setVisibility(View.VISIBLE);    // Show "Delete" button
        } else {
            addCollectionButton.setVisibility(View.VISIBLE); // Show "Add Collection" button
            deleteButton.setVisibility(View.GONE);           // Hide "Delete" button
        }
    }

}
