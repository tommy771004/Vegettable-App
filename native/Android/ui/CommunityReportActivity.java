package com.example.produce.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.produce.R;
import com.example.produce.CommunityPriceDto;

public class CommunityReportActivity extends AppCompatActivity {

    private EditText etMarketName;
    private EditText etProduceName;
    private EditText etPrice;
    private Button btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_report);

        etMarketName = findViewById(R.id.etMarketName);
        etProduceName = findViewById(R.id.etProduceName);
        etPrice = findViewById(R.id.etPrice);
        btnSubmit = findViewById(R.id.btnSubmit);

        btnSubmit.setOnClickListener(v -> submitReport());
    }

    private void submitReport() {
        String market = etMarketName.getText().toString();
        String produce = etProduceName.getText().toString();
        String priceStr = etPrice.getText().toString();

        if (market.isEmpty() || produce.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "請填寫完整資訊", Toast.LENGTH_SHORT).show();
            return;
        }

        // 為何修改：原先 API 呼叫被註解掉，回報資料只停留在前端沒有送到後端。
        // 現在透過 ProduceService.reportCommunityPrice 真正呼叫後端 API，
        // 後端會將回報存入資料庫並累積使用者的貢獻積分 (遊戲化機制)。
        double price = Double.parseDouble(priceStr);
        CommunityPriceDto report = new CommunityPriceDto(market, produce, price, System.currentTimeMillis());

        com.example.produce.data.ProduceService produceService = new com.example.produce.data.ProduceService(this);
        produceService.reportCommunityPrice(report, new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, java.io.IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(CommunityReportActivity.this, "回報失敗，請檢查網路連線", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) {
                runOnUiThread(() -> {
                    Toast.makeText(CommunityReportActivity.this, "感謝您的回報！已獲得 +5 貢獻積分", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }
}
