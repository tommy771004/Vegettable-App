package com.example.produceapp.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.produceapp.R
import com.example.produceapp.network.RetrofitClient
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SeasonalCalendarActivity : AppCompatActivity() {

    private lateinit var tvCurrentSeason: TextView
    private lateinit var lvSeasonalCrops: ListView
    private lateinit var viewModel: ProduceViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seasonal_calendar)

        tvCurrentSeason = findViewById(R.id.tvCurrentSeason)
        lvSeasonalCrops = findViewById(R.id.lvSeasonalCrops)

        val factory = ProduceViewModelFactory(RetrofitClient.instance)
        viewModel = ViewModelProvider(this, factory).get(ProduceViewModel::class.java)

        setupSeasonalData()
    }

    private fun setupSeasonalData() {
        lifecycleScope.launch {
            viewModel.seasonalCrops.collectLatest { crops ->
                if (crops.isNotEmpty()) {
                    // Assuming first crop's season is representative or we need a separate API for current season info
                    val currentSeason = crops.firstOrNull()?.season ?: "Unknown"
                    tvCurrentSeason.text = "目前節氣：$currentSeason"

                    val cropStrings = crops.map { "${it.cropName} - ${it.description}" }
                    val adapter = ArrayAdapter(this@SeasonalCalendarActivity, android.R.layout.simple_list_item_1, cropStrings)
                    lvSeasonalCrops.adapter = adapter
                }
            }
        }
    }
}
