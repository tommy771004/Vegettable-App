package com.example.produce.data;

// 新增功能：價格異常警告 (Price Anomaly Detection) 的 DTO
public class PriceAnomalyDto {
    public String cropCode;
    public String cropName;
    public double currentPrice;
    public double previousPrice;
    public double increasePercentage;
    public String alertMessage;
}
