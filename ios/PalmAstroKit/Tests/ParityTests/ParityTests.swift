import Foundation
import Testing
import CoreContracts
@testable import ScoringEngine
@testable import ContentEngine
@testable import AstroEngine

/// Cross-platform parity harness (PRD §10, Workstream C6).
///
/// Fixtures live in `ios/shared-fixtures/{astro,scoring,content,guidance}/*.json` and are
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
    static var hasGuidanceFixtures: Bool { !fixtureURLs(subdirectory: "guidance").isEmpty }
    static var hasAstroFixtures: Bool { !fixtureURLs(subdirectory: "astro").isEmpty }

    // MARK: - Astro parity

    private struct AstroFixture: Decodable {
        struct Input: Decodable {
            let birthday: CivilDate
            let birthTime: CivilTime?
            let birthPlaceLat: Double?
            let birthPlaceLon: Double?
        }

        let input: Input
        let expected: AstroResult
    }

    /// Guards the whole astro surface — signal ids, their order, magnitudes and
    /// safety tags — which ScoringInput-level fixtures structurally cannot see,
    /// because they start from an already-computed AstroResult. Several cases
    /// sit within 0.08 deg of a 30-deg sign boundary, where any change to the
    /// lunar series or the ascendant latitude clamp flips the resolved element.
    @Test(.enabled(
        if: ParityTests.hasAstroFixtures,
        "No fixtures in ios/shared-fixtures/astro — generated from the Android engines at integration time."
    ))
    func astroParityFixtures() throws {
        let engine = AstroEngineImpl()
        for url in Self.fixtureURLs(subdirectory: "astro") {
            let fixture = try JSONDecoder().decode(AstroFixture.self, from: Data(contentsOf: url))
            let actual = engine.compute(
                birthday: fixture.input.birthday,
                birthTime: fixture.input.birthTime,
                birthPlaceLat: fixture.input.birthPlaceLat,
                birthPlaceLon: fixture.input.birthPlaceLon
            )
            let name = url.lastPathComponent

            #expect(actual.calcLevel == fixture.expected.calcLevel, "\(name): calcLevel")
            #expect(
                actual.signals.map(\.signalId) == fixture.expected.signals.map(\.signalId),
                "\(name): signal ids and order"
            )
            #expect(actual.signals == fixture.expected.signals, "\(name): signal shape")
        }
    }

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

    // MARK: - Guidance parity

    private struct GuidanceFixture: Decodable {
        struct Input: Decodable {
            let payloads: [String: SemanticPayload]
            let overallGrade: String
            let language: String
        }

        let input: Input
        let expected: Guidance
    }

    @Test(.enabled(
        if: ParityTests.hasGuidanceFixtures,
        "No fixtures in ios/shared-fixtures/guidance — generated from the Android engines at integration time."
    ))
    func guidanceParityFixtures() throws {
        let builder = try GuidanceBuilder()
        for url in Self.fixtureURLs(subdirectory: "guidance") {
            let fixture = try JSONDecoder().decode(GuidanceFixture.self, from: Data(contentsOf: url))
            let actual = builder.build(
                payloads: fixture.input.payloads,
                overallGrade: fixture.input.overallGrade,
                language: fixture.input.language
            )
            let name = url.lastPathComponent

            #expect(actual.monthTheme == fixture.expected.monthTheme, "\(name): monthTheme")
            #expect(actual.weekPlan == fixture.expected.weekPlan, "\(name): weekPlan")
            #expect(
                actual.strengths.map(\.signalId) == fixture.expected.strengths.map(\.signalId),
                "\(name): strengths selection/order"
            )
            #expect(actual.strengths == fixture.expected.strengths, "\(name): strengths")
            #expect(
                actual.mindful.map(\.signalId) == fixture.expected.mindful.map(\.signalId),
                "\(name): mindful selection/order"
            )
            #expect(actual.mindful == fixture.expected.mindful, "\(name): mindful")
        }
    }

    // MARK: - Guidance golden snapshots (Android engine-content goldens)

    /// The Android GuidanceGoldenSnapshotTest fixes one input and commits the
    /// resulting Guidance to engine-content/src/test/resources/golden/. When
    /// this checkout carries the Android tree, replicate the same input
    /// through the Swift composer + builder and compare structurally.
    static var goldenGuidanceURLs: [(language: String, url: URL)] {
        let golden = fixturesRootURL
            .deletingLastPathComponent()  // ios/
            .deletingLastPathComponent()  // repo root
            .appendingPathComponent("engine-content/src/test/resources/golden", isDirectory: true)
        return ["en", "zh-TW"]
            .map { ($0, golden.appendingPathComponent("guidance_\($0).json")) }
            .filter { FileManager.default.fileExists(atPath: $0.1.path) }
    }

    @Test(.enabled(
        if: !ParityTests.goldenGuidanceURLs.isEmpty,
        "Android golden guidance snapshots not present in this checkout."
    ))
    func guidanceMatchesAndroidGoldenSnapshots() throws {
        let composer = try ContentComposerImpl()
        let builder = try GuidanceBuilder()
        for (language, url) in Self.goldenGuidanceURLs {
            // Same fixed input as the Kotlin GuidanceGoldenSnapshotTest.
            let input = ContentInput(
                scoringResult: ScoringResult(
                    domainScores: ["career": 72, "wealth": 58, "family": 65, "health": 40],
                    subdimScores: ["career.focus": 75],
                    grade: "Stable", confidence: "high", confidenceReasons: ["full_scan"],
                    explainability: [
                        ExplainEntry(signalId: "PALM_HEADLINE_LONG_CLEAR", mapping: "PALM_HEADLINE_LONG_CLEAR → career", contribution: 3.6),
                        ExplainEntry(signalId: "ASTRO_JUPITER_STRONG", mapping: "ASTRO_JUPITER_STRONG → wealth", contribution: 2.1),
                        ExplainEntry(signalId: "PALM_HEARTLINE_STRONG", mapping: "PALM_HEARTLINE_STRONG → family", contribution: 2.4),
                        ExplainEntry(signalId: "PALM_LIFELINE_CLEAR", mapping: "PALM_LIFELINE_CLEAR → health", contribution: 1.8),
                    ],
                    matchedBuckets: [], rulesetVersion: "2.0.0"
                ),
                deltaResult: nil, tone: .SCIENTIFIC, entitlements: [],
                calcLevel: .L2, monthKey: "2026-07", language: language
            )
            let expected = try JSONDecoder().decode(Guidance.self, from: Data(contentsOf: url))
            let actual = builder.build(
                payloads: composer.compose(input: input),
                overallGrade: "Stable",
                language: language
            )
            #expect(actual == expected, "guidance golden snapshot mismatch [\(language)]")
        }
    }
}
