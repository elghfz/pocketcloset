package com.fz.pocketcloset.temporaryFragments;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fz.pocketcloset.mainFragments.ClothingManager;
import com.fz.pocketcloset.helpers.DatabaseHelper;
import com.fz.pocketcloset.helpers.ImagePickerHelper;
import com.fz.pocketcloset.MainActivity;
import com.fz.pocketcloset.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AddClothesFragment extends Fragment {
    private ImageView imageView;
    private EditText tagsEditText;
    private ImageButton saveButton, cancelButton;
    private List<Uri> imageUris;
    private int currentIndex = 0;
    private DatabaseHelper dbHelper;
    private List<ContentValues> pendingItems; // To hold all pending clothing items

    private Set<String> currentTags; // For dynamically added tags
    private List<String> suggestedTags; // Suggested tags from the database
    private TagSuggestionsAdapter tagSuggestionsAdapter; // Adapter for the RecyclerView
    private LinearLayout addedTagsContainer; // For displaying added tags
    private RecyclerView tagSuggestionsRecyclerView;

    public static AddClothesFragment newInstance(List<Uri> imageUris) {
        AddClothesFragment fragment = new AddClothesFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList("imageUris", new ArrayList<>(imageUris));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            imageUris = getArguments().getParcelableArrayList("imageUris");
        }
        dbHelper = new DatabaseHelper(requireContext());
        pendingItems = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_clothes, container, false);

        // Initialize UI elements
        imageView = view.findViewById(R.id.imageView);
        tagsEditText = view.findViewById(R.id.tagsEditText);
        saveButton = view.findViewById(R.id.saveButton);
        cancelButton = view.findViewById(R.id.cancelButton);
        ImageButton addTagButton = view.findViewById(R.id.addTagButton);
        addedTagsContainer = view.findViewById(R.id.addedTagsContainer);
        tagSuggestionsRecyclerView = view.findViewById(R.id.tagSuggestionsRecyclerView);

        // Initialize tag lists
        currentTags = new HashSet<>();
        suggestedTags = new ArrayList<>(fetchSuggestedTags()); // Fetch tags from DB or ClothingManager

        // Set up the tag suggestions RecyclerView
        tagSuggestionsAdapter = new TagSuggestionsAdapter(suggestedTags, tag -> {
            addTagToContainer(tag);
            updateSuggestions(tag);
        });
        tagSuggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        tagSuggestionsRecyclerView.setAdapter(tagSuggestionsAdapter);

        // Add tag button click listener
        addTagButton.setOnClickListener(v -> {
            String inputText = tagsEditText.getText().toString().trim();
            if (!inputText.isEmpty()) {
                addTagToContainer(inputText);
                updateSuggestions(inputText);
                tagsEditText.setText(""); // Clear the input field
            } else {
                Toast.makeText(requireContext(), "Tag cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        // Set up button listeners
        saveButton.setOnClickListener(v -> handleSaveClick());
        cancelButton.setOnClickListener(v -> cancelProcess());

        // Display the first image
        displayCurrentImage();

        return view;
    }

    private void addTagToContainer(String tag) {
        if (!tag.isEmpty() && !currentTags.contains(tag)) {
            currentTags.add(tag);

            // Create a TextView for the new tag
            TextView tagView = new TextView(requireContext());
            tagView.setText(tag);
            tagView.setPadding(16, 8, 16, 8);
            tagView.setBackgroundResource(R.drawable.tag_background);

            // Set up removal on click
            tagView.setOnClickListener(v -> {
                currentTags.remove(tag);
                addedTagsContainer.removeView(tagView);
                updateSuggestions(null); // Refresh suggestions
                if (currentTags.isEmpty()) {
                    addedTagsContainer.setVisibility(View.GONE);
                }
            });

            addedTagsContainer.addView(tagView);
            addedTagsContainer.setVisibility(View.VISIBLE);
        }
    }

    private void updateSuggestions(String tagToRemove) {
        // Re-fetch combined tags from the database and pending items
        suggestedTags.clear();
        suggestedTags.addAll(fetchSuggestedTags());

        // Remove already added tags from suggestions
        suggestedTags.removeAll(currentTags);

        // Optionally, remove the tag that was just added
        if (tagToRemove != null) {
            suggestedTags.remove(tagToRemove);
        }

        // Notify the adapter about changes
        tagSuggestionsAdapter.notifyDataSetChanged();
    }


    private List<String> fetchSuggestedTags() {
        Set<String> combinedTags = new HashSet<>();

        // Fetch tags from the database
        combinedTags.addAll(new ClothingManager(requireContext()).getAllTags());

        // Fetch tags from pending items
        for (ContentValues values : pendingItems) {
            String tags = values.getAsString("tags");
            if (tags != null && !tags.isEmpty()) {
                Collections.addAll(combinedTags, tags.split(","));
            }
        }

        return new ArrayList<>(combinedTags);
    }

    private void commitTransaction() {
        ClothingManager clothingManager = new ClothingManager(requireContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction(); // Start a transaction to ensure atomicity

        try {
            for (ContentValues values : pendingItems) {
                String imagePath = values.getAsString("imagePath");
                String tags = values.getAsString("tags");

                ContentValues clothingValues = new ContentValues();
                clothingValues.put("imagePath", imagePath);
                clothingValues.put("tags", tags);

                // Insert the clothing item into the database
                db.insertOrThrow("CLOTHES", null, clothingValues);
            }

            db.setTransactionSuccessful(); // Mark the transaction as successful
            Toast.makeText(requireContext(), "All items saved successfully!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("AddClothesFragment", "Error committing transaction: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error saving items. Transaction aborted.", Toast.LENGTH_SHORT).show();
        } finally {
            db.endTransaction(); // End the transaction (commit or rollback)
        }

        pendingItems.clear(); // Clear the pending items list
        hideFragment(); // Close the fragment after saving
    }


    private void displayCurrentImage() {
        if (currentIndex < imageUris.size()) {
            Uri currentUri = imageUris.get(currentIndex);

            try {
                // Load the image as a Bitmap
                Bitmap sourceBitmap = BitmapFactory.decodeStream(requireContext().getContentResolver().openInputStream(currentUri));

                // Process the Bitmap using addImageOnTransparentSquare
                Bitmap processedBitmap = ImagePickerHelper.addImageOnTransparentSquare(sourceBitmap);

                // Display the processed Bitmap
                imageView.setImageBitmap(processedBitmap);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Failed to load image.", Toast.LENGTH_SHORT).show();
            }

            // Reset tags for the new item
            currentTags.clear();
            addedTagsContainer.removeAllViews();
            addedTagsContainer.setVisibility(View.GONE);
        } else {
            commitTransaction(); // All items processed, commit the transaction
        }
    }

    private void handleSaveClick() {
        if (currentTags.isEmpty()) {
            Toast.makeText(requireContext(), "Please add at least one tag.", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri currentUri = imageUris.get(currentIndex);
        String imagePath = ImagePickerHelper.copyImageToPrivateStorage(requireContext(), currentUri);

        if (imagePath != null) {
            ContentValues values = new ContentValues();
            values.put("tags", String.join(",", currentTags)); // Save tags as a comma-separated string
            values.put("imagePath", imagePath);

            // Add to pending items
            pendingItems.add(values);

            currentIndex++;
            displayCurrentImage(); // Show the next image or commit transaction if last

            // Refresh suggestions to include tags from this saved item
            updateSuggestions(null);
        } else {
            Toast.makeText(requireContext(), "Failed to save image. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }


    private void cancelProcess() {
        pendingItems.clear(); // Discard all pending items
        Toast.makeText(requireContext(), "Process cancelled.", Toast.LENGTH_SHORT).show();
        hideFragment();
    }

    private void hideFragment() {
        // Hide the dynamic container
        View container = requireActivity().findViewById(R.id.dynamic_fragment_container);
        if (container != null) {
            container.setVisibility(View.GONE);
        }

        // Call the refreshClothingList method of MainActivity
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).refreshClothingList();
        }

        // Exit the fragment
        requireActivity().getSupportFragmentManager().popBackStack();
    }
}
