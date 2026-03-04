import SwiftUI

struct SeasonalCrop: Identifiable {
    let id = UUID()
    let name: String
    let emoji: String
    let tips: String
}

struct SeasonalCalendarView: View {
    let currentSeason = "立秋 (約 8/7 - 8/22)"
    
    let seasonalCrops: [SeasonalCrop] = [
        SeasonalCrop(name: "西瓜", emoji: "🍉", tips: "拍打聲音清脆，蒂頭捲曲"),
        SeasonalCrop(name: "小黃瓜", emoji: "🥒", tips: "瓜體直挺，表面有刺"),
        SeasonalCrop(name: "茄子", emoji: "🍆", tips: "表皮光滑發亮，蒂頭無枯萎"),
        SeasonalCrop(name: "空心菜", emoji: "🥬", tips: "葉片翠綠，莖部不發黑")
    ]
    
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
                
                List(seasonalCrops) { crop in
                    HStack(spacing: 16) {
                        Text(crop.emoji)
                            .font(.system(size: 40))
                        
                        VStack(alignment: .leading, spacing: 4) {
                            Text(crop.name)
                                .font(.headline)
                            Text("挑選訣竅：\(crop.tips)")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding(.vertical, 8)
                }
                .listStyle(PlainListStyle())
            }
            .navigationTitle("當季蔬果曆")
        }
    }
}

struct SeasonalCalendarView_Previews: PreviewProvider {
    static var previews: some View {
        SeasonalCalendarView()
    }
}
