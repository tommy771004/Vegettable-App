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

    private void processVoiceQuery(String query) {
        // 簡易關鍵字比對，實務上可串接 Gemini AI 分析語意
        if (query.contains("高麗菜")) {
            String response = "阿嬤，今天台北一市場的高麗菜，批發價是一斤 25 元。";
            tvResult.setText(response);
            ttsHelper.speak(response);
        } else if (query.contains("番茄")) {
            String response = "阿嬤，今天牛番茄比較貴，一斤要 45 元喔。";
            tvResult.setText(response);
            ttsHelper.speak(response);
        } else {
            String response = "阿嬤，我聽不懂您說的菜名，請再說一次。";
            tvResult.setText(response);
            ttsHelper.speak(response);
        }
    }

    @Override
    protected void onDestroy() {
        if (ttsHelper != null) {
            ttsHelper.shutdown();
        }
        super.onDestroy();
    }
}
