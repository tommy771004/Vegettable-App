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

        double price = Double.parseDouble(priceStr);
        CommunityPriceDto report = new CommunityPriceDto(market, produce, price, System.currentTimeMillis());

        // 呼叫後端 API 提交在地回報資料
        // produceService.submitCommunityReport(report).enqueue(...)
        
        Toast.makeText(this, "感謝您的回報！", Toast.LENGTH_SHORT).show();
        finish();
    }
}
