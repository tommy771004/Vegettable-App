package com.example.produce.ui;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.produce.R;
import java.util.ArrayList;
import java.util.List;

public class SeasonalCalendarActivity extends AppCompatActivity {

    private TextView tvCurrentSeason;
    private ListView lvSeasonalCrops;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seasonal_calendar);

        tvCurrentSeason = findViewById(R.id.tvCurrentSeason);
        lvSeasonalCrops = findViewById(R.id.lvSeasonalCrops);

        setupSeasonalData();
    }

    private void setupSeasonalData() {
        // 模擬當前節氣與當季農產品
        String currentSeason = "立秋 (約 8/7 - 8/22)";
        tvCurrentSeason.setText("目前節氣：" + currentSeason);

        List<String> crops = new ArrayList<>();
        crops.add("🍉 西瓜 - 挑選訣竅：拍打聲音清脆，蒂頭捲曲");
        crops.add("🥒 小黃瓜 - 挑選訣竅：瓜體直挺，表面有刺");
        crops.add("🍆 茄子 - 挑選訣竅：表皮光滑發亮，蒂頭無枯萎");
        crops.add("🥬 空心菜 - 挑選訣竅：葉片翠綠，莖部不發黑");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, crops);
        lvSeasonalCrops.setAdapter(adapter);
    }
}
