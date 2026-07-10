import Foundation

/// Local persistence abstraction for the iOS app (PRD §48 DataStore module).
/// Values are Codable documents grouped into named collections. The launch
/// implementation is a JSON-file store with zero external dependencies;
/// a GRDB/SwiftData-backed implementation can replace it behind the same
/// protocol post-launch if needed.
public protocol DataStoring: AnyObject {
    func save<T: Encodable>(_ value: T, collection: String, key: String) throws
    func load<T: Decodable>(_ type: T.Type, collection: String, key: String) throws -> T?
    func listKeys(collection: String) throws -> [String]
    func delete(collection: String, key: String) throws
    /// Removes every collection and document (delete-all-data, PRD §28).
    func deleteAll() throws
}

public enum DataStoreError: Error, Equatable {
    case invalidKey(String)
    case invalidCollection(String)
}
