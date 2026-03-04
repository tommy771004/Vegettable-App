package com.example.produce.network;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

// 後續優化與邏輯修正：身分驗證攔截器 (Auth Interceptor)
// 解決問題：原本的 API 呼叫沒有帶上使用者身分，後端無法區分是誰在收藏農產品。
// 現在透過 Interceptor，自動在每一個發送給後端的 Request Header 中加入 X-User-Id (例如手機的 Device ID 或 UUID)。
public class AuthInterceptor implements Interceptor {
    private String deviceId;

    public AuthInterceptor(String deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        // 自動注入 X-User-Id Header
        Request newRequest = originalRequest.newBuilder()
                .header("X-User-Id", deviceId)
                .build();

        return chain.proceed(newRequest);
    }
}
