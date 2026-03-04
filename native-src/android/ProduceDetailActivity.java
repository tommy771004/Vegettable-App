package com.example.produceapp;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ProduceDetailActivity extends AppCompatActivity {
    private TextView nameText, levelText;
    private LineChart lineChart;
    private ProduceApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_produce_detail);

        nameText = findViewById(R.id.detailNameText);
        levelText = findViewById(R.id.detailLevelText);
        lineChart = findViewById(R.id.lineChart);

        // 初始化 Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:5000")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ProduceApiService.class);

        // 接收從 MainActivity 傳來的資料
        String id = getIntent().getStringExtra("id");
        String name = getIntent().getStringExtra("name");
        String level = getIntent().getStringExtra("level");
        double currentPrice = getIntent().getDoubleExtra("currentPrice", 0);
        boolean isRetailMode = getIntent().getBooleanExtra("isRetailMode", false);

        nameText.setText(name);
        
        // 設定狀態文字與顏色
        if ("Cheap".equals(level)) {
            levelText.setText("🟢 便宜");
            levelText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else if ("Expensive".equals(level)) {
            levelText.setText("🔴 偏貴");
            levelText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else {
            levelText.setText("🔵 平穩");
            levelText.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        }

        fetchHistoryData(id, currentPrice, isRetailMode);
    }

    private void fetchHistoryData(String id, double currentPrice, boolean isRetailMode) {
        apiService.getHistoryData(id, currentPrice).enqueue(new Callback<List<PriceHistory>>() {
            @Override
            public void onResponse(Call<List<PriceHistory>> call, Response<List<PriceHistory>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    setupChart(response.body(), isRetailMode);
                }
            }

            @Override
            public void onFailure(Call<List<PriceHistory>> call, Throwable t) {
                Toast.makeText(ProduceDetailActivity.this, "無法載入歷史資料", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupChart(List<PriceHistory> historyData, boolean isRetailMode) {
        List<Entry> entries = new ArrayList<>();
        List<String> dates = new ArrayList<>();

        for (int i = 0; i < historyData.size(); i++) {
            PriceHistory data = historyData.get(i);
            dates.add(data.date);
            
            double displayPrice = isRetailMode ? (data.price * 2.5 * 0.6) : data.price;
            entries.add(new Entry(i, (float) displayPrice));
        }

        LineDataSet dataSet = new LineDataSet(entries, "價格趨勢");
        dataSet.setColor(getResources().getColor(android.R.color.holo_blue_light));
        dataSet.setLineWidth(3f);
        dataSet.setCircleColor(getResources().getColor(android.R.color.holo_blue_dark));
        dataSet.setCircleRadius(5f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(getResources().getColor(android.R.color.holo_blue_light));
        dataSet.setFillAlpha(50);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // 平滑曲線

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        // X 軸設定
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dates));
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        // 圖表整體設定
        lineChart.getDescription().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.animateX(1000);
        lineChart.invalidate();
    }
}
