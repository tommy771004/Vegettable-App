import Foundation

struct Configuration {
    static var apiBaseUrl: String {
        guard let path = Bundle.main.path(forResource: "Config", ofType: "plist"),
              let dict = NSDictionary(contentsOfFile: path) as? [String: Any],
              let url = dict["API_BASE_URL"] as? String, !url.isEmpty else {
            assertionFailure("API_BASE_URL not found in Config.plist. Please configure it before running.")
            return ""
        }
        return url
    }
}
