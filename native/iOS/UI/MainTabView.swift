import SwiftUI

enum BottomTab {
    case home, favorites, grocery, explore, settings
}

struct MainTabView: View {
    @State private var currentTab: BottomTab = .home
    // 單一 ViewModel 實例：HomeScreen 和 FavoritesScreen 共享同一份資料，
    // 避免切換頁籤時重複發送 API 請求
    @StateObject private var sharedViewModel = ProduceViewModel()

    var body: some View {
        ZStack(alignment: .bottom) {
            LinearGradient(
                colors: [Color(hex: "E8F5E9"), Color(hex: "C8E6C9")],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            Group {
                switch currentTab {
                case .home:
                    HomeScreen()
                        .environmentObject(sharedViewModel)
                case .favorites:
                    FavoritesScreen()
                        .environmentObject(sharedViewModel)
                case .grocery:
                    SmartGroceryListScreen()
                case .explore:
                    ExploreMenuView()
                case .settings:
                    SettingsScreen()
                        .environmentObject(sharedViewModel)
                }
            }
            .padding(.bottom, 80)

            HStack {
                TabBarItem(icon: "house.fill", title: "首頁", isSelected: currentTab == .home) {
                    currentTab = .home
                }
                Spacer()
                TabBarItem(icon: "heart.fill", title: "收藏", isSelected: currentTab == .favorites) {
                    currentTab = .favorites
                }
                Spacer()
                TabBarItem(icon: "cart.fill", title: "買菜", isSelected: currentTab == .grocery) {
                    currentTab = .grocery
                }
                Spacer()
                TabBarItem(icon: "square.grid.2x2.fill", title: "探索", isSelected: currentTab == .explore) {
                    currentTab = .explore
                }
                Spacer()
                TabBarItem(icon: "gearshape.fill", title: "設定", isSelected: currentTab == .settings) {
                    currentTab = .settings
                }
            }
            .padding(.vertical, 12)
            .padding(.horizontal, 24)
            .liquidGlass()
            .padding(.horizontal, 24)
            .padding(.bottom, 16)
        }
    }
}

// 探索功能選單：原本無導覽入口的孤立畫面統一由此進入
struct ExploreMenuView: View {
    var body: some View {
        NavigationView {
            List {
                Section {
                    NavigationLink(destination: ElderlyModeView()) {
                        Label("長輩友善模式", systemImage: "person.2.fill")
                    }
                    NavigationLink(destination: SeasonalCalendarView()) {
                        Label("當季蔬果日曆", systemImage: "calendar")
                    }
                    NavigationLink(destination: CommunityReportView()) {
                        Label("社群物價回報", systemImage: "megaphone.fill")
                    }
                    NavigationLink(destination: PriceAlertSetupView()) {
                        Label("目標提醒設定", systemImage: "bell.badge.fill")
                    }
                    NavigationLink(destination: MarketComparisonView()) {
                        Label("市場比價查詢", systemImage: "storefront.fill")
                    }
                } header: {
                    Text("進階功能")
                }

                // 我的貢獻：顯示使用者點數、等級與升級進度
                Section {
                    UserStatsCard()
                } header: {
                    Text("我的貢獻")
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle("探索")
        }
    }
}

// MARK: - 使用者貢獻統計卡片
/// 呼叫 /user-stats 顯示使用者的累積點數、等級標籤與升級進度條
struct UserStatsCard: View {
    @State private var stats: UserStatsDto? = nil
    @State private var isLoading = true
    @State private var loadError: String? = nil

    var body: some View {
        Group {
            if isLoading {
                HStack {
                    ProgressView()
                    Text("載入中...")
                        .foregroundColor(.secondary)
                        .padding(.leading, 8)
                }
                .padding(.vertical, 8)
            } else if let error = loadError {
                Text("無法載入：\(error)")
                    .foregroundColor(.red)
                    .font(.caption)
            } else if let stats = stats {
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            HStack(spacing: 6) {
                                Text(levelEmoji(stats.level))
                                    .font(.title2)
                                Text(stats.level)
                                    .font(.headline)
                                    .foregroundColor(Color(hex: "1B5E20"))
                            }
                            Text("貢獻點數：\(stats.contributionPoints) 分")
                                .font(.subheadline)
                                .foregroundColor(.primary)
                            Text("已回報 \(stats.reportCount) 次物價")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                    }
                    // 升級進度條（每 100 點升一階）
                    let progress = Double(stats.contributionPoints % 100)
                    ProgressView(value: progress, total: 100)
                        .tint(Color(hex: "4CAF50"))
                    Text("距離下一等級還差 \(100 - Int(progress)) 點")
                        .font(.caption2)
                        .foregroundColor(.gray)
                }
                .padding(.vertical, 8)
            }
        }
        .task {
            do {
                stats = try await ProduceService.shared.getUserStats()
            } catch {
                loadError = error.localizedDescription
            }
            isLoading = false
        }
    }

    private func levelEmoji(_ level: String) -> String {
        switch level {
        case "市場達人": return "🏆"
        case "精打細算": return "⭐"
        default:        return "🌱"
        }
    }
}

struct TabBarItem: View {
    let icon: String
    let title: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.system(size: 22))
                Text(title)
                    .font(.caption)
                    .fontWeight(isSelected ? .bold : .regular)
            }
            .foregroundColor(isSelected ? Color(hex: "1B5E20") : Color(hex: "81C784"))
        }
    }
}
