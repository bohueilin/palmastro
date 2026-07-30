import Foundation
import SwiftUI
import CoreContracts
import PalmFeatureEngine
import AstroEngine
import ScoringEngine
import ContentEngine
import SafetyEngine
import AnalyticsService
import DataStore

/// Locally stored user profile (PRD §52). Sensitive fields never leave the
/// device; wiped by delete-all-data.
struct UserProfile: Codable, Equatable {
    var displayName: String = ""
    var birthday: CivilDate?
    var birthTime: CivilTime?
    var birthPlaceLat: Double?
    var birthPlaceLon: Double?
    var dominantHand: Hand = .RIGHT
    var relationshipStatus: String = ""       // optional, free-only launch keeps it local
    var tone: Tone = .SCIENTIFIC
    var language: String = "system"           // "system" | "en" | "zh-TW"
    var reminders: String = "off"             // opt-in per EXECUTION_SPEC
    var retainRawMedia: Bool = true           // 24h retention default (PRD §15)
    var onboardingComplete: Bool = false
}

/// App-wide dependency container and result pipeline.
/// Pipeline per EXECUTION_SPEC: compose → validate() each payload → replace
/// violations with the engine-provided safe fallback → render via ToneRenderer
/// → filter().
@MainActor
final class AppModel: ObservableObject {

    @Published var profile: UserProfile
    @Published private(set) var latestResult: MonthlyResult?
    @Published private(set) var history: [MonthlyResult] = []

    let store: JSONFileDataStore
    let resultRepository: ResultRepositoryImpl
    let journalRepository: JournalRepositoryImpl
    let entitlementService: EntitlementServiceImpl
    let analytics: AnalyticsEmitterImpl
    let flags = FeatureFlags.shared
    lazy var purchaseService = PurchaseService(entitlements: entitlementService)

    private let extractor = PalmFeatureExtractorImpl()
    private let astroEngine = AstroEngineImpl()
    private let scoringEngine: ScoringEngineImpl?
    private let deltaEngine = DeltaEngineImpl()
    private let composer: ContentComposerImpl?
    private let renderer: ToneRendererImpl?
    private let safetyFilter: SafetyFilterImpl?
    private let guidanceBuilder: GuidanceBuilder?

    private static let profileCollection = "profile"
    private static let profileKey = "user"

    init() {
        let root = Self.dataRootURL()
        store = JSONFileDataStore(rootURL: root)
        resultRepository = ResultRepositoryImpl(store: store)
        journalRepository = JournalRepositoryImpl(store: store)
        entitlementService = EntitlementServiceImpl(store: store)
        analytics = AnalyticsEmitterImpl { _, _ in
            // Launch posture: local sink only. A network sink can be attached
            // post-launch; every event still passes the privacy wrapper.
        }

        // Engines validate their bundled resources on startup (PRD §49);
        // failures surface as a disabled pipeline rather than a crash.
        scoringEngine = try? ScoringEngineImpl()
        composer = try? ContentComposerImpl()
        renderer = try? ToneRendererImpl()
        safetyFilter = try? SafetyFilterImpl()
        guidanceBuilder = try? GuidanceBuilder()

        profile = (try? store.load(UserProfile.self, collection: Self.profileCollection, key: Self.profileKey))
            .flatMap { $0 } ?? UserProfile()
        latestResult = resultRepository.listHistory(limit: 1).first
        history = resultRepository.listHistory(limit: 24)
        _ = KeychainInstallId.currentId()

        // Apply iOS file protection to everything under the data root.
        try? FileManager.default.setAttributes(
            [.protectionKey: FileProtectionType.complete],
            ofItemAtPath: root.path
        )
    }

    private static func dataRootURL() -> URL {
        let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
            ?? FileManager.default.temporaryDirectory
        return base.appendingPathComponent("PalmAstroData", isDirectory: true)
    }

    /// Directory for 24h-retained raw scan media (PRD §15).
    static func rawMediaURL() -> URL {
        FileManager.default.temporaryDirectory.appendingPathComponent("scan-media", isDirectory: true)
    }

    // MARK: - Profile

    func saveProfile() {
        try? store.save(profile, collection: Self.profileCollection, key: Self.profileKey)
    }

    /// Effective content language: explicit choice, else the system language
    /// narrowed to the launch set (en / zh-TW).
    var contentLanguage: String {
        if profile.language != "system" { return profile.language }
        let preferred = Locale.preferredLanguages.first ?? "en"
        if preferred.hasPrefix("zh-Hant") || preferred.hasPrefix("zh-TW") || preferred.hasPrefix("zh-HK") {
            return "zh-TW"
        }
        if preferred.hasPrefix("zh") { return "zh-TW" }
        return "en"
    }

