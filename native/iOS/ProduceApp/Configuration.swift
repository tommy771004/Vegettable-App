import Foundation

struct Configuration {
    static var apiBaseUrl: String {
        guard let path = Bundle.main.path(forResource: "Config", ofType: "plist"),
              let dict = NSDictionary(contentsOfFile: path) as? [String: Any],
              let url = dict["API_BASE_URL"] as? String else {
            return "https://ais-dev-gyv3my74fwisdg5piudwph-424197195798.asia-east1.run.app/api/produce/" // Fallback
        }
        return url
    }
}
