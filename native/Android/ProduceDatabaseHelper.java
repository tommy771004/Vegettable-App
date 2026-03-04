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
}
