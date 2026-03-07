import SwiftUI
import Charts

struct PriceTrendView: View {
    var historical: [HistoricalPriceDto]
    var predicted: [PricePredictionDto]

    private var hasData: Bool {
        !historical.isEmpty || !predicted.isEmpty
    }

    var body: some View {
        Group {
            if !hasData {
                VStack(spacing: 8) {
                    Image(systemName: "chart.line.uptrend.xyaxis")
                        .font(.largeTitle)
                        .foregroundColor(.gray)
                    Text("暫無價格趨勢資料")
                        .foregroundColor(.gray)
                        .font(.caption)
                }
                .frame(height: 200)
                .accessibilityLabel("無價格趨勢資料")
            } else {
                Chart {
                    // 歷史價格 (實線，深綠色)
                    ForEach(historical, id: \.date) { item in
                        LineMark(
                            x: .value("日期", item.date),
                            y: .value("價格", item.avgPrice)
                        )
                        .foregroundStyle(Color(hex: "2E7D32") ?? .green)
                        .symbol(Circle())
                        .interpolationMethod(.catmullRom)
                        .accessibilityLabel("\(item.date) 歷史均價 \(item.avgPrice) 元")
                    }

                    // 預測價格 (虛線，橘色)
                    ForEach(predicted, id: \.date) { item in
                        LineMark(
                            x: .value("日期", item.date),
                            y: .value("價格", item.predictedPrice)
                        )
                        .foregroundStyle(.orange)
                        .lineStyle(StrokeStyle(lineWidth: 2, dash: [5, 5]))
                        .symbol(Circle())
                        .interpolationMethod(.catmullRom)
                        .accessibilityLabel("\(item.date) 預測價格 \(item.predictedPrice) 元")
                    }
                }
                .frame(height: 200)
                .chartXAxis {
                    AxisMarks(values: .automatic(desiredCount: 5))
                }
                .accessibilityLabel("價格趨勢圖，包含 \(historical.count) 筆歷史資料及 \(predicted.count) 筆預測資料")
            }
        }
    }
}
