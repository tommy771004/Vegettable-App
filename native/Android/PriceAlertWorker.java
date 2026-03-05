package com.example.produce.worker;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class PriceAlertWorker extends Worker {
    private static final String TAG = "PriceAlertWorker";
    private static final String CHANNEL_ID = "produce_price_alerts";

    public PriceAlertWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Checking for price drops in background...");
        
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        final Result[] result = {Result.success()};
        
        com.example.produce.data.ProduceService service = new com.example.produce.data.ProduceService(getApplicationContext());
        
        service.getFavorites(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, java.io.IOException e) {
                Log.e(TAG, "Failed to fetch favorites", e);
                result[0] = Result.retry();
                latch.countDown();
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Server returned error: " + response.code());
                    result[0] = Result.failure();
                    latch.countDown();
                    return;
                }
                
                String json = response.body().string();
                try {
                    java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<java.util.List<com.example.produce.models.FavoriteAlertDto>>(){}.getType();
                    java.util.List<com.example.produce.models.FavoriteAlertDto> favorites = new com.google.gson.Gson().fromJson(json, type);
                    
                    if (favorites != null) {
                        for (com.example.produce.models.FavoriteAlertDto fav : favorites) {
                            if (fav.isAlertTriggered) {
                                sendNotification(fav.produceName, fav.currentPrice);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing favorites", e);
                } finally {
                    latch.countDown();
                }
            }
        });

        try {
            latch.await(30, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return Result.failure();
        }
        
        return result[0];
    }

    private void sendNotification(String produceName, double price) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("📉 價格降價通知！")
                .setContentText(produceName + " 目前批發價已降至 $" + price + "，快去選購！")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        // notificationManager.notify(1, builder.build()); // Requires POST_NOTIFICATIONS permission in Android 13+
    }
}
