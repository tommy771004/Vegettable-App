package com.example.produceapp.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

// 新增功能：離線快取機制 (Offline Caching)
// Room Database 定義，負責管理本地端快取資料庫
@Database(entities = {ProduceEntity.class}, version = 1, exportSchema = false)
public abstract class ProduceDatabase extends RoomDatabase {
    public abstract ProduceDao produceDao();
}
