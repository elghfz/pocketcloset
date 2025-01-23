package com.fz.pocketcloset.temporaryFragments;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;

import com.fz.pocketcloset.helpers.BackgroundRemover;
import com.google.android.flexbox.FlexboxLayout;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


import com.fz.pocketcloset.mainFragments.ClothingManager;
import com.fz.pocketcloset.helpers.DatabaseHelper;
import com.fz.pocketcloset.helpers.ImagePickerHelper;
import com.fz.pocketcloset.MainActivity;
import com.fz.pocketcloset.R;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
 //   private TagSuggestionsAdapter tagSuggestionsAdapter; // Adapter for the RecyclerView
    private FlexboxLayout addedTagsContainer; // For displaying added tags
    private FlexboxLayout tagSuggestionsContainer;



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
            if (imageUris == null || imageUris.isEmpty()) {
                Log.e("AddClothesFragment", "No image URIs found in arguments.");
                cancelProcess();
            }
        }
        dbHelper = new DatabaseHelper(requireContext());
        pendingItems = new ArrayList<>();
        suggestedTags = fetchSuggestedTags();
    }

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
        tagSuggestionsContainer = view.findViewById(R.id.tagSuggestionsContainer);

        // Initialize tag lists
        currentTags = new HashSet<>();
        suggestedTags = new ArrayList<>(fetchSuggestedTags());

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

        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (imageUris != null && !imageUris.isEmpty()) {
            displayCurrentImage();
        } else {
            Log.e("AddClothesFragment", "No images found to display.");
            Toast.makeText(requireContext(), "No images available.", Toast.LENGTH_SHORT).show();
            cancelProcess();
        }

        // Create and register callback for back press
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        cancelProcess();
                    }
                });
    }


    private void addTagToContainer(String tag) {
        if (!tag.isEmpty() && !currentTags.contains(tag)) {
            currentTags.add(tag);

            // Create a TextView for the new tag
            TextView tagView = new TextView(requireContext());
            tagView.setText(tag);
            tagView.setPadding(16, 8, 16, 8);
            tagView.setBackgroundResource(R.drawable.tag_background);
            tagView.setTextSize(14);

            tagView.setGravity(android.view.Gravity.CENTER);

            // Set layout params for Flexbox
            FlexboxLayout.LayoutParams layoutParams = new FlexboxLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            layoutParams.setMargins(8, 8, 8, 8);
            tagView.setLayoutParams(layoutParams);

            // Set up removal on click
            tagView.setOnClickListener(v -> {
                currentTags.remove(tag);
                addedTagsContainer.removeView(tagView);
                updateSuggestions(null);
                if (currentTags.isEmpty()) {
                    addedTagsContainer.setVisibility(View.GONE);
                }
            });

            // Add the tag to the container
            addedTagsContainer.addView(tagView);
            addedTagsContainer.setVisibility(View.VISIBLE);
        }
    }




    private void updateSuggestions(String tagToRemove) {
        tagSuggestionsContainer.removeAllViews(); // Clear current suggestions
        suggestedTags.clear(); // Clear the suggested tags list
        suggestedTags.addAll(fetchSuggestedTags()); // Fetch new suggestions from the database or pending items

        // Remove already added tags
        suggestedTags.removeAll(currentTags);

        // Optionally, remove the tag that was just added
        if (tagToRemove != null) {
            suggestedTags.remove(tagToRemove);
        }

        // Populate suggestions in the FlexboxLayout
        for (String tag : suggestedTags) {
            TextView suggestionView = new TextView(requireContext());
            suggestionView.setText(tag);
            suggestionView.setPadding(16, 8, 16, 8);
            suggestionView.setBackgroundResource(R.drawable.tag_background);
            suggestionView.setTextSize(14);

            // Set layout params for Flexbox
            FlexboxLayout.LayoutParams layoutParams = new FlexboxLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            layoutParams.setMargins(8, 8, 8, 8);
            suggestionView.setLayoutParams(layoutParams);

            // Add click listener to add the tag
            suggestionView.setOnClickListener(v -> {
                addTagToContainer(tag);
                updateSuggestions(tag);
            });

            tagSuggestionsContainer.addView(suggestionView);
        }
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

        return new ArrayList<>(combinedTags); // Return as a list
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
                Bitmap sourceBitmap = BitmapFactory.decodeStream(
                        requireContext().getContentResolver().openInputStream(currentUri)
                );

                if (sourceBitmap != null) {
                    imageView.setImageBitmap(sourceBitmap);
                } else {
                    Toast.makeText(requireContext(), "Failed to load image.", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Failed to load image.", Toast.LENGTH_SHORT).show();
                Log.e("AddClothesFragment", "Error loading image: " + e.getMessage(), e);
            }

            currentTags.clear();
            addedTagsContainer.removeAllViews();
            addedTagsContainer.setVisibility(View.GONE);

            // Refresh suggested tags
            suggestedTags = fetchSuggestedTags();
            updateSuggestions(null);
        } else {
            commitTransaction();
        }
    }

    private String saveUriToFile(Uri uri) {
        try {
            File outputDir = new File(requireContext().getFilesDir(), "processed_images");
            if (!outputDir.exists() && !outputDir.mkdirs()) {
                Log.e("AddClothesFragment", "Failed to create directory: " + outputDir.getAbsolutePath());
                return null;
            }

            File outputFile = new File(outputDir, "processed_" + System.currentTimeMillis() + ".png");
            Bitmap bitmap = BitmapFactory.decodeStream(requireContext().getContentResolver().openInputStream(uri));

            if (bitmap != null) {
                try (FileOutputStream out = new FileOutputStream(outputFile)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    return outputFile.getAbsolutePath();
                }
            }
        } catch (Exception e) {
            Log.e("AddClothesFragment", "Error saving Uri to file: " + e.getMessage(), e);
        }
        return null;
    }



    private void handleSaveClick() {
        if (currentTags.isEmpty()) {
            Toast.makeText(requireContext(), "Please add at least one tag.", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri currentUri = imageUris.get(currentIndex);

        // Save the image to a permanent directory
        String permanentPath = saveUriToFile(currentUri);

        if (permanentPath != null) {
            ContentValues values = new ContentValues();
            values.put("tags", String.join(",", currentTags)); // Save tags as a comma-separated string
            values.put("imagePath", permanentPath); // Save permanent file path

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
