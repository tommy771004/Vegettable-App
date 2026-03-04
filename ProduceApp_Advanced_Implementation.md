# 「菜價 App」進階實戰指南：真實資料串接與進階功能

這份指南將帶您把原本的「菜價 App」升級為**具備真實政府開放資料、快取機制、圖表顯示、下拉更新與零售價換算**的商業級應用程式。

---

## 第一部分：後端 API (.NET 8) - 真實資料與快取

我們將串接**農業部農產品交易行情 API**，並使用 `IMemoryCache` 來快取資料，避免頻繁請求政府伺服器導致被阻擋。

### 1. 安裝必要套件
在 VS Code 終端機中執行：
```bash
dotnet add package Microsoft.Extensions.Caching.Memory
```

### 2. 完整 `Program.cs` 實作
將 `Program.cs` 替換為以下內容。這段程式碼會自動去政府 API 抓取「台北一」市場的最新交易資料，並快取 1 小時。

```csharp
using System.Text.Json;
using Microsoft.Extensions.Caching.Memory;

var builder = WebApplication.CreateBuilder(args);

// 註冊 HttpClient 與 MemoryCache
builder.Services.AddHttpClient();
builder.Services.AddMemoryCache();

// 允許 CORS (讓跨網域測試更方便)
builder.Services.AddCors(options => {
    options.AddDefaultPolicy(policy => policy.AllowAnyOrigin().AllowAnyMethod().AllowAnyHeader());
});

var app = builder.Build();
app.UseCors();

app.MapGet("/api/produce", async (IHttpClientFactory clientFactory, IMemoryCache cache) =>
{
    const string cacheKey = "ProduceData_Taipei1";
    
    // 1. 嘗試從快取讀取
    if (!cache.TryGetValue(cacheKey, out List<ProduceItem>? produceList))
    {
        produceList = new List<ProduceItem>();
        var client = clientFactory.CreateClient();
        
        try
        {
            // 2. 呼叫農業部真實 API (農產品交易行情)
            var response = await client.GetStringAsync("https://data.moa.gov.tw/Service/OpenData/FromM/FarmTransData.aspx");
            using var document = JsonDocument.Parse(response);
            
            foreach (var element in document.RootElement.EnumerateArray())
            {
                var marketName = element.GetProperty("市場名稱").GetString();
                var cropName = element.GetProperty("作物名稱").GetString();
                
                // 只取「台北一」市場，且過濾掉空名稱的防呆處理
                if (marketName == "台北一" && !string.IsNullOrWhiteSpace(cropName))
                {
                    var price = element.GetProperty("平均價").GetDouble();
                    var code = element.GetProperty("作物代號").GetString() ?? "";
                    
                    // 簡單分類邏輯 (F開頭通常是水果)
                    var category = code.StartsWith("F") ? "水果" : "蔬菜";
                    
                    produceList.Add(new ProduceItem
                    {
                        Id = code,
                        Name = cropName,
                        Category = category,
                        CurrentPrice = price,
                        Level = price > 80 ? "Expensive" : (price < 30 ? "Cheap" : "Fair")
                    });
                }
            }
            
            // 3. 寫入快取，設定 1 小時過期
            var cacheOptions = new MemoryCacheEntryOptions()
                .SetAbsoluteExpiration(TimeSpan.FromHours(1));
            cache.Set(cacheKey, produceList, cacheOptions);
        }
        catch (Exception ex)
        {
            return Results.Problem("無法取得農業部資料：" + ex.Message);
        }
    }

    // 回傳前 100 筆避免資料量過大導致手機端卡頓
    return Results.Ok(produceList?.Take(100));
});

app.Run();

// 資料模型
public class ProduceItem
{
    public string Id { get; set; } = "";
    public string Name { get; set; } = "";
    public string Category { get; set; } = "";
    public double CurrentPrice { get; set; }
    public string Level { get; set; } = "";
}
```

---

## 第二部分：iOS App (SwiftUI) - 圖表、下拉更新與零售換算

### 1. 更新 `ProduceItem.swift`
```swift
import Foundation

struct ProduceItem: Identifiable, Codable {
    let id: String
    let name: String
    let category: String
    let currentPrice: Double
    let level: String
}

// 模擬歷史資料用的結構
struct PriceHistory: Identifiable {
    let id = UUID()
    let date: String
    let price: Double
}
```

### 2. 更新 `ProduceViewModel.swift` (加入 async/await)
```swift
import Foundation

@MainActor
class ProduceViewModel: ObservableObject {
    @Published var items: [ProduceItem] = []
    @Published var isLoading = false
    @Published var errorMessage: String? = nil
    
    init() {
        Task { await fetchData() }
    }
    
    func fetchData() async {
        isLoading = true
        errorMessage = nil
        
        guard let url = URL(string: "http://localhost:5000/api/produce") else { return }
        
        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            let decodedData = try JSONDecoder().decode([ProduceItem].self, from: data)
            self.items = decodedData
        } catch {
            self.errorMessage = "載入失敗：\(error.localizedDescription)"
        }
        
        isLoading = false
    }
}
```

