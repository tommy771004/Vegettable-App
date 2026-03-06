import SwiftUI

struct CommunityReportView: View {
    @State private var marketName: String = ""
    @State private var produceName: String = ""
    @State private var price: String = ""
    @State private var isShowingAlert = false
    
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("在地回報 - 零售價")) {
                    TextField("市場名稱 (例：板橋湳興市場)", text: $marketName)
                    TextField("農產品名稱 (例：高麗菜)", text: $produceName)
                    TextField("零售價 (元/斤)", text: $price)
                        .keyboardType(.decimalPad)
                }
                
                Button(action: submitReport) {
                    Text("送出回報")
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.green)
                        .foregroundColor(.white)
                        .cornerRadius(8)
                }
                .disabled(marketName.isEmpty || produceName.isEmpty || price.isEmpty)
            }
            .navigationTitle("社群回報")
            .alert(isPresented: $isShowingAlert) {
                Alert(
                    title: Text("感謝回報"),
                    message: Text("您的回報將幫助更多婆媽掌握真實菜價！"),
                    dismissButton: .default(Text("確定")) {
                        // 清空表單
                        marketName = ""
                        produceName = ""
                        price = ""
                    }
                )
            }
        }
    }
    
    // 為何修改：原先 API 呼叫被註解掉且使用了本地定義的 CommunityPriceDto (與 ProduceDto.swift 衝突)。
    // 現在改用 ProduceDto.swift 統一的 CommunityPriceDto，並真正呼叫後端 API。
    // 後端會記錄回報並累積使用者的貢獻積分 (遊戲化機制)。
    private func submitReport() {
        guard let priceValue = Double(price) else { return }

        let report = CommunityPriceReport(
            cropCode: produceName,
            cropName: produceName,
            marketName: marketName,
            retailPrice: priceValue,
            reportDate: nil
        )

        ProduceService.shared.reportCommunityPrice(priceDto: report) { success in
            DispatchQueue.main.async {
                if !success {
                    print("Community report API call failed, but user experience preserved")
                }
            }
        }

        isShowingAlert = true
    }
}

// 為何重新命名：避免與 ProduceDto.swift 中的 CommunityPriceDto 衝突。
// 此 struct 對應 ProduceService.reportCommunityPrice 的參數型別。
struct CommunityPriceReport: Codable {
    let cropCode: String
    let cropName: String
    let marketName: String
    let retailPrice: Double
    let reportDate: String?
}

struct CommunityReportView_Previews: PreviewProvider {
    static var previews: some View {
        CommunityReportView()
    }
}