    // MARK: - Result pipeline

    /// Runs the full on-device pipeline for a completed scan session and
    /// persists the monthly result.
    func processScanSession(_ session: ScanSessionSummary) {
        guard let scoringEngine, let composer, let safetyFilter,
              let birthday = profile.birthday else { return }

        analytics.emit(eventName: "inference_start", props: [:])

        let palmResult = extractor.extract(bestFrames: session.angleResults, hand: session.hand)
        let astroResult = astroEngine.compute(
            birthday: birthday,
            birthTime: profile.birthTime,
            birthPlaceLat: profile.birthPlaceLat,
            birthPlaceLon: profile.birthPlaceLon
        )
        let scoring = scoringEngine.score(input: ScoringInput(
            palmFeatures: palmResult,
            astroResult: astroResult,
            userContext: UserContext(dominantHand: profile.dominantHand, oneHandOnly: false),
            rulesetVersion: "2.0.0"
        ))

        let monthKey = Self.monthKey(for: Date())
        var deltaResult: DeltaResult?
        if let previous = history.first(where: { $0.monthKey != monthKey }) {
            let provisional = MonthlyResult(
                resultId: UUID().uuidString, monthKey: monthKey, scanSessionId: session.sessionId,
                scoringResult: scoring, semanticPayloads: [:],
                scanQualityScore: session.overallQualityScore,
                featureCoverage: palmResult.featureCoverage,
                createdAt: Int64(Date().timeIntervalSince1970 * 1000)
            )
            deltaResult = deltaEngine.computeDelta(prev: previous, current: provisional)
        }

        let language = contentLanguage
        var payloads = composer.compose(input: ContentInput(
            scoringResult: scoring,
            deltaResult: deltaResult,
            tone: profile.tone,
            entitlements: purchaseService.ownedProductIds,
            calcLevel: astroResult.calcLevel,
            monthKey: monthKey,
            language: language
        ))

        // Safety pipeline: validate every payload; replace violations with
        // the engine-provided safe fallback payload.
        for (domain, payload) in payloads {
            let check = safetyFilter.validate(payload: payload)
            if !check.passed {
                payloads[domain] = safetyFilter.safeFallbackPayload(
                    domain: domain,
                    monthKey: monthKey,
                    calcLevel: astroResult.calcLevel,
                    language: language,
                    scoreCard: payload.scoreCard
                )
            }
        }

        let result = MonthlyResult(
            resultId: UUID().uuidString,
            monthKey: monthKey,
            scanSessionId: session.sessionId,
            scoringResult: scoring,
            semanticPayloads: payloads,
            scanQualityScore: session.overallQualityScore,
            featureCoverage: palmResult.featureCoverage,
            createdAt: Int64(Date().timeIntervalSince1970 * 1000)
        )
        try? resultRepository.saveMonthlyResult(result)
        latestResult = result
        history = resultRepository.listHistory(limit: 24)
        analytics.emit(eventName: "inference_success", props: ["calc_level": astroResult.calcLevel.rawValue.lowercased()])
    }

    /// Rendered, tone-adjusted, safety-filtered report text for one domain.
    func renderedReport(for payload: SemanticPayload) -> RenderedReport? {
        guard let renderer, let safetyFilter else { return nil }
        let rendered = renderer.render(payload: payload, tone: profile.tone)
        return safetyFilter.filter(rendered: rendered, language: payload.language)
    }

    /// "Understand your reading" guidance (lean into / be mindful of / gentle
    /// plan) derived deterministically from a stored result. Template copy
    /// only — nothing generated, nothing recomposed.
    func guidance(for result: MonthlyResult) -> Guidance? {
        guard let guidanceBuilder else { return nil }
        let language = result.semanticPayloads[Domains.career]?.language
            ?? result.semanticPayloads.values.first?.language
            ?? contentLanguage
        return guidanceBuilder.build(
            payloads: result.semanticPayloads,
            overallGrade: result.scoringResult.grade,
            language: language
        )
    }

    static func monthKey(for date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM"
        return formatter.string(from: date)
    }

    // MARK: - Delete all data (PRD §28)

    /// Wipes results, journal, profile, entitlement mirror, raw scan media,
    /// preference flags, and rotates the install id.
    func deleteAllData() {
        analytics.emit(eventName: "delete_all_data_confirm", props: [:])
        try? store.deleteAll()
        try? FileManager.default.removeItem(at: Self.rawMediaURL())
        if let bundleId = Bundle.main.bundleIdentifier {
            UserDefaults.standard.removePersistentDomain(forName: bundleId)
        }
        KeychainInstallId.rotate()
        ReminderScheduler.disable()
        profile = UserProfile()
        latestResult = nil
        history = []
    }
}
