package com.example.produce.utils;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

// 新增功能：無障礙設計 (Accessibility) - 語音播報輔助
public class TextToSpeechHelper implements TextToSpeech.OnInitListener {
    private TextToSpeech tts;
    private boolean isReady = false;

    public TextToSpeechHelper(Context context) {
        // 初始化 TextToSpeech
        tts = new TextToSpeech(context, this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // 預設使用系統當前語言 (支援多語系 i18n，如印尼語、越南語、中文)
            int result = tts.setLanguage(Locale.getDefault());
            
            // 如果需要強制指定語言，可以使用 Locale("id", "ID") 或 Locale("vi", "VN")
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isReady = true;
                // 針對長輩，可以稍微調慢語速，讓聽覺更清晰
                tts.setSpeechRate(0.85f); 
            }
        }
    }

    // 播報指定的文字 (例如：價格異常警報、農產品名稱與價格)
    public void speak(String text) {
        if (isReady && text != null && !text.isEmpty()) {
            // QUEUE_FLUSH 會中斷當前播報並立刻播報新內容
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    // 停止播報並釋放資源 (應在 Activity/Fragment 的 onDestroy 中呼叫)
    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
