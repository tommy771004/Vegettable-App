package com.example.produceapp.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

// 新增功能：離線快取機制 (Offline Caching)
// Room Database Entity，用於在無網路時快取「今日熱門農產品」與「我的收藏」
@Entity(tableName = "produce_cache")
public class ProduceEntity {
    @PrimaryKey
    @NonNull
    public String cropCode;
    
    public String cropName;
    public String marketCode;
    public String marketName;
    public double avgPrice;
    public double transQuantity;
    public String date;
    
    // 標記此資料屬於哪種快取 (例如: "TOP_VOLUME", "FAVORITE")
    public String cacheType; 
}
