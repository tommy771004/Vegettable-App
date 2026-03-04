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

        double targetPrice = Double.parseDouble(targetPriceStr);

        // 呼叫後端 API 設定到價提醒 (FCM)
        // produceService.setPriceAlert(market, produce, targetPrice).enqueue(...)
        
        Toast.makeText(this, "已設定到價提醒！", Toast.LENGTH_SHORT).show();
        finish();
    }
}
