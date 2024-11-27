package com.fz.pocketcloset;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class CollectionsManager {

    private static final String TAG = "CollectionsManager";
    private final DatabaseHelper dbHelper;

    public CollectionsManager(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void addCollection(String name) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("name", name);
            db.insert("Collections", null, values);
            db.close();
            Log.d(TAG, "Collection added: " + name);
        } catch (Exception e) {
            Log.e(TAG, "Error adding collection: " + e.getMessage(), e);
        }
    }

    public List<Collection> getAllCollections() {
        List<Collection> collections = new ArrayList<>();
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM Collections", null);

            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                collections.add(new Collection(id, name));
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            Log.e(TAG, "Error fetching collections: " + e.getMessage(), e);
        }
        return collections;
    }

    public void assignClothingToCollection(int clothingId, int collectionId) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("clothes_id", clothingId);
            values.put("collection_id", collectionId);
            db.insert("Clothes_Collections", null, values);
            db.close();
            Log.d(TAG, "Clothing assigned to collection: clothingId=" + clothingId + ", collectionId=" + collectionId);
        } catch (Exception e) {
            Log.e(TAG, "Error assigning clothing to collection: " + e.getMessage(), e);
        }
    }

    public void deleteCollection(int id) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("Collections", "id = ?", new String[]{String.valueOf(id)});
            db.close();
            Log.d(TAG, "Collection deleted with ID: " + id);
        } catch (Exception e) {
            Log.e(TAG, "Error deleting collection: " + e.getMessage(), e);
        }
    }

    public void updateCollectionName(int id, String newName) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("name", newName);

            int rowsUpdated = db.update("Collections", values, "id = ?", new String[]{String.valueOf(id)});
            db.close();

            if (rowsUpdated > 0) {
                Log.d(TAG, "Collection updated successfully: ID " + id);
            } else {
                Log.d(TAG, "No collection found with ID: " + id);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating collection: " + e.getMessage(), e);
        }
    }
}
