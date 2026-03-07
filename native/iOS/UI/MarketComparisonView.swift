import SwiftUI

// =============================================================================
// MarketComparisonView.swift — 各市場農產品批發比價畫面
//
// 功能說明：
//   使用者輸入農產品名稱（例："高麗菜"），App 呼叫後端
//   GET /api/produce/compare/{cropName}，返回各市場當日批發均價。
//   結果依均價由低到高排序，最便宜的市場以綠色醒目標示。
//   每筆結果可點擊「前往導航」開啟 Apple Maps 搜尋該市場。
//
// 對應 Android 版本：MarketFinderCard.kt（比價功能區塊）
// =============================================================================

struct MarketComparisonView: View {
    @State private var cropQuery: String = ""
    @State private var results: [MarketComparisonDto] = []
    @State private var isLoading = false
    @State private var hasSearched = false
    @State private var errorMsg: String? = nil

    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // 搜尋欄
                HStack(spacing: 12) {
                    TextField("輸入農產品名稱，例：高麗菜", text: $cropQuery)
                        .textFieldStyle(.roundedBorder)
                        .submitLabel(.search)
                        .onSubmit { performSearch() }

                    Button(action: performSearch) {
                        if isLoading {
                            ProgressView().frame(width: 44, height: 36)
                        } else {
                            Image(systemName: "magnifyingglass")
                                .frame(width: 44, height: 36)
                                .background(Color(hex: "4CAF50"))
                                .foregroundColor(.white)
                                .cornerRadius(8)
                        }
                    }
                    .disabled(cropQuery.trimmingCharacters(in: .whitespaces).isEmpty || isLoading)
                }
                .padding()
                .background(Color(.systemGroupedBackground))

                if let error = errorMsg {
                    Text(error)
                        .foregroundColor(.red)
                        .font(.caption)
                        .padding(.horizontal)
                }

                List {
                    if hasSearched && results.isEmpty {
                        Text("查無「\(cropQuery)」的比價資料")
                            .foregroundColor(.secondary)
                            .listRowBackground(Color.clear)
                    }

                    ForEach(Array(results.enumerated()), id: \.offset) { index, item in
                        MarketComparisonRow(rank: index, item: item)
                    }
                }
                .listStyle(.insetGrouped)
            }
            .navigationTitle("市場比價")
            .navigationBarTitleDisplayMode(.large)
        }
    }

    private func performSearch() {
        let query = cropQuery.trimmingCharacters(in: .whitespaces)
        guard !query.isEmpty else { return }
        isLoading = true
        errorMsg = nil

        Task {
            do {
                results = try await ProduceService.shared.getMarketComparison(cropName: query)
                hasSearched = true
            } catch {
                errorMsg = "查詢失敗：\(error.localizedDescription)"
            }
            isLoading = false
        }
    }
}

// MARK: - 單筆比價列表行
private struct MarketComparisonRow: View {
    let rank: Int
    let item: MarketComparisonDto

    var body: some View {
        HStack(spacing: 12) {
            // 排名標籤
            Text(rankEmoji)
                .font(.title2)
                .frame(width: 36)

            VStack(alignment: .leading, spacing: 3) {
                Text(item.marketName)
                    .font(.headline)
                    .foregroundColor(.primary)
                Text("交易量：\(Int(item.transQuantity)) 公斤・\(item.date)")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Spacer()

            VStack(alignment: .trailing, spacing: 4) {
                Text("$\(String(format: "%.1f", item.avgPrice))")
                    .font(.title3)
                    .fontWeight(.bold)
                    .foregroundColor(rank == 0 ? Color(hex: "2E7D32") : .primary)
                Text("元/公斤")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }

            // Apple Maps 導航按鈕
            Button {
                openMaps(marketName: item.marketName)
            } label: {
                Image(systemName: "location.fill")
                    .foregroundColor(Color(hex: "FF9800"))
                    .font(.system(size: 18))
                    .padding(8)
                    .background(Color(hex: "FF9800").opacity(0.1))
                    .clipShape(Circle())
            }
            .buttonStyle(.plain)
        }
        .padding(.vertical, 4)
        .listRowBackground(
            rank == 0
            ? Color(hex: "4CAF50").opacity(0.08)
            : Color(.systemBackground)
        )
    }

    private var rankEmoji: String {
        switch rank {
        case 0: return "🥇"
        case 1: return "🥈"
        case 2: return "🥉"
        default: return "  "
        }
    }

    private func openMaps(marketName: String) {
        let encoded = marketName.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        if let url = URL(string: "maps://?q=\(encoded)") {
            UIApplication.shared.open(url)
        }
    }
}
