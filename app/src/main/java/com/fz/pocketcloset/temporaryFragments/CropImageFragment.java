package com.fz.pocketcloset.temporaryFragments;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.canhub.cropper.CropImageView;
import com.fz.pocketcloset.R;
import com.fz.pocketcloset.helpers.ImagePickerHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CropImageFragment extends Fragment {

    private static final String TAG = "CropImageFragment";

    private static final String ARG_IMAGE_URIS = "imageUris";

    private CropImageView cropImageView;
    private View processingOverlay;
    Button cropButton;
    Button cancelButton;
    Button rotateButton;

    private Queue<Uri> imageQueue = new LinkedList<>();
    private List<Uri> croppedImageUris = new ArrayList<>();

    public static CropImageFragment newInstance(List<Uri> imageUris) {
        CropImageFragment fragment = new CropImageFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_IMAGE_URIS, new ArrayList<>(imageUris));
        fragment.setArguments(args);
        return fragment;
    }

    static {
        System.loadLibrary("opencv_java4");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "CropImageFragment: onCreateView called");
        View view = inflater.inflate(R.layout.fragment_crop_image, container, false);

        cropImageView = view.findViewById(R.id.cropImageView);
        processingOverlay = view.findViewById(R.id.progressOverlay);

        cropButton = view.findViewById(R.id.cropButton);
        cancelButton = view.findViewById(R.id.cancelButton);
        rotateButton = view.findViewById(R.id.rotateButton);

        if (getArguments() != null) {
            List<Uri> imageUris = getArguments().getParcelableArrayList(ARG_IMAGE_URIS);
            if (imageUris != null) {
                imageQueue.addAll(imageUris);
                loadNextImage();
            }
        }

        cropButton.setOnClickListener(v -> cropImage());
        cancelButton.setOnClickListener(v -> cancelCropping());
        rotateButton.setOnClickListener(v -> rotateImage());

        cropImageView.setFixedAspectRatio(true);
        cropImageView.setAspectRatio(1, 1);

        return view;
    }

    private void rotateImage() {
        Log.d(TAG, "rotateImage: Rotating image by 90 degrees");
        cropImageView.rotateImage(90);
    }

    private void loadNextImage() {
        if (!imageQueue.isEmpty()) {
            Uri nextImageUri = imageQueue.poll();
            cropImageView.setImageUriAsync(nextImageUri);
        } else {
            Log.d(TAG, "loadNextImage: All images cropped. Proceeding to background removal.");
            proceedToBackgroundRemoval();
        }
    }

    private void cropImage() {
        Bitmap croppedImage = cropImageView.getCroppedImage();

        if (croppedImage != null) {
            try {
                File croppedFile = new File(requireContext().getCacheDir(), "cropped_image_" + System.currentTimeMillis() + ".png");
                try (FileOutputStream out = new FileOutputStream(croppedFile)) {
                    croppedImage.compress(Bitmap.CompressFormat.PNG, 100, out);
                }

                Uri croppedUri = Uri.fromFile(croppedFile);
                croppedImageUris.add(croppedUri);

                Log.d(TAG, "cropImage: Cropped image saved to: " + croppedFile.getAbsolutePath());

                if (imageQueue.isEmpty()) {
                    // Last image, show processing screen and proceed
                    cropImageView.setVisibility(View.GONE);
                    cropButton.setVisibility(View.GONE);
                    cancelButton.setVisibility(View.GONE);
                    rotateButton.setVisibility(View.GONE);
                    proceedToBackgroundRemoval();
                } else {
                    // Load next image for cropping
                    loadNextImage();
                }
            } catch (Exception e) {
                Log.e(TAG, "cropImage: Error saving cropped image: " + e.getMessage(), e);
                Toast.makeText(requireContext(), "Failed to crop image.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "cropImage: Cropping failed, croppedImage is null");
            Toast.makeText(requireContext(), "Failed to crop image.", Toast.LENGTH_SHORT).show();
        }
    }

    private void cancelCropping() {
        Log.d(TAG, "cancelCropping: User canceled the cropping process");
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void proceedToBackgroundRemoval() {
        showProcessingScreen();

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            List<Uri> processedImageUris = new ArrayList<>();
            for (Uri uri : croppedImageUris) {
                String processedImagePath = ImagePickerHelper.copyImageToPrivateStorage(requireContext(), uri);
                if (processedImagePath != null) {
                    processedImageUris.add(Uri.fromFile(new File(processedImagePath)));
                }
            }

            requireActivity().runOnUiThread(() -> {
                hideProcessingScreen();

                if (!processedImageUris.isEmpty()) {
                    moveToAddClothesFragment(processedImageUris);
                } else {
                    Log.e(TAG, "No processed images found. Aborting transition.");
                    Toast.makeText(requireContext(), "Failed to process images.", Toast.LENGTH_SHORT).show();
                }
            });
        });
        executorService.shutdown();
    }


    private void moveToAddClothesFragment(List<Uri> processedImageUris) {
        if (processedImageUris != null && !processedImageUris.isEmpty()) {
            AddClothesFragment fragment = AddClothesFragment.newInstance(processedImageUris);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.dynamic_fragment_container, fragment, "AddClothesFragment")
                    .addToBackStack(null)
                    .commit();
        } else {
            Log.e(TAG, "No processed images to send to AddClothesFragment.");
            Toast.makeText(requireContext(), "Failed to move to AddClothesFragment.", Toast.LENGTH_SHORT).show();
        }
    }


    private void showProcessingScreen() {
        if (isAdded() && processingOverlay != null) {
            processingOverlay.setVisibility(View.VISIBLE);
        }
    }

    private void hideProcessingScreen() {
        if (isAdded() && processingOverlay != null) {
            processingOverlay.setVisibility(View.GONE);
        }
    }

}
