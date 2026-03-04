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
}
