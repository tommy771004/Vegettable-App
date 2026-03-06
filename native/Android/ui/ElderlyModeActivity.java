package com.example.produce.ui;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.produce.R;
import com.example.produce.TextToSpeechHelper;
import java.util.ArrayList;

public class ElderlyModeActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SPEECH_INPUT = 1000;
    private TextView tvResult;
    private Button btnVoiceSearch;
    private TextToSpeechHelper ttsHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elderly_mode);

        tvResult = findViewById(R.id.tvResult);
        btnVoiceSearch = findViewById(R.id.btnVoiceSearch);
        ttsHelper = new TextToSpeechHelper(this);

        btnVoiceSearch.setOnClickListener(v -> startVoiceRecognition());
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-TW");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "阿嬤，請說您想查什麼菜？（例如：高麗菜一斤多少錢）");

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
        } catch (Exception e) {
            Toast.makeText(this, "您的裝置不支援語音輸入", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                String spokenText = result.get(0);
                processVoiceQuery(spokenText);
            }
        }
    }

    // 為何修改：原先只硬編碼高麗菜和番茄兩種蔬菜的回應，其他一律回「聽不懂」。
    // 現在改為呼叫後端 API，將語音辨識結果當作關鍵字搜尋真實菜價，
    // 讓長輩可以查詢任何農產品。
    private void processVoiceQuery(String query) {
        String loadingMsg = "阿嬤，正在幫您查「" + query + "」的價格...";
        tvResult.setText(loadingMsg);
        ttsHelper.speak(loadingMsg);

        com.example.produce.data.ProduceService produceService = new com.example.produce.data.ProduceService(this);
        produceService.fetchProduceData(query, 1, new com.example.produce.data.ProduceService.ProduceDataCallback() {
            @Override
            public void onSuccess(com.example.produce.models.PaginatedResponse response) {
                runOnUiThread(() -> {
                    if (response.data != null && !response.data.isEmpty()) {
                        Object first = response.data.get(0);
                        // 使用 Gson 取得欄位 (簡化處理)
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        String json = gson.toJson(first);
                        com.example.produce.models.ProduceDto dto = gson.fromJson(json, com.example.produce.models.ProduceDto.class);
                        String reply = "阿嬤，今天" + dto.marketName + "的" + dto.cropName + "，批發價是一斤 " + (int) dto.avgPrice + " 元。";
                        tvResult.setText(reply);
                        ttsHelper.speak(reply);
                    } else {
                        String reply = "阿嬤，今天找不到「" + query + "」的價格，請換個菜名再試試。";
                        tvResult.setText(reply);
                        ttsHelper.speak(reply);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    String reply = "阿嬤，網路好像有問題，等一下再查看看。";
                    tvResult.setText(reply);
                    ttsHelper.speak(reply);
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (ttsHelper != null) {
            ttsHelper.shutdown();
        }
        super.onDestroy();
    }
}
