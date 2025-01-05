package com.fz.pocketcloset;

import android.app.AlertDialog;
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
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.function.Consumer;

public class ImagePickerHelper {

    private static final String TAG = "ImagePickerHelper";

    private final Context context;
    private final DatabaseHelper dbHelper;
    private final Consumer<Void> onClothingAdded;
    private final int clothingId; // -1 for new items, > 0 for updating existing items

    private Uri selectedImageUri;

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
        pickImageLauncher.launch(intent);
    }

    public void handleImageResult(Uri selectedImageUri) {
        if (selectedImageUri != null) {
            if (clothingId > 0) {
                updateClothingImage(selectedImageUri);
            } else {
                // Show dialog for adding new item
                showAddClothingDetailsDialog(selectedImageUri);
            }
        }
    }

    private void showAddClothingDetailsDialog(Uri selectedImageUri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Add Clothing Item");

        View dialogView = ((MainActivity) context).getLayoutInflater().inflate(R.layout.dialog_add_clothes, null);
        EditText inputTags = dialogView.findViewById(R.id.input_tags);

        builder.setView(dialogView);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String tags = inputTags.getText().toString();

            if (selectedImageUri != null) {
                String privateImagePath = copyImageToPrivateStorage(selectedImageUri);
                if (privateImagePath != null) {
                    saveClothingItem(tags, privateImagePath);
                    onClothingAdded.accept(null);
                    Toast.makeText(context, "Clothing item added successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Failed to save image. Please try again.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Image is required.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }


    private void updateClothingImage(Uri selectedImageUri) {
        if (selectedImageUri != null) {
            String privateImagePath = copyImageToPrivateStorage(selectedImageUri);
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


    private String copyImageToPrivateStorage(Uri sourceUri) {
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

    private Bitmap addImageOnTransparentSquare(Bitmap sourceBitmap) {
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
