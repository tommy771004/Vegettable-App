import SwiftUI
import Charts

struct PriceData: Identifiable {
    let id = UUID()
    let date: Date
    let price: Double
}

struct PriceChartView: View {
    let priceHistory: [PriceData] = [
        PriceData(date: Calendar.current.date(byAdding: .day, value: -6, to: Date())!, price: 25.5),
        PriceData(date: Calendar.current.date(byAdding: .day, value: -5, to: Date())!, price: 26.0),
        PriceData(date: Calendar.current.date(byAdding: .day, value: -4, to: Date())!, price: 24.5),
        PriceData(date: Calendar.current.date(byAdding: .day, value: -3, to: Date())!, price: 28.0),
        PriceData(date: Calendar.current.date(byAdding: .day, value: -2, to: Date())!, price: 30.5),
        PriceData(date: Calendar.current.date(byAdding: .day, value: -1, to: Date())!, price: 35.0),
        PriceData(date: Date(), price: 32.0)
    ]

    var body: some View {
        VStack(alignment: .leading) {
            Text("高麗菜價格走勢 (近7天)")
                .font(.headline)
                .padding(.bottom, 8)
            
            Chart {
                ForEach(priceHistory) { item in
                    LineMark(
                        x: .value("日期", item.date, unit: .day),
                        y: .value("價格 (元/斤)", item.price)
                    )
                    .foregroundStyle(.green)
                    .symbol(Circle())
                    .interpolationMethod(.catmullRom)
                    
                    AreaMark(
                        x: .value("日期", item.date, unit: .day),
                        y: .value("價格 (元/斤)", item.price)
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
                AxisMarks(values: .stride(by: .day)) { value in
                    AxisGridLine()
                    AxisTick()
                    AxisValueLabel(format: .dateTime.month().day())
                }
            }
        }
        .padding()
        .background(Color(UIColor.systemBackground))
        .cornerRadius(12)
        .shadow(radius: 5)
    }
}

struct PriceChartView_Previews: PreviewProvider {
    static var previews: some View {
        PriceChartView()
    }
}
