import SwiftUI

// =============================================================================
// SmartGroceryListScreen.swift — 智慧買菜清單（iOS）
//
// 功能：
//   - 輸入蔬菜名稱後自動查詢今日批發均價，預估費用
//   - 每筆品項支援 ±數量、勾選已買、左滑刪除
//   - Hero Banner 顯示預估總花費與購買進度
//   - 清單以 JSON 存入 UserDefaults，重啟 App 不遺失
//   - 「清除已買」一鍵清除所有已勾選品項
//   - 針對 35–60 歲使用者：大字體、高對比、友善操作
// =============================================================================

// MARK: - 資料模型

struct GroceryItem: Identifiable, Codable {
    var id: UUID = UUID()
    var name: String
    var quantity: Int = 1
    var estimatedPricePerUnit: Double = 0.0
    var isChecked: Bool = false
}

// MARK: - ViewModel

@MainActor
final class GroceryListViewModel: ObservableObject {
    @Published var items: [GroceryItem] = []
    @Published var isSearching = false
    @Published var alertMessage: String? = nil

    private let udKey = "grocery_items_v1"

    init() { load() }

    // 持久化
    func save() {
        if let data = try? JSONEncoder().encode(items) {
            UserDefaults.standard.set(data, forKey: udKey)
        }
    }
    private func load() {
        guard let data = UserDefaults.standard.data(forKey: udKey),
              let saved = try? JSONDecoder().decode([GroceryItem].self, from: data)
        else { return }
        items = saved
    }

    // 新增品項（查詢均價）
    func addItem(name: String) async {
        let trimmed = name.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return }
        isSearching = true
        defer { isSearching = false }

        var price: Double = 0.0
        do {
            let result = try await ProduceService.shared.searchCropPrice(name: trimmed)
            price = result
        } catch {
            alertMessage = "找不到「\(trimmed)」的今日均價，已設為 0"
        }

        items.append(GroceryItem(name: trimmed, estimatedPricePerUnit: price))
        save()
    }

    // 切換已購狀態
    func toggle(_ item: GroceryItem) {
        guard let idx = items.firstIndex(where: { $0.id == item.id }) else { return }
        items[idx].isChecked.toggle()
        save()
    }

    // 調整數量
    func changeQty(_ item: GroceryItem, delta: Int) {
        guard let idx = items.firstIndex(where: { $0.id == item.id }) else { return }
        items[idx].quantity = max(1, items[idx].quantity + delta)
        save()
    }

    // 刪除
    func delete(_ item: GroceryItem) {
        items.removeAll { $0.id == item.id }
        save()
    }

    // 清除已勾選
    func clearChecked() {
        items.removeAll { $0.isChecked }
        save()
    }

    var pendingItems:  [GroceryItem] { items.filter { !$0.isChecked } }
    var checkedItems:  [GroceryItem] { items.filter { $0.isChecked } }
    var totalCost:     Double { pendingItems.reduce(0) { $0 + Double($1.quantity) * $1.estimatedPricePerUnit } }
    var totalCount:    Int { items.count }
    var progress:      Double { totalCount == 0 ? 0 : Double(checkedItems.count) / Double(totalCount) }
}

// MARK: - 主畫面

struct SmartGroceryListScreen: View {
    @StateObject private var vm = GroceryListViewModel()
    @State private var newName = ""
    @State private var showClearAlert = false
    @FocusState private var fieldFocused: Bool

