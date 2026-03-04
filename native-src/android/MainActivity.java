package com.example.produceapp;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ProduceAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Switch retailSwitch;
    private Spinner marketSpinner;
    private List<ProduceItem> items = new ArrayList<>();
    private List<String> markets = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;
    private ProduceApiService apiService;
    private String currentMarket = "台北一";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化 Retrofit (注意使用 10.0.2.2)
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:5000")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ProduceApiService.class);

        // 初始化 UI
        recyclerView = findViewById(R.id.recyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        retailSwitch = findViewById(R.id.retailSwitch);
        marketSpinner = findViewById(R.id.marketSpinner);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProduceAdapter(items, item -> {
            // 點擊列表項目，跳轉至詳細圖表頁面
            android.content.Intent intent = new android.content.Intent(MainActivity.this, ProduceDetailActivity.class);
            intent.putExtra("id", item.id);
            intent.putExtra("name", item.name);
            intent.putExtra("level", item.level);
            intent.putExtra("currentPrice", item.currentPrice);
            intent.putExtra("isRetailMode", retailSwitch.isChecked());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        // 設定 Spinner
        markets.add("台北一"); // 預設選項
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, markets);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        marketSpinner.setAdapter(spinnerAdapter);

        marketSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedMarket = markets.get(position);
                if (!selectedMarket.equals(currentMarket)) {
                    currentMarket = selectedMarket;
                    swipeRefreshLayout.setRefreshing(true);
                    fetchData();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 下拉更新監聽
        swipeRefreshLayout.setOnRefreshListener(this::fetchData);

        // 零售價切換監聽
        retailSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            adapter.setRetailMode(isChecked);
        });

        // 載入市場列表與初始資料
        fetchMarkets();
        swipeRefreshLayout.setRefreshing(true);
        fetchData();
    }

    private void fetchMarkets() {
        apiService.getMarkets().enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    markets.clear();
                    markets.addAll(response.body());
                    spinnerAdapter.notifyDataSetChanged();
                    
                    // 確保預設選中「台北一」
                    int defaultPosition = markets.indexOf("台北一");
                    if (defaultPosition >= 0) {
                        marketSpinner.setSelection(defaultPosition);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "無法載入市場列表", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchData() {
        apiService.getProduceData(currentMarket).enqueue(new Callback<List<ProduceItem>>() {
            @Override
            public void onResponse(Call<List<ProduceItem>> call, Response<List<ProduceItem>> response) {
                swipeRefreshLayout.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    items.clear();
                    items.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<ProduceItem>> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(MainActivity.this, "載入失敗: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
