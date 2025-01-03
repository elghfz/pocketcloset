package com.fz.pocketcloset;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
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

    private Uri selectedImageUri;

    private final ActivityResultLauncher<Intent> pickImageLauncher;

    public ImagePickerHelper(Fragment fragment, DatabaseHelper dbHelper, Consumer<Void> onClothingAdded) {
        this.context = fragment.requireContext();
        this.dbHelper = dbHelper;
        this.onClothingAdded = onClothingAdded;

        // Register the ActivityResultLauncher using the fragment's lifecycle
        pickImageLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        showAddClothingDetailsDialog();
                    }
                }
        );
    }

    public void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void showAddClothingDetailsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Add Clothing Item");

        View dialogView = ((MainActivity) context).getLayoutInflater().inflate(R.layout.dialog_add_clothes, null);
        EditText inputTags = dialogView.findViewById(R.id.input_tags);

        builder.setView(dialogView);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String tags = inputTags.getText().toString();

            if (selectedImageUri != null) {
                // Save the image to private storage and get the new URI
                String privateImagePath = copyImageToPrivateStorage(selectedImageUri);
                if (privateImagePath != null) {
                    saveClothingItem(tags, privateImagePath);
                    onClothingAdded.accept(null); // Call the provided Consumer
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

    private String copyImageToPrivateStorage(Uri sourceUri) {
        try {
            // Create a unique filename
            String fileName = "clothing_" + System.currentTimeMillis() + ".jpg";
            File privateDir = context.getFilesDir();
            File destinationFile = new File(privateDir, fileName);

            try (InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
                 FileOutputStream outputStream = new FileOutputStream(destinationFile)) {

                if (inputStream == null) {
                    throw new Exception("InputStream is null for the provided URI");
                }

                byte[] buffer = new byte[1024];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                Log.d(TAG, "Image saved to private storage: " + destinationFile.getAbsolutePath());
                return destinationFile.getAbsolutePath();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error copying image to private storage: " + e.getMessage(), e);
            return null;
        }
    }

    private void saveClothingItem(String tags, String imagePath) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("tags", tags);
        values.put("imagePath", imagePath); // Save the path to the copied image
        db.insert("Clothes", null, values);
        db.close();
    }
}
