import SwiftUI

struct PriceAlertSetupView: View {
    @State private var marketName: String = ""
    @State private var produceName: String = ""
    @State private var targetPrice: String = ""
    @State private var isShowingAlert = false
    
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("設定到價提醒")) {
                    TextField("市場名稱 (例：台北一)", text: $marketName)
                    TextField("農產品名稱 (例：牛番茄)", text: $produceName)
                    TextField("目標價格 (跌破此價格通知)", text: $targetPrice)
                        .keyboardType(.decimalPad)
                }
                
                Button(action: setupAlert) {
                    Text("設定提醒")
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(8)
                }
                .disabled(marketName.isEmpty || produceName.isEmpty || targetPrice.isEmpty)
            }
            .navigationTitle("價格追蹤")
            .alert(isPresented: $isShowingAlert) {
                Alert(
                    title: Text("設定成功"),
                    message: Text("當【\(marketName)】的【\(produceName)】跌破 \(targetPrice) 元時，我們將發送推播通知您！"),
                    dismissButton: .default(Text("確定")) {
                        // 清空表單
                        marketName = ""
                        produceName = ""
                        targetPrice = ""
                    }
                )
            }
        }
    }
    
    private func setupAlert() {
        guard let priceValue = Double(targetPrice) else { return }
        
        // 呼叫後端 API 設定到價提醒 (FCM)
        // ProduceService.shared.setPriceAlert(market: marketName, produce: produceName, targetPrice: priceValue)
        
        isShowingAlert = true
    }
}

struct PriceAlertSetupView_Previews: PreviewProvider {
    static var previews: some View {
        PriceAlertSetupView()
    }
}
