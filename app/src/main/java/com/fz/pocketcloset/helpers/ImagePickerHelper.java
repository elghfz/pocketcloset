package com.fz.pocketcloset.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.fz.pocketcloset.R;
import com.fz.pocketcloset.temporaryFragments.AddClothesFragment;

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

    public ImagePickerHelper(Context context, DatabaseHelper dbHelper, Consumer<Void> onClothingAdded, int clothingId, ActivityResultLauncher<Intent> pickImageLauncher) {
        this.context = context;
        this.dbHelper = dbHelper;
        this.onClothingAdded = onClothingAdded;
        this.clothingId = clothingId;
        this.pickImageLauncher = pickImageLauncher;
    }

    public void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");

        if (clothingId > 0) {
            // Restrict to a single image if updating
            pickImageLauncher.launch(intent);
        } else {
            // Allow multiple images if adding new clothing items
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            pickImageLauncher.launch(intent);
        }
    }


    public void handleImageResult(Intent data) {
        if (data == null) {
            Toast.makeText(context, "No image selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (clothingId > 0) {
            // Update logic for a single image
            if (data.getData() != null) {
                updateClothingImage(data.getData());
            } else {
                Toast.makeText(context, "Please select a single image for updating.", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Add logic for multiple images
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
        if (imageUris.isEmpty()) {
            Toast.makeText(context, "No images selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ensure the context is an instance of AppCompatActivity
        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;

            // Replace the fragment using the activity's FragmentManager
            AddClothesFragment fragment = AddClothesFragment.newInstance(imageUris);
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.dynamic_fragment_container, fragment, "AddClothesFragment")
                    .addToBackStack(null)
                    .commit();

            // Make the container visible if it's part of the current activity's layout
            View container = activity.findViewById(R.id.dynamic_fragment_container);
            if (container != null) {
                container.setVisibility(View.VISIBLE);
            }
        } else {
            Log.e(TAG, "Context is not an instance of AppCompatActivity. Cannot handle multiple images.");
        }
    }


    private void updateClothingImage(Uri selectedImageUri) {
        if (selectedImageUri != null) {
            // Pass the context explicitly to the copyImageToPrivateStorage method
            String privateImagePath = ImagePickerHelper.copyImageToPrivateStorage(context, selectedImageUri);
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
        } else {
            Toast.makeText(context, "Image is required.", Toast.LENGTH_SHORT).show();
        }
    }


    public static String copyImageToPrivateStorage(Context context, Uri sourceUri) {
        try {
            String fileName = "clothing_" + System.currentTimeMillis() + ".png";
            File privateDir = context.getFilesDir();
            File destinationFile = new File(privateDir, fileName);

            Bitmap sourceBitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(sourceUri));
            Bitmap outputBitmap = addImageOnTransparentSquare(sourceBitmap);

            try (FileOutputStream outputStream = new FileOutputStream(destinationFile)) {
                outputBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            }

            Log.d(TAG, "Image saved to private storage: " + destinationFile.getAbsolutePath());
            return destinationFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Error copying image to private storage: " + e.getMessage(), e);
            return null;
        }
    }


    public static Bitmap addImageOnTransparentSquare(Bitmap sourceBitmap) {
        int squareSize = Math.max(sourceBitmap.getWidth(), sourceBitmap.getHeight());
        Bitmap outputBitmap = Bitmap.createBitmap(squareSize, squareSize, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(outputBitmap);
        canvas.drawARGB(0, 0, 0, 0);

        int left = (squareSize - sourceBitmap.getWidth()) / 2;
        int top = (squareSize - sourceBitmap.getHeight()) / 2;

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        canvas.drawBitmap(sourceBitmap, left, top, paint);

        return outputBitmap;
    }

    private void saveClothingItem(String tags, String imagePath) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("tags", tags);
        values.put("imagePath", imagePath);
        db.insert("Clothes", null, values);
        db.close();
    }
}
