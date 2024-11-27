package com.fz.pocketcloset;

import android.app.AlertDialog;
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

import java.util.List;

public class CollectionsFragment extends Fragment {

    private static final String TAG = "CollectionsFragment";
    private RecyclerView recyclerView;
    private CollectionAdapter adapter;
    private DatabaseHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_collections, container, false);

        try {
            // Initialize DatabaseHelper
            dbHelper = new DatabaseHelper(requireContext());

            // Set up RecyclerView
            recyclerView = view.findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

            // Set up Add Collection button
            Button addCollectionButton = view.findViewById(R.id.button_add_collection);
            addCollectionButton.setOnClickListener(v -> showAddCollectionDialog());

            // Load Collections
            loadCollections();

        } catch (Exception e) {
            Log.e(TAG, "Error initializing CollectionsFragment: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error loading collections.", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    void loadCollections() {
        try {
            // Fetch collections from the database
            List<Collection> collections = new CollectionsManager(requireContext()).getAllCollections();

            // Initialize adapter
            adapter = new CollectionAdapter(
                    collections,
                    requireContext(),
                    dbHelper,
                    collection -> showEditCollectionDialog(collection),
                    collection -> deleteCollection(collection)
            );

            recyclerView.setAdapter(adapter);

        } catch (Exception e) {
            Log.e(TAG, "Error loading collections: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error loading collections.", Toast.LENGTH_SHORT).show();
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

    private void showEditCollectionDialog(Collection collection) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit Collection");

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_collection, null);
        builder.setView(dialogView);

        // Input for collection name
        EditText inputName = dialogView.findViewById(R.id.input_collection_name);
        inputName.setText(collection.getName());

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = inputName.getText().toString();
            if (!newName.isEmpty()) {
                new CollectionsManager(requireContext()).updateCollectionName(collection.getId(), newName);
                loadCollections(); // Refresh list
                Toast.makeText(requireContext(), "Collection updated!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Name cannot be empty.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void deleteCollection(Collection collection) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Delete Collection")
                .setMessage("Are you sure you want to delete this collection?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    new CollectionsManager(requireContext()).deleteCollection(collection.getId());
                    loadCollections(); // Refresh list
                    Toast.makeText(requireContext(), "Collection deleted!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}
