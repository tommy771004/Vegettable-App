import SwiftUI
import Charts

// =============================================================================
// CropDetailScreen.swift — 農產品詳情頁面
//
// 進入方式：從 HomeScreen 點擊任一「今日菜價」卡片
//
// 包含：
//   1. 頂部 Hero Card：當日價格、市場、漲跌幅
//   2. 30 天歷史價格折線圖（Swift Charts，iOS 16+）
//   3. 7 日趨勢預測 Badge（後端 /forecast API）
//   4. 全市場比價清單（後端 /compare API，由低到高）
//   5. 快速加入收藏 Button
//
// 設計原則（針對 35-60 歲使用者）：
//   - 最小字體 17pt，關鍵資訊 28pt+
//   - 高對比顏色（深綠 / 紅 / 白底）
//   - 所有可點擊區域 ≥ 48pt 高度
//   - 清楚的區段標題與圖示
// =============================================================================

struct CropDetailScreen: View {
    let produce: ProduceDto

    @State private var history: [HistoricalPriceDto] = []
    @State private var forecast: PricePredictionResponse? = nil
    @State private var markets: [MarketComparisonDto] = []
    @State private var loadState: LoadState = .loading
    @State private var showFavoriteSheet = false
    @State private var targetPriceInput = ""
    @State private var favoriteToast: String? = nil
    @EnvironmentObject var viewModel: ProduceViewModel

    enum LoadState { case loading, success, failure(String) }

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(hex: "E8F5E9"), Color(hex: "C8E6C9")],
                startPoint: .topLeading, endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 20) {
                    // 1. Hero Card
                    DetailHeroCard(produce: produce)
                        .padding(.horizontal)

                    // 2. 趨勢預測 Badge
                    if let fc = forecast {
                        ForecastBadge(forecast: fc)
                            .padding(.horizontal)
                    }

                    switch loadState {
                    case .loading:
                        VStack(spacing: 16) {
                            SkeletonView().frame(height: 200).padding(.horizontal)
                            SkeletonView().frame(height: 140).padding(.horizontal)
                        }
                    case .failure(let msg):
                        ErrorCard(message: msg)
                            .padding(.horizontal)
                    case .success:
                        // 3. 30 天折線圖
                        if !history.isEmpty {
                            HistoryChartCard(history: history, cropName: produce.cropName)
                                .padding(.horizontal)
                        }

                        // 4. 市場比價
                        if !markets.isEmpty {
                            MarketRankingCard(markets: markets, currentMarket: produce.marketName)
                                .padding(.horizontal)
                        }
                    }

                    // 底部 padding
                    Spacer(minLength: 20)
                }
                .padding(.top, 16)
                .padding(.bottom, 32)
            }
        }
        .navigationTitle(produce.cropName)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    targetPriceInput = String(format: "%.1f", produce.avgPrice)
                    showFavoriteSheet = true
                } label: {
                    Label("收藏", systemImage: "heart.fill")
                        .foregroundColor(Color(hex: "E91E63"))
                }
            }
        }
        .sheet(isPresented: $showFavoriteSheet) {
            AddFavoriteSheet(
                produce: produce,
                targetPriceInput: $targetPriceInput,
                onConfirm: { price in
                    Task {
                        let ok = await viewModel.addToFavorites(produceId: produce.cropCode, targetPrice: price)
                        favoriteToast = ok
                            ? "✅ \(produce.cropName) 已加入收藏"
                            : "⚠️ 加入失敗，請稍後再試"
                        showFavoriteSheet = false
                        DispatchQueue.main.asyncAfter(deadline: .now() + 3) { favoriteToast = nil }
                    }
                }
            )
            .presentationDetents([.height(320)])
        }
        .overlay(alignment: .bottom) {
            if let toast = favoriteToast {
                Text(toast)
                    .font(.subheadline.bold())
                    .padding(.horizontal, 20)
                    .padding(.vertical, 12)
                    .background(Color(hex: "2E7D32").opacity(0.95))
                    .foregroundColor(.white)
                    .clipShape(Capsule())
                    .shadow(radius: 8)
                    .padding(.bottom, 24)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .animation(.spring(duration: 0.4), value: favoriteToast)
            }
        }
        .task { await loadData() }
    }

    private func loadData() async {
        loadState = .loading
        async let historyTask = ProduceService.shared.fetchPriceHistory(produceId: produce.cropCode)
        async let forecastTask = ProduceService.shared.getForecast(produceId: produce.cropCode)
        async let marketTask = ProduceService.shared.getMarketComparison(cropName: produce.cropName)

        do {
            let (h, fc, m) = try await (historyTask, forecastTask, marketTask)
            history = h
            forecast = fc
            markets = m
            loadState = .success
        } catch {
            loadState = .failure(error.localizedDescription)
        }
    }
}

