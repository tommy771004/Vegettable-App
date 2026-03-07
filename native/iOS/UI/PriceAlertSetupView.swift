import SwiftUI

struct PriceAlertSetupView: View {
    @State private var marketName: String = ""
    @State private var produceName: String = ""
    @State private var targetPrice: String = ""
    @State private var isShowingAlert = false
    @State private var isShowingErrorAlert = false
    @State private var isSaving = false
    
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
                    if isSaving {
                        ProgressView()
                            .frame(maxWidth: .infinity)
                            .padding()
                    } else {
                        Text("設定提醒")
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.blue)
                            .foregroundColor(.white)
                            .cornerRadius(8)
                    }
                }
                .disabled(marketName.isEmpty || produceName.isEmpty || targetPrice.isEmpty || isSaving)
            }
            .navigationTitle("價格追蹤")
            .alert(isPresented: $isShowingAlert) {
                Alert(
                    title: Text("設定成功"),
                    message: Text("當【\(marketName)】的【\(produceName)】跌破 \(targetPrice) 元時，我們將發送推播通知您！"),
                    dismissButton: .default(Text("確定")) {
                        marketName = ""
                        produceName = ""
                        targetPrice = ""
                    }
                )
            }
            .alert("設定失敗", isPresented: $isShowingErrorAlert) {
                Button("確定", role: .cancel) {}
            } message: {
                Text("無法儲存提醒，請確認網路連線後再試。")
            }
        }
    }
    
    // 為何修改：原先 API 呼叫被註解掉，按鈕按下去只彈 Alert 卻沒有真正同步到後端，
    // 使用者設定的到價提醒不會生效。現在透過 syncFavorite 將提醒寫入後端，
    // 後端 PriceAlertWorker 背景服務會定期檢查並發送 FCM 推播。
    private func setupAlert() {
        guard let priceValue = Double(targetPrice), priceValue > 0 else { return }

        // Request notification permission first so user sees it before committing
        PriceAlertNotifier.shared.requestPermission()

        isSaving = true
        ProduceService.shared.syncFavorite(produceId: produceName, targetPrice: priceValue) { success in
            DispatchQueue.main.async {
                isSaving = false
                if success {
                    isShowingAlert = true
                } else {
                    isShowingErrorAlert = true
                }
            }
        }
    }
}

struct PriceAlertSetupView_Previews: PreviewProvider {
    static var previews: some View {
        PriceAlertSetupView()
    }
}