    var body: some View {
        NavigationView {
            ZStack {
                LinearGradient(
                    colors: [Color(hex: "E8F5E9"), Color(hex: "C8E6C9")],
                    startPoint: .topLeading, endPoint: .bottomTrailing
                )
                .ignoresSafeArea()

                VStack(spacing: 0) {
                    // Hero Banner
                    GroceryHeroBanner(
                        totalCost: vm.totalCost,
                        checkedCount: vm.checkedItems.count,
                        totalCount: vm.totalCount,
                        progress: vm.progress
                    )
                    .padding(.horizontal, 16)
                    .padding(.top, 8)

                    // 輸入欄
                    GroceryInputRow(
                        text: $newName,
                        isSearching: vm.isSearching,
                        focused: $fieldFocused
                    ) {
                        Task { await vm.addItem(name: newName); newName = "" }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)

                    // 清單
                    if vm.items.isEmpty {
                        emptyStateView
                    } else {
                        groceryList
                    }
                }
            }
            .navigationTitle("智慧買菜清單")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    if !vm.checkedItems.isEmpty {
                        Button {
                            showClearAlert = true
                        } label: {
                            Label("清除已買 (\(vm.checkedItems.count))", systemImage: "checkmark.circle.fill")
                                .font(.subheadline)
                                .foregroundColor(Color(hex: "1976D2"))
                        }
                    }
                }
            }
            .alert("清除已買品項", isPresented: $showClearAlert) {
                Button("清除", role: .destructive) { vm.clearChecked() }
                Button("取消", role: .cancel) {}
            } message: {
                Text("將刪除 \(vm.checkedItems.count) 筆已勾選品項，此動作無法復原。")
            }
            .alert("提示", isPresented: Binding(
                get: { vm.alertMessage != nil },
                set: { if !$0 { vm.alertMessage = nil } }
            )) {
                Button("確定") { vm.alertMessage = nil }
            } message: {
                Text(vm.alertMessage ?? "")
            }
        }
    }

    // MARK: 清單本體
    @ViewBuilder
    private var groceryList: some View {
        List {
            if !vm.pendingItems.isEmpty {
                Section {
                    ForEach(vm.pendingItems) { item in
                        GroceryItemRow(item: item, vm: vm)
                            .listRowBackground(Color.clear)
                            .listRowSeparator(.hidden)
                            .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                                Button(role: .destructive) { vm.delete(item) } label: {
                                    Label("刪除", systemImage: "trash")
                                }
                            }
                    }
                } header: {
                    Text("待購（\(vm.pendingItems.count) 項）")
                        .font(.subheadline.bold())
                        .foregroundColor(Color(hex: "558B2F"))
                }
            }

            if !vm.checkedItems.isEmpty {
                Section {
                    ForEach(vm.checkedItems) { item in
                        GroceryItemRow(item: item, vm: vm)
                            .listRowBackground(Color.clear)
                            .listRowSeparator(.hidden)
                            .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                                Button(role: .destructive) { vm.delete(item) } label: {
                                    Label("刪除", systemImage: "trash")
                                }
                            }
                    }
                } header: {
                    Text("已購（\(vm.checkedItems.count) 項）")
                        .font(.subheadline)
                        .foregroundColor(.gray)
                }
            }
        }
        .listStyle(.plain)
        .background(Color.clear)
        .scrollContentBackground(.hidden)
    }

    // MARK: 空狀態
    private var emptyStateView: some View {
        VStack(spacing: 20) {
            Spacer()
            Image(systemName: "cart.badge.plus")
                .font(.system(size: 64, weight: .light))
                .foregroundColor(Color(hex: "81C784"))
                .symbolEffect(.pulse)
            Text("清單是空的！")
                .font(.title2.bold())
                .foregroundColor(Color(hex: "2E7D32"))
            Text("輸入蔬菜名稱，系統自動查詢\n今日批發均價，預估花費")
                .font(.body)
                .multilineTextAlignment(.center)
                .foregroundColor(Color(hex: "558B2F"))
            Spacer()
        }
        .padding()
    }
}

// MARK: - Hero Banner

private struct GroceryHeroBanner: View {
    let totalCost: Double
    let checkedCount: Int
    let totalCount: Int
    let progress: Double

    @State private var animatedProgress: Double = 0

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [Color(hex: "1B5E20"), Color(hex: "2E7D32"), Color(hex: "388E3C")],
                        startPoint: .topLeading, endPoint: .bottomTrailing
                    )
                )
                .shadow(color: Color(hex: "1B5E20").opacity(0.4), radius: 12, x: 0, y: 6)

            HStack(alignment: .center, spacing: 16) {
                // 左側：金額
                VStack(alignment: .leading, spacing: 6) {
                    Text("預估待購金額")
                        .font(.subheadline)
                        .foregroundColor(.white.opacity(0.8))
                    AnimatedCostText(value: totalCost)

                    if totalCount > 0 {
                        ProgressView(value: animatedProgress)
                            .progressViewStyle(LinearProgressViewStyle(tint: Color(hex: "81C784")))
                            .scaleEffect(x: 1, y: 1.4, anchor: .center)
                            .padding(.top, 4)
                    }
                }

                Spacer()

                // 右側：進度環
                if totalCount > 0 {
                    VStack(spacing: 6) {
                        ZStack {
                            Circle()
                                .stroke(Color.white.opacity(0.2), lineWidth: 5)
                                .frame(width: 60, height: 60)
                            Circle()
                                .trim(from: 0, to: animatedProgress)
                                .stroke(Color(hex: "81C784"), style: StrokeStyle(lineWidth: 5, lineCap: .round))
                                .frame(width: 60, height: 60)
                                .rotationEffect(.degrees(-90))
                                .animation(.spring(duration: 0.8), value: animatedProgress)
                            Text("\(Int(progress * 100))%")
                                .font(.caption.bold())
                                .foregroundColor(.white)
                        }
                        Text("已買 \(checkedCount)/\(totalCount)")
                            .font(.caption2)
                            .foregroundColor(.white.opacity(0.8))
                    }
                }
            }
            .padding(20)
        }
        .frame(height: totalCount > 0 ? 120 : 100)
        .onAppear { animatedProgress = progress }
        .onChange(of: progress) { animatedProgress = $0 }
    }
}

