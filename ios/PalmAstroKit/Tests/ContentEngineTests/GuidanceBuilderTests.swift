import Foundation
import Testing
import CoreContracts
@testable import ContentEngine

/// GuidanceBuilder tests mirroring the Kotlin engine-content cases
/// (GuidanceBuilderTest.kt): bucket-generic fallback, deterministic
/// signal-backed selection with stable tie-breaks, week plan bucketing, month
/// theme, and language fallback. Cross-platform agreement is asserted
/// separately by the guidance parity fixtures.
@Suite struct GuidanceBuilderTests {

    // MARK: - Input helpers (mirroring the Kotlin makeInput/compose flow)

    private func entry(_ signalId: String, _ domain: String, _ contribution: Double) -> ExplainEntry {
        ExplainEntry(signalId: signalId, mapping: "\(signalId) → \(domain)", contribution: contribution)
    }

    private func makeInput(
        scores: [String: Int],
        explain: [ExplainEntry] = [],
        language: String = "en",
        grade: String = "Stable"
    ) -> ContentInput {
        ContentInput(
            scoringResult: ScoringResult(
                domainScores: scores, subdimScores: [:],
                grade: grade, confidence: "high", confidenceReasons: [],
                explainability: explain, matchedBuckets: [], rulesetVersion: "2.0.0"
            ),
            deltaResult: nil, tone: .SCIENTIFIC, entitlements: [],
            calcLevel: .L2, monthKey: "2026-07", language: language
        )
    }

    private let evenScores = ["career": 50, "wealth": 50, "family": 50, "health": 50]

    private func compose(
        scores: [String: Int],
        explain: [ExplainEntry] = [],
        language: String = "en",
        grade: String = "Stable"
    ) throws -> [String: SemanticPayload] {
        try ContentComposerImpl().compose(
            input: makeInput(scores: scores, explain: explain, language: language, grade: grade)
        )
    }

    // MARK: - Bucket fallback

    @Test func bucketGenericsBackTheGuidanceWhenNoSignalsContributed() throws {
        let builder = try GuidanceBuilder()
        let scores = ["career": 85, "wealth": 30, "family": 60, "health": 45]
        let guidance = builder.build(
            payloads: try compose(scores: scores, grade: "Building"),
            overallGrade: "Building",
            language: "en"
        )

        #expect(guidance.strengths.count == 3)
        #expect(guidance.strengths.allSatisfy { $0.signalId == nil }, "generic strengths carry no signalId")
        #expect(guidance.strengths.map(\.domain) == ["career", "family", "health"])

        #expect(guidance.mindful.count == 2)
        #expect(guidance.mindful.allSatisfy { $0.signalId == nil }, "generic mindful carry no signalId")
        #expect(guidance.mindful.map(\.domain) == ["wealth", "health"])

        #expect(guidance.weekPlan.count == 4)
        #expect(!guidance.monthTheme.isBlank)
        for item in guidance.strengths + guidance.mindful {
            #expect(!item.title.isBlank, "\(item.domain) title blank")
            #expect(!item.body.isBlank, "\(item.domain) body blank")
            #expect(!item.action.isBlank, "\(item.domain) action blank")
            #expect(!item.body.contains("{domain}"), "\(item.domain) leaked the placeholder")
        }
    }

    @Test func genericFallbackIsKeyedByScoreBucket() throws {
        let builder = try GuidanceBuilder()
        // career is the top-scoring domain in both runs; only its bucket
        // changes (peak vs rising), so its generic copy must change too.
        let peak = builder.build(
            payloads: try compose(scores: ["career": 90, "wealth": 10, "family": 10, "health": 10]),
            overallGrade: "Building", language: "en"
        )
        let rising = builder.build(
            payloads: try compose(scores: ["career": 66, "wealth": 10, "family": 10, "health": 10]),
            overallGrade: "Building", language: "en"
        )
        let peakCareer = try #require(peak.strengths.first { $0.domain == "career" })
        let risingCareer = try #require(rising.strengths.first { $0.domain == "career" })
        #expect(peakCareer.body != risingCareer.body, "peak/rising buckets must yield different generic copy")
    }

    // MARK: - Signal-backed order

