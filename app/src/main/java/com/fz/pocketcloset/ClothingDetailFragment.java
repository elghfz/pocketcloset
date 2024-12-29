package com.fz.pocketcloset;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ClothingDetailFragment extends Fragment {

    private static final String TAG = "ClothingDetailFragment";
    private ImageView clothingImageView;
    private TextView tagsTextView;
    private RecyclerView collectionsRecyclerView;
    private int clothingId;
    private String originFragment; // Tracks where the user came from
    private DatabaseHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_clothing_detail, container, false);

        try {
            clothingId = requireArguments().getInt("clothing_id");
            originFragment = requireArguments().getString("origin", "ClothesFragment"); // Default to ClothesFragment
            dbHelper = new DatabaseHelper(requireContext());

            clothingImageView = view.findViewById(R.id.clothingImageView);
            tagsTextView = view.findViewById(R.id.tagsTextView);
            collectionsRecyclerView = view.findViewById(R.id.collectionsRecyclerView);
            collectionsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

            // Load clothing details
            loadClothingDetails();

            // Set button click listeners
            Button saveButton = view.findViewById(R.id.button_save);
            saveButton.setOnClickListener(v -> navigateBack());

            Button editButton = view.findViewById(R.id.button_edit_clothing);
            editButton.setOnClickListener(v -> showEditTagsDialog());

            Button addToCollectionButton = view.findViewById(R.id.button_add_to_collection);
            addToCollectionButton.setOnClickListener(v -> showAddToCollectionDialog());

            Button deleteButton = view.findViewById(R.id.button_delete_clothing);
            deleteButton.setOnClickListener(v -> deleteClothing());

        } catch (Exception e) {
            Log.e(TAG, "Error initializing ClothingDetailFragment: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error loading clothing details.", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void loadClothingDetails() {
        try {
            ClothingManager clothingManager = new ClothingManager(requireContext());
            ClothingItem clothing = clothingManager.getClothingById(clothingId);

            if (clothing != null) {
                // Set clothing image and tags
                clothingImageView.setImageURI(clothing.getImagePath() != null ? Uri.parse(clothing.getImagePath()) : null);
                tagsTextView.setText(clothing.getTags());

                // Load collections it belongs to
                List<Collection> collections = new CollectionsManager(requireContext()).getCollectionsForClothing(clothingId);
                CollectionAdapter adapter = new CollectionAdapter(
                        collections,
                        collection -> {
                            // Open the CollectionDetailFragment for the selected collection
                            if (getActivity() instanceof MainActivity) {
                                ((MainActivity) getActivity()).openCollectionDetail(collection.getId(), collection.getName());
                            }
                        },
                        null, // No long-click handler for collection items here
                        false
                );
                collectionsRecyclerView.setAdapter(adapter);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading clothing details: " + e.getMessage(), e);
        }
    }

    private void showEditTagsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit Tags");

        final EditText input = new EditText(requireContext());
        input.setText(tagsTextView.getText().toString());
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newTags = input.getText().toString().trim();
            if (!newTags.isEmpty()) {
                updateClothingTags(newTags);
            } else {
                Toast.makeText(requireContext(), "Tags cannot be empty!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void updateClothingTags(String newTags) {
        try {
            new ClothingManager(requireContext()).updateClothingItem(clothingId, newTags);
            tagsTextView.setText(newTags);
            Toast.makeText(requireContext(), "Tags updated!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error updating clothing tags: " + e.getMessage(), e);
        }
    }

    private void showAddToCollectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add to Collection");

        List<Collection> collections = new CollectionsManager(requireContext()).getAllCollections();
        String[] collectionNames = new String[collections.size()];
        boolean[] checkedItems = new boolean[collections.size()];

        for (int i = 0; i < collections.size(); i++) {
            collectionNames[i] = collections.get(i).getName();
            checkedItems[i] = false;
        }

        builder.setMultiChoiceItems(collectionNames, checkedItems, (dialog, which, isChecked) -> {
            checkedItems[which] = isChecked;
        });

        builder.setPositiveButton("Add", (dialog, which) -> {
            for (int i = 0; i < collections.size(); i++) {
                if (checkedItems[i]) {
                    new CollectionsManager(requireContext()).assignClothingToCollection(clothingId, collections.get(i).getId());
                }
            }
            loadClothingDetails(); // Refresh collections list
            Toast.makeText(requireContext(), "Added to selected collections!", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void navigateBack() {
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            if ("CollectionDetailFragment".equals(originFragment)) {
                mainActivity.navigateBackToCollectionDetail(); // Navigate back to CollectionDetailFragment
            } else {
                mainActivity.navigateBackToClothesFragment(); // Navigate back to ClothesFragment
            }
        }
    }

    private void deleteClothing() {
        try {
            new ClothingManager(requireContext()).deleteClothingItem(clothingId);

            Toast.makeText(requireContext(), "Clothing item deleted!", Toast.LENGTH_SHORT).show();

            // Navigate back and refresh the list in the parent fragment
            navigateBack();

            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).refreshClothingList(); // Add a method in MainActivity to trigger refresh
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting clothing item: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Failed to delete clothing item.", Toast.LENGTH_SHORT).show();
        }
    }



}
