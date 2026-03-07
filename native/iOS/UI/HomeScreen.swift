import SwiftUI

// 毛玻璃效果 Modifier (淡綠色底)
struct LiquidGlassModifier: ViewModifier {
    func body(content: Content) -> some View {
        content
            .background(.ultraThinMaterial)
            .background(Color(hex: "A5D6A7").opacity(0.25)) // 淡綠色半透明
            .cornerRadius(16)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(Color.white.opacity(0.6), lineWidth: 1)
            )
            .shadow(color: Color.black.opacity(0.05), radius: 5, x: 0, y: 2)
    }
}

extension View {
    func liquidGlass() -> some View {
        self.modifier(LiquidGlassModifier())
    }
}

struct HomeScreen: View {
    // 由 MainTabView 透過 environmentObject 注入，與 FavoritesScreen 共享同一份資料
    @EnvironmentObject var viewModel: ProduceViewModel
    private let ttsHelper = TextToSpeechHelper()

    // 收藏 Sheet 狀態
    @State private var favoriteTarget: ProduceDto? = nil
    @State private var targetPriceInput: String = ""
    @State private var favoriteResult: String? = nil

    var body: some View {
        NavigationView {
            ZStack {
                // 漸層背景，讓毛玻璃效果更明顯
                LinearGradient(
                    colors: [Color(hex: "E8F5E9"), Color(hex: "C8E6C9")],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .ignoresSafeArea()
                
                ScrollView {
                    VStack(alignment: .leading, spacing: 24) {

                        // 0. 🌟 Hero 摘要橫幅
                        HeroBanner(viewModel: viewModel)
                            .padding(.horizontal)

                        // 1. 價格異常警報
                        switch viewModel.anomalies {
                        case .loading:
                            SkeletonView()
                                .frame(height: 60)
                                .padding(.horizontal)
                        case .failure(let error):
                            ErrorView(message: error.localizedDescription) {
                                Task { await viewModel.fetchDashboardData() }
                            }
                        case .success(let anomalies):
                            if !anomalies.isEmpty {
                                VStack(alignment: .leading, spacing: 12) {
                                    Text("⚠️ 價格異常警報")
                                        .font(.headline)
                                        .foregroundColor(.red)
                                        .padding(.horizontal)
                                    
                                    ForEach(anomalies) { anomaly in
                                        HStack(alignment: .top) {
                                            Image(systemName: "exclamationmark.triangle.fill")
                                                .foregroundColor(.red)
                                                .padding(.top, 2)
                                            Text(anomaly.alertMessage)
                                                .font(.subheadline)
                                                .foregroundColor(.red)
                                        }
                                        .padding()
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                        .liquidGlass()
                                        .padding(.horizontal)
                                    }
                                }
                            }
                        }
                        
                        // 2. 歷史價格與 7 日趨勢預測 (Line Chart)
                        if case .success(let history) = viewModel.historicalData,
                           case .success(let predicted) = viewModel.predictedData,
                           (!history.isEmpty || !predicted.isEmpty) {
                            VStack(alignment: .leading, spacing: 12) {
                                Text("📈 \(history.first?.date != nil ? "近期" : "高麗菜") 價格趨勢與預測")
                                    .font(.headline)
                                    .foregroundColor(Color(hex: "2E7D32"))
                                    .padding(.horizontal)
                                
                                PriceTrendView(
                                    historical: history,
                                    predicted: predicted
                                )
                                .padding()
                                .liquidGlass()
                                .padding(.horizontal)
                            }
                        } else if case .loading = viewModel.historicalData {
                            SkeletonView()
                                .frame(height: 200)
                                .padding(.horizontal)
                        }
                        
                        // 3. 熱門交易農產品
                        switch viewModel.topVolume {
                        case .loading:
                            HStack(spacing: 16) {
                                ForEach(0..<3) { _ in
                                    SkeletonView()
                                        .frame(width: 120, height: 80)
                                }
                            }
                            .padding(.horizontal)
                        case .failure:
                            Text("無法載入熱門交易")
                                .foregroundColor(.red)
                                .padding(.horizontal)
                        case .success(let topVolume):
                            if !topVolume.isEmpty {
                                VStack(alignment: .leading, spacing: 12) {
                                    Text("🔥 今日熱門交易")
                                        .font(.headline)
                                        .foregroundColor(Color(hex: "2E7D32"))
                                        .padding(.horizontal)
                                    
                                    ScrollView(.horizontal, showsIndicators: false) {
                                        HStack(spacing: 16) {
                                            ForEach(topVolume) { crop in
                                                VStack(alignment: .leading, spacing: 6) {
                                                    Text(crop.cropName)
                                                        .font(.subheadline)
                                                        .fontWeight(.bold)
                                                        .foregroundColor(Color(hex: "1B5E20"))
                                                    Text("$\(String(format: "%.1f", crop.avgPrice))/kg")
                                                        .font(.caption)
                                                        .foregroundColor(Color(hex: "388E3C"))
                                                }
                                                .padding()
                                                .frame(width: 120, alignment: .leading)
                                                .liquidGlass()
                                            }
                                        }
                                        .padding(.horizontal)
                                        .padding(.bottom, 4)
                                    }
                                }
                            }
                        }
                        
                        // 4. 今日菜價列表（35-60 歲友善大卡片，點擊進入詳情）
                        VStack(alignment: .leading, spacing: 12) {
                            HStack {
                                Text("📋 今日菜價")
                                    .font(.title3.bold())
                                    .foregroundColor(Color(hex: "2E7D32"))
                                Spacer()
                                Text("點擊查看詳情 →")
                                    .font(.caption)
                                    .foregroundColor(Color(hex: "81C784"))
                            }
                            .padding(.horizontal)

                            switch viewModel.dailyPrices {
                            case .loading:
                                ForEach(0..<5) { _ in
                                    SkeletonView().frame(height: 90).padding(.horizontal)
                                }
                            case .failure(let error):
                                ErrorView(message: error.localizedDescription) {
                                    Task { await viewModel.fetchDashboardData() }
                                }
                            case .success(let dailyPrices):
                                if dailyPrices.isEmpty {
                                    EmptyStateView(message: "今日暫無菜價資料", systemImage: "cart")
                                } else {
                                    LazyVStack(spacing: 14) {
                                        ForEach(dailyPrices) { produce in
                                            // NavigationLink → CropDetailScreen
                                            NavigationLink(destination:
                                                CropDetailScreen(produce: produce)
                                                    .environmentObject(viewModel)
                                            ) {
                                                ProduceRowCard(
                                                    produce: produce,
                                                    onSpeak: {
                                                        ttsHelper.speak(text: "今日 \(produce.cropName) 價格是每公斤 \(String(format: "%.0f", produce.avgPrice)) 元")
                                                    },
                                                    onFavorite: {
                                                        favoriteTarget = produce
                                                        targetPriceInput = String(format: "%.1f", produce.avgPrice)
                                                    }
                                                )
                                            }
                                            .buttonStyle(.plain)
                                            .padding(.horizontal)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .padding(.vertical)
                }
            }
            .navigationTitle("農產品批發價查詢")
            .refreshable {
                await viewModel.fetchDashboardData()
            }
            // 加入收藏 Sheet
            .sheet(item: $favoriteTarget) { produce in
                AddFavoriteSheet(
                    produce: produce,
                    targetPriceInput: $targetPriceInput,
                    onConfirm: { price in
                        Task {
                            let ok = await viewModel.addToFavorites(produceId: produce.cropCode, targetPrice: price)
                            favoriteResult = ok ? "✅ \(produce.cropName) 已加入收藏，目標價 $\(String(format: "%.1f", price))/kg" : "⚠️ 加入失敗，請稍後再試"
                            DispatchQueue.main.asyncAfter(deadline: .now() + 3) { favoriteResult = nil }
                        }
                    }
                )
                .presentationDetents([.height(320)])
            }
            // 收藏成功 Toast
            .overlay(alignment: .bottom) {
                if let msg = favoriteResult {
                    Text(msg)
                        .font(.subheadline)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(Color(hex: "2E7D32").opacity(0.92))
                        .foregroundColor(.white)
                        .cornerRadius(20)
                        .padding(.bottom, 24)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                }
            }
            .animation(.easeInOut(duration: 0.3), value: favoriteResult)
        }
    }
}

// MARK: - 加入收藏 Sheet
private struct AddFavoriteSheet: View {
    let produce: ProduceDto
    @Binding var targetPriceInput: String
    let onConfirm: (Double) -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(spacing: 20) {
            Text("❤️ 加入收藏：\(produce.cropName)")
                .font(.headline)
                .padding(.top, 20)

            Text("設定到價提醒：當價格低於此目標價時，系統將發送推播通知。")
                .font(.caption)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            TextField("目標提醒價格 (元/公斤)", text: $targetPriceInput)
                .keyboardType(.decimalPad)
                .textFieldStyle(.roundedBorder)
                .padding(.horizontal)

            HStack(spacing: 12) {
                Button("取消") { dismiss() }
                    .foregroundColor(.secondary)

                Button("確認收藏") {
                    if let price = Double(targetPriceInput) {
                        onConfirm(price)
                        dismiss()
                    }
                }
                .buttonStyle(.borderedProminent)
                .tint(Color(hex: "4CAF50"))
                .disabled(Double(targetPriceInput) == nil)
            }
            .padding(.horizontal)
        }
        .padding(.bottom, 20)
    }
}

// MARK: - Hero 摘要橫幅
/// 首頁頂部深綠漸層 Banner，顯示今日日期與行情筆數，對齊 Android Hero 設計
private struct HeroBanner: View {
    let viewModel: ProduceViewModel

    private var todayString: String {
        let fmt = DateFormatter()
        fmt.locale = Locale(identifier: "zh_TW")
        fmt.dateFormat = "M月d日"
        return fmt.string(from: Date())
    }

    private var totalCount: Int {
        if case .success(let prices) = viewModel.dailyPrices { return prices.count }
        return 0
    }

    var body: some View {
        ZStack(alignment: .leading) {
            LinearGradient(
                colors: [Color(hex: "1B5E20"), Color(hex: "2E7D32"), Color(hex: "388E3C")],
                startPoint: .topLeading, endPoint: .bottomTrailing
            )
            .cornerRadius(20)

            HStack {
                VStack(alignment: .leading, spacing: 6) {
                    Text("🌿 今日農產批發")
                        .font(.caption)
                        .foregroundColor(.white.opacity(0.8))
                    Text(todayString)
                        .font(.title)
                        .fontWeight(.black)
                        .foregroundColor(.white)
                    if totalCount > 0 {
                        Text("共 \(totalCount) 筆今日行情")
                            .font(.caption)
                            .foregroundColor(.white.opacity(0.7))
                    }
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 8) {
                    Text("🇹🇼 臺灣農業")
                        .font(.caption2)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(Color.white.opacity(0.2))
                        .clipShape(Capsule())

                    Text("即時更新")
                        .font(.caption2)
                        .foregroundColor(.white)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(Color(hex: "4CAF50").opacity(0.4))
                        .clipShape(Capsule())
                }
            }
            .padding(20)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 110)
    }
}

// MARK: - 今日菜價大卡片（35-60 歲友善設計）
/// 針對中高齡使用者設計的菜價列表項目：
///   - 農產品名稱 20pt bold，市場名稱 15pt
///   - 價格欄位 26pt heavy，視覺重心清晰
///   - 整張卡片可點（NavigationLink），最小高度 80pt
///   - 喇叭與收藏按鈕維持獨立，不觸發導覽
struct ProduceRowCard: View {
    let produce: ProduceDto
    let onSpeak: () -> Void
    let onFavorite: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            // 左側：作物資訊
            VStack(alignment: .leading, spacing: 5) {
                Text(produce.cropName)
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(Color(hex: "1B5E20"))
                Text(produce.marketName)
                    .font(.system(size: 14))
                    .foregroundColor(Color(hex: "4CAF50"))
                Text(produce.date)
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }

            Spacer()

            // 右側：價格 + 操作按鈕
            VStack(alignment: .trailing, spacing: 6) {
                Text("$\(String(format: "%.1f", produce.avgPrice))")
                    .font(.system(size: 26, weight: .heavy, design: .rounded))
                    .foregroundColor(Color(hex: "2E7D32"))
                    .contentTransition(.numericText())
                    .animation(.easeInOut(duration: 0.4), value: produce.avgPrice)

                Text("元/公斤")
                    .font(.caption2)
                    .foregroundColor(.secondary)

                HStack(spacing: 10) {
                    // 語音朗讀
                    Button(action: onSpeak) {
                        Image(systemName: "speaker.wave.2.fill")
                            .font(.system(size: 18))
                            .foregroundColor(Color(hex: "388E3C"))
                            .frame(width: 36, height: 36)
                            .background(Color(hex: "E8F5E9"))
                            .clipShape(Circle())
                    }
                    .buttonStyle(.plain)

                    // 加入收藏
                    Button(action: onFavorite) {
                        Image(systemName: "heart.fill")
                            .font(.system(size: 18))
                            .foregroundColor(Color(hex: "E91E63"))
                            .frame(width: 36, height: 36)
                            .background(Color(hex: "FCE4EC"))
                            .clipShape(Circle())
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .frame(minHeight: 80)
        .liquidGlass()
        .overlay(alignment: .trailing) {
            // 右箭頭提示可點擊
            Image(systemName: "chevron.right")
                .font(.caption.bold())
                .foregroundColor(Color(hex: "81C784"))
                .padding(.trailing, 8)
                .padding(.top, 8)
                .frame(maxHeight: .infinity, alignment: .top)
        }
    }
}

// Hex Color Extension
extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (1, 1, 1, 0)
        }
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue:  Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}
