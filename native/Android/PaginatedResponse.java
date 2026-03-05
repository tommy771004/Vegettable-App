package com.example.produce.models;

import java.util.List;

public class PaginatedResponse<T> {
    public int currentPage;
    public int totalPages;
    public int totalItems;
    public List<T> data;
}