// 數字跳動動畫（35-60 歲友善大字體）
private struct AnimatedCostText: View {
    let value: Double
    @State private var displayed: Double = 0

    var body: some View {
        Text("\(Int(displayed)) 元")
            .font(.system(size: 36, weight: .heavy, design: .rounded))
            .foregroundColor(.white)
            .contentTransition(.numericText())
            .animation(.spring(duration: 0.6), value: displayed)
            .onAppear { displayed = value }
            .onChange(of: value) { displayed = $0 }
    }
}

// MARK: - 輸入欄

private struct GroceryInputRow: View {
    @Binding var text: String
    let isSearching: Bool
    var focused: FocusState<Bool>.Binding
    let onAdd: () -> Void

    var body: some View {
        HStack(spacing: 10) {
            TextField("想買什麼菜？（例：高麗菜）", text: $text)
                .focused(focused)
                .font(.body)
                .padding(12)
                .background(.ultraThinMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 14))
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color(hex: "4CAF50").opacity(0.5), lineWidth: 1))
                .onSubmit(onAdd)

            Button(action: onAdd) {
                ZStack {
                    Circle()
                        .fill(Color(hex: "2E7D32"))
                        .frame(width: 50, height: 50)
                    if isSearching {
                        ProgressView().tint(.white)
                    } else {
                        Image(systemName: "plus")
                            .font(.title3.bold())
                            .foregroundColor(.white)
                    }
                }
            }
            .disabled(text.trimmingCharacters(in: .whitespaces).isEmpty || isSearching)
        }
    }
}

// MARK: - 品項列

private struct GroceryItemRow: View {
    let item: GroceryItem
    @ObservedObject var vm: GroceryListViewModel

    var body: some View {
        HStack(spacing: 12) {
            // 勾選
            Button { vm.toggle(item) } label: {
                Image(systemName: item.isChecked ? "checkmark.circle.fill" : "circle")
                    .font(.system(size: 26))
                    .foregroundColor(item.isChecked ? Color(hex: "4CAF50") : Color(hex: "81C784"))
            }
            .buttonStyle(.plain)

            // 品項資訊
            VStack(alignment: .leading, spacing: 3) {
                Text(item.name)
                    .font(.headline)
                    .foregroundColor(item.isChecked ? .gray : Color(hex: "1B5E20"))
                    .strikethrough(item.isChecked)
                if item.estimatedPricePerUnit > 0 {
                    Text("均價 \(String(format: "%.1f", item.estimatedPricePerUnit))/kg × \(item.quantity) = \(Int(item.estimatedPricePerUnit * Double(item.quantity))) 元")
                        .font(.subheadline)
                        .foregroundColor(item.isChecked ? .gray.opacity(0.6) : Color(hex: "558B2F"))
                } else {
                    Text("×\(item.quantity)")
                        .font(.subheadline)
                        .foregroundColor(.gray)
                }
            }

            Spacer()

            // 數量 ±（已勾選則隱藏）
            if !item.isChecked {
                HStack(spacing: 4) {
                    Button { vm.changeQty(item, delta: -1) } label: {
                        Image(systemName: "minus.circle.fill")
                            .font(.title2)
                            .foregroundColor(item.quantity > 1 ? Color(hex: "2E7D32") : Color(hex: "A5D6A7"))
                    }
                    .disabled(item.quantity <= 1)
                    .buttonStyle(.plain)

                    Text("\(item.quantity)")
                        .font(.headline.monospacedDigit())
                        .frame(minWidth: 24)
                        .foregroundColor(Color(hex: "1B5E20"))

                    Button { vm.changeQty(item, delta: 1) } label: {
                        Image(systemName: "plus.circle.fill")
                            .font(.title2)
                            .foregroundColor(Color(hex: "2E7D32"))
                    }
                    .buttonStyle(.plain)
                }
                .transition(.opacity.combined(with: .scale))
            }
        }
        .padding(14)
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .shadow(color: Color.black.opacity(0.05), radius: 4, x: 0, y: 2)
        .animation(.easeInOut(duration: 0.2), value: item.isChecked)
    }
}
