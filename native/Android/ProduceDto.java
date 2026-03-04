package com.example.produce.models;

import java.util.List;

// 統一的資料傳輸物件 (DTO)，確保前後端欄位完全一致
public class ProduceDto {
    public String cropCode;
    public String cropName;
    public String marketCode;
    public String marketName;
    public double avgPrice;
    public double transQuantity;
    public String date;
}

public class PaginatedResponse<T> {
    public int currentPage;
    public int totalPages;
    public int totalItems;
    public List<T> data;
}

// 新增功能：我的收藏與價格提醒 (My Favorites & Price Alerts) 的 DTO
public class FavoriteAlertDto {
    public String produceId;
    public String produceName;
    public double targetPrice;
    public double currentPrice;
    public boolean isAlertTriggered;
}