### 3. 完整 `ContentView.swift` (加入 Charts 與下拉更新)
請確保在檔案最上方 `import Charts`。

```swift
import SwiftUI
import Charts

struct ContentView: View {
    @StateObject private var viewModel = ProduceViewModel()
    @State private var searchText = ""
    @State private var selectedCategory = "全部"
    @State private var isRetailMode = false // 零售價模式
    
    let categories = ["全部", "蔬菜", "水果"]
    
    var filteredItems: [ProduceItem] {
        var result = viewModel.items
        if selectedCategory != "全部" {
            result = result.filter { $0.category == selectedCategory }
        }
        if !searchText.isEmpty {
            result = result.filter { $0.name.contains(searchText) }
        }
        return result
    }
    
    var body: some View {
        NavigationView {
            ZStack {
                Color(UIColor.systemGroupedBackground).ignoresSafeArea()
                
                VStack(spacing: 0) {
                    // 分類與零售切換區
                    HStack {
                        Picker("分類", selection: $selectedCategory) {
                            ForEach(categories, id: \.self) { Text($0) }
                        }
                        .pickerStyle(.segmented)
                        
                        Toggle("零售估價", isOn: $isRetailMode)
                            .toggleStyle(.switch)
                            .labelsHidden()
                            .padding(.leading, 8)
                        Text("零售(台斤)")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .padding()
                    .background(Color(UIColor.systemBackground))
                    
                    if viewModel.isLoading && viewModel.items.isEmpty {
                        Spacer()
                        ProgressView("載入最新行情中...")
                        Spacer()
                    } else if let error = viewModel.errorMessage {
                        Spacer()
                        Text(error).foregroundColor(.red)
                        Button("重試") { Task { await viewModel.fetchData() } }
                            .padding()
                        Spacer()
                    } else {
                        List(filteredItems) { item in
                            NavigationLink(destination: ProduceDetailView(item: item, isRetailMode: isRetailMode)) {
                                ProduceRow(item: item, isRetailMode: isRetailMode)
                            }
                        }
                        .listStyle(.insetGrouped)
                        .refreshable {
                            await viewModel.fetchData() // 下拉更新
                        }
                    }
                }
            }
            .navigationTitle("台北一市場行情")
            .searchable(text: $searchText, prompt: "搜尋菜名...")
        }
    }
}

struct ProduceRow: View {
    let item: ProduceItem
    let isRetailMode: Bool
    
    // 批發價(公斤) 轉 零售價(台斤) 估算： 批發價 * 2.5倍 * 0.6(公斤轉台斤)
    var displayPrice: Double {
        isRetailMode ? (item.currentPrice * 2.5 * 0.6) : item.currentPrice
    }
    
    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 6) {
                Text(item.name).font(.headline)
                Text(item.category).font(.caption).foregroundColor(.secondary)
            }
            Spacer()
            VStack(alignment: .trailing) {
                Text("$\(String(format: "%.1f", displayPrice))")
                    .font(.title3).fontWeight(.bold)
                    .foregroundColor(item.level == "Cheap" ? .green : (item.level == "Expensive" ? .red : .blue))
                Text(isRetailMode ? "/台斤" : "/公斤")
                    .font(.caption2).foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}

// 點擊進入的詳細圖表頁面
struct ProduceDetailView: View {
    let item: ProduceItem
    let isRetailMode: Bool
    
    // 根據當前價格，隨機生成過去 7 天的波動趨勢 (模擬真實圖表)
    var mockHistory: [PriceHistory] {
        let calendar = Calendar.current
        let today = Date()
        return (0..<7).reversed().map { i in
            let date = calendar.date(byAdding: .day, value: -i, to: today)!
            let formatter = DateFormatter()
            formatter.dateFormat = "MM/dd"
            let randomVariance = Double.random(in: -5.0...5.0)
            let price = max(1.0, item.currentPrice + randomVariance)
            let displayPrice = isRetailMode ? (price * 2.5 * 0.6) : price
            return PriceHistory(date: formatter.string(from: date), price: displayPrice)
        }
    }
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                HStack {
                    Text(item.name).font(.largeTitle).bold()
                    Spacer()
                    Text(item.level == "Cheap" ? "🟢 便宜" : (item.level == "Expensive" ? "🔴 偏貴" : "🔵 平穩"))
                        .font(.headline)
                }
                
                VStack(alignment: .leading) {
                    Text("近七日價格趨勢").font(.headline)
                    
                    // SwiftUI Charts 圖表實作
                    Chart {
                        ForEach(mockHistory) { data in
                            LineMark(
                                x: .value("日期", data.date),
                                y: .value("價格", data.price)
                            )
                            .symbol(Circle())
                            .interpolationMethod(.catmullRom) // 平滑曲線
                            
                            AreaMark(
                                x: .value("日期", data.date),
                                y: .value("價格", data.price)
                            )
                            .foregroundStyle(Gradient(colors: [.blue.opacity(0.3), .clear]))
                        }
                    }
                    .frame(height: 250)
                    .chartYAxis {
                        AxisMarks(position: .leading)
                    }
                }
                .padding()
                .background(Color(UIColor.secondarySystemGroupedBackground))
                .cornerRadius(16)
            }
            .padding()
        }
        .navigationTitle("行情分析")
        .navigationBarTitleDisplayMode(.inline)
        .background(Color(UIColor.systemGroupedBackground).ignoresSafeArea())
    }
}
```

