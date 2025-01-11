package com.fz.pocketcloset.mainFragments;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.fz.pocketcloset.helpers.DatabaseHelper;
import com.fz.pocketcloset.items.ClothingItem;
import com.fz.pocketcloset.items.Outfit;

import java.util.ArrayList;
import java.util.List;

public class OutfitManager {
    private static final String TAG = "OutfitManager";
    private final DatabaseHelper dbHelper;

    public OutfitManager(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public List<Outfit> getAllOutfits() {
        List<Outfit> outfits = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT * FROM Outfits";
        try (Cursor cursor = db.rawQuery(query, null)) {
            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    String combinedImagePath = cursor.getString(cursor.getColumnIndexOrThrow("combinedImagePath"));

                    outfits.add(new Outfit(id, name, combinedImagePath));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching outfits: " + e.getMessage(), e);
        }

        return outfits;
    }

    public void addOutfit(String name, String imagePath, List<ClothingItem> clothes) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long outfitId = -1;

        try {
            db.beginTransaction();

            ContentValues values = new ContentValues();
            values.put("name", name);
            values.put("combinedImagePath", imagePath);
            outfitId = db.insert("Outfits", null, values);

            if (outfitId != -1) {
                for (ClothingItem clothing : clothes) {
                    ContentValues mappingValues = new ContentValues();
                    mappingValues.put("clothes_id", clothing.getId());
                    mappingValues.put("outfit_id", outfitId);
                    db.insert("Clothes_Outfits", null, mappingValues);
                }
            }

            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error adding outfit: " + e.getMessage(), e);
        } finally {
            db.endTransaction();
        }
    }

    public boolean deleteOutfit(int outfitId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int result = db.delete("Outfits", "id = ?", new String[]{String.valueOf(outfitId)});
        return result > 0; // Returns true if delete was successful
    }

    public boolean updateOutfit(int outfitId, String newName, String newCombinedImagePath) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", newName);
        values.put("combinedImagePath", newCombinedImagePath);

        int result = db.update("Outfits", values, "id = ?", new String[]{String.valueOf(outfitId)});
        return result > 0; // Returns true if update was successful
    }
}
