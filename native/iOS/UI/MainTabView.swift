import SwiftUI

enum BottomTab {
    case home, favorites, explore, settings
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
                case .explore:
                    ExploreMenuView()
                case .settings:
                    SettingsScreen()
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
                } header: {
                    Text("進階功能")
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle("探索")
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
