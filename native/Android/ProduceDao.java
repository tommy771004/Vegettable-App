package com.example.produceapp.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

// 新增功能：離線快取機制 (Offline Caching)
// Room Database DAO，提供對快取資料的存取方法
@Dao
public interface ProduceDao {
    @Query("SELECT * FROM produce_cache WHERE cacheType = :type")
    List<ProduceEntity> getCachedProduceByType(String type);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ProduceEntity> produceList);

    @Query("DELETE FROM produce_cache WHERE cacheType = :type")
    void clearCacheByType(String type);
}
