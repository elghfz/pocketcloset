package com.fz.pocketcloset;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public class AddClothesAdapter {
    private final List<Uri> imageUris;
    private final List<String> tagsList; // Store tags for each image
    private final DatabaseHelper dbHelper;
    private int currentIndex = 0; // Keep track of the current image being displayed
    private final Context context;

    public AddClothesAdapter(Context context, List<Uri> imageUris, DatabaseHelper dbHelper) {
        this.context = context;
        this.imageUris = imageUris;
        this.tagsList = new ArrayList<>(Collections.nCopies(imageUris.size(), ""));
        this.dbHelper = dbHelper;
    }

    public void bindCurrentItem(View itemView) {
        ImageView imageView = itemView.findViewById(R.id.imageView);
        EditText tagsEditText = itemView.findViewById(R.id.tagsEditText);
        Button saveButton = itemView.findViewById(R.id.saveButton);

        Uri currentUri = imageUris.get(currentIndex);
        imageView.setImageURI(currentUri);

        // Pre-fill tags if already edited
        tagsEditText.setText(tagsList.get(currentIndex));

        saveButton.setOnClickListener(v -> {
            String tags = tagsEditText.getText().toString().trim();
            tagsList.set(currentIndex, tags); // Update tags list for the current image
            Toast.makeText(context, "Tags saved! Proceeding to next item.", Toast.LENGTH_SHORT).show();
            showNextItem(itemView);
        });
    }

    private void showNextItem(View itemView) {
        currentIndex++;
        if (currentIndex < imageUris.size()) {
            bindCurrentItem(itemView); // Bind the next image
        } else {
            // Transaction is complete
            Toast.makeText(context, "All items saved!", Toast.LENGTH_SHORT).show();
            if (context instanceof AppCompatActivity) {
                ((AppCompatActivity) context).getSupportFragmentManager().popBackStack(); // Exit the fragment
            }
        }
    }

}

