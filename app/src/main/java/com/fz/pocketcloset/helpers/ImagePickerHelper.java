package com.fz.pocketcloset.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.fz.pocketcloset.R;
import com.fz.pocketcloset.temporaryFragments.AddClothesFragment;
import com.fz.pocketcloset.temporaryFragments.CropImageFragment;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ImagePickerHelper {

    private static final String TAG = "ImagePickerHelper";

    private final Context context;
    private final DatabaseHelper dbHelper;
    private final Consumer<Void> onClothingAdded;
    private final int clothingId; // -1 for new items, > 0 for updating an existing item

    private final ActivityResultLauncher<Intent> pickImageLauncher;
    private List<Uri> originalImageUris; // Store all picked images

    public ImagePickerHelper(Context context, DatabaseHelper dbHelper, Consumer<Void> onClothingAdded, int clothingId, ActivityResultLauncher<Intent> pickImageLauncher) {
        this.context = context;
        this.dbHelper = dbHelper;
        this.onClothingAdded = onClothingAdded;
        this.clothingId = clothingId;
        this.pickImageLauncher = pickImageLauncher;
    }

    public void handleImageResult(Intent data) {
        if (data == null) {
            Toast.makeText(context, "No image selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (clothingId > 0) {
            if (data.getData() != null) {
                updateClothingImage(data.getData());
            } else {
                Toast.makeText(context, "Please select a single image for updating.", Toast.LENGTH_SHORT).show();
            }
        } else {
            List<Uri> imageUris = new ArrayList<>();
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    imageUris.add(data.getClipData().getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                imageUris.add(data.getData());
            }

            if (!imageUris.isEmpty()) {
                handleMultipleImages(imageUris);
            } else {
                Toast.makeText(context, "No images selected.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleMultipleImages(List<Uri> imageUris) {
        if (imageUris == null || imageUris.isEmpty()) {
            Toast.makeText(context, "No images selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        originalImageUris = imageUris;

        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;

            // Ensure dynamic_fragment_container is visible
            View container = activity.findViewById(R.id.dynamic_fragment_container);
            if (container != null) {
                container.setVisibility(View.VISIBLE);
            }

            // Clear existing fragments in the container
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.dynamic_fragment_container, CropImageFragment.newInstance(originalImageUris), "CropImageFragment")
                    .addToBackStack("CropImageFragment")
                    .commit();
        }
    }
    public void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");

        if (clothingId > 0) {
            pickImageLauncher.launch(intent);
        } else {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            pickImageLauncher.launch(intent);
        }
    }

    private void updateClothingImage(Uri selectedImageUri) {
        String privateImagePath = copyImageToPrivateStorage(context, selectedImageUri);
        if (privateImagePath != null) {
            try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
                ContentValues values = new ContentValues();
                values.put("imagePath", privateImagePath);

                int rowsUpdated = db.update("Clothes", values, "id = ?", new String[]{String.valueOf(clothingId)});
                if (rowsUpdated > 0) {
                    Toast.makeText(context, "Image updated successfully!", Toast.LENGTH_SHORT).show();
                    onClothingAdded.accept(null);
                } else {
                    Toast.makeText(context, "Failed to update image.", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating clothing image: " + e.getMessage(), e);
                Toast.makeText(context, "Error updating clothing image.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, "Failed to save image. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    public static String copyImageToPrivateStorage(Context context, Uri sourceUri) {
        try {
            String fileName = "clothing_" + System.currentTimeMillis() + ".png";
            File privateDir = context.getFilesDir();
            File destinationFile = new File(privateDir, fileName);

            Bitmap originalBitmap = BitmapFactory.decodeStream(
                    context.getContentResolver().openInputStream(sourceUri)
            );

            if (originalBitmap == null) {
                throw new IllegalArgumentException("Failed to decode image from Uri: " + sourceUri);
            }

            Mat sourceMat = new Mat();
            Utils.bitmapToMat(originalBitmap, sourceMat);

            Mat backgroundRemovedMat = BackgroundRemover.removeBackground(sourceMat);
            if (backgroundRemovedMat == null) {
                throw new RuntimeException("Background removal failed for image: " + sourceUri);
            }

            Bitmap backgroundRemovedBitmap = Bitmap.createBitmap(
                    backgroundRemovedMat.cols(), backgroundRemovedMat.rows(), Bitmap.Config.ARGB_8888
            );
            Utils.matToBitmap(backgroundRemovedMat, backgroundRemovedBitmap);

            try (FileOutputStream outputStream = new FileOutputStream(destinationFile)) {
                backgroundRemovedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            }

            Log.d(TAG, "Image with transparency saved to private storage: " + destinationFile.getAbsolutePath());
            return destinationFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Error saving image with transparency: " + e.getMessage(), e);
            return null;
        }
    }
}
