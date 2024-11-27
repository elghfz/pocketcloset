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
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import java.util.function.Consumer;

public class ImagePickerHelper {

    private final Context context;
    private final DatabaseHelper dbHelper;
    private final Consumer<Void> onClothingAdded;

    private Uri selectedImageUri;

    private final ActivityResultLauncher<Intent> pickImageLauncher;

    public ImagePickerHelper(Context context, DatabaseHelper dbHelper, Consumer<Void> onClothingAdded) {
        this.context = context;
        this.dbHelper = dbHelper;
        this.onClothingAdded = onClothingAdded;

        pickImageLauncher = ((MainActivity) context).registerForActivityResult(
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
                saveClothingItem(tags, selectedImageUri.toString());
                onClothingAdded.accept(null); // Call the provided Consumer
                Toast.makeText(context, "Clothing item added successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Image is required.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
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