    @Test func signalBackedStrengthsRankByContributionWithDistinctDomains() throws {
        let builder = try GuidanceBuilder()
        let scores = ["career": 80, "wealth": 70, "family": 68, "health": 30]
        let explain = [
            entry("PALM_HEADLINE_LONG_CLEAR", "career", 3.6),
            entry("PALM_FATELINE_STRONG", "career", 3.2),
            entry("PALM_FATELINE_STRONG", "wealth", 2.8),
            entry("PALM_HEARTLINE_STRONG", "family", 2.4),
            entry("PALM_LIFELINE_CLEAR", "health", 1.8),
            entry("PALM_LIFELINE_FAINT", "health", -4.9),
            entry("PALM_HEADLINE_CHAINED", "career", -4.3),
        ]
        let guidance = builder.build(
            payloads: try compose(scores: scores, explain: explain),
            overallGrade: "Stable", language: "en"
        )

        #expect(guidance.strengths.map { [$0.signalId, $0.domain] } == [
            ["PALM_HEADLINE_LONG_CLEAR", "career"],
            ["PALM_FATELINE_STRONG", "wealth"],
            ["PALM_HEARTLINE_STRONG", "family"],
        ])
        #expect(guidance.mindful.map { [$0.signalId, $0.domain] } == [
            ["PALM_LIFELINE_FAINT", "health"],
            ["PALM_HEADLINE_CHAINED", "career"],
        ])
    }

    @Test func mindfulKeepsOneCardPerSignalAcrossDomains() throws {
        let builder = try GuidanceBuilder()
        let explain = [
            entry("PALM_LIFELINE_FAINT", "health", -4.9),
            entry("PALM_LIFELINE_FAINT", "family", -3.2),
            entry("PALM_HEARTLINE_THIN", "family", -4.5),
        ]
        let guidance = builder.build(
            payloads: try compose(scores: evenScores, explain: explain),
            overallGrade: "Stable", language: "en"
        )
        #expect(guidance.mindful.map { [$0.signalId, $0.domain] } == [
            ["PALM_LIFELINE_FAINT", "health"],
            ["PALM_HEARTLINE_THIN", "family"],
        ])
    }

    @Test func positiveSignalWithoutGuidanceCopyFallsThroughToTheNextCandidate() throws {
        let builder = try GuidanceBuilder()
        let explain = [
            entry("ASTRO_JUPITER_STRONG", "wealth", 5.0),
            entry("PALM_HEADLINE_LONG_CLEAR", "career", 3.6),
        ]
        let guidance = builder.build(
            payloads: try compose(scores: evenScores, explain: explain),
            overallGrade: "Stable", language: "en"
        )
        #expect(guidance.strengths.first?.signalId == "PALM_HEADLINE_LONG_CLEAR")
        let wealth = try #require(guidance.strengths.first { $0.domain == "wealth" })
        #expect(wealth.signalId == nil, "wealth backfilled generically")
    }

    @Test func equalContributionsTieBreakByDomainOrder() throws {
        let builder = try GuidanceBuilder()
        let explain = [
            entry("PALM_HEADLINE_LONG_CLEAR", "career", 2.0),
            entry("PALM_FATELINE_STRONG", "wealth", 2.0),
            entry("PALM_HEARTLINE_STRONG", "family", 2.0),
            entry("PALM_LIFELINE_CLEAR", "health", 2.0),
        ]
        let guidance = builder.build(
            payloads: try compose(scores: ["career": 70, "wealth": 70, "family": 70, "health": 70], explain: explain),
            overallGrade: "Growing", language: "en"
        )
        // All contributions equal -> Domains.all order, then signalId.
        #expect(guidance.strengths.map(\.domain) == ["career", "wealth", "family"])
    }

    @Test func buildIsDeterministic() throws {
        let builder = try GuidanceBuilder()
        let explain = [
            entry("PALM_HEADLINE_LONG_CLEAR", "career", 3.6),
            entry("PALM_LIFELINE_FAINT", "health", -4.9),
        ]
        let payloads = try compose(scores: evenScores, explain: explain)
        let first = builder.build(payloads: payloads, overallGrade: "Stable", language: "en")
        for _ in 0..<10 {
            #expect(builder.build(payloads: payloads, overallGrade: "Stable", language: "en") == first)
        }
    }

    // MARK: - Week plan

    @Test func weekPlanSwitchesHighAndLowVariantsAtScore65() throws {
        let builder = try GuidanceBuilder()
        let templates = try ContentTemplates.loadDefault()
        let scores = ["career": 65, "wealth": 64, "family": 90, "health": 10]
        let guidance = builder.build(
            payloads: try compose(scores: scores),
            overallGrade: "Stable", language: "en"
        )
        #expect(guidance.weekPlan.count == 4)
        let domains = templates.guidance.domains
        func plan(_ domain: String, _ key: String) throws -> String {
            templates.localized(try #require(domains[domain]?.monthPlan[key]), language: "en")
        }
        #expect(guidance.weekPlan[0] == (try plan("career", "high")))
        #expect(guidance.weekPlan[1] == (try plan("wealth", "low")))
        #expect(guidance.weekPlan[2] == (try plan("family", "high")))
        #expect(guidance.weekPlan[3] == (try plan("health", "low")))
    }

    // MARK: - Month theme

    @Test func monthThemeFollowsTheOverallGrade() throws {
        let builder = try GuidanceBuilder()
        let payloads = try compose(scores: evenScores)
        var themes: Set<String> = []
        for grade in ["Growing", "Stable", "Building", "Watchout"] {
            let theme = builder.build(payloads: payloads, overallGrade: grade, language: "en").monthTheme
            #expect(!theme.isBlank, "\(grade) theme blank")
            themes.insert(theme)
        }
        #expect(themes.count == 4, "each grade has its own theme")
    }

    @Test func unknownGradeFallsBackToTheStableTheme() throws {
        let builder = try GuidanceBuilder()
        let payloads = try compose(scores: evenScores)
        #expect(
            builder.build(payloads: payloads, overallGrade: "NotAGrade", language: "en").monthTheme
                == builder.build(payloads: payloads, overallGrade: "Stable", language: "en").monthTheme
        )
    }

    // MARK: - Language fallback

    @Test func unsupportedLanguageFallsBackToEnglishEntirely() throws {
        let builder = try GuidanceBuilder()
        let payloads = try compose(scores: evenScores)
        #expect(
            builder.build(payloads: payloads, overallGrade: "Stable", language: "fr")
                == builder.build(payloads: payloads, overallGrade: "Stable", language: "en")
        )
    }

    @Test func languagesWithoutGuidanceCopyFallBackPerFieldToEnglish() throws {
        // ja is a supported template language, but guidance copy ships en + zh-TW.
        let builder = try GuidanceBuilder()
        let payloads = try compose(scores: evenScores)
        let en = builder.build(payloads: payloads, overallGrade: "Stable", language: "en")
        let ja = builder.build(payloads: payloads, overallGrade: "Stable", language: "ja")
        #expect(ja.monthTheme == en.monthTheme)
        #expect(ja.strengths.map(\.title) == en.strengths.map(\.title))
        #expect(ja.weekPlan == en.weekPlan)
    }

    @Test func zhTWGuidanceUsesTraditionalChineseCopy() throws {
        let builder = try GuidanceBuilder()
        let payloads = try compose(scores: evenScores, language: "zh-TW")
        let zh = builder.build(payloads: payloads, overallGrade: "Stable", language: "zh-TW")
        let en = builder.build(payloads: payloads, overallGrade: "Stable", language: "en")
        #expect(zh.monthTheme.contains("月份"), "zh-TW theme localized: \(zh.monthTheme)")
        #expect(zh.strengths.first?.title != en.strengths.first?.title)
        #expect(zh.monthTheme.unicodeScalars.contains { $0.value > 0x2E80 })
    }

    // MARK: - Tolerant decoding

    @Test func templatesWithoutGuidanceSectionYieldEmptyGuidance() throws {
        let minimal = """
        {"version": "2.0.0", "defaultLanguage": "en", "languages": ["en"]}
        """
        let templates = try ContentTemplates.fromJSON(Data(minimal.utf8))
        #expect(templates.guidance == GuidanceTemplates())
        let payloads = try compose(scores: evenScores)
        let guidance = GuidanceBuilder(templates: templates)
            .build(payloads: payloads, overallGrade: "Stable", language: "en")
        #expect(guidance.isEmpty)
    }

    @Test func unknownAndPartialGuidanceKeysDecodeSafely() throws {
        // The canonical Android file is copied over the local resource
        // verbatim — extra keys and partial entries must not break decoding.
        let json = """
        {
          "version": "2.1.0",
          "defaultLanguage": "en",
          "languages": ["en"],
          "buckets": {"high": {"min": 65, "max": 100}, "low": {"min": 0, "max": 64}},
          "guidance": {
            "futureKey": {"whatever": true},
            "signals": {
              "PALM_X": {"leanInto": {"title": {"en": "T"}, "extra": 1}, "unknown": []}
            },
            "domains": {
              "career": {
                "strengths": {"high": {"body": {"en": "B"}}},
                "monthPlan": {"high": {"en": "P"}},
                "futureField": "ignored"
              }
            },
            "monthTheme": {"Stable": {"en": "S"}}
          }
        }
        """
        let templates = try ContentTemplates.fromJSON(Data(json.utf8))
        #expect(templates.guidance.signals["PALM_X"]?.leanInto?.title["en"] == "T")
        #expect(templates.guidance.signals["PALM_X"]?.mindfulOf == nil)
        #expect(templates.guidance.domains["career"]?.strengths["high"]?.body["en"] == "B")
        #expect(templates.guidance.domains["career"]?.mindful.isEmpty == true)

        let builder = GuidanceBuilder(templates: templates)
        let payload = SemanticPayload(
            domain: "career", monthKey: "2026-07", calcLevel: .L1, confidence: "med",
            observations: [],
            interpretation: Interpretation(pattern: "p"),
            blindspot: "b", actionToday: "t", actionWeek: "w", prompt: "q", safetyNotes: [],
            explainability: [entry("PALM_X", "career", 1.0)],
            scoreCard: ScoreCard(totalScore: 80, grade: "Stable", delta: nil, comparabilityScore: nil, subdims: [:])
        )
        let guidance = builder.build(
            payloads: ["career": payload], overallGrade: "Stable", language: "en"
        )
        #expect(guidance.monthTheme == "S")
        #expect(guidance.strengths.first?.title == "T")
        #expect(guidance.weekPlan == ["P"])
    }

    // MARK: - Canonical vocabulary completeness (mirrors GuidanceTemplateTest)

    private static let guidanceLanguages = ["en", "zh-TW"]
    private static let positivePalm = [
        "PALM_HEADLINE_LONG_CLEAR", "PALM_HEARTLINE_STRONG",
        "PALM_LIFELINE_CLEAR", "PALM_FATELINE_STRONG",
    ]
    private static let negativePalm = [
        "PALM_HEADLINE_CHAINED", "PALM_FATELINE_BREAKS",
        "PALM_HEARTLINE_THIN", "PALM_LIFELINE_FAINT", "PALM_MINOR_LINES_DENSE",
    ]
    private static let astroScored = [
        "ASTRO_SUN_FIRE", "ASTRO_SUN_EARTH", "ASTRO_SUN_AIR", "ASTRO_SUN_WATER",
        "ASTRO_SUN_CARDINAL", "ASTRO_SUN_FIXED", "ASTRO_SUN_MUTABLE",
        "ASTRO_MOON_FIRE", "ASTRO_MOON_EARTH", "ASTRO_MOON_AIR", "ASTRO_MOON_WATER",
        "ASTRO_ASC_FIRE", "ASTRO_ASC_EARTH", "ASTRO_ASC_AIR", "ASTRO_ASC_WATER",
    ]

    private func expectComplete(_ context: String, _ copy: GuidanceCopy) {
        for lang in Self.guidanceLanguages {
            let title = copy.title[lang] ?? ""
            let body = copy.body[lang] ?? ""
            let action = copy.action[lang] ?? ""
            #expect(!title.isBlank, "\(context).title[\(lang)] blank")
            #expect(!body.isBlank, "\(context).body[\(lang)] blank")
            #expect(!action.isBlank, "\(context).action[\(lang)] blank")
        }
    }

    @Test func rulesetSignalsHaveDirectionAppropriateGuidanceCopy() throws {
        let signals = try ContentTemplates.loadDefault().guidance.signals
        for id in Self.positivePalm {
            expectComplete("\(id).leanInto", try #require(signals[id]?.leanInto, "\(id) missing leanInto"))
        }
        for id in Self.negativePalm {
            expectComplete("\(id).mindfulOf", try #require(signals[id]?.mindfulOf, "\(id) missing mindfulOf"))
        }
        for id in Self.astroScored {
            expectComplete("\(id).leanInto", try #require(signals[id]?.leanInto, "\(id) missing leanInto"))
            expectComplete("\(id).mindfulOf", try #require(signals[id]?.mindfulOf, "\(id) missing mindfulOf"))
        }
    }

    @Test func everyScoreMapsToStrengthsAndMindfulGenericsInEveryDomain() throws {
        let templates = try ContentTemplates.loadDefault()
        for domain in Domains.all {
            let domainTemplate = try #require(templates.guidance.domains[domain], "\(domain) missing guidance")
            for score in 0...100 {
                #expect(templates.bucketValue(domainTemplate.strengths, score: score) != nil,
                        "\(domain).strengths uncovered at \(score)")
                #expect(templates.bucketValue(domainTemplate.mindful, score: score) != nil,
                        "\(domain).mindful uncovered at \(score)")
            }
            for key in ["high", "low"] {
                let line = try #require(domainTemplate.monthPlan[key], "\(domain).monthPlan.\(key) missing")
                for lang in Self.guidanceLanguages {
                    let text = line[lang] ?? ""
                    #expect(!text.isBlank, "\(domain).monthPlan.\(key)[\(lang)] blank")
                }
            }
        }
        for grade in ["Growing", "Stable", "Building", "Watchout"] {
            let theme = try #require(templates.guidance.monthTheme[grade], "monthTheme.\(grade) missing")
            for lang in Self.guidanceLanguages {
                let text = theme[lang] ?? ""
                #expect(!text.isBlank, "monthTheme.\(grade)[\(lang)] blank")
            }
        }
    }
}
