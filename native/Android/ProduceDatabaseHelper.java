package com.example.produce.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ProduceDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "produce_cache.db";
    private static final int DATABASE_VERSION = 1;

    public ProduceDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create table for offline caching
        db.execSQL(
            "CREATE TABLE produce_cache (" +
            "market_code TEXT PRIMARY KEY, " +
            "json_data TEXT, " +
            "updated_at INTEGER)"
        );
        
        // Create table for user favorites
        db.execSQL(
            "CREATE TABLE favorites (" +
            "produce_id TEXT PRIMARY KEY, " +
            "name TEXT, " +
            "target_price REAL)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS produce_cache");
        db.execSQL("DROP TABLE IF EXISTS favorites");
        onCreate(db);
    }

    // Save produce list to cache
    public void saveProduceList(java.util.List<com.example.produce.models.ProduceDto> list) {
        if (list == null || list.isEmpty()) return;
        
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // Clear old cache for simplicity in this demo, or use upsert logic
            db.delete("produce_cache", null, null);
            
            android.content.ContentValues values = new android.content.ContentValues();
            // We store the whole list as a single JSON blob for "ALL" or "daily-prices" cache
            // In a real app, you might normalize this.
            // Using "ALL" as a key for the main list
            values.put("market_code", "ALL"); 
            values.put("json_data", new com.google.gson.Gson().toJson(list));
            values.put("updated_at", System.currentTimeMillis());
            
            db.insertWithOnConflict("produce_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    // Get produce list from cache
    public java.util.List<com.example.produce.models.ProduceDto> getProduceList(String keyword, int page) {
        SQLiteDatabase db = this.getReadableDatabase();
        android.database.Cursor cursor = db.query("produce_cache", new String[]{"json_data"}, "market_code = ?", new String[]{"ALL"}, null, null, null);
        
        if (cursor != null && cursor.moveToFirst()) {
            String jsonData = cursor.getString(0);
            cursor.close();
            try {
                java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<java.util.List<com.example.produce.models.ProduceDto>>(){}.getType();
                return new com.google.gson.Gson().fromJson(jsonData, type);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (cursor != null) cursor.close();
        return null;
    }
}
