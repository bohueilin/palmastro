import Foundation
import Testing
import CoreContracts
@testable import ScoringEngine
@testable import ContentEngine

/// Cross-platform parity harness (PRD §10, Workstream C6).
///
/// Fixtures live in `ios/shared-fixtures/{scoring,content}/*.json` and are
/// generated from the Android engines (the reference implementation). Each
/// fixture is `{ "input": ..., "expected": ... }`. While the directories are
/// empty the tests are skipped — fixture generation happens at integration
/// time (see ios/shared-fixtures/README.md).
@Suite struct ParityTests {

    // MARK: - Fixture discovery

    /// ios/shared-fixtures, located relative to this source file:
    /// .../ios/PalmAstroKit/Tests/ParityTests/ParityTests.swift
    static var fixturesRootURL: URL {
        URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()  // ParityTests/
            .deletingLastPathComponent()  // Tests/
            .deletingLastPathComponent()  // PalmAstroKit/
            .deletingLastPathComponent()  // ios/
            .appendingPathComponent("shared-fixtures", isDirectory: true)
    }

    static func fixtureURLs(subdirectory: String) -> [URL] {
        let dir = fixturesRootURL.appendingPathComponent(subdirectory, isDirectory: true)
        guard let contents = try? FileManager.default.contentsOfDirectory(
            at: dir, includingPropertiesForKeys: nil
        ) else { return [] }
        return contents.filter { $0.pathExtension == "json" }.sorted { $0.path < $1.path }
    }

    static var hasScoringFixtures: Bool { !fixtureURLs(subdirectory: "scoring").isEmpty }
    static var hasContentFixtures: Bool { !fixtureURLs(subdirectory: "content").isEmpty }

    // MARK: - Scoring parity

    private struct ScoringFixture: Decodable {
        let input: ScoringInput
        let expected: ScoringResult
    }

    @Test(.enabled(
        if: ParityTests.hasScoringFixtures,
        "No fixtures in ios/shared-fixtures/scoring — generated from the Android engines at integration time."
    ))
    func scoringParityFixtures() throws {
        let engine = try ScoringEngineImpl()
        for url in Self.fixtureURLs(subdirectory: "scoring") {
            let fixture = try JSONDecoder().decode(ScoringFixture.self, from: Data(contentsOf: url))
            let actual = engine.score(input: fixture.input)
            let name = url.lastPathComponent

            #expect(actual.domainScores == fixture.expected.domainScores, "\(name): domainScores")
            #expect(actual.grade == fixture.expected.grade, "\(name): grade")
            #expect(actual.confidence == fixture.expected.confidence, "\(name): confidence")
            #expect(actual.confidenceReasons == fixture.expected.confidenceReasons, "\(name): confidenceReasons")
            #expect(actual.rulesetVersion == fixture.expected.rulesetVersion, "\(name): rulesetVersion")

            #expect(
                actual.explainability.map(\.signalId) == fixture.expected.explainability.map(\.signalId),
                "\(name): explainability order"
            )
            for (a, e) in zip(actual.explainability, fixture.expected.explainability) {
                #expect(a.mapping == e.mapping, "\(name): mapping")
                #expect(abs(a.contribution - e.contribution) < 1e-9, "\(name): contribution \(a.signalId)")
            }
        }
    }

    // MARK: - Content parity

    private struct ContentFixture: Decodable {
        let input: ContentInput
        let expected: [String: SemanticPayload]
    }

    @Test(.enabled(
        if: ParityTests.hasContentFixtures,
        "No fixtures in ios/shared-fixtures/content — generated from the Android engines at integration time."
    ))
    func contentParityFixtures() throws {
        let composer = try ContentComposerImpl()
        for url in Self.fixtureURLs(subdirectory: "content") {
            let fixture = try JSONDecoder().decode(ContentFixture.self, from: Data(contentsOf: url))
            let actual = composer.compose(input: fixture.input)
            let name = url.lastPathComponent

            #expect(Set(actual.keys) == Set(fixture.expected.keys), "\(name): domains")
            for (domain, expected) in fixture.expected {
                guard let payload = actual[domain] else {
                    Issue.record("\(name): missing domain \(domain)")
                    continue
                }
                #expect(payload.language == expected.language, "\(name)/\(domain): language")
                #expect(payload.calcLevel == expected.calcLevel, "\(name)/\(domain): calcLevel")
                #expect(payload.confidence == expected.confidence, "\(name)/\(domain): confidence")
                #expect(payload.interpretation == expected.interpretation, "\(name)/\(domain): interpretation")
                #expect(payload.blindspot == expected.blindspot, "\(name)/\(domain): blindspot")
                #expect(payload.actionToday == expected.actionToday, "\(name)/\(domain): actionToday")
                #expect(payload.actionWeek == expected.actionWeek, "\(name)/\(domain): actionWeek")
                #expect(payload.prompt == expected.prompt, "\(name)/\(domain): prompt")
                #expect(payload.safetyNotes == expected.safetyNotes, "\(name)/\(domain): safetyNotes")
                #expect(payload.scoreCard == expected.scoreCard, "\(name)/\(domain): scoreCard")
                #expect(payload.observations == expected.observations, "\(name)/\(domain): observations")
            }
        }
    }
}
