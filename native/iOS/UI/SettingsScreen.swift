import SwiftUI

struct SettingsScreen: View {
    private var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0"
    }

    var body: some View {
        NavigationView {
            ZStack {
                // 背景透明，由外層 MainTabView 提供漸層底色
                Color.clear.ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 16) {
                        SettingRow(
                            icon: "bell.fill",
                            title: "推播通知設定",
                            subtitle: "管理價格異常與達標提醒",
                            action: { /* TODO: navigate to notification settings */ }
                        )
                        SettingRow(
                            icon: "globe",
                            title: "語言與語音",
                            subtitle: "切換印尼語/越南語及語音速度",
                            action: { /* TODO: navigate to language settings */ }
                        )
                        SettingRow(
                            icon: "arrow.triangle.2.circlepath.circle.fill",
                            title: "離線快取狀態",
                            subtitle: "上次同步時間: 剛剛",
                            action: { /* TODO: trigger cache refresh */ }
                        )
                        SettingRow(
                            icon: "info.circle.fill",
                            title: "關於 App",
                            subtitle: "版本 \(appVersion)",
                            action: { /* TODO: navigate to about page */ }
                        )
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
    var action: (() -> Void)? = nil

    var body: some View {
        Button(action: { action?() }) {
            HStack(spacing: 16) {
                Image(systemName: icon)
                    .font(.title)
                    .foregroundColor(Color(hex: "2E7D32"))
                    .frame(width: 32)
                    .accessibilityHidden(true)

                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.headline)
                        .foregroundColor(Color(hex: "1B5E20"))
                    Text(subtitle)
                        .font(.subheadline)
                        .foregroundColor(Color(hex: "388E3C"))
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundColor(Color(hex: "81C784"))
                    .accessibilityHidden(true)
            }
            .padding()
            .liquidGlass()
        }
        .buttonStyle(.plain)
        .accessibilityLabel(title)
        .accessibilityHint(subtitle)
    }
}
