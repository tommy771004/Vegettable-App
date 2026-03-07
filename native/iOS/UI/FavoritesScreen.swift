import SwiftUI
import UIKit

struct FavoritesScreen: View {
    // 由 MainTabView 透過 environmentObject 注入，與 HomeScreen 共享同一份資料
    @EnvironmentObject var viewModel: ProduceViewModel
    private let hapticFeedback = UINotificationFeedbackGenerator()
    @State private var addedToListName: String? = nil

    // 修改目標提醒價格
    @State private var showEditSheet = false
    @State private var editTarget: FavoriteAlertDto? = nil
    @State private var editPriceInput: String = ""
    @State private var editResultMsg: String? = nil

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
                                            Text(item.produceName)
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
                                    // 左滑：複製名稱 + 修改目標提醒價格
                                    .swipeActions(edge: .leading, allowsFullSwipe: false) {
                                        Button {
                                            hapticFeedback.notificationOccurred(.success)
                                            UIPasteboard.general.string = item.produceName
                                            addedToListName = item.produceName
                                        } label: {
                                            Label("複製名稱", systemImage: "cart.badge.plus")
                                        }
                                        .tint(Color(hex: "4CAF50"))

                                        Button {
                                            hapticFeedback.notificationOccurred(.warning)
                                            editTarget = item
                                            editPriceInput = String(format: "%.1f", item.targetPrice)
                                            showEditSheet = true
                                        } label: {
                                            Label("修改提醒", systemImage: "bell.badge.waveform.fill")
                                        }
                                        .tint(Color(hex: "1976D2"))
                                    }
                                    // 右滑：刪除收藏（即時從清單移除，呼叫後端 DELETE）
                                    .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                                        Button(role: .destructive) {
                                            hapticFeedback.notificationOccurred(.warning)
                                            Task { await viewModel.removeFavorite(produceId: item.produceId) }
                                        } label: {
                                            Label("刪除", systemImage: "trash.fill")
                                        }
                                    }
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
            // 修改目標提醒價格的底部 Sheet
            .sheet(isPresented: $showEditSheet) {
                EditTargetPriceSheet(
                    item: editTarget,
                    priceInput: $editPriceInput,
                    onConfirm: { newPrice in
                        guard let item = editTarget else { return }
                        Task {
                            let ok = await viewModel.updateFavoriteTargetPrice(
                                produceId: item.produceId,
                                targetPrice: newPrice
                            )
                            showEditSheet = false
                            editResultMsg = ok
                                ? "✅ \(item.produceName) 目標提醒已更新"
                                : "⚠️ 更新失敗，請稍後再試"
                        }
                    },
                    onCancel: { showEditSheet = false }
                )
                .presentationDetents([.height(320)])
            }
            // Toast：複製成功 / 修改結果
            .overlay(alignment: .bottom) {
                VStack(spacing: 8) {
                    if let name = addedToListName {
                        ToastView(message: "「\(name)」已複製，可貼入買菜清單")
                            .onAppear {
                                DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                                    withAnimation { addedToListName = nil }
                                }
                            }
                    }
                    if let result = editResultMsg {
                        ToastView(message: result)
                            .onAppear {
                                DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                                    withAnimation { editResultMsg = nil }
                                }
                            }
                    }
                }
                .padding(.bottom, 24)
            }
            .animation(.easeInOut(duration: 0.3), value: addedToListName)
            .animation(.easeInOut(duration: 0.3), value: editResultMsg)
        }
    }
}

// MARK: - 共用 Toast 元件

private struct ToastView: View {
    let message: String
    var body: some View {
        Text(message)
            .font(.subheadline)
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(Color(hex: "2E7D32").opacity(0.9))
            .foregroundColor(.white)
            .cornerRadius(20)
            .transition(.move(edge: .bottom).combined(with: .opacity))
    }
}

// MARK: - 修改目標提醒價格 Sheet

private struct EditTargetPriceSheet: View {
    let item: FavoriteAlertDto?
    @Binding var priceInput: String
    let onConfirm: (Double) -> Void
    let onCancel: () -> Void

    var body: some View {
        VStack(spacing: 20) {
            // 標題
            HStack {
                Image(systemName: "bell.badge.waveform.fill")
                    .foregroundColor(Color(hex: "1976D2"))
                    .font(.title2)
                Text("修改目標提醒價格")
                    .font(.headline)
                    .fontWeight(.bold)
                    .foregroundColor(Color(hex: "1B5E20"))
                Spacer()
            }

            if let item = item {
                Text("農產品：\(item.produceName)")
                    .font(.subheadline)
                    .foregroundColor(Color(hex: "388E3C"))
                    .frame(maxWidth: .infinity, alignment: .leading)

                Text("目前市場均價：$\(String(format: "%.1f", item.currentPrice))/kg")
                    .font(.subheadline)
                    .foregroundColor(Color(hex: "558B2F"))
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            // 目標價格輸入框
            TextField("輸入目標提醒價格（元/公斤）", text: $priceInput)
                .keyboardType(.decimalPad)
                .padding()
                .background(Color.white.opacity(0.8))
                .cornerRadius(12)
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: "81C784"), lineWidth: 1.5))

            // 確認 / 取消按鈕
            HStack(spacing: 16) {
                Button("取消") { onCancel() }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(Color.gray.opacity(0.2))
                    .foregroundColor(.secondary)
                    .cornerRadius(12)

                Button("確認更新") {
                    if let price = Double(priceInput), price > 0 {
                        onConfirm(price)
                    }
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(Color(hex: "1976D2"))
                .foregroundColor(.white)
                .cornerRadius(12)
                .disabled(Double(priceInput) == nil || (Double(priceInput) ?? 0) <= 0)
            }
        }
        .padding(24)
        .background(
            LinearGradient(
                colors: [Color(hex: "E8F5E9"), Color(hex: "C8E6C9")],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
    }
}
