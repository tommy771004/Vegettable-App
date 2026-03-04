import SwiftUI
import Speech

struct ElderlyModeView: View {
    @State private var isRecording = false
    @State private var recognizedText = ""
    @State private var resultText = "阿嬤，您想查什麼菜？\n請按下方按鈕說話。"
    
    private let speechRecognizer = SFSpeechRecognizer(locale: Locale(identifier: "zh-TW"))
    @State private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
    @State private var recognitionTask: SFSpeechRecognitionTask?
    private let audioEngine = AVAudioEngine()
    private let ttsHelper = TextToSpeechHelper()

    var body: some View {
        VStack(spacing: 40) {
            Text("長輩語音查詢")
                .font(.system(size: 40, weight: .bold))
                .foregroundColor(.green)
            
            Text(resultText)
                .font(.system(size: 32, weight: .medium))
                .multilineTextAlignment(.center)
                .padding()
                .frame(maxWidth: .infinity, minHeight: 200)
                .background(Color.yellow.opacity(0.2))
                .cornerRadius(20)
            
            Button(action: {
                if isRecording {
                    stopRecording()
                } else {
                    startRecording()
                }
            }) {
                Image(systemName: isRecording ? "stop.circle.fill" : "mic.circle.fill")
                    .resizable()
                    .frame(width: 120, height: 120)
                    .foregroundColor(isRecording ? .red : .blue)
                    .shadow(radius: 10)
            }
            
            Text(isRecording ? "正在聆聽..." : "點擊麥克風說話")
                .font(.system(size: 24))
                .foregroundColor(.gray)
            
            Spacer()
        }
        .padding()
        .onAppear {
            SFSpeechRecognizer.requestAuthorization { authStatus in
                // 處理授權狀態
            }
        }
    }
    
    private func startRecording() {
        if recognitionTask != nil {
            recognitionTask?.cancel()
            recognitionTask = nil
        }
        
        let audioSession = AVAudioSession.sharedInstance()
        try? audioSession.setCategory(.record, mode: .measurement, options: .duckOthers)
        try? audioSession.setActive(true, options: .notifyOthersOnDeactivation)
        
        recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
        
        let inputNode = audioEngine.inputNode
        guard let recognitionRequest = recognitionRequest else { fatalError("Unable to create a SFSpeechAudioBufferRecognitionRequest object") }
        
        recognitionRequest.shouldReportPartialResults = true
        
        recognitionTask = speechRecognizer?.recognitionTask(with: recognitionRequest) { result, error in
            var isFinal = false
            
            if let result = result {
                self.recognizedText = result.bestTranscription.formattedString
                isFinal = result.isFinal
            }
            
            if error != nil || isFinal {
                self.audioEngine.stop()
                inputNode.removeTap(onBus: 0)
                self.recognitionRequest = nil
                self.recognitionTask = nil
                self.isRecording = false
                
                self.processVoiceQuery(query: self.recognizedText)
            }
        }
        
        let recordingFormat = inputNode.outputFormat(forBus: 0)
        inputNode.installTap(onBus: 0, bufferSize: 1024, format: recordingFormat) { (buffer: AVAudioPCMBuffer, when: AVAudioTime) in
            self.recognitionRequest?.append(buffer)
        }
        
        audioEngine.prepare()
        try? audioEngine.start()
        
        isRecording = true
    }
    
    private func stopRecording() {
        audioEngine.stop()
        recognitionRequest?.endAudio()
        isRecording = false
    }
    
    private func processVoiceQuery(query: String) {
        if query.contains("高麗菜") {
            let response = "阿嬤，今天台北一市場的高麗菜，批發價是一斤 25 元。"
            resultText = response
            ttsHelper.speak(text: response)
        } else if query.contains("番茄") {
            let response = "阿嬤，今天牛番茄比較貴，一斤要 45 元喔。"
            resultText = response
            ttsHelper.speak(text: response)
        } else {
            let response = "阿嬤，我聽不懂您說的菜名，請再說一次。"
            resultText = response
            ttsHelper.speak(text: response)
        }
    }
}

struct ElderlyModeView_Previews: PreviewProvider {
    static var previews: some View {
        ElderlyModeView()
    }
}
