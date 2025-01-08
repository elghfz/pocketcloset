package com.fz.pocketcloset.temporaryFragments;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
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
import java.util.List;

public class AddClothesFragment extends Fragment {
    private ImageView imageView;
    private EditText tagsEditText;
    private ImageButton saveButton, cancelButton;
    private List<Uri> imageUris;
    private int currentIndex = 0;
    private DatabaseHelper dbHelper;
    private List<ContentValues> pendingItems; // To hold all pending clothing items

    private List<String> currentTags; // For dynamically added tags
    private List<String> suggestedTags; // Suggested tags from the database
    private TagSuggestionsAdapter tagSuggestionsAdapter; // Adapter for the RecyclerView
    private LinearLayout addedTagsContainer; // For displaying added tags

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
        RecyclerView tagSuggestionsRecyclerView = view.findViewById(R.id.tagSuggestionsRecyclerView);

        // Initialize tag lists
        currentTags = new ArrayList<>();
        suggestedTags = new ArrayList<>(fetchSuggestedTags()); // Fetch tags from DB or ClothingManager

        // Set up the tag suggestions RecyclerView
        tagSuggestionsAdapter = new TagSuggestionsAdapter(suggestedTags, tag -> addTag(tag));
        tagSuggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        tagSuggestionsRecyclerView.setAdapter(tagSuggestionsAdapter);

        // Add tag button click listener
        addTagButton.setOnClickListener(v -> {
            String inputText = tagsEditText.getText().toString().trim();
            if (!inputText.isEmpty()) {
                String[] tags = inputText.split(","); // Split input by commas
                for (String tag : tags) {
                    addTag(tag.trim()); // Add each tag individually after trimming
                }
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

    private void addTag(String tag) {
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
                if (currentTags.isEmpty()) {
                    addedTagsContainer.setVisibility(View.GONE);
                }
            });

            addedTagsContainer.addView(tagView);
            addedTagsContainer.setVisibility(View.VISIBLE);
        }
    }


    private List<String> fetchSuggestedTags() {
        return new ClothingManager(requireContext()).getAllTags(); // Use the existing implementation
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
            pendingItems.add(values); // Add to the pending list

            currentIndex++;
            displayCurrentImage(); // Show the next image or commit transaction if last
        } else {
            Toast.makeText(requireContext(), "Failed to save image. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void commitTransaction() {
        ClothingManager clothingManager = new ClothingManager(requireContext());
        try {
            // Use the addMultipleClothingItems method from the existing implementation
            List<String> tagsList = new ArrayList<>();
            for (ContentValues values : pendingItems) {
                tagsList.add(values.getAsString("tags"));
            }

            clothingManager.addMultipleClothingItems(imageUris, tagsList, requireContext());
            Toast.makeText(requireContext(), "All items saved successfully!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error saving items. Transaction aborted.", Toast.LENGTH_SHORT).show();
        } finally {
            hideFragment();
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
