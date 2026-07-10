import Foundation
import CoreContracts

/// `ResultRepository` (contracts StorageInterfaces) backed by any `DataStoring`.
/// Monthly results are keyed by month key ("YYYY-MM"), newest first in history.
public final class ResultRepositoryImpl: ResultRepository {

    public static let collection = "results"

    private let store: DataStoring

    public init(store: DataStoring) {
        self.store = store
    }

    public func saveMonthlyResult(_ result: MonthlyResult) throws {
        try store.save(result, collection: Self.collection, key: result.monthKey)
    }

    public func getMonthlyResult(monthKey: String) -> MonthlyResult? {
        (try? store.load(MonthlyResult.self, collection: Self.collection, key: monthKey)) ?? nil
    }

    public func listHistory(limit: Int) -> [MonthlyResult] {
        guard limit > 0, let keys = try? store.listKeys(collection: Self.collection) else { return [] }
        return keys.sorted(by: >)
            .prefix(limit)
            .compactMap { getMonthlyResult(monthKey: $0) }
    }
}

/// Local entitlement persistence (`EntitlementService`). StoreKit remains the
/// source of truth; this mirror keeps entitlement checks synchronous and
/// offline-safe. With `iap_enabled=false` at launch it simply stays empty.
public final class EntitlementServiceImpl: EntitlementService {

    public static let collection = "entitlements"
    private static let key = "active"

    private let store: DataStoring

    public init(store: DataStoring) {
        self.store = store
    }

    public func hasEntitlement(productId: String) -> Bool {
        activeEntitlements().contains(productId)
    }

    public func activeEntitlements() -> Set<String> {
        let stored = (try? store.load([String].self, collection: Self.collection, key: Self.key)) ?? nil
        return Set(stored ?? [])
    }

    public func grant(productId: String) throws {
        var active = activeEntitlements()
        active.insert(productId)
        try persist(active)
    }

    public func revoke(productId: String) throws {
        var active = activeEntitlements()
        active.remove(productId)
        try persist(active)
    }

    public func replaceAll(with productIds: Set<String>) throws {
        try persist(productIds)
    }

    private func persist(_ active: Set<String>) throws {
        try store.save(active.sorted(), collection: Self.collection, key: Self.key)
    }
}

/// Journal entries (PRD §13.6, §52): local-only, plain user text that never
/// leaves the device and is wiped by delete-all-data.
public struct JournalEntry: Codable, Equatable, Sendable {
    public let entryId: String
    public let monthKey: String
    public let domain: String
    public let text: String
    public let createdAt: Int64

    public init(entryId: String, monthKey: String, domain: String, text: String, createdAt: Int64) {
        self.entryId = entryId
        self.monthKey = monthKey
        self.domain = domain
        self.text = text
        self.createdAt = createdAt
    }
}

public final class JournalRepositoryImpl {

    public static let collection = "journal"

    private let store: DataStoring

    public init(store: DataStoring) {
        self.store = store
    }

    public func save(_ entry: JournalEntry) throws {
        try store.save(entry, collection: Self.collection, key: entry.entryId)
    }

    public func list() -> [JournalEntry] {
        guard let keys = try? store.listKeys(collection: Self.collection) else { return [] }
        return keys
            .compactMap { (try? store.load(JournalEntry.self, collection: Self.collection, key: $0)) ?? nil }
            .sorted { $0.createdAt > $1.createdAt }
    }

    public func delete(entryId: String) throws {
        try store.delete(collection: Self.collection, key: entryId)
    }
}
