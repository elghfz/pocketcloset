package com.fz.pocketcloset;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    public void addCollection(String name, String emoji) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("name", name);
            values.put("emoji", emoji);
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
            Cursor cursor = db.rawQuery("SELECT id, name, emoji FROM Collections", null); // Include the emoji column

            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String emoji = cursor.getString(cursor.getColumnIndexOrThrow("emoji")); // Fetch the emoji
                Collection collection = new Collection(id, name);
                collection.setEmoji(emoji); // Set the emoji for the collection
                collections.add(collection);
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

    public Collection getCollectionById(int collectionId) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        Collection collection = null;

        try {
            db = dbHelper.getReadableDatabase();
            String query = "SELECT id, name, emoji FROM Collections WHERE id = ?";
            cursor = db.rawQuery(query, new String[]{String.valueOf(collectionId)});

            if (cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String emoji = cursor.getString(cursor.getColumnIndexOrThrow("emoji"));

                collection = new Collection(id, name);
                collection.setEmoji(emoji);
            }
        } catch (Exception e) {
            Log.e("CollectionsManager", "Error fetching collection by ID: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }

        return collection;
    }


    public void updateCollectionEmoji(int collectionId, String newEmoji) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("emoji", newEmoji);

            int rowsUpdated = db.update("Collections", values, "id = ?", new String[]{String.valueOf(collectionId)});
            db.close();

            if (rowsUpdated > 0) {
                Log.d("CollectionsManager", "Emoji updated successfully for Collection ID: " + collectionId);
            } else {
                Log.d("CollectionsManager", "No collection found with ID: " + collectionId);
            }
        } catch (Exception e) {
            Log.e("CollectionsManager", "Error updating emoji: " + e.getMessage(), e);
        }
    }


    public List<ClothingItem> getClothesInCollection(int collectionId) {
        List<ClothingItem> clothes = new ArrayList<>();
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String query = "SELECT c.id, c.imagePath, c.tags FROM Clothes c " +
                    "JOIN Clothes_Collections cc ON c.id = cc.clothes_id " +
                    "WHERE cc.collection_id = ?";
            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(collectionId)});

            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String imagePath = cursor.getString(cursor.getColumnIndexOrThrow("imagePath"));
                String tags = cursor.getString(cursor.getColumnIndexOrThrow("tags"));
                clothes.add(new ClothingItem(id, imagePath, tags));
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            Log.e("CollectionsManager", "Error fetching clothes in collection: " + e.getMessage(), e);
        }
        return clothes;
    }

    public List<ClothingItem> getAvailableClothesForCollection(int collectionId) {
        List<ClothingItem> clothes = new ArrayList<>();
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String query = "SELECT * FROM Clothes WHERE id NOT IN (" +
                    "SELECT clothes_id FROM Clothes_Collections WHERE collection_id = ?)";
            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(collectionId)});

            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String imagePath = cursor.getString(cursor.getColumnIndexOrThrow("imagePath"));
                String tags = cursor.getString(cursor.getColumnIndexOrThrow("tags"));
                clothes.add(new ClothingItem(id, imagePath, tags));
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            Log.e("CollectionsManager", "Error fetching available clothes: " + e.getMessage(), e);
        }
        return clothes;
    }

    // Method to remove clothing from a specific collection (without deleting the item)
    public void removeClothingFromCollection(int clothingItemId, int collectionId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            // Delete the entry from Clothes_Collections table based on the clothingItemId and collectionId
            db.delete("Clothes_Collections",
                    "clothes_id = ? AND collection_id = ?",
                    new String[]{String.valueOf(clothingItemId), String.valueOf(collectionId)});
            Log.d("CollectionsManager", "Clothing removed from collection successfully.");
        } catch (Exception e) {
            Log.e("CollectionsManager", "Error removing clothing from collection: " + e.getMessage(), e);
        } finally {
            db.close();
        }
    }

    public List<Collection> getCollectionsForClothing(int clothingId) {
        List<Collection> collections = new ArrayList<>();
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String query = "SELECT c.id, c.name, c.emoji " +
                    "FROM Collections c " +
                    "INNER JOIN Clothes_Collections cc " +
                    "ON c.id = cc.collection_id " +
                    "WHERE cc.clothes_id = ?";
            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(clothingId)});

            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String emoji = cursor.getString(cursor.getColumnIndexOrThrow("emoji"));
                collections.add(new Collection(id, name, emoji));
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            Log.e(TAG, "Error fetching collections for clothing: " + e.getMessage(), e);
        }
        return collections;
    }


    public List<Collection> getAvailableCollectionsForClothing(int clothingId) {
        List<Collection> availableCollections = new ArrayList<>();

        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            // Query to get all collections
            String allCollectionsQuery = "SELECT * FROM Collections";
            Cursor allCollectionsCursor = db.rawQuery(allCollectionsQuery, null);

            // Query to get collections already assigned to the clothing item
            String assignedCollectionsQuery = "SELECT collection_id FROM Clothes_Collections WHERE clothes_id = ?";
            Cursor assignedCollectionsCursor = db.rawQuery(assignedCollectionsQuery, new String[]{String.valueOf(clothingId)});

            // Store assigned collection IDs in a set
            Set<Integer> assignedCollectionIds = new HashSet<>();
            if (assignedCollectionsCursor.moveToFirst()) {
                do {
                    assignedCollectionIds.add(assignedCollectionsCursor.getInt(0));
                } while (assignedCollectionsCursor.moveToNext());
            }
            assignedCollectionsCursor.close();

            // Iterate through all collections and add unassigned ones to the list
            if (allCollectionsCursor.moveToFirst()) {
                do {
                    @SuppressLint("Range") int collectionId = allCollectionsCursor.getInt(allCollectionsCursor.getColumnIndex("id"));
                    if (!assignedCollectionIds.contains(collectionId)) {
                        @SuppressLint("Range") String collectionName = allCollectionsCursor.getString(allCollectionsCursor.getColumnIndex("name"));
                        @SuppressLint("Range") String collectionEmoji = allCollectionsCursor.getString(allCollectionsCursor.getColumnIndex("emoji"));

                        Collection collection = new Collection(collectionId, collectionName, collectionEmoji);
                        availableCollections.add(collection);
                    }
                } while (allCollectionsCursor.moveToNext());
            }
            allCollectionsCursor.close();

            db.close();
        } catch (Exception e) {
            Log.e("CollectionsManager", "Error fetching available collections: " + e.getMessage(), e);
        }

        return availableCollections;
    }

    public void updateCollectionName(int collectionId, String newName) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("name", newName);

            int rowsUpdated = db.update("Collections", values, "id = ?", new String[]{String.valueOf(collectionId)});
            db.close();

            if (rowsUpdated > 0) {
                Log.d(TAG, "Collection name updated successfully for ID: " + collectionId);
            } else {
                Log.d(TAG, "No collection found with ID: " + collectionId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating collection name: " + e.getMessage(), e);
        }
    }

}
