import Foundation
import CoreData

class ProduceCoreDataStore {
    static let shared = ProduceCoreDataStore()
    let persistentContainer: NSPersistentContainer

    private init() {
        persistentContainer = NSPersistentContainer(name: "ProduceModel")
        persistentContainer.loadPersistentStores { description, error in
            if let error = error {
                fatalError("Core Data store failed to load: \(error.localizedDescription)")
            }
        }
    }

    // Save fetched JSON data for offline use
    func saveCache(marketCode: String, data: Data) {
        let context = persistentContainer.viewContext
        
        let fetchRequest = NSFetchRequest<NSManagedObject>(entityName: "ProduceCache")
        fetchRequest.predicate = NSPredicate(format: "marketCode == %@", marketCode)
        
        do {
            let results = try context.fetch(fetchRequest)
            let cacheEntity = results.first ?? NSEntityDescription.insertNewObject(forEntityName: "ProduceCache", into: context)
            
            cacheEntity.setValue(marketCode, forKey: "marketCode")
            cacheEntity.setValue(data, forKey: "jsonData")
            cacheEntity.setValue(Date(), forKey: "updatedAt")
            
            try context.save()
        } catch {
            print("Failed to save cache: \(error.localizedDescription)")
        }
    }

    // Fetch cached data when offline
    func fetchCache(marketCode: String) -> Data? {
        let context = persistentContainer.viewContext
        let fetchRequest = NSFetchRequest<NSManagedObject>(entityName: "ProduceCache")
        fetchRequest.predicate = NSPredicate(format: "marketCode == %@", marketCode)

        do {
            let results = try context.fetch(fetchRequest)
            return results.first?.value(forKey: "jsonData") as? Data
        } catch {
            print("Failed to fetch cache: \(error.localizedDescription)")
            return nil
        }
    }

    // 將 API 回傳的農產品列表存入本地快取 (供離線模式使用)
    // 為何新增：ProduceRepository 呼叫了此方法但原先未實作，導致編譯錯誤
    func saveProduceList(_ items: [ProduceDto]) {
        do {
            let encoder = JSONEncoder()
            let data = try encoder.encode(items)
            saveCache(marketCode: "ALL_PRODUCE", data: data)
        } catch {
            print("Failed to encode produce list for caching: \(error.localizedDescription)")
        }
    }

    // 從本地快取讀取農產品列表 (離線模式)
    // 為何新增：ProduceRepository 在網路失敗時需要讀取快取資料
    func getProduceList(keyword: String = "", page: Int = 1) -> [ProduceDto]? {
        guard let data = fetchCache(marketCode: "ALL_PRODUCE") else { return nil }

        do {
            let decoder = JSONDecoder()
            var items = try decoder.decode([ProduceDto].self, from: data)

            if !keyword.isEmpty {
                items = items.filter { $0.cropName.contains(keyword) || $0.marketName.contains(keyword) }
            }

            let pageSize = 20
            let startIndex = (page - 1) * pageSize
            let endIndex = min(startIndex + pageSize, items.count)

            guard startIndex < items.count else { return [] }
            return Array(items[startIndex..<endIndex])
        } catch {
            print("Failed to decode cached produce list: \(error.localizedDescription)")
            return nil
        }
    }
}
