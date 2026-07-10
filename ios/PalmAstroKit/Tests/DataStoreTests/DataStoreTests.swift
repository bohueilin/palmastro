import Foundation
import Testing
import CoreContracts
@testable import DataStore

/// Each test gets its own temp-dir-backed store (init runs per test);
/// directories are cleaned up in deinit.
final class DataStoreTests {

    private let rootURL: URL
    private let store: JSONFileDataStore

    init() {
        rootURL = FileManager.default.temporaryDirectory
            .appendingPathComponent("palmastro-datastore-tests-\(UUID().uuidString)")
        store = JSONFileDataStore(rootURL: rootURL)
    }

    deinit {
        try? FileManager.default.removeItem(at: rootURL)
    }

    private func makeMonthlyResult(monthKey: String, careerScore: Int = 60) -> MonthlyResult {
        MonthlyResult(
            resultId: "r-\(monthKey)",
            monthKey: monthKey,
            scanSessionId: "s-\(monthKey)",
            scoringResult: ScoringResult(
                domainScores: ["career": careerScore, "wealth": 55, "family": 52, "health": 58],
                subdimScores: [:], grade: "Stable", confidence: "med",
                confidenceReasons: [], explainability: [], matchedBuckets: [],
                rulesetVersion: "2.0.0"
            ),
            semanticPayloads: [:],
            scanQualityScore: 80,
            featureCoverage: 0.7,
            createdAt: 1_780_000_000_000
        )
    }

    @Test func saveAndLoadRoundTrip() throws {
        let result = makeMonthlyResult(monthKey: "2026-07")
        try store.save(result, collection: "results", key: result.monthKey)
        let loaded = try store.load(MonthlyResult.self, collection: "results", key: "2026-07")
        #expect(loaded == result)
    }

    @Test func loadMissingReturnsNil() throws {
        #expect(try store.load(MonthlyResult.self, collection: "results", key: "2099-01") == nil)
    }

    @Test func listKeysAndDelete() throws {
        try store.save(["a": 1], collection: "misc", key: "one")
        try store.save(["b": 2], collection: "misc", key: "two")
        #expect(try store.listKeys(collection: "misc") == ["one", "two"])

        try store.delete(collection: "misc", key: "one")
        #expect(try store.listKeys(collection: "misc") == ["two"])
    }

    @Test func deleteAllWipesEverything() throws {
        try store.save(makeMonthlyResult(monthKey: "2026-06"), collection: "results", key: "2026-06")
        try store.save(["k": "v"], collection: "profile", key: "user")
        try store.deleteAll()
        #expect(try store.listKeys(collection: "results") == [])
        #expect(try store.listKeys(collection: "profile") == [])
        #expect(try store.load(MonthlyResult.self, collection: "results", key: "2026-06") == nil)
    }

    @Test func pathTraversalKeysRejected() {
        #expect(throws: (any Error).self) {
            try self.store.save(["x": 1], collection: "results", key: "../escape")
        }
        #expect(throws: (any Error).self) {
            try self.store.save(["x": 1], collection: "a/b", key: "ok")
        }
        #expect(throws: (any Error).self) {
            try self.store.save(["x": 1], collection: "results", key: ".hidden")
        }
    }

    // MARK: - ResultRepositoryImpl

    @Test func resultRepositoryHistoryNewestFirstWithLimit() throws {
        let repo = ResultRepositoryImpl(store: store)
        try repo.saveMonthlyResult(makeMonthlyResult(monthKey: "2026-05"))
        try repo.saveMonthlyResult(makeMonthlyResult(monthKey: "2026-07"))
        try repo.saveMonthlyResult(makeMonthlyResult(monthKey: "2026-06"))

        let history = repo.listHistory(limit: 2)
        #expect(history.map(\.monthKey) == ["2026-07", "2026-06"])
        #expect(repo.getMonthlyResult(monthKey: "2026-05")?.monthKey == "2026-05")
        #expect(repo.getMonthlyResult(monthKey: "2026-01") == nil)
    }

    @Test func resultRepositoryOverwritesSameMonth() throws {
        let repo = ResultRepositoryImpl(store: store)
        try repo.saveMonthlyResult(makeMonthlyResult(monthKey: "2026-07", careerScore: 50))
        try repo.saveMonthlyResult(makeMonthlyResult(monthKey: "2026-07", careerScore: 70))
        #expect(repo.getMonthlyResult(monthKey: "2026-07")?.scoringResult.domainScores["career"] == 70)
        #expect(repo.listHistory(limit: 10).count == 1)
    }

    // MARK: - EntitlementServiceImpl

    @Test func entitlementsGrantRevokePersist() throws {
        let service = EntitlementServiceImpl(store: store)
        #expect(!service.hasEntitlement(productId: ProductIds.careerPack))

        try service.grant(productId: ProductIds.careerPack)
        #expect(service.hasEntitlement(productId: ProductIds.careerPack))

        // A fresh instance over the same store sees the persisted state.
        let reloaded = EntitlementServiceImpl(store: store)
        #expect(reloaded.hasEntitlement(productId: ProductIds.careerPack))

        try service.revoke(productId: ProductIds.careerPack)
        #expect(!service.hasEntitlement(productId: ProductIds.careerPack))
    }

    @Test func entitlementsClearedByDeleteAll() throws {
        let service = EntitlementServiceImpl(store: store)
        try service.grant(productId: ProductIds.bundle)
        try store.deleteAll()
        #expect(!service.hasEntitlement(productId: ProductIds.bundle))
    }

    // MARK: - JournalRepositoryImpl

    @Test func journalSaveListDelete() throws {
        let repo = JournalRepositoryImpl(store: store)
        try repo.save(JournalEntry(entryId: "e1", monthKey: "2026-07", domain: "career", text: "note one", createdAt: 100))
        try repo.save(JournalEntry(entryId: "e2", monthKey: "2026-07", domain: "health", text: "note two", createdAt: 200))

        let entries = repo.list()
        #expect(entries.map(\.entryId) == ["e2", "e1"], "newest first")

        try repo.delete(entryId: "e2")
        #expect(repo.list().map(\.entryId) == ["e1"])
    }
}
