package com.fz.pocketcloset.detailFragments;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import androidx.activity.OnBackPressedCallback;
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
import androidx.recyclerview.widget.RecyclerView;

import com.fz.pocketcloset.items.ClothingItem;
import com.fz.pocketcloset.items.Outfit;
import com.fz.pocketcloset.mainFragments.ClothingManager;
import com.fz.pocketcloset.items.Collection;
import com.fz.pocketcloset.mainFragments.CollectionAdapter;
import com.fz.pocketcloset.mainFragments.CollectionsManager;
import com.fz.pocketcloset.helpers.DatabaseHelper;
import com.fz.pocketcloset.helpers.ImagePickerHelper;
import com.fz.pocketcloset.MainActivity;
import com.fz.pocketcloset.R;
import com.fz.pocketcloset.items.SelectableItem;
import com.fz.pocketcloset.mainFragments.OutfitAdapter;
import com.fz.pocketcloset.mainFragments.OutfitManager;
import com.fz.pocketcloset.temporaryFragments.SelectionAdapter;
import com.fz.pocketcloset.temporaryFragments.SelectionFragment;
//import com.fz.pocketcloset.temporaryFragments.TagSuggestionsAdapter;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClothingDetailFragment extends Fragment implements SelectionFragment.SelectionListener {

    private static final String TAG = "ClothingDetailFragment";
    private ImagePickerHelper imagePickerHelper;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ImageView clothingImageView;
    private RecyclerView collectionsRecyclerView, outfitsRecyclerView;;
    private int clothingId;
    private String originFragment; // Tracks where the user came from
    private FlexboxLayout tagsContainer;
    private View imageOverlay;
    private ImageButton editImageButton;
    private View backgroundClickableArea;

    ImageButton addTagButton;



    private DatabaseHelper dbHelper;

    public void onResume() {
        super.onResume();
        reloadData(); // Ensure data is refreshed whenever the fragment becomes active
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get clothingId from arguments or saved instance state
        int clothingId = requireArguments().getInt("clothing_id", -1);

        // Initialize ImagePickerHelper with the clothingId for updating images
        imagePickerHelper = new ImagePickerHelper(
                requireContext(),
                new DatabaseHelper(requireContext()),
                unused -> {
                    // Refresh the fragment data after updating the image
                    loadClothingDetails();
                },
                clothingId,
                pickImageLauncher // The launcher will be registered next
        );

        // Register ActivityResultLauncher
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                        // Handle image result using ImagePickerHelper
                        imagePickerHelper.handleImageResult(result.getData());
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

            tagsContainer = view.findViewById(R.id.addedTagsContainer);
            clothingImageView = view.findViewById(R.id.clothingImageView);
            collectionsRecyclerView = view.findViewById(R.id.collectionsRecyclerView);
            outfitsRecyclerView = view.findViewById(R.id.outfitsRecyclerView);
            imageOverlay = view.findViewById(R.id.imageOverlay);
            editImageButton = view.findViewById(R.id.editImageButton);
            backgroundClickableArea = view.findViewById(R.id.backgroundClickableArea);

            GridLayoutManager collectionsLayoutManager = new GridLayoutManager(requireContext(), 3);
            GridLayoutManager outfitsLayoutManager = new GridLayoutManager(requireContext(), 3);
            collectionsRecyclerView.setLayoutManager(collectionsLayoutManager);
            outfitsRecyclerView.setLayoutManager(outfitsLayoutManager);

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

            ImageButton saveButton = view.findViewById(R.id.button_save);
            saveButton.setOnClickListener(v -> navigateBack());

            ImageButton addToCollectionButton = view.findViewById(R.id.button_add_to_collection);
            addToCollectionButton.setOnClickListener(v -> showAddToCollectionFragment());

            ImageButton deleteButton = view.findViewById(R.id.button_delete_clothing);
            deleteButton.setOnClickListener(v -> deleteClothing());

            ImageButton addTagButton = view.findViewById(R.id.addTagButton);
            addTagButton.setOnClickListener(v -> showTagsEditor());




        } catch (Exception e) {
            Log.e(TAG, "Error initializing ClothingDetailFragment: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error loading clothing details.", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Create and register callback for back press
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        navigateBack();
                    }
                });
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
                // Safely load image
                clothingImageView.setImageURI(clothing.getImagePath() != null ? Uri.parse(clothing.getImagePath()) : null);

                // Set tags dynamically
                String[] tags = clothing.getTags() != null ? clothing.getTags().split(",") : new String[0];
                tagsContainer.removeAllViews();
                for (String tag : tags) {
                    TextView tagView = new TextView(requireContext());
                    tagView.setText(tag.trim());
                    tagView.setTextSize(14);
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

                // Load collections
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

                //load outfits
                loadOutfitsForClothing();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading clothing details: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error loading clothing details.", Toast.LENGTH_SHORT).show();
        }
    }


    private void loadOutfitsForClothing() {
        try (SQLiteDatabase db = dbHelper.getReadableDatabase()) {
            String query = "SELECT o.id, o.name, o.combinedImagePath " +
                    "FROM Clothes_Outfits co " +
                    "JOIN Outfits o ON co.outfit_id = o.id " +
                    "WHERE co.clothes_id = ?";
            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(clothingId)});

            List<Outfit> outfits = new ArrayList<>();
            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    String combinedImagePath = cursor.getString(cursor.getColumnIndexOrThrow("combinedImagePath"));
                    outfits.add(new Outfit(id, name, combinedImagePath));
                } while (cursor.moveToNext());
            }
            cursor.close();

            // Set up the adapter and RecyclerView
            if (!outfits.isEmpty()) {
                OutfitAdapter outfitAdapter = new OutfitAdapter(
                        outfits,
                        outfit -> Toast.makeText(requireContext(), "Clicked on outfit: " + outfit.getName(), Toast.LENGTH_SHORT).show(),
                        outfit -> Toast.makeText(requireContext(), "Long-clicked outfit: " + outfit.getName(), Toast.LENGTH_SHORT).show()
                );
                outfitsRecyclerView.setAdapter(outfitAdapter);
                outfitsRecyclerView.setVisibility(View.VISIBLE); // Ensure RecyclerView is visible
            } else {
                outfitsRecyclerView.setVisibility(View.GONE); // Hide if no outfits are found
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading outfits for clothing: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Failed to load outfits.", Toast.LENGTH_SHORT).show();
        }
    }


    private void showTagsEditor() {
        if (getView() == null) return;

        View selectionContainer = getView().findViewById(R.id.selection_fragment_container);
        View mainContent = getView().findViewById(R.id.mainContent);

        // Hide main content and show selection container
        mainContent.setVisibility(View.GONE);
        selectionContainer.setVisibility(View.VISIBLE);

        // Inflate the tag editor layout
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View editorView = inflater.inflate(R.layout.inline_tag_editor, (ViewGroup) selectionContainer, false);

        ((ViewGroup) selectionContainer).removeAllViews();
        ((ViewGroup) selectionContainer).addView(editorView);

        // Initialize tag editor components
        ImageView imageView = editorView.findViewById(R.id.imageView);
        EditText newTagInput = editorView.findViewById(R.id.newTagInput);
        ImageButton addTagButton = editorView.findViewById(R.id.addTagButton);
        FlexboxLayout tagSuggestionsContainer = editorView.findViewById(R.id.tagSuggestionsContainer);
        FlexboxLayout addedTagsContainer = editorView.findViewById(R.id.addedTagsContainer);
        ImageButton saveButton = editorView.findViewById(R.id.saveButton);
        ImageButton cancelButton = editorView.findViewById(R.id.cancelButton);

        // Load clothing image into editor
        loadImageIntoEditor(imageView);

        // Step 1: Load existing tags into the container
        ClothingManager clothingManager = new ClothingManager(requireContext());
        ClothingItem clothing = clothingManager.getClothingById(clothingId);
        Set<String> addedTagsSet = new HashSet<>(); // Track added tags to exclude from suggestions

        if (clothing != null) {
            String[] existingTags = clothing.getTags() != null ? clothing.getTags().split(",") : new String[0];
            for (String tag : existingTags) {
                addTagToContainer(tag.trim(), addedTagsContainer, tagSuggestionsContainer);
                addedTagsSet.add(tag.trim());
            }
        }

        // Step 2: Initialize tag suggestions excluding already-added tags
        List<String> suggestedTags = clothingManager.getAllTags();
        List<String> filteredSuggestions = new ArrayList<>();
        for (String tag : suggestedTags) {
            if (!addedTagsSet.contains(tag)) {
                filteredSuggestions.add(tag);
            }
        }

        // Add suggestions to the FlexboxLayout
        for (String tag : filteredSuggestions) {
            TextView suggestionView = new TextView(requireContext());
            suggestionView.setText(tag);
            suggestionView.setPadding(16, 8, 16, 8);
            suggestionView.setBackgroundResource(R.drawable.tag_background);
            suggestionView.setTextSize(14);

            FlexboxLayout.LayoutParams layoutParams = new FlexboxLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            layoutParams.setMargins(8, 8, 8, 8);
            suggestionView.setLayoutParams(layoutParams);

            suggestionView.setOnClickListener(v -> {
                addTagToContainer(tag, addedTagsContainer, tagSuggestionsContainer);
                filteredSuggestions.remove(tag);
                tagSuggestionsContainer.removeView(suggestionView);
            });

            tagSuggestionsContainer.addView(suggestionView);
        }

        // Handle adding a new tag
        addTagButton.setOnClickListener(v -> {
            String tagText = newTagInput.getText().toString().trim();
            if (!tagText.isEmpty()) {
                addTagToContainer(tagText, addedTagsContainer, tagSuggestionsContainer);
                newTagInput.setText("");
            } else {
                Toast.makeText(requireContext(), "Tag cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        // Save tags
        saveButton.setOnClickListener(v -> {
            List<String> newTags = getTagsFromContainer(addedTagsContainer);
            updateClothingTags(String.join(",", newTags));
            hideTagsEditor();
        });

        // Cancel editing
        cancelButton.setOnClickListener(v -> hideTagsEditor());
    }



    private void addTagToContainer(String tag, FlexboxLayout container, @Nullable FlexboxLayout tagSuggestionsContainer) {
        // Check if the tag is already added
        for (int i = 0; i < container.getChildCount(); i++) {
            TextView existingTag = (TextView) container.getChildAt(i);
            if (existingTag.getText().toString().equalsIgnoreCase(tag)) {
                Toast.makeText(requireContext(), "Tag already added", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Create a new TextView for the tag
        TextView tagView = new TextView(requireContext());
        tagView.setText(tag); // Set the tag text
        tagView.setPadding(16, 8, 16, 8); // Padding inside the tag
        tagView.setBackgroundResource(R.drawable.tag_background); // Background drawable
        tagView.setTextSize(14); // Text size

        tagView.setGravity(android.view.Gravity.CENTER); // Center the text

        // Set layout params for Flexbox
        FlexboxLayout.LayoutParams layoutParams = new FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(8, 8, 8, 8); // Margins between tags
        tagView.setLayoutParams(layoutParams);

        // Set up removal on click
        tagView.setOnClickListener(v -> {
            container.removeView(tagView); // Remove the tag from the container

            // If tagSuggestionsContainer is provided, add the tag back to suggestions
            if (tagSuggestionsContainer != null) {
                TextView suggestionView = new TextView(requireContext());
                suggestionView.setText(tag);
                suggestionView.setPadding(16, 8, 16, 8);
                suggestionView.setBackgroundResource(R.drawable.tag_background);
                suggestionView.setTextSize(14);

                FlexboxLayout.LayoutParams suggestionLayoutParams = new FlexboxLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                suggestionLayoutParams.setMargins(8, 8, 8, 8);
                suggestionView.setLayoutParams(suggestionLayoutParams);

                suggestionView.setOnClickListener(suggestion -> {
                    addTagToContainer(tag, container, tagSuggestionsContainer);
                    tagSuggestionsContainer.removeView(suggestionView);
                });

                tagSuggestionsContainer.addView(suggestionView);
            }
        });

        // Add the tag to the FlexboxLayout
        container.addView(tagView);
        container.setVisibility(View.VISIBLE); // Ensure the container is visible
    }




    private List<String> getTagsFromContainer(FlexboxLayout container) {
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            TextView tagView = (TextView) container.getChildAt(i);
            tags.add(tagView.getText().toString().trim());
        }
        return tags;
    }



    private void loadImageIntoEditor(ImageView imageView) {
        try {
            ClothingManager clothingManager = new ClothingManager(requireContext());
            String imagePath = clothingManager.getClothingById(clothingId).getImagePath();
            if (imagePath != null) {
                imageView.setImageURI(Uri.parse(imagePath));
            } else {
                imageView.setImageResource(R.drawable.placeholder_clothing_item); // Fallback for no image
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading image into editor: " + e.getMessage(), e);
            imageView.setImageResource(R.drawable.placeholder_clothing_item);
        }
    }


    private void hideTagsEditor() {
        if (getView() == null) return;

        View selectionContainer = getView().findViewById(R.id.selection_fragment_container);
        View mainContent = getView().findViewById(R.id.mainContent);

        selectionContainer.setVisibility(View.GONE);
        mainContent.setVisibility(View.VISIBLE);
    }




    private void updateClothingTags(String newTags) {
        try {
            new ClothingManager(requireContext()).updateClothingItem(clothingId, newTags);
            reloadData();
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
        backgroundClickableArea.setOnClickListener(v -> hideEditOptions());
    }

    private void hideEditOptions() {
        imageOverlay.setVisibility(View.GONE);
        editImageButton.setVisibility(View.GONE);
        backgroundClickableArea.setOnClickListener(null);
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
            // Delete associated outfits
            List<Integer> linkedOutfitIds = new ArrayList<>();
            SQLiteDatabase db = new DatabaseHelper(requireContext()).getReadableDatabase();
            String query = "SELECT outfit_id FROM Clothes_Outfits WHERE clothes_id = ?";
            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(clothingId)});

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    linkedOutfitIds.add(cursor.getInt(0));
                }
                cursor.close();
            }

            // Delete the clothing item
            new ClothingManager(requireContext()).deleteClothingItem(clothingId);

            // Delete linked outfits
            for (int outfitId : linkedOutfitIds) {
                new OutfitManager(requireContext()).deleteOutfit(outfitId);
            }

            Toast.makeText(requireContext(), "Clothing item and associated outfits deleted!", Toast.LENGTH_SHORT).show();

            // Notify MainActivity and refresh the outfits fragment
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();

                // Prepare arguments for MainActivity
                Bundle args = new Bundle();
                args.putInt("collection_id", getArguments().getInt("collection_id", -1));
                args.putString("collection_name", getArguments().getString("collection_name", ""));

                // Notify MainActivity
                mainActivity.handleClothingDeletion(originFragment, args);
                mainActivity.refreshOutfitsFragment(); // Refresh the outfits fragment
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting clothing item: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Failed to delete clothing item.", Toast.LENGTH_SHORT).show();
        }
    }


    public void reloadData() {
        try {
            Log.d(TAG, "Reloading clothing details...");
            if (getView() != null) {
                tagsContainer.removeAllViews();
                loadClothingDetails();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reloading clothing data: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Failed to reload clothing details.", Toast.LENGTH_SHORT).show();
        }
    }



}
