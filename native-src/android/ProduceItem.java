package com.example.produceapp;

public class ProduceItem {
    public String id;
    public String name;
    public String category;
    public double currentPrice;
    public String level;

    public ProduceItem(String id, String name, String category, double currentPrice, String level) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.currentPrice = currentPrice;
        this.level = level;
    }
}
