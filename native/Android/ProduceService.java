package com.example.produce.data;

import com.example.produce.models.PaginatedResponse;
import com.example.produce.models.ProduceDto;
import com.example.produce.network.AuthInterceptor;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;

public class ProduceService {
    private static final String BASE_URL = "https://api.yourbackend.com/api/produce";
    
    // 邏輯修正：將 AuthInterceptor 注入 OkHttpClient，自動帶上 X-User-Id
    private OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(new AuthInterceptor("user-device-uuid-12345"))
            .build();
            
    private Gson gson = new Gson();

    public interface ProduceDataCallback {
        void onSuccess(PaginatedResponse<ProduceDto> response);
        void onFailure(Exception e);
    }

    public void fetchProduceData(String keyword, int page, ProduceDataCallback callback) {
        String url = BASE_URL + "/daily-prices?keyword=" + (keyword != null ? keyword : "") + "&page=" + page + "&pageSize=20";
        Request request = new Request.Builder().url(url).build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onFailure(new IOException("Unexpected code " + response));
                    return;
                }
                
                String responseData = response.body().string();
                
                try {
                    Type type = new TypeToken<PaginatedResponse<ProduceDto>>(){}.getType();
                    PaginatedResponse<ProduceDto> result = gson.fromJson(responseData, type);
                    callback.onSuccess(result);
                } catch (Exception e) {
                    callback.onFailure(e);
                }
            }
        });
    }

    public void fetchPriceHistory(String produceId, Callback callback) {
        String url = BASE_URL + "/history/" + produceId;
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(callback);
    }

    // 新增功能：市場比價 (Market Comparison)
    public void comparePrices(String cropName, Callback callback) {
        String url = BASE_URL + "/compare/" + cropName;
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(callback);
    }

    // 新增功能：價格預測與趨勢分析 (Price Forecasting)
    public void getForecast(String produceId, Callback callback) {
        String url = BASE_URL + "/forecast/" + produceId;
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(callback);
    }

    // 新增功能：熱門交易農產品 (Top Volume Crops)
    public void getTopVolumeCrops(Callback callback) {
        String url = BASE_URL + "/top-volume";
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(callback);
    }

    // 邏輯修正：移除 body 中的 userId，因為已經透過 AuthInterceptor 放在 Header 中了
    public void syncFavorite(String produceId, double targetPrice, Callback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("produceId", produceId);
            json.put("targetPrice", targetPrice);

            RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(BASE_URL + "/favorites")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 新增功能：我的收藏與價格提醒 (My Favorites & Price Alerts)
    public void getFavorites(Callback callback) {
        String url = BASE_URL + "/favorites";
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(callback);
    }

    // 新增功能：移除收藏 (Remove Favorite)
    public void removeFavorite(String produceId, Callback callback) {
        String url = BASE_URL + "/favorites/" + produceId;
        Request request = new Request.Builder().url(url).delete().build();
        client.newCall(request).enqueue(callback);
    }

    // 新增功能：社群回報機制 (Community Retail Price)
    public void reportCommunityPrice(CommunityPriceDto priceDto, Callback callback) {
        try {
            String json = gson.toJson(priceDto);
            RequestBody body = RequestBody.create(
                json,
                MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(BASE_URL + "/community-price")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getCommunityPrices(String cropCode, Callback callback) {
        String url = BASE_URL + "/community-price/" + cropCode;
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(callback);
    }

    // 新增功能：當季盛產日曆 (Seasonal Crop Calendar)
    public void getSeasonalCrops(Callback callback) {
        String url = BASE_URL + "/seasonal";
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(callback);
    }

    // 新增功能：價格異常警告 (Price Anomaly Detection)
    public void getPriceAnomalies(Callback callback) {
        String url = BASE_URL + "/anomalies";
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(callback);
    }
}
