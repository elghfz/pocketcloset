package com.fz.pocketcloset;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ClothingDetailFragment extends Fragment implements SelectionFragment.SelectionListener {

    private static final String TAG = "ClothingDetailFragment";
    private ImagePickerHelper imagePickerHelper;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ImageView clothingImageView;
    private RecyclerView collectionsRecyclerView;
    private int clothingId;
    private String originFragment; // Tracks where the user came from
    private LinearLayout tagsContainer;
    private View imageOverlay;
    private ImageButton editImageButton;
    private View backgroundClickableArea;

    private DatabaseHelper dbHelper;

    public void onResume() {
        super.onResume();
        reloadData(); // Ensure data is refreshed whenever the fragment becomes active
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register ActivityResultLauncher
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (imagePickerHelper != null) {
                            imagePickerHelper.handleImageResult(selectedImageUri);
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_clothing_detail, container, false);

        try {
            clothingId = requireArguments().getInt("clothing_id");
            originFragment = requireArguments().getString("origin", "ClothesFragment"); // Default to ClothesFragment
            dbHelper = new DatabaseHelper(requireContext());

            tagsContainer = view.findViewById(R.id.tagsContainer);
            clothingImageView = view.findViewById(R.id.clothingImageView);
            collectionsRecyclerView = view.findViewById(R.id.collectionsRecyclerView);
            imageOverlay = view.findViewById(R.id.imageOverlay);
            editImageButton = view.findViewById(R.id.editImageButton);
            backgroundClickableArea = view.findViewById(R.id.backgroundClickableArea);

            GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 3);
            collectionsRecyclerView.setLayoutManager(layoutManager);

            // Initialize ImagePickerHelper
            imagePickerHelper = new ImagePickerHelper(requireContext(), dbHelper, result -> {
                // Refresh clothing details after update
                loadClothingDetails();
            }, clothingId, pickImageLauncher);

            // Set initial visibility
            imageOverlay.setVisibility(View.GONE);
            editImageButton.setVisibility(View.GONE);

            // Load clothing details
            loadClothingDetails();

            // Set button click listeners

            clothingImageView.setOnClickListener(v -> showEditOptions());
            editImageButton.setOnClickListener(v -> openImagePicker());
            backgroundClickableArea.setOnClickListener(v -> hideEditOptions());

            ImageButton saveButton = view.findViewById(R.id.button_save);
            saveButton.setOnClickListener(v -> navigateBack());

            ImageButton editButton = view.findViewById(R.id.button_edit_clothing);
            editButton.setOnClickListener(v -> showEditTagsDialog());

            ImageButton addToCollectionButton = view.findViewById(R.id.button_add_to_collection);
            addToCollectionButton.setOnClickListener(v -> showAddToCollectionFragment());

            ImageButton deleteButton = view.findViewById(R.id.button_delete_clothing);
            deleteButton.setOnClickListener(v -> deleteClothing());



        } catch (Exception e) {
            Log.e(TAG, "Error initializing ClothingDetailFragment: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error loading clothing details.", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void loadClothingDetails() {
        try {
            if (getView() == null) {
                Log.e(TAG, "View is not ready. Skipping loadClothingDetails.");
                return;
            }

            ClothingManager clothingManager = new ClothingManager(requireContext());
            ClothingItem clothing = clothingManager.getClothingById(clothingId);

            if (clothing != null) {
                clothingImageView.setImageURI(clothing.getImagePath() != null ? Uri.parse(clothing.getImagePath()) : null);

                // Set tags dynamically
                String[] tags = clothing.getTags() != null ? clothing.getTags().split(",") : new String[0];
                tagsContainer.removeAllViews();
                for (String tag : tags) {
                    TextView tagView = new TextView(requireContext());
                    tagView.setText(tag.trim());
                    tagView.setTextSize(14);
                    tagView.setTextColor(ResourcesCompat.getColor(getResources(), android.R.color.primary_text_dark, requireContext().getTheme()));
                    tagView.setPadding(16, 8, 16, 8);
                    tagView.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.tag_background, requireContext().getTheme()));

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(8, 4, 8, 4);
                    tagView.setLayoutParams(params);

                    tagsContainer.addView(tagView);
                }

                List<Collection> collections = new CollectionsManager(requireContext()).getCollectionsForClothing(clothingId);

                GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 3);
                collectionsRecyclerView.setLayoutManager(layoutManager);

                CollectionAdapter adapter = new CollectionAdapter(
                        collections,
                        collection -> {
                            if (getActivity() instanceof MainActivity) {
                                MainActivity mainActivity = (MainActivity) getActivity();
                                mainActivity.openCollectionDetail(
                                        collection.getId(),
                                        collection.getName(),
                                        "ClothingDetailFragment"
                                );
                            }
                        },
                        null,
                        false,
                        null
                );
                collectionsRecyclerView.setAdapter(adapter);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading clothing details: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error loading clothing details.", Toast.LENGTH_SHORT).show();
        }
    }


    private void showEditTagsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit Tags");

        final EditText input = new EditText(requireContext());
        // Concatenate the current tags into a comma-separated string for editing
        String currentTags = "";
        if (tagsContainer != null && tagsContainer.getChildCount() > 0) {
            for (int i = 0; i < tagsContainer.getChildCount(); i++) {
                TextView tagView = (TextView) tagsContainer.getChildAt(i);
                currentTags += tagView.getText().toString() + (i < tagsContainer.getChildCount() - 1 ? ", " : "");
            }
        }
        input.setText(currentTags.trim());
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
            // Update the clothing item in the database
            new ClothingManager(requireContext()).updateClothingItem(clothingId, newTags);

            // Ensure the tagsContainer is cleared and updated dynamically
            if (getView() == null) {
                Log.e(TAG, "View is not ready. Skipping tag update.");
                return;
            }

            tagsContainer.removeAllViews(); // Clear previous views

            String[] tagsArray = newTags != null ? newTags.split(",") : new String[0];

            for (String tag : tagsArray) {
                TextView tagView = new TextView(requireContext());
                tagView.setText(tag.trim());
                tagView.setTextSize(14);
                tagView.setTextColor(ResourcesCompat.getColor(getResources(), android.R.color.primary_text_dark, requireContext().getTheme()));
                tagView.setPadding(16, 8, 16, 8); // Padding for the oval shape
                tagView.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.tag_background, requireContext().getTheme()));

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(8, 4, 8, 4); // Margins between tags
                tagView.setLayoutParams(params);

                tagsContainer.addView(tagView); // Add tag to container
            }

            Toast.makeText(requireContext(), "Tags updated!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error updating clothing tags: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Failed to update tags.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddToCollectionFragment() {
        List<Collection> availableCollections = new CollectionsManager(requireContext())
                .getAvailableCollectionsForClothing(clothingId);

        if (availableCollections.isEmpty()) {
            Toast.makeText(requireContext(), "No available collections.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<SelectableItem> collectionItems = new ArrayList<>(availableCollections);
        boolean[] selectedItems = new boolean[availableCollections.size()];

        SelectionFragment fragment = SelectionFragment.newInstance(
                "Add to Collections",
                collectionItems,
                selectedItems
        );

        getChildFragmentManager().beginTransaction()
                .replace(R.id.selection_fragment_container, fragment, "SelectionFragment")
                .addToBackStack(null)
                .commit();

        // Show the selection fragment and hide all other content
        View rootView = getView();
        if (rootView != null) {
            rootView.findViewById(R.id.selection_fragment_container).setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.mainContent).setVisibility(View.GONE);
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
            // Fetch all collections
            List<Collection> collections = new CollectionsManager(requireContext()).getAllCollections();

            if (collections == null || collections.isEmpty()) {
                Toast.makeText(requireContext(), "No collections available.", Toast.LENGTH_SHORT).show();
                return;
            }

            SQLiteDatabase db = new DatabaseHelper(requireContext()).getWritableDatabase();
            db.beginTransaction(); // Start transaction for consistency

            try {
                // Iterate over selected collections
                for (int i = 0; i < selectedItems.length; i++) {
                    if (selectedItems[i]) {
                        int collectionId = collections.get(i).getId();

                        // Avoid duplicate assignments
                        if (!isClothingAlreadyAssigned(clothingId, collectionId, db)) {
                            ContentValues values = new ContentValues();
                            values.put("clothes_id", clothingId);
                            values.put("collection_id", collectionId);
                            db.insert("Clothes_Collections", null, values);

                            Log.d(TAG, "Assigned clothing ID: " + clothingId + " to collection ID: " + collectionId);
                        } else {
                            Log.d(TAG, "Clothing ID: " + clothingId + " already assigned to collection ID: " + collectionId);
                        }
                    }
                }

                db.setTransactionSuccessful(); // Commit transaction
            } finally {
                db.endTransaction(); // End transaction
            }

            Toast.makeText(requireContext(), "Clothing item added to selected collections!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error assigning clothing to collections: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "An error occurred while saving the selection.", Toast.LENGTH_SHORT).show();
        }

        // Refresh collections and UI
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).refreshCollections();
        }

        loadClothingDetails(); // Refresh the clothing details
        hideSelectionFragment(); // Restore the main content
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
        try {
            // Attempt to clear selections if the adapter is available
            Fragment fragment = getChildFragmentManager().findFragmentByTag("SelectionFragment");
            if (fragment != null && fragment.getView() != null) {
                RecyclerView recyclerView = fragment.getView().findViewById(R.id.selectionRecyclerView);
                if (recyclerView != null && recyclerView.getAdapter() instanceof SelectionAdapter) {
                    SelectionAdapter adapter = (SelectionAdapter) recyclerView.getAdapter();
                    adapter.clearSelections(); // Clear all selected items
                }
            }

            // Remove the fragment from the child fragment manager
            if (fragment != null) {
                getChildFragmentManager().beginTransaction()
                        .remove(fragment)
                        .commit();
            }

            // Restore visibility of all hidden views
            if (getView() != null) {
                View selectionContainer = getView().findViewById(R.id.selection_fragment_container);
                View mainContent = getView().findViewById(R.id.mainContent); // Includes backgroundClickableArea

                if (selectionContainer != null && mainContent != null) {
                    selectionContainer.setVisibility(View.GONE); // Hide the selection fragment
                    mainContent.setVisibility(View.VISIBLE); // Restore main content visibility
                }
            }

            // Notify MainActivity to refresh parent fragments
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).refreshCollections();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error hiding selection fragment: " + e.getMessage(), e);
        }
    }



    private void showEditOptions() {
        imageOverlay.setVisibility(View.VISIBLE);
        editImageButton.setVisibility(View.VISIBLE);
    }

    private void hideEditOptions() {
        imageOverlay.setVisibility(View.GONE);
        editImageButton.setVisibility(View.GONE);
    }

    private void openImagePicker() {
        if (imagePickerHelper != null) {
            imagePickerHelper.openImagePicker();

            // Hide edit options after opening the picker
            hideEditOptions();
        }
    }


    void navigateBack() {
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();

            String origin = getArguments() != null ? getArguments().getString("origin", "ClothesFragment") : "ClothesFragment";

            if ("CollectionDetailFragment".equals(origin)) {
                int collectionId = getArguments().getInt("collection_id", -1);
                String collectionName = getArguments().getString("collection_name");

                if (collectionId != -1 && collectionName != null) {
                    mainActivity.openCollectionDetail(collectionId, collectionName, origin);
                } else {
                    Log.e(TAG, "Missing collection ID or name for navigation.");
                }
            } else {
                mainActivity.navigateBackToClothesFragment();
            }
        }
    }


    private void deleteClothing() {
        try {
            // Delete the clothing item
            new ClothingManager(requireContext()).deleteClothingItem(clothingId);

            Toast.makeText(requireContext(), "Clothing item deleted!", Toast.LENGTH_SHORT).show();

            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();

                // Prepare arguments for MainActivity
                Bundle args = new Bundle();
                args.putInt("collection_id", getArguments().getInt("collection_id", -1));
                args.putString("collection_name", getArguments().getString("collection_name", ""));

                // Notify MainActivity
                mainActivity.handleClothingDeletion(originFragment, args);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting clothing item: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Failed to delete clothing item.", Toast.LENGTH_SHORT).show();
        }
    }



    public void reloadData() {
        try {
            Log.d(TAG, "Reloading clothing details...");
            loadClothingDetails(); // Reuse the existing method to reload data
        } catch (Exception e) {
            Log.e(TAG, "Error reloading clothing data: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Failed to reload clothing details.", Toast.LENGTH_SHORT).show();
        }
    }







}
