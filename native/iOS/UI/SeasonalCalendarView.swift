import SwiftUI

// 為何修改：原先使用硬編碼的 4 種蔬果且節氣寫死為「立秋」。
// 現在改為從後端 /seasonal API 動態取得當季農產品，並根據當前月份自動計算節氣，
// 讓資料隨著季節自動更新，不需要手動維護。

struct SeasonalCrop: Identifiable {
    let id = UUID()
    let name: String
    let emoji: String
    let tips: String
}

struct SeasonalCalendarView: View {
    @State private var seasonalCrops: [SeasonalCrop] = []
    @State private var isLoading = true
    @State private var isOfflineMode = false

    // 根據當前月份自動判斷節氣
    private var currentSeason: String {
        let month = Calendar.current.component(.month, from: Date())
        switch month {
        case 1: return "小寒 ~ 大寒"
        case 2: return "立春 ~ 雨水"
        case 3: return "驚蟄 ~ 春分"
        case 4: return "清明 ~ 穀雨"
        case 5: return "立夏 ~ 小滿"
        case 6: return "芒種 ~ 夏至"
        case 7: return "小暑 ~ 大暑"
        case 8: return "立秋 ~ 處暑"
        case 9: return "白露 ~ 秋分"
        case 10: return "寒露 ~ 霜降"
        case 11: return "立冬 ~ 小雪"
        case 12: return "大雪 ~ 冬至"
        default: return "未知節氣"
        }
    }

    // 根據作物名稱自動對應 emoji（更具體的名稱需優先判斷）
    private func emojiFor(_ name: String) -> String {
        if name.contains("番茄") { return "🍅" }          // 優先於「茄」
        if name.contains("木瓜") { return "🥭" }          // 優先於「瓜」
        if name.contains("南瓜") { return "🎃" }          // 優先於「瓜」
        if name.contains("苦瓜") { return "🥒" }          // 優先於「瓜」
        if name.contains("胡蘿蔔") { return "🥕" }        // 優先於「蘿蔔」
        if name.contains("蘿蔔") { return "🥕" }
        if name.contains("葡萄") { return "🍇" }
        if name.contains("柑") || name.contains("桔") { return "🍊" }
        if name.contains("茄") { return "🍆" }
        if name.contains("瓜") { return "🍉" }
        if name.contains("菠") { return "🥬" }
        if name.contains("甘藍") { return "🥬" }
        if name.contains("菜") { return "🥬" }
        return "🌱"
    }

    var body: some View {
        NavigationView {
            VStack(alignment: .leading) {
                HStack {
                    Text("目前節氣：")
                        .font(.headline)
                        .foregroundColor(.gray)
                    Text(currentSeason)
                        .font(.title3)
                        .fontWeight(.bold)
                        .foregroundColor(.green)
                }
                .padding()
                .background(Color.green.opacity(0.1))
                .cornerRadius(12)
                .padding(.horizontal)

                if isOfflineMode {
                    HStack {
                        Image(systemName: "wifi.slash")
                        Text("離線模式：顯示預設資料，可能非最新")
                            .font(.caption)
                    }
                    .foregroundColor(.orange)
                    .padding(.horizontal)
                }

                if isLoading {
                    ProgressView("載入當季蔬果...")
                        .padding()
                } else if seasonalCrops.isEmpty {
                    Text("目前沒有當季蔬果資料")
                        .foregroundColor(.secondary)
                        .padding()
                } else {
                    List(seasonalCrops) { crop in
                        HStack(spacing: 16) {
                            Text(crop.emoji)
                                .font(.system(size: 40))

                            VStack(alignment: .leading, spacing: 4) {
                                Text(crop.name)
                                    .font(.headline)
                                Text(crop.tips)
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)
                            }
                        }
                        .padding(.vertical, 8)
                    }
                    .listStyle(PlainListStyle())
                }
            }
            .navigationTitle("當季蔬果曆")
            .onAppear {
                loadSeasonalCrops()
            }
        }
    }

    private func loadSeasonalCrops() {
        ProduceService.shared.getSeasonalCrops { result in
            DispatchQueue.main.async {
                self.isLoading = false
                switch result {
                case .success(let crops):
                    self.seasonalCrops = crops.map { dto in
                        SeasonalCrop(
                            name: dto.cropName,
                            emoji: self.emojiFor(dto.cropName),
                            tips: dto.description
                        )
                    }
                case .failure:
                    // API 失敗時使用預設資料並標示離線模式
                    self.isOfflineMode = true
                    self.seasonalCrops = [
                        SeasonalCrop(name: "高麗菜", emoji: "🥬", tips: "挑選葉片緊密、拿起來有重量感"),
                        SeasonalCrop(name: "白蘿蔔", emoji: "🥕", tips: "表皮光滑、輕敲有清脆聲"),
                        SeasonalCrop(name: "番茄", emoji: "🍅", tips: "顏色鮮紅均勻、蒂頭翠綠"),
                        SeasonalCrop(name: "菠菜", emoji: "🥬", tips: "葉片翠綠、莖部不發黑")
                    ]
                }
            }
        }
    }
}

struct SeasonalCalendarView_Previews: PreviewProvider {
    static var previews: some View {
        SeasonalCalendarView()
    }
}
