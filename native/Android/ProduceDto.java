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
