import SwiftUI
import Charts

struct ContentView: View {
    @StateObject private var viewModel = ProduceViewModel()
    @State private var searchText = ""
    @State private var selectedCategory = "全部"
    @State private var isRetailMode = false // 零售價模式
    @State private var selectedMarket = "台北一" // 預設市場
    
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
                    // 市場選擇與零售切換區
                    VStack(spacing: 12) {
                        HStack {
                            Text("市場：")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                            
                            Picker("選擇市場", selection: $selectedMarket) {
                                ForEach(viewModel.markets, id: \.self) { market in
                                    Text(market).tag(market)
                                }
                            }
                            .pickerStyle(.menu)
                            .onChange(of: selectedMarket) { newValue in
                                Task { await viewModel.fetchData(for: newValue) }
                            }
                            
                            Spacer()
                            
                            Toggle("零售估價", isOn: $isRetailMode)
                                .toggleStyle(.switch)
                                .labelsHidden()
                            Text("零售(台斤)")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        
                        Picker("分類", selection: $selectedCategory) {
                            ForEach(categories, id: \.self) { Text($0) }
                        }
                        .pickerStyle(.segmented)
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
                        Button("重試") { Task { await viewModel.fetchData(for: selectedMarket) } }
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
                            await viewModel.fetchData(for: selectedMarket) // 下拉更新
                        }
                    }
                }
            }
            .navigationTitle("當令行情")
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
    @StateObject private var viewModel = ProduceViewModel()
    @State private var historyData: [PriceHistory] = []
    @State private var isLoadingHistory = true
    
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
                    
                    if isLoadingHistory {
                        ProgressView()
                            .frame(height: 250)
                            .frame(maxWidth: .infinity)
                    } else if historyData.isEmpty {
                        Text("暫無歷史資料")
                            .foregroundColor(.secondary)
                            .frame(height: 250)
                            .frame(maxWidth: .infinity)
                    } else {
                        // SwiftUI Charts 圖表實作
                        Chart {
                            ForEach(historyData) { data in
                                let displayPrice = isRetailMode ? (data.price * 2.5 * 0.6) : data.price
                                
                                LineMark(
                                    x: .value("日期", data.date),
                                    y: .value("價格", displayPrice)
                                )
                                .symbol(Circle())
                                .interpolationMethod(.catmullRom) // 平滑曲線
                                
                                AreaMark(
                                    x: .value("日期", data.date),
                                    y: .value("價格", displayPrice)
                                )
                                .foregroundStyle(Gradient(colors: [.blue.opacity(0.3), .clear]))
                            }
                        }
                        .frame(height: 250)
                        .chartYAxis {
                            AxisMarks(position: .leading)
                        }
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
        .task {
            // 載入真實歷史資料
            historyData = await viewModel.fetchHistory(for: item.id, currentPrice: item.currentPrice)
            isLoadingHistory = false
        }
    }
}
