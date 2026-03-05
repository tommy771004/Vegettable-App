package com.example.produceapp.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.produceapp.R
import com.example.produceapp.network.RetrofitClient
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PriceChartActivity : AppCompatActivity() {

    private lateinit var lineChart: LineChart
    private lateinit var viewModel: ProduceViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_price_chart)

        lineChart = findViewById(R.id.lineChart)
        
        val factory = ProduceViewModelFactory(RetrofitClient.instance)
        viewModel = ViewModelProvider(this, factory).get(ProduceViewModel::class.java)

        setupChartObserver()
    }

    private fun setupChartObserver() {
        lifecycleScope.launch {
            viewModel.historicalData.collectLatest { history ->
                if (history.isNotEmpty()) {
                    val entries = history.mapIndexed { index, dto ->
                        Entry(index.toFloat(), dto.avgPrice)
                    }
                    
                    val dataSet = LineDataSet(entries, "高麗菜價格走勢 (近7天)")
                    // Set colors etc.
                    
                    val lineData = LineData(dataSet)
                    lineChart.data = lineData
                    lineChart.invalidate()
                }
            }
        }
    }
}
