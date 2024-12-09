package com.fz.pocketcloset;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ClothingManager {

    private static final String TAG = "ClothingManager";
    private final DatabaseHelper dbHelper;

    public ClothingManager(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    /**
     * Retrieve all clothing items from the database.
     */
    public List<ClothingItem> getAllClothingItems() {
        List<ClothingItem> clothingList = new ArrayList<>();
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.rawQuery("SELECT * FROM Clothes", null)) {

            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String imagePath = cursor.getString(cursor.getColumnIndexOrThrow("imagePath"));
                String tags = cursor.getString(cursor.getColumnIndexOrThrow("tags"));
                clothingList.add(new ClothingItem(id, imagePath, tags));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching clothing items: " + e.getMessage(), e);
        }
        return clothingList;
    }

    /**
     * Add a new clothing item to the database.
     *
     * @param tags The tags/metadata for the new clothing item.
     */
    public void addClothingItem(String tags) {
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put("tags", tags);
            values.put("imagePath", ""); // Placeholder for imagePath, if not provided

            db.insert("Clothes", null, values);
        } catch (Exception e) {
            Log.e(TAG, "Error adding clothing item: " + e.getMessage(), e);
        }
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

    /**
     * Remove a clothing item from a specific collection.
     *
     * @param clothingItemId    The ID of the clothing item to remove.
     * @param collectionId      The ID of the collection from which to remove the item.
     */
    public void removeClothingFromCollection(int clothingItemId, int collectionId) {
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            db.delete("CollectionItems", "clothingId = ? AND collectionId = ?",
                    new String[]{String.valueOf(clothingItemId), String.valueOf(collectionId)});
        } catch (Exception e) {
            Log.e(TAG, "Error removing clothing item from collection: " + e.getMessage(), e);
        }
    }

    public DatabaseHelper getDbHelper() {
        return dbHelper;
    }
}
