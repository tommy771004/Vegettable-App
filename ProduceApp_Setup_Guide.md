# 「菜價 App」全端開發與編譯完整指南 (macOS)

這是一份為您量身打造的**「菜價 App」全端開發與編譯完整指南**。針對您的 macOS 環境，我們將分為三個部分：**後端 (.NET 8 API)**、**iOS (Xcode)** 以及 **Android (Android Studio)**。

---

## 零、環境準備 (macOS)

在開始之前，請確保您的 Mac 已安裝以下工具：

1. **.NET 8 SDK**: 請至 [Microsoft 官網](https://dotnet.microsoft.com/download) 下載並安裝 macOS 版本的 .NET 8.0 SDK。
2. **Visual Studio Code**: 下載 VS Code，並在擴充功能中安裝 **C# Dev Kit**。
3. **Xcode**: 請至 Mac App Store 下載安裝（需保留足夠的硬碟空間）。
4. **Android Studio**: 請至 [Android 開發者官網](https://developer.android.com/studio) 下載安裝。

---

## 第一部分：後端 API (.NET 8 + VS Code)

我們將建立一個簡單的 RESTful API 來提供菜價資料。

### 步驟 1：建立專案
1. 打開 Mac 的 **終端機 (Terminal)**。
2. 執行以下指令建立一個新的 Web API 專案：
   ```bash
   mkdir ProduceAppStack
   cd ProduceAppStack
   dotnet new webapi -n ProduceApi
   cd ProduceApi
   ```
3. 用 VS Code 打開專案：
   ```bash
   code .
   ```

### 步驟 2：撰寫 API 程式碼
在 VS Code 中，打開 `Program.cs`，將內容全部替換為以下 Minimal API 程式碼：

```csharp
var builder = WebApplication.CreateBuilder(args);
var app = builder.Build();

// 建立一個簡單的菜價資料模型
var produceData = new[]
{
    new { id = "1", name = "高麗菜", category = "蔬菜", currentPrice = 25.5, level = "Cheap" },
    new { id = "2", name = "香蕉", category = "水果", currentPrice = 30.0, level = "Fair" },
    new { id = "3", name = "豬肉", category = "肉品", currentPrice = 120.0, level = "Expensive" },
    new { id = "4", name = "吳郭魚", category = "漁產", currentPrice = 65.0, level = "Fair" }
};

// 設定 API 路由
app.MapGet("/api/produce", () =>
{
    return Results.Ok(produceData);
});

app.Run();
```

### 步驟 3：啟動 API 伺服器
為了避免手機模擬器遇到 SSL 憑證問題，我們使用 HTTP 模式啟動：
1. 在 VS Code 的終端機中輸入：
   ```bash
   dotnet run --launch-profile http
   ```
2. 終端機會顯示監聽的網址（通常是 `http://localhost:5000` 或類似的 Port）。請記下這個網址。您可以打開瀏覽器測試 `http://localhost:5000/api/produce`，應該會看到 JSON 資料。

---

## 第二部分：iOS App (Xcode + SwiftUI)

### 步驟 1：建立 Xcode 專案
1. 打開 **Xcode**，選擇 **Create a new Xcode project**。
2. 選擇 **iOS** -> **App**，點擊 Next。
3. 填寫專案資訊：
   * Product Name: `ProduceApp`
   * Interface: **SwiftUI**
   * Language: **Swift**
4. 選擇存檔位置（建議存在剛剛的 `ProduceAppStack` 資料夾旁）。

### 步驟 2：允許本地 HTTP 連線 (App Transport Security)
因為我們本地測試使用的是 `http://` 而非 `https://`，需要設定權限：
1. 在 Xcode 左側導覽列點擊專案根目錄 `ProduceApp`。
2. 選擇 **Info** 標籤頁。
3. 右鍵點擊任意一列，選擇 **Add Row**。
4. 輸入 `App Transport Security Settings` (按 Enter)。
5. 點開它左邊的箭頭，在它底下再新增一個 Row，選擇 `Allow Arbitrary Loads`，並將右側的 Value 改為 **YES**。

### 步驟 3：貼上程式碼並串接 API

**1. 建立 `ProduceItem.swift` (File -> New -> File -> Swift File)**
```swift
import Foundation

struct ProduceItem: Identifiable, Codable {
    let id: String
    let name: String
    let category: String
    let currentPrice: Double
    let level: String
}
```

**2. 建立 `ProduceViewModel.swift` (File -> New -> File -> Swift File)**
```swift
import Foundation

class ProduceViewModel: ObservableObject {
    @Published var items: [ProduceItem] = []
    @Published var isLoading = false
    
    init() { fetchData() }
    
    func fetchData() {
        isLoading = true
        // iOS 模擬器可以直接使用 localhost 存取 Mac 本機的 API
        guard let url = URL(string: "http://localhost:5000/api/produce") else { return }
        
        URLSession.shared.dataTask(with: url) { data, response, error in
            if let data = data {
                do {
                    let decodedData = try JSONDecoder().decode([ProduceItem].self, from: data)
                    DispatchQueue.main.async {
                        self.items = decodedData
                        self.isLoading = false
                    }
                } catch {
                    print("解析錯誤: \(error)")
                }
            }
        }.resume()
    }
}
```

**3. 修改 `ContentView.swift`**
```swift
import SwiftUI

struct ContentView: View {
    @StateObject private var viewModel = ProduceViewModel()
    @State private var searchText = ""
    
    var filteredItems: [ProduceItem] {
        if searchText.isEmpty {
            return viewModel.items
        } else {
            return viewModel.items.filter { $0.name.contains(searchText) }
        }
    }
    
    var body: some View {
        NavigationView {
            ZStack {
                Color(UIColor.systemGroupedBackground)
                    .ignoresSafeArea()
                
                if viewModel.isLoading {
                    ProgressView("載入行情中...")
                } else {
                    ScrollView {
                        LazyVStack(spacing: 16) {
                            ForEach(filteredItems) { item in
                                ProduceRow(item: item)
                            }
                        }
                        .padding()
                    }
                }
            }
            .navigationTitle("當令行情")
            .searchable(text: $searchText, prompt: "搜尋品項...")
        }
    }
}

struct ProduceRow: View {
    let item: ProduceItem
    
    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 6) {
                Text(item.name)
                    .font(.title3)
                    .fontWeight(.bold)
                
                Text(item.category)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            VStack(alignment: .trailing, spacing: 4) {
                Text("$\(String(format: "%.1f", item.currentPrice))")
                    .font(.title2)
                    .fontWeight(.black)
                    .foregroundColor(priceColor(for: item.level))
                
                Text(levelText(for: item.level))
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundColor(priceColor(for: item.level))
            }
        }
        .padding()
        .background(Color(UIColor.secondarySystemGroupedBackground))
        .cornerRadius(16)
        .shadow(color: Color.black.opacity(0.05), radius: 5, x: 0, y: 2)
    }
    
    func priceColor(for level: String) -> Color {
        switch level {
        case "Cheap": return .green
        case "Fair": return .blue
        case "Expensive": return .red
        default: return .primary
        }
    }
    
    func levelText(for level: String) -> String {
        switch level {
        case "Cheap": return "相對便宜"
        case "Fair": return "價格平穩"
        case "Expensive": return "價格偏貴"
        default: return "未知"
        }
    }
}
```

### 步驟 4：編譯與執行
1. 在 Xcode 頂部選擇一個模擬器（例如 iPhone 15 Pro）。
2. 按下左上角的 **Play 按鈕 (Cmd + R)**。
3. App 啟動後，就會自動從您的 .NET API 抓取資料並顯示！

---

## 第三部分：Android App (Android Studio + Java)

### 步驟 1：建立 Android 專案
1. 打開 **Android Studio**，選擇 **New Project**。
2. 選擇 **Empty Views Activity**，點擊 Next。
3. 填寫專案資訊：
   * Name: `ProduceApp`
   * Package name: `com.example.produceapp`
   * Language: **Java**
   * Minimum SDK: API 24
4. 點擊 Finish 等待 Gradle 同步完成。

### 步驟 2：設定網路權限與 HTTP 允許
1. 打開 `app/src/main/AndroidManifest.xml`。
2. 在 `<application>` 標籤**上方**加入網路權限：
   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   ```
3. 在 `<application>` 標籤**內部**加入允許明文傳輸（為了測試 HTTP）：
   ```xml
   android:usesCleartextTraffic="true"
   ```

### 步驟 3：建立 UI 佈局 (XML)
1. 在 `res/layout/` 目錄下，打開 `activity_main.xml`，將內容替換為：
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="#F5F5F5">

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="當令行情"
        android:textSize="28sp"
        android:textStyle="bold"
        android:textColor="#000000"
        android:padding="24dp"
        android:background="#FFFFFF"
        android:elevation="4dp"/>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recyclerView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:padding="16dp"
        android:clipToPadding="false"/>

</LinearLayout>
```

2. 在 `res/layout/` 目錄下，右鍵 -> New -> Layout Resource File，命名為 `item_produce.xml`，內容為：
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="12dp"
    app:cardCornerRadius="16dp"
    app:cardElevation="2dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="16dp"
        android:gravity="center_vertical">

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical">

            <TextView
                android:id="@+id/nameText"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="20sp"
                android:textColor="#000000"
                android:textStyle="bold"/>

            <TextView
                android:id="@+id/categoryText"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="14sp"
                android:textColor="#888888"
                android:layout_marginTop="4dp"/>
        </LinearLayout>

        <TextView
            android:id="@+id/priceText"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="24sp"
            android:textColor="#34C759"
            android:textStyle="bold"/>

    </LinearLayout>
</androidx.cardview.widget.CardView>
```

### 步驟 4：貼上 Java 程式碼並串接 API
在 `app/src/main/java/com/example/produceapp/` 目錄下：

**1. 建立 `ProduceItem.java`**
```java
package com.example.produceapp;

public class ProduceItem {
    public String id;
    public String name;
    public String category;
    public double currentPrice;
    public String level;

    public ProduceItem(String id, String name, String category, double currentPrice, String level) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.currentPrice = currentPrice;
        this.level = level;
    }
}
```

**2. 建立 `ProduceAdapter.java`**
```java
package com.example.produceapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ProduceAdapter extends RecyclerView.Adapter<ProduceAdapter.ViewHolder> {
    private List<ProduceItem> items;

    public ProduceAdapter(List<ProduceItem> items) {
        this.items = items;
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
        holder.priceText.setText(String.format("$%.1f", item.currentPrice));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView categoryText;
        TextView priceText;

        public ViewHolder(View view) {
            super(view);
            nameText = view.findViewById(R.id.nameText);
            categoryText = view.findViewById(R.id.categoryText);
            priceText = view.findViewById(R.id.priceText);
        }
    }
}
```

**3. 修改 `MainActivity.java`**
```java
package com.example.produceapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ProduceAdapter adapter;
    private List<ProduceItem> items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProduceAdapter(items);
        recyclerView.setAdapter(adapter);

        fetchDataFromApi();
    }

    private void fetchDataFromApi() {
        new Thread(() -> {
            try {
                // 注意：Android 模擬器存取 Mac 本機的 localhost 必須使用 10.0.2.2
                URL url = new URL("http://10.0.2.2:5000/api/produce");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONArray jsonArray = new JSONArray(response.toString());
                items.clear();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    items.add(new ProduceItem(
                            obj.getString("id"),
                            obj.getString("name"),
                            obj.getString("category"),
                            obj.getDouble("currentPrice"),
                            obj.getString("level")
                    ));
                }

                // 切換回主執行緒更新 UI
                runOnUiThread(() -> adapter.notifyDataSetChanged());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
```

### 步驟 5：編譯與執行
1. 在 Android Studio 頂部選擇一個模擬器 (例如 Pixel 7 API 34)。
2. 按下綠色的 **Run 按鈕 (Shift + F10)**。
3. App 啟動後，就會透過 `10.0.2.2` 成功連線到您的 Mac 本機 .NET API 並顯示資料！

---

## 💡 核心重點總結 (Troubleshooting)
* **API 必須保持執行**：在測試手機 App 時，VS Code 的終端機必須保持 `dotnet run` 正在執行的狀態。
* **Localhost 的差異**：
  * **iOS 模擬器**與 Mac 共用網路，所以 API 網址寫 `http://localhost:5000`。
  * **Android 模擬器**有自己的虛擬網路，所以存取 Mac 本機必須寫 `http://10.0.2.2:5000`。
* **HTTP 權限**：現代手機系統預設阻擋未加密的 HTTP 連線，所以 iOS 的 `App Transport Security` 和 Android 的 `usesCleartextTraffic` 在本地開發階段是必須設定的。
