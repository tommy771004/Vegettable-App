import SwiftUI

struct SettingsScreen: View {
    var body: some View {
        NavigationView {
            ZStack {
                // 背景透明，由外層 MainTabView 提供漸層底色
                Color.clear.ignoresSafeArea()
                
                ScrollView {
                    VStack(spacing: 16) {
                        SettingRow(icon: "bell.fill", title: "推播通知設定", subtitle: "管理價格異常與達標提醒")
                        SettingRow(icon: "globe", title: "語言與語音", subtitle: "切換印尼語/越南語及語音速度")
                        SettingRow(icon: "arrow.triangle.2.circlepath.circle.fill", title: "離線快取狀態", subtitle: "上次同步時間: 剛剛")
                        SettingRow(icon: "info.circle.fill", title: "關於 App", subtitle: "版本 1.0.0 (iOS 26 Liquid Glass Style)")
                    }
                    .padding()
                }
            }
            .navigationTitle("設定")
        }
    }
}

struct SettingRow: View {
    let icon: String
    let title: String
    let subtitle: String
    
    var body: some View {
        HStack(spacing: 16) {
            Image(systemName: icon)
                .font(.title)
                .foregroundColor(Color(hex: "2E7D32"))
                .frame(width: 32)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.headline)
                    .foregroundColor(Color(hex: "1B5E20"))
                Text(subtitle)
                    .font(.subheadline)
                    .foregroundColor(Color(hex: "388E3C"))
            }
            Spacer()
        }
        .padding()
        .liquidGlass()
    }
}
