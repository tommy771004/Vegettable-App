package com.example.produce.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.example.produce.R;
import java.util.ArrayList;
import java.util.List;

public class PriceChartActivity extends AppCompatActivity {

    private LineChart lineChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_price_chart);

        lineChart = findViewById(R.id.lineChart);
        setupChart();
    }

    private void setupChart() {
        List<Entry> entries = new ArrayList<>();
        // 模擬過去 7 天的價格資料
        entries.add(new Entry(1f, 25.5f));
        entries.add(new Entry(2f, 26.0f));
        entries.add(new Entry(3f, 24.5f));
        entries.add(new Entry(4f, 28.0f));
        entries.add(new Entry(5f, 30.5f));
        entries.add(new Entry(6f, 35.0f));
        entries.add(new Entry(7f, 32.0f));

        LineDataSet dataSet = new LineDataSet(entries, "高麗菜價格走勢 (近7天)");
        dataSet.setColor(getResources().getColor(R.color.colorPrimary));
        dataSet.setValueTextColor(getResources().getColor(R.color.colorText));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.getDescription().setEnabled(false);
        lineChart.animateX(1000);
        lineChart.invalidate(); // refresh
    }
}
