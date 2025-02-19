package com.fz.pocketcloset.mainFragments;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.fz.pocketcloset.helpers.DatabaseHelper;
import com.fz.pocketcloset.helpers.ImagePickerHelper;
import com.fz.pocketcloset.items.ClothingItem;

import java.util.ArrayList;
import java.util.List;

public class ClothingManager {

    private static final String TAG = "ClothingManager";
    private final DatabaseHelper dbHelper;

    public ClothingManager(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    /**
     * Add multiple clothing items to the database.
     *
     * @param imageUris List of image URIs to be saved.
     * @param tags List of tags corresponding to each URI.
     * @param context The context required for file storage.
     */
    public void addMultipleClothingItems(List<Uri> imageUris, List<String> tags, Context context) {
        if (imageUris.size() != tags.size()) {
            Log.e(TAG, "Mismatch between image URIs and tags count.");
            return;
        }

        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            db.beginTransaction();
            try {
                for (int i = 0; i < imageUris.size(); i++) {
                    Uri imageUri = imageUris.get(i);
                    String tag = tags.get(i);
                    String imagePath = ImagePickerHelper.copyImageToPrivateStorage(context, imageUri);

                    if (imagePath != null) {
                        ContentValues values = new ContentValues();
                        values.put("imagePath", imagePath);
                        values.put("tags", tag);

                        db.insert("Clothes", null, values);
                    } else {
                        Log.e(TAG, "Failed to save image for URI: " + imageUri.toString());
                    }
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding multiple clothing items: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieve all clothing items from the database.
     */
    public List<ClothingItem> getAllClothingItems() {
        List<ClothingItem> clothingList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM Clothes", null);

        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") int id = cursor.getInt(cursor.getColumnIndex("id"));
                @SuppressLint("Range") String imagePath = cursor.getString(cursor.getColumnIndex("imagePath"));
                @SuppressLint("Range") String tags = cursor.getString(cursor.getColumnIndex("tags")); // Ensure tags are fetched

                clothingList.add(new ClothingItem(id, imagePath, tags));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return clothingList;
    }

    /**
     * Update an existing clothing item in the database.
     *
     * @param id   The ID of the clothing item to update.
     * @param tags The new tags/metadata for the clothing item.
     */
    public void updateClothingItem(int id, String tags) {
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put("tags", tags);

            db.update("Clothes", values, "id = ?", new String[]{String.valueOf(id)});
        } catch (Exception e) {
            Log.e(TAG, "Error updating clothing item: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a clothing item from the database by ID.
     *
     * @param id The ID of the clothing item to delete.
     */
    public void deleteClothingItem(int id) {
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            db.delete("Clothes", "id = ?", new String[]{String.valueOf(id)});
        } catch (Exception e) {
            Log.e(TAG, "Error deleting clothing item: " + e.getMessage(), e);
        }
    }

    /**
     * Delete multiple clothing items from the database by their IDs.
     *
     * @param ids The list of IDs of the clothing items to delete.
     */
    public void deleteMultipleItems(List<Integer> ids) {
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            db.beginTransaction();
            try {
                for (int id : ids) {
                    db.delete("Clothes", "id = ?", new String[]{String.valueOf(id)});
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting multiple items: " + e.getMessage(), e);
        }
    }


    public ClothingItem getClothingById(int clothingId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ClothingItem clothingItem = null;

        String query = "SELECT * FROM Clothes WHERE id = ?";
        try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(clothingId)})) {
            if (cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String imagePath = cursor.getString(cursor.getColumnIndexOrThrow("imagePath"));
                String tags = cursor.getString(cursor.getColumnIndexOrThrow("tags"));

                clothingItem = new ClothingItem(id, imagePath, tags);
            }
        } catch (Exception e) {
            Log.e("ClothingManager", "Error fetching clothing by ID: " + e.getMessage(), e);
        } finally {
            db.close();
        }

        return clothingItem;
    }

    public List<String> getAllTags() {
        List<String> tags = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.rawQuery("SELECT DISTINCT tags FROM Clothes", null)) {
            while (cursor.moveToNext()) {
                String tagsString = cursor.getString(0);
                if (tagsString != null && !tagsString.isEmpty()) {
                    String[] splitTags = tagsString.split(","); // Assuming tags are comma-separated
                    for (String tag : splitTags) {
                        String trimmedTag = tag.trim();
                        if (!tags.contains(trimmedTag)) {
                            tags.add(trimmedTag);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("ClothingManager", "Error fetching tags: " + e.getMessage(), e);
        }

        return tags;
    }


}
