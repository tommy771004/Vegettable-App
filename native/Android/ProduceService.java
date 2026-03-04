package com.example.produce.data;

import com.example.produce.models.PaginatedResponse;
import com.example.produce.models.ProduceDto;
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
    private OkHttpClient client = new OkHttpClient();
    private Gson gson = new Gson();

    // 定義自訂的 Callback 介面，讓 UI 層可以直接拿到解析好的 Java 物件
    public interface ProduceDataCallback {
        void onSuccess(PaginatedResponse<ProduceDto> response);
        void onFailure(Exception e);
    }

    // 邏輯修正：加入分頁與搜尋參數，並使用 Gson 自動將 JSON 轉為 Java Object (ProduceDto)
    // 解決問題：原本只回傳 String，App 端需要寫大量 try-catch 解析 JSON，且容易發生 NullPointerException。
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
                    // 自動將後端的 JSON 映射到 Java 的 PaginatedResponse<ProduceDto>
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

    public void syncFavorite(String userId, String produceId, double targetPrice, Callback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("userId", userId);
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
}