// MARK: - Hero Card

private struct DetailHeroCard: View {
    let produce: ProduceDto

    // 假設前一日均價暫無資料，顯示當日均價（後續可從 history 計算）
    var body: some View {
        VStack(spacing: 0) {
            // 深綠色頂部
            HStack {
                VStack(alignment: .leading, spacing: 6) {
                    Text(produce.cropName)
                        .font(.system(size: 28, weight: .heavy, design: .rounded))
                        .foregroundColor(.white)
                    Text(produce.marketName)
                        .font(.subheadline)
                        .foregroundColor(.white.opacity(0.8))
                    Text(produce.date)
                        .font(.caption)
                        .foregroundColor(.white.opacity(0.6))
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 4) {
                    Text("$\(String(format: "%.1f", produce.avgPrice))")
                        .font(.system(size: 42, weight: .heavy, design: .rounded))
                        .foregroundColor(.white)
                    Text("元 / 公斤")
                        .font(.callout)
                        .foregroundColor(.white.opacity(0.8))
                }
            }
            .padding(20)
            .background(
                LinearGradient(
                    colors: [Color(hex: "1B5E20"), Color(hex: "2E7D32")],
                    startPoint: .topLeading, endPoint: .bottomTrailing
                )
            )

            // 白色底部：交易量
            HStack {
                Label("交易量", systemImage: "scale.3d")
                    .font(.subheadline)
                    .foregroundColor(Color(hex: "558B2F"))
                Spacer()
                Text("\(String(format: "%.0f", produce.transQuantity)) 公斤")
                    .font(.subheadline.bold())
                    .foregroundColor(Color(hex: "1B5E20"))
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .background(.ultraThinMaterial)
        }
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .shadow(color: Color(hex: "1B5E20").opacity(0.25), radius: 10, x: 0, y: 5)
    }
}

// MARK: - 趨勢預測 Badge

private struct ForecastBadge: View {
    let forecast: PricePredictionResponse

    private var trendColor: Color {
        switch forecast.trend {
        case "Up":     return Color(hex: "D32F2F")
        case "Down":   return Color(hex: "1976D2")
        default:       return Color(hex: "F57F17")
        }
    }
    private var trendIcon: String {
        switch forecast.trend {
        case "Up":     return "arrow.up.right.circle.fill"
        case "Down":   return "arrow.down.right.circle.fill"
        default:       return "minus.circle.fill"
        }
    }
    private var trendLabel: String {
        switch forecast.trend {
        case "Up":     return "預測上漲"
        case "Down":   return "預測下跌"
        default:       return "趨勢平穩"
        }
    }

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: trendIcon)
                .font(.system(size: 32))
                .foregroundColor(trendColor)

            VStack(alignment: .leading, spacing: 4) {
                Text(trendLabel)
                    .font(.headline)
                    .foregroundColor(trendColor)
                Text(forecast.message)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }

            Spacer()

            VStack(spacing: 2) {
                Text("近7日均價")
                    .font(.caption2)
                    .foregroundColor(.secondary)
                Text("$\(String(format: "%.1f", forecast.recentAverage))")
                    .font(.headline.bold())
                    .foregroundColor(trendColor)
            }
        }
        .padding(16)
        .background(.ultraThinMaterial)
        .background(trendColor.opacity(0.06))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(trendColor.opacity(0.3), lineWidth: 1.5)
        )
    }
}

// MARK: - 30 天歷史折線圖

private struct HistoryChartCard: View {
    let history: [HistoricalPriceDto]
    let cropName: String

