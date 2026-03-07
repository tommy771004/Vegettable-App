import SwiftUI
import UIKit

// =============================================================================
// SettingsScreen.swift — 設定頁面
//
// 功能：
//   1. 推播通知設定：跳轉 iOS 系統通知設定（開啟/關閉 FCM 通知）
//   2. 語言與語音：跳轉 iOS 系統語言設定
//   3. 離線快取狀態：顯示上次同步時間，點擊強制重新整理
//   4. 關於 App：版本資訊 Alert（版本號、資料來源）
// =============================================================================

struct SettingsScreen: View {
    @EnvironmentObject var viewModel: ProduceViewModel

    private var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0"
    }

    @State private var lastSyncTime: String = {
        let fmt = DateFormatter()
        fmt.dateFormat = "HH:mm"
        return fmt.string(from: Date())
    }()
    @State private var isSyncing = false
    @State private var showAboutAlert = false

    var body: some View {
        NavigationView {
            ZStack {
                LinearGradient(
                    colors: [Color(hex: "E8F5E9"), Color(hex: "C8E6C9")],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 16) {
                        // 1. 推播通知設定 → 系統通知設定
                        SettingRow(
                            icon: "bell.fill",
                            title: "推播通知設定",
                            subtitle: "管理價格異常與達標提醒",
                            action: {
                                if let url = URL(string: UIApplication.openNotificationSettingsURLString) {
                                    UIApplication.shared.open(url)
                                }
                            }
                        )

                        // 2. 語言與語音 → 系統設定
                        SettingRow(
                            icon: "globe",
                            title: "語言與語音",
                            subtitle: "切換系統語言（印尼語/越南語）",
                            action: {
                                if let url = URL(string: UIApplication.openSettingsURLString) {
                                    UIApplication.shared.open(url)
                                }
                            }
                        )

                        // 3. 離線快取狀態：點擊重新整理資料
                        SettingRow(
                            icon: isSyncing ? "arrow.triangle.2.circlepath" : "arrow.triangle.2.circlepath.circle.fill",
                            title: "離線快取狀態",
                            subtitle: isSyncing ? "同步中..." : "上次同步時間：\(lastSyncTime)",
                            action: {
                                guard !isSyncing else { return }
                                isSyncing = true
                                Task {
                                    await viewModel.fetchDashboardData()
                                    let fmt = DateFormatter()
                                    fmt.dateFormat = "HH:mm"
                                    lastSyncTime = fmt.string(from: Date())
                                    isSyncing = false
                                }
                            }
                        )

                        // 4. 關於 App
                        SettingRow(
                            icon: "info.circle.fill",
                            title: "關於 App",
                            subtitle: "版本 \(appVersion) · 農委會資料",
                            action: { showAboutAlert = true }
                        )
                    }
                    .padding()
                }
            }
            .navigationTitle("設定")
            .alert("關於蔬菜市場 App", isPresented: $showAboutAlert) {
                Button("關閉", role: .cancel) {}
            } message: {
                Text("""
版本：\(appVersion)
資料來源：行政院農業委員會農產品交易行情站
天氣資料：中央氣象署 RSS 即時資料

本 App 提供台灣農產品批發價格查詢、天氣預警及省錢食譜推薦，協助消費者把握最佳採購時機，支援多語言服務外籍移工族群。
""")
            }
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
