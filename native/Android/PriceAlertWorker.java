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
        
        // 1. Fetch latest prices from ProduceService
        // 2. Query local SQLite (ProduceDatabaseHelper) for user's favorite items & target prices
        // 3. Compare current prices with target prices
        
        // Mocking a price drop detection
        boolean priceDropped = true; 
        String produceName = "高麗菜";
        double currentPrice = 15.5;

        if (priceDropped) {
            sendNotification(produceName, currentPrice);
        }

        return Result.success();
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
