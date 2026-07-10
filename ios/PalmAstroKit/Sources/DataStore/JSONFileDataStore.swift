import Foundation

/// JSON-file implementation of `DataStoring`. Documents live at
/// `<root>/<collection>/<key>.json`; writes are atomic. On iOS the app layer
/// passes a root inside Application Support with
/// `FileProtectionType.complete` applied (PRD §27); on macOS (tests) any
/// temporary directory works.
public final class JSONFileDataStore: DataStoring {

    private let rootURL: URL
    private let fileManager: FileManager
    private let encoder: JSONEncoder
    private let decoder: JSONDecoder

    /// Allowed characters for collection names and keys — prevents path
    /// traversal via crafted identifiers.
    private static let identifierCharacters = CharacterSet(
        charactersIn: "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789._-"
    )

    public init(rootURL: URL, fileManager: FileManager = .default) {
        self.rootURL = rootURL
        self.fileManager = fileManager
        self.encoder = JSONEncoder()
        self.encoder.outputFormatting = [.sortedKeys]
        self.decoder = JSONDecoder()
    }

    public func save<T: Encodable>(_ value: T, collection: String, key: String) throws {
        let url = try documentURL(collection: collection, key: key)
        try fileManager.createDirectory(
            at: url.deletingLastPathComponent(),
            withIntermediateDirectories: true
        )
        let data = try encoder.encode(value)
        try data.write(to: url, options: .atomic)
    }

    public func load<T: Decodable>(_ type: T.Type, collection: String, key: String) throws -> T? {
        let url = try documentURL(collection: collection, key: key)
        guard fileManager.fileExists(atPath: url.path) else { return nil }
        let data = try Data(contentsOf: url)
        return try decoder.decode(type, from: data)
    }

    public func listKeys(collection: String) throws -> [String] {
        let dir = try collectionURL(collection)
        guard fileManager.fileExists(atPath: dir.path) else { return [] }
        return try fileManager.contentsOfDirectory(at: dir, includingPropertiesForKeys: nil)
            .filter { $0.pathExtension == "json" }
            .map { $0.deletingPathExtension().lastPathComponent }
            .sorted()
    }

    public func delete(collection: String, key: String) throws {
        let url = try documentURL(collection: collection, key: key)
        if fileManager.fileExists(atPath: url.path) {
            try fileManager.removeItem(at: url)
        }
    }

    public func deleteAll() throws {
        guard fileManager.fileExists(atPath: rootURL.path) else { return }
        let contents = try fileManager.contentsOfDirectory(at: rootURL, includingPropertiesForKeys: nil)
        for item in contents {
            try fileManager.removeItem(at: item)
        }
    }

    // MARK: - Paths

    private func collectionURL(_ collection: String) throws -> URL {
        guard Self.isValidIdentifier(collection) else {
            throw DataStoreError.invalidCollection(collection)
        }
        return rootURL.appendingPathComponent(collection, isDirectory: true)
    }

    private func documentURL(collection: String, key: String) throws -> URL {
        guard Self.isValidIdentifier(key) else {
            throw DataStoreError.invalidKey(key)
        }
        return try collectionURL(collection).appendingPathComponent("\(key).json")
    }

    private static func isValidIdentifier(_ value: String) -> Bool {
        !value.isEmpty
            && value.unicodeScalars.allSatisfy { identifierCharacters.contains($0) }
            && !value.hasPrefix(".")
    }
}
