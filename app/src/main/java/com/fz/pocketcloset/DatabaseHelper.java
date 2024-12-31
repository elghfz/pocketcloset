package com.fz.pocketcloset;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    //Database info
    private static final String DATABASE_NAME = "ClosetApp.db";
    private static final int DATABASE_VERSION = 3;

    //Tables names
    private static final String TABLE_CLOTHES = "Clothes";
    private static final String TABLE_COLLECTIONS = "Collections";
    private static final String TABLE_OUTFITS = "Outfits";
    private static final String TABLE_CLOTHES_COLLECTIONS = "Clothes_Collections";
    private static final String TABLE_CLOTHES_OUTFITS = "Clothes_Outfits";

   //Table creation queries
   private static final String CREATE_TABLE_CLOTHES =
           "CREATE TABLE " + TABLE_CLOTHES + " (" +
                   "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                   "imagePath TEXT NOT NULL, " +
                   "tags TEXT" +
                   ");";

    // Collections Table Schema
    private static final String CREATE_TABLE_COLLECTIONS = "CREATE TABLE Collections (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT NOT NULL, " +
            "emoji TEXT DEFAULT '📁'" + // Add the emoji column here
            ");";


    // Outfits Table Schema
    private static final String CREATE_TABLE_OUTFITS =
            "CREATE TABLE " + TABLE_OUTFITS + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL, " +
                    "combinedImagePath TEXT" +
                    ");";

    // Clothes_Collections Table Schema
    private static final String CREATE_TABLE_CLOTHES_COLLECTIONS =
            "CREATE TABLE " + TABLE_CLOTHES_COLLECTIONS + " (" +
                    "clothes_id INTEGER NOT NULL, " +
                    "collection_id INTEGER NOT NULL, " +
                    "PRIMARY KEY (clothes_id, collection_id), " +
                    "FOREIGN KEY (clothes_id) REFERENCES Clothes(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (collection_id) REFERENCES Collections(id) ON DELETE CASCADE" +
                    ");";

    // Clothes_Outfits Table Schema
    private static final String CREATE_TABLE_CLOTHES_OUTFITS =
            "CREATE TABLE " + TABLE_CLOTHES_OUTFITS + " (" +
                    "clothes_id INTEGER NOT NULL, " +
                    "outfit_id INTEGER NOT NULL, " +
                    "PRIMARY KEY (clothes_id, outfit_id), " +
                    "FOREIGN KEY (clothes_id) REFERENCES Clothes(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (outfit_id) REFERENCES Outfits(id) ON DELETE CASCADE" +
                    ");";


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_CLOTHES);
        db.execSQL(CREATE_TABLE_COLLECTIONS);
        db.execSQL(CREATE_TABLE_OUTFITS);
        db.execSQL(CREATE_TABLE_CLOTHES_COLLECTIONS);
        db.execSQL(CREATE_TABLE_CLOTHES_OUTFITS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CLOTHES_OUTFITS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CLOTHES_COLLECTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_OUTFITS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COLLECTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CLOTHES);
        onCreate(db);
    }

}
