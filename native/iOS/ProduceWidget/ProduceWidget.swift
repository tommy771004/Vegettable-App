import WidgetKit
import SwiftUI

// 1. 定義 Widget 的資料結構
struct ProduceEntry: TimelineEntry {
    let date: Date
    let cropName: String
    let price: String
    let trend: String
}

// 2. 提供 Widget 資料的 Provider
struct ProduceProvider: TimelineProvider {
    func placeholder(in context: Context) -> ProduceEntry {
        ProduceEntry(date: Date(), cropName: "高麗菜", price: "$ 25.0", trend: "▲ 15%")
    }

    func getSnapshot(in context: Context, completion: @escaping (ProduceEntry) -> ()) {
        let entry = ProduceEntry(date: Date(), cropName: "高麗菜", price: "$ 25.0", trend: "▲ 15%")
        completion(entry)
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<Entry>) -> ()) {
        // 實務上這裡會從 App Group 的 UserDefaults 或 SwiftData 讀取最新菜價
        let entry = ProduceEntry(date: Date(), cropName: "高麗菜", price: "$ 25.0", trend: "▲ 15%")
        let timeline = Timeline(entries: [entry], policy: .atEnd)
        completion(timeline)
    }
}

// 3. Widget 的 UI 視圖 (Liquid Glass 風格)
struct ProduceWidgetEntryView : View {
    var entry: ProduceProvider.Entry

    var body: some View {
        ZStack {
            // 背景漸層
            LinearGradient(gradient: Gradient(colors: [Color(hex: "E8F5E9"), Color(hex: "C8E6C9")]),
                           startPoint: .topLeading,
                           endPoint: .bottomTrailing)
            
            VStack(spacing: 8) {
                Text("今日 \(entry.cropName)")
                    .font(.headline)
                    .foregroundColor(Color(hex: "1B5E20"))
                
                Text(entry.price)
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundColor(.black)
                
                Text(entry.trend)
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundColor(.red)
            }
            .padding()
        }
    }
}

// 4. 註冊 Widget
@main
struct ProduceWidget: Widget {
    let kind: String = "ProduceWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: ProduceProvider()) { entry in
            ProduceWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("今日菜價")
        .description("在首頁快速查看您關注的蔬菜價格。")
        .supportedFamilies([.systemSmall])
    }
}

// 輔助 Color Extension
extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (1, 1, 1, 0)
        }

        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue:  Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}
