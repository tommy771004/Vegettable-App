package com.example.produceapp;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ProduceApiService {
    @GET("/api/produce")
    Call<List<ProduceItem>> getProduceData(@Query("market") String market);

    @GET("/api/markets")
    Call<List<String>> getMarkets();

    @GET("/api/produce/{id}/history")
    Call<List<PriceHistory>> getHistoryData(@Path("id") String id, @Query("currentPrice") double currentPrice);
}
