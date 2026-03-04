import SwiftUI

enum BottomTab {
    case home, favorites, settings
}

struct MainTabView: View {
    @State private var currentTab: BottomTab = .home
    
    var body: some View {
        ZStack(alignment: .bottom) {
            // 全站漸層底色 (iOS 26 Liquid Glass 風格)
            LinearGradient(
                colors: [Color(hex: "E8F5E9"), Color(hex: "C8E6C9")],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()
            
            // 主要內容區域
            Group {
                switch currentTab {
                case .home:
                    HomeScreen()
                case .favorites:
                    FavoritesScreen()
                case .settings:
                    SettingsScreen()
                }
            }
            .padding(.bottom, 80) // 預留空間給懸浮導覽列
            
            // 懸浮式底部導覽列 (Floating Liquid Glass Bottom Navigation)
            HStack {
                TabBarItem(icon: "house.fill", title: "首頁", isSelected: currentTab == .home) {
                    currentTab = .home
                }
                Spacer()
                TabBarItem(icon: "heart.fill", title: "收藏", isSelected: currentTab == .favorites) {
                    currentTab = .favorites
                }
                Spacer()
                TabBarItem(icon: "gearshape.fill", title: "設定", isSelected: currentTab == .settings) {
                    currentTab = .settings
                }
            }
            .padding(.vertical, 12)
            .padding(.horizontal, 32)
            .liquidGlass()
            .padding(.horizontal, 24)
            .padding(.bottom, 16)
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
                    .font(.system(size: 24))
                Text(title)
                    .font(.caption)
                    .fontWeight(isSelected ? .bold : .regular)
            }
            .foregroundColor(isSelected ? Color(hex: "1B5E20") : Color(hex: "81C784"))
        }
    }
}