---

## 第三部分：Android App (Java) - Retrofit、MPAndroidChart 與下拉更新

在 Android 中，我們將導入 `Retrofit` 來處理 API，並使用 `SwipeRefreshLayout`。

### 1. 修改 `build.gradle (Module: app)`
在 `dependencies` 區塊加入以下套件，然後點擊右上角 **Sync Now**：
```gradle
dependencies {
    // ... 原有套件
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'androidx.swiperefreshlayout:swiperefreshlayout:1.1.0'
    implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0' // 圖表庫
}
```
*(注意：MPAndroidChart 需要在 `settings.gradle` 的 `dependencyResolutionManagement` -> `repositories` 中加入 `maven { url 'https://jitpack.io' }`)*

### 2. 建立 Retrofit API 介面 (`ProduceApiService.java`)
```java
package com.example.produceapp;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

public interface ProduceApiService {
    @GET("/api/produce")
    Call<List<ProduceItem>> getProduceData();
}
```

### 3. 修改 `activity_main.xml` (加入下拉更新與零售切換)
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="#F5F5F5">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="16dp"
        android:background="#FFFFFF"
        android:gravity="center_vertical"
        android:elevation="4dp">
        
        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="台北一市場行情"
            android:textSize="24sp"
            android:textStyle="bold"
            android:textColor="#000000"/>

        <Switch
            android:id="@+id/retailSwitch"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="零售(台斤) "/>
    </LinearLayout>

    <androidx.swiperefreshlayout.widget.SwipeRefreshLayout
        android:id="@+id/swipeRefreshLayout"
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/recyclerView"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:padding="16dp"
            android:clipToPadding="false"/>

    </androidx.swiperefreshlayout.widget.SwipeRefreshLayout>

</LinearLayout>
```

### 4. 修改 `ProduceAdapter.java` (支援零售價切換)
```java
// ... 省略 import
public class ProduceAdapter extends RecyclerView.Adapter<ProduceAdapter.ViewHolder> {
    private List<ProduceItem> items;
    private boolean isRetailMode = false;

    public ProduceAdapter(List<ProduceItem> items) { this.items = items; }

    public void setRetailMode(boolean isRetailMode) {
        this.isRetailMode = isRetailMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_produce, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProduceItem item = items.get(position);
        holder.nameText.setText(item.name);
        holder.categoryText.setText(item.category);
        
        // 批發價(公斤) 轉 零售價(台斤)
        double displayPrice = isRetailMode ? (item.currentPrice * 2.5 * 0.6) : item.currentPrice;
        holder.priceText.setText(String.format("$%.1f", displayPrice));
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ... ViewHolder 保持不變
}
```

### 5. 完整 `MainActivity.java` (Retrofit 實作)
```java
package com.example.produceapp;

import android.os.Bundle;
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
    private List<ProduceItem> items = new ArrayList<>();
    private ProduceApiService apiService;

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

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProduceAdapter(items);
        recyclerView.setAdapter(adapter);

        // 下拉更新監聽
        swipeRefreshLayout.setOnRefreshListener(this::fetchData);

        // 零售價切換監聽
        retailSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            adapter.setRetailMode(isChecked);
        });

        // 首次載入資料
        swipeRefreshLayout.setRefreshing(true);
        fetchData();
    }

    private void fetchData() {
        apiService.getProduceData().enqueue(new Callback<List<ProduceItem>>() {
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
```

---
**恭喜！** 透過這份進階指南，您的 App 現在已經具備了：
1. **真實資料**：直接從政府開放資料平台抓取。
2. **快取機制**：後端保護機制，提升效能。
3. **實用功能**：批發與零售價一鍵切換、下拉更新。
4. **視覺化**：iOS 端實作了美觀的 SwiftUI Charts 折線圖。