    private var minPrice: Double { history.map(\.avgPrice).min() ?? 0 }
    private var maxPrice: Double { history.map(\.avgPrice).max() ?? 1 }
    private var priceRange: Double { maxPrice - minPrice }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Label("30 天價格走勢", systemImage: "chart.line.uptrend.xyaxis")
                    .font(.headline)
                    .foregroundColor(Color(hex: "1B5E20"))
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text("最高 $\(String(format: "%.1f", maxPrice))")
                        .font(.caption.bold())
                        .foregroundColor(Color(hex: "D32F2F"))
                    Text("最低 $\(String(format: "%.1f", minPrice))")
                        .font(.caption.bold())
                        .foregroundColor(Color(hex: "1976D2"))
                }
            }

            Chart(history) { point in
                AreaMark(
                    x: .value("日期", point.date),
                    yStart: .value("基線", minPrice - priceRange * 0.1),
                    yEnd: .value("均價", point.avgPrice)
                )
                .foregroundStyle(
                    LinearGradient(
                        colors: [Color(hex: "4CAF50").opacity(0.3), Color(hex: "4CAF50").opacity(0)],
                        startPoint: .top, endPoint: .bottom
                    )
                )
                LineMark(
                    x: .value("日期", point.date),
                    y: .value("均價", point.avgPrice)
                )
                .foregroundStyle(Color(hex: "2E7D32"))
                .lineStyle(StrokeStyle(lineWidth: 2.5))
                PointMark(
                    x: .value("日期", point.date),
                    y: .value("均價", point.avgPrice)
                )
                .foregroundStyle(Color(hex: "2E7D32"))
                .symbolSize(20)
            }
            .chartXAxis {
                AxisMarks(values: .stride(by: 7)) { _ in
                    AxisGridLine(stroke: StrokeStyle(dash: [2, 3]))
                        .foregroundStyle(Color.gray.opacity(0.3))
                    AxisValueLabel()
                        .font(.caption2)
                        .foregroundStyle(Color.secondary)
                }
            }
            .chartYAxis {
                AxisMarks { value in
                    AxisGridLine(stroke: StrokeStyle(dash: [2, 3]))
                        .foregroundStyle(Color.gray.opacity(0.3))
                    AxisValueLabel {
                        if let v = value.as(Double.self) {
                            Text("$\(String(format: "%.0f", v))")
                                .font(.caption2)
                                .foregroundStyle(Color.secondary)
                        }
                    }
                }
            }
            .frame(height: 200)
        }
        .padding(16)
        .liquidGlass()
    }
}

// MARK: - 市場比價排行

private struct MarketRankingCard: View {
    let markets: [MarketComparisonDto]
    let currentMarket: String

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("全台市場比價", systemImage: "storefront.fill")
                .font(.headline)
                .foregroundColor(Color(hex: "1B5E20"))

            ForEach(Array(markets.enumerated()), id: \.element.marketName) { idx, market in
                HStack(spacing: 12) {
                    // 名次 badge
                    ZStack {
                        Circle()
                            .fill(idx == 0 ? Color(hex: "F9A825") : Color(hex: "E8F5E9"))
                            .frame(width: 32, height: 32)
                        Text("#\(idx + 1)")
                            .font(.caption.bold())
                            .foregroundColor(idx == 0 ? .white : Color(hex: "558B2F"))
                    }

                    VStack(alignment: .leading, spacing: 2) {
                        Text(market.marketName)
                            .font(.subheadline.bold())
                            .foregroundColor(market.marketName == currentMarket ? Color(hex: "1B5E20") : .primary)
                        Text("交易量 \(String(format: "%.0f", market.transQuantity)) kg")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }

                    Spacer()

                    Text("$\(String(format: "%.1f", market.avgPrice))")
                        .font(.system(size: 20, weight: .bold, design: .rounded))
                        .foregroundColor(idx == 0 ? Color(hex: "1976D2") : Color(hex: "2E7D32"))

                    // 最低價標籤
                    if idx == 0 {
                        Text("最低")
                            .font(.caption2.bold())
                            .padding(.horizontal, 6)
                            .padding(.vertical, 3)
                            .background(Color(hex: "1976D2").opacity(0.15))
                            .foregroundColor(Color(hex: "1976D2"))
                            .clipShape(Capsule())
                    }
                }
                .padding(12)
                .background(market.marketName == currentMarket
                    ? Color(hex: "E8F5E9")
                    : Color.clear
                )
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(
                            market.marketName == currentMarket
                                ? Color(hex: "4CAF50").opacity(0.5)
                                : Color.gray.opacity(0.1),
                            lineWidth: 1
                        )
                )

                if idx < markets.count - 1 {
                    Divider().padding(.leading, 44)
                }
            }
        }
        .padding(16)
        .liquidGlass()
    }
}

// MARK: - 錯誤卡片

private struct ErrorCard: View {
    let message: String
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.title2)
                .foregroundColor(.orange)
            Text("資料載入失敗：\(message)")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .padding()
        .liquidGlass()
    }
}
