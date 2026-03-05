package com.example.produce.repository;

import com.example.produce.data.ProduceDatabaseHelper;
import com.example.produce.data.ProduceService;
import com.example.produce.models.PaginatedResponse;
import com.example.produce.models.ProduceDto;

import java.util.List;

public class ProduceRepository {
    private ProduceService apiService;
    private ProduceDatabaseHelper localDb;

    public ProduceRepository(ProduceService apiService, ProduceDatabaseHelper localDb) {
        this.apiService = apiService;
        this.localDb = localDb;
    }

    // 邏輯修正與優化 2：Repository Pattern (單一資料來源模式)
    // 解決問題：之前 API (ProduceService) 和本地資料庫 (ProduceDatabaseHelper) 是分開的。
    // 手機在沒網路時會直接 Crash。現在透過 Repository 統一管理：先打 API，成功就存入本地 DB；失敗就讀取本地 DB。
    public void getDailyPrices(String keyword, int page, ProduceService.ProduceDataCallback callback) {
        
        // 1. 向後端 API 請求最新資料
        apiService.fetchProduceData(keyword, page, new ProduceService.ProduceDataCallback() {
            @Override
            public void onSuccess(PaginatedResponse<ProduceDto> response) {
                // 2. 成功取得資料後，非同步寫入 SQLite 本地資料庫 (快取)
                localDb.saveProduceList(response.data); 
                
                // 3. 將最新資料回傳給 UI (ViewModel / Activity)
                callback.onSuccess(response);
            }

            @Override
            public void onFailure(Exception e) {
                // 4. 如果斷網或 API 伺服器掛掉，改從 SQLite 讀取最後一次的快取資料 (離線模式)
                List<ProduceDto> cachedData = localDb.getProduceList(keyword, page);
                if (cachedData != null && !cachedData.isEmpty()) {
                    PaginatedResponse<ProduceDto> offlineResponse = new PaginatedResponse<>();
                    offlineResponse.data = cachedData;
                    callback.onSuccess(offlineResponse);
                } else {
                    callback.onFailure(e);
                }
            }
        });
    }
}
