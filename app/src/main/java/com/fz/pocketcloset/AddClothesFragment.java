package com.fz.pocketcloset;

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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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

        // Set up button listeners
        saveButton.setOnClickListener(v -> handleSaveClick());
        cancelButton.setOnClickListener(v -> cancelProcess());

        // Display the first image
        displayCurrentImage();

        return view;
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

            tagsEditText.setText(""); // Clear the tags field for a new item
        } else {
            commitTransaction(); // All items processed, commit the transaction
        }
    }


    private void handleSaveClick() {
        String tags = tagsEditText.getText().toString().trim();
        Uri currentUri = imageUris.get(currentIndex);

        if (tags.isEmpty()) {
            Toast.makeText(requireContext(), "Tags cannot be empty. Please enter tags.", Toast.LENGTH_SHORT).show();
            return;
        }

        String imagePath = ImagePickerHelper.copyImageToPrivateStorage(requireContext(), currentUri);

        if (imagePath != null) {
            ContentValues values = new ContentValues();
            values.put("tags", tags);
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
            // Use the addMultipleClothingItems method
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
