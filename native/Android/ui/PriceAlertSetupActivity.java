package com.example.produce.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.produce.R;

public class PriceAlertSetupActivity extends AppCompatActivity {

    private EditText etMarketName;
    private EditText etProduceName;
    private EditText etTargetPrice;
    private Button btnSetAlert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_price_alert_setup);

        etMarketName = findViewById(R.id.etMarketName);
        etProduceName = findViewById(R.id.etProduceName);
        etTargetPrice = findViewById(R.id.etTargetPrice);
        btnSetAlert = findViewById(R.id.btnSetAlert);

        btnSetAlert.setOnClickListener(v -> setupAlert());
    }

    private void setupAlert() {
        String market = etMarketName.getText().toString();
        String produce = etProduceName.getText().toString();
        String targetPriceStr = etTargetPrice.getText().toString();

        if (market.isEmpty() || produce.isEmpty() || targetPriceStr.isEmpty()) {
            Toast.makeText(this, "請填寫完整資訊", Toast.LENGTH_SHORT).show();
            return;
        }

        // 為何修改：原先 API 呼叫被註解掉，到價提醒沒有真正同步到後端。
        // 現在透過 syncFavorite 將提醒寫入後端資料庫，
        // 後端 PriceAlertWorker 會定期檢查並透過 FCM 發送推播通知。
        double targetPrice = Double.parseDouble(targetPriceStr);

        com.example.produce.data.ProduceService produceService = new com.example.produce.data.ProduceService(this);
        produceService.syncFavorite(produce, targetPrice, new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, java.io.IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(PriceAlertSetupActivity.this, "設定失敗，請檢查網路連線", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) {
                runOnUiThread(() -> {
                    Toast.makeText(PriceAlertSetupActivity.this, "已設定到價提醒！", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }
}
