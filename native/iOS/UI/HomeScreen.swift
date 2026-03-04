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
    @StateObject private var viewModel = ProduceViewModel()
    private let ttsHelper = TextToSpeechHelper()
    
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
                        
                        // 1. 價格異常警報
                        if !viewModel.anomalies.isEmpty {
                            VStack(alignment: .leading, spacing: 12) {
                                Text("⚠️ 價格異常警報")
                                    .font(.headline)
                                    .foregroundColor(.red)
                                    .padding(.horizontal)
                                
                                ForEach(viewModel.anomalies, id: \.cropCode) { anomaly in
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
                        
                        // 2. 歷史價格與 7 日趨勢預測 (Line Chart)
                        if !viewModel.historicalData.isEmpty || !viewModel.predictedData.isEmpty {
                            VStack(alignment: .leading, spacing: 12) {
                                Text("📈 高麗菜 價格趨勢與預測")
                                    .font(.headline)
                                    .foregroundColor(Color(hex: "2E7D32"))
                                    .padding(.horizontal)
                                
                                PriceTrendView(
                                    historical: viewModel.historicalData,
                                    predicted: viewModel.predictedData
                                )
                                .padding()
                                .liquidGlass()
                                .padding(.horizontal)
                            }
                        }
                        
                        // 3. 熱門交易農產品
                        if !viewModel.topVolume.isEmpty {
                            VStack(alignment: .leading, spacing: 12) {
                                Text("🔥 今日熱門交易")
                                    .font(.headline)
                                    .foregroundColor(Color(hex: "2E7D32"))
                                    .padding(.horizontal)
                                
                                ScrollView(.horizontal, showsIndicators: false) {
                                    HStack(spacing: 16) {
                                        ForEach(viewModel.topVolume, id: \.cropCode) { crop in
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
                        
                        // 4. 今日菜價列表
                        VStack(alignment: .leading, spacing: 12) {
                            Text("📋 今日菜價")
                                .font(.headline)
                                .foregroundColor(Color(hex: "2E7D32"))
                                .padding(.horizontal)
                            
                            LazyVStack(spacing: 16) {
                                ForEach(viewModel.dailyPrices, id: \.cropCode) { produce in
                                    HStack {
                                        VStack(alignment: .leading, spacing: 4) {
                                            Text(produce.cropName)
                                                .font(.headline)
                                                .foregroundColor(Color(hex: "1B5E20"))
                                            Text(produce.marketName)
                                                .font(.caption)
                                                .foregroundColor(Color(hex: "4CAF50"))
                                        }
                                        Spacer()
                                        
                                        Text("$\(String(format: "%.1f", produce.avgPrice))")
                                            .font(.title3)
                                            .fontWeight(.bold)
                                            .foregroundColor(Color(hex: "2E7D32"))
                                        
                                        Button(action: {
                                            ttsHelper.speak(text: "今日 \(produce.cropName) 價格是 \(produce.avgPrice) 元")
                                        }) {
                                            Image(systemName: "speaker.wave.2.fill")
                                                .foregroundColor(Color(hex: "388E3C"))
                                                .padding(.leading, 12)
                                        }
                                    }
                                    .padding()
                                    .liquidGlass()
                                    .padding(.horizontal)
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
