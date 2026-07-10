// Mirrors contracts/src/main/kotlin/com/palmastro/contracts/interfaces/*.kt.
// Engine interface names keep the Kotlin names where possible; interfaces whose
// Kotlin name collides with a Swift module name (AstroEngine, ScoringEngine)
// carry a `Protocol` suffix to avoid module/type ambiguity.

// MARK: ScanInterfaces.kt

public protocol QualityGate {
    func scoreFrame(blur: Float, glare: Float, exposure: Float, coverage: Float, stability: Float) -> QualityScores
    func selectBestFrame(frames: [QualityScores]) -> Int
    func evaluateAngle(angle: Angle, bestScore: QualityScores) -> AngleGateResult
}

// MARK: EngineInterfaces.kt

public protocol PalmFeatureExtractor {
    func extract(bestFrames: [Angle: BestFrameResult], hand: Hand) -> PalmFeatureResult
}

public protocol AstroEngineProtocol {
    func compute(
        birthday: CivilDate,
        birthTime: CivilTime?,
        birthPlaceLat: Double?,
        birthPlaceLon: Double?
    ) -> AstroResult
}

public protocol ScoringEngineProtocol {
    func score(input: ScoringInput) -> ScoringResult
}

public protocol DeltaEngine {
    func computeDelta(prev: MonthlyResult, current: MonthlyResult) -> DeltaResult
}

public protocol ContentComposer {
    func compose(input: ContentInput) -> [String: SemanticPayload]
}

public protocol Renderer {
    func render(payload: SemanticPayload, tone: Tone) -> RenderedReport
}

public struct SafetyCheckResult: Codable, Equatable, Sendable {
    public let passed: Bool
    public let violations: [String]

    public init(passed: Bool, violations: [String]) {
        self.passed = passed
        self.violations = violations
    }
}

public protocol SafetyFilter {
    func validate(payload: SemanticPayload) -> SafetyCheckResult
    func filter(rendered: RenderedReport) -> RenderedReport
}

// MARK: AnalyticsInterfaces.kt

public protocol AnalyticsEmitter {
    func emit(eventName: String, props: [String: Any])
}

// MARK: StorageInterfaces.kt

public protocol ResultRepository {
    func saveMonthlyResult(_ result: MonthlyResult) throws
    func getMonthlyResult(monthKey: String) -> MonthlyResult?
    func listHistory(limit: Int) -> [MonthlyResult]
}

public protocol EntitlementService {
    func hasEntitlement(productId: String) -> Bool
}
