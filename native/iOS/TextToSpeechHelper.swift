import Foundation
import AVFoundation

// 新增功能：無障礙設計 (Accessibility) - 語音播報輔助
class TextToSpeechHelper {
    static let shared = TextToSpeechHelper()
    private let synthesizer = AVSpeechSynthesizer()

    private init() {
        // 確保在靜音模式下也能根據需求發聲 (視 App 需求而定)
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .spokenAudio, options: .duckOthers)
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            print("Failed to set audio session category.")
        }
    }

    // 播報指定的文字，支援傳入語言代碼 (例如: "id-ID" 印尼語, "vi-VN" 越南語, "zh-TW" 繁體中文)
    func speak(text: String, languageCode: String? = nil) {
        // 停止當前正在播報的語音
        stop()
        
        let utterance = AVSpeechUtterance(string: text)
        
        // 如果沒有指定語言，則使用系統當前語言
        let lang = languageCode ?? Locale.current.language.languageCode?.identifier ?? "zh-TW"
        utterance.voice = AVSpeechSynthesisVoice(language: lang)
        
        // 針對長輩無障礙設計：稍微調慢語速 (預設是 0.5，調慢至 0.45)，讓聽覺更清晰
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate * 0.9 
        utterance.pitchMultiplier = 1.0 // 正常音調
        utterance.volume = 1.0 // 最大音量

        synthesizer.speak(utterance)
    }
    
    // 停止播報
    func stop() {
        if synthesizer.isSpeaking {
            synthesizer.stopSpeaking(at: .immediate)
        }
    }
}
