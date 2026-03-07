import SwiftUI

struct FavoritesScreen: View {
    @StateObject private var viewModel = ProduceViewModel()
    
    var body: some View {
        NavigationView {
            ZStack {
                // 漸層背景，與 HomeScreen 保持一致
                LinearGradient(
                    colors: [Color(hex: "E8F5E9"), Color(hex: "C8E6C9")],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .ignoresSafeArea()
                
                ScrollView {
                    switch viewModel.favorites {
                    case .loading:
                        VStack(spacing: 16) {
                            ForEach(0..<5) { _ in
                                SkeletonView()
                                    .frame(height: 100)
                                    .padding(.horizontal)
                            }
                        }
                        .padding(.vertical)
                        
                    case .failure(let error):
                        ErrorView(message: error.localizedDescription) {
                            Task { await viewModel.fetchDashboardData() }
                        }
                        .padding(.top, 50)
                        
                    case .success(let favorites):
                        if favorites.isEmpty {
                            EmptyStateView(message: "還沒有收藏嗎？試試看搜尋高麗菜", systemImage: "heart.slash")
                                .padding(.top, 50)
                        } else {
                            LazyVStack(spacing: 16) {
                                ForEach(favorites) { item in
                                    HStack {
                                        VStack(alignment: .leading, spacing: 6) {
                                            Text(item.cropName)
                                                .font(.title3)
                                                .fontWeight(.bold)
                                                .foregroundColor(Color(hex: "1B5E20"))
                                            
                                            Text("目前價格: $\(String(format: "%.1f", item.currentPrice))/kg")
                                                .font(.subheadline)
                                                .foregroundColor(Color(hex: "388E3C"))
                                            
                                            Text("目標提醒: $\(String(format: "%.1f", item.targetPrice))/kg")
                                                .font(.subheadline)
                                                .foregroundColor(Color(hex: "558B2F"))
                                        }
                                        
                                        Spacer()
                                        
                                        VStack(alignment: .trailing) {
                                            if item.isAlertTriggered {
                                                Image(systemName: "bell.badge.fill")
                                                    .foregroundColor(Color(hex: "E65100"))
                                                    .font(.title)
                                                
                                                Text("已達標！")
                                                    .font(.caption)
                                                    .fontWeight(.bold)
                                                    .foregroundColor(Color(hex: "E65100"))
                                                    .padding(.top, 2)
                                            } else {
                                                Image(systemName: "bell.fill")
                                                    .foregroundColor(Color(hex: "81C784"))
                                                    .font(.title)
                                                
                                                Text("追蹤中")
                                                    .font(.caption)
                                                    .foregroundColor(Color(hex: "81C784"))
                                                    .padding(.top, 2)
                                            }
                                        }
                                    }
                                    .padding()
                                    // 複用 HomeScreen.swift 中定義的 liquidGlass()
                                    .liquidGlass()
                                    .padding(.horizontal)
                                }
                            }
                            .padding(.vertical)
                        }
                    }
                }
            }
            .navigationTitle("我的收藏與追蹤")
            .refreshable {
                await viewModel.fetchDashboardData()
            }
        }
    }
}
