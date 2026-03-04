import SwiftUI
import Charts

struct PriceTrendView: View {
    var historical: [HistoricalPriceDto]
    var predicted: [PricePredictionDto]

    var body: some View {
        Chart {
            // 歷史價格 (實線，深綠色)
            ForEach(historical, id: \.date) { item in
                LineMark(
                    x: .value("Date", item.date),
                    y: .value("Price", item.avgPrice)
                )
                .foregroundStyle(Color(hex: "2E7D32"))
                .symbol(Circle())
                .interpolationMethod(.catmullRom)
            }
            
            // 預測價格 (虛線，橘色)
            ForEach(predicted, id: \.date) { item in
                LineMark(
                    x: .value("Date", item.date),
                    y: .value("Price", item.predictedPrice)
                )
                .foregroundStyle(.orange)
                .lineStyle(StrokeStyle(lineWidth: 2, dash: [5, 5]))
                .symbol(Circle())
                .interpolationMethod(.catmullRom)
            }
        }
        .frame(height: 200)
        .chartXAxis {
            AxisMarks(values: .automatic(desiredCount: 5))
        }
    }
}
