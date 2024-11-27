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

    public List<ClothingItem> getAllClothingItems() {
        List<ClothingItem> clothingList = new ArrayList<>();
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM Clothes", null);

            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String imagePath = cursor.getString(cursor.getColumnIndexOrThrow("imagePath"));
                String tags = cursor.getString(cursor.getColumnIndexOrThrow("tags"));
                clothingList.add(new ClothingItem(id, imagePath, tags));
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            Log.e(TAG, "Error fetching clothing items: " + e.getMessage(), e);
        }
        return clothingList;
    }

    public void updateClothingItem(int id, String tags) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("tags", tags);

            db.update("Clothes", values, "id = ?", new String[]{String.valueOf(id)});
            db.close();
        } catch (Exception e) {
            Log.e(TAG, "Error updating clothing item: " + e.getMessage(), e);
        }
    }

    public void deleteClothingItem(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("Clothes", "id = ?", new String[]{String.valueOf(id)});
        db.close();
    }

}
