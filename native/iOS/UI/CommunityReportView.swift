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
    
    private func submitReport() {
        guard let priceValue = Double(price) else { return }
        let report = CommunityPriceDto(marketName: marketName, produceName: produceName, price: priceValue, timestamp: Date().timeIntervalSince1970)
        
        // 呼叫後端 API 提交在地回報資料
        // ProduceService.shared.submitCommunityReport(report)
        
        isShowingAlert = true
    }
}

struct CommunityPriceDto: Codable {
    let marketName: String
    let produceName: String
    let price: Double
    let timestamp: TimeInterval
}

struct CommunityReportView_Previews: PreviewProvider {
    static var previews: some View {
        CommunityReportView()
    }
}
