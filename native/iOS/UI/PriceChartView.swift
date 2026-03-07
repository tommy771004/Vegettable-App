import SwiftUI
import Charts

struct PriceData: Identifiable {
    let id = UUID()
    let date: Date
    let price: Double
}

struct PriceChartView: View {
    let produceId: String
    let produceName: String

    @State private var priceHistory: [PriceData] = []
    @State private var isLoading = true
    @State private var errorMessage: String?

    private static let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        return f
    }()

    var body: some View {
        VStack(alignment: .leading) {
            Text("\(produceName) 價格走勢 (近30天)")
                .font(.headline)
                .padding(.bottom, 8)

            if isLoading {
                ProgressView("載入中...")
                    .frame(maxWidth: .infinity)
                    .frame(height: 250)
            } else if let error = errorMessage {
                VStack(spacing: 8) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.largeTitle)
                        .foregroundColor(.secondary)
                    Text(error)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 250)
            } else if priceHistory.isEmpty {
                VStack(spacing: 8) {
                    Image(systemName: "chart.line.downtrend.xyaxis")
                        .font(.largeTitle)
                        .foregroundColor(.secondary)
                    Text("暫無歷史資料")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 250)
            } else {
                Chart {
                    ForEach(priceHistory) { item in
                        LineMark(
                            x: .value("日期", item.date, unit: .day),
                            y: .value("價格 (元/公斤)", item.price)
                        )
                        .foregroundStyle(.green)
                        .symbol(Circle())
                        .interpolationMethod(.catmullRom)

                        AreaMark(
                            x: .value("日期", item.date, unit: .day),
                            y: .value("價格 (元/公斤)", item.price)
                        )
                        .foregroundStyle(
                            .linearGradient(
                                colors: [.green.opacity(0.3), .clear],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                    }
                }
                .frame(height: 250)
                .chartXAxis {
                    AxisMarks(values: .stride(by: .day, count: 7)) { value in
                        AxisGridLine()
                        AxisTick()
                        AxisValueLabel(format: .dateTime.month().day())
                    }
                }
            }
        }
        .padding()
        .background(Color(UIColor.systemBackground))
        .cornerRadius(12)
        .shadow(radius: 5)
        .task {
            await loadData()
        }
    }

    private func loadData() async {
        isLoading = true
        errorMessage = nil
        do {
            let dtos = try await ProduceService.shared.fetchPriceHistory(produceId: produceId)
            priceHistory = dtos.compactMap { dto in
                guard let date = Self.dateFormatter.date(from: dto.date) else { return nil }
                return PriceData(date: date, price: dto.avgPrice)
            }.sorted { $0.date < $1.date }
            isLoading = false
        } catch {
            errorMessage = "載入失敗，請稍後再試"
            isLoading = false
        }
    }
}

struct PriceChartView_Previews: PreviewProvider {
    static var previews: some View {
        PriceChartView(produceId: "LA1", produceName: "高麗菜")
    }
}
