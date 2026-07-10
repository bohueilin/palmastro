// Mirrors contracts/src/main/kotlin/com/palmastro/contracts/Models.kt exactly.
// JSON field names are identical to the Kotlin property names. Fields that
// carry Kotlin default values decode leniently (missing key -> default) to
// match kotlinx.serialization behaviour.

/// Canonical domain taxonomy (PRD §46). Single source for the four domains.
public enum Domains {
    public static let career = "career"
    public static let wealth = "wealth"
    public static let family = "family"
    public static let health = "health"
    public static let all = [career, wealth, family, health]
}

/// Store product IDs shared across Google Play and App Store (PRD §22, §46).
public enum ProductIds {
    public static let careerPack = "palmastro.pack.career"
    public static let wealthPack = "palmastro.pack.wealth"
    public static let bundle = "palmastro.pack.bundle"
    public static let all = [careerPack, wealthPack, bundle]
}

public struct QualityScores: Codable, Equatable, Sendable {
    public let blur: Float
    public let glare: Float
    public let exposure: Float
    public let coverage: Float
    public let stability: Float
    public let composite: Int

    public init(blur: Float, glare: Float, exposure: Float, coverage: Float, stability: Float, composite: Int) {
        self.blur = blur
        self.glare = glare
        self.exposure = exposure
        self.coverage = coverage
        self.stability = stability
        self.composite = composite
    }
}

/// Normalized hand landmark point (MediaPipe/Vision coordinate space, 0..1).
public struct LandmarkPoint: Codable, Equatable, Sendable {
    public let x: Float
    public let y: Float
    public let z: Float

    public init(x: Float, y: Float, z: Float) {
        self.x = x
        self.y = y
        self.z = z
    }
}

/// Intensity statistics sampled along one canonical palm-line region.
/// Computed on-device from the captured frame; carries no raw imagery.
public struct LineRegionMetrics: Codable, Equatable, Sendable {
    public let region: String          // headline | heartline | lifeline | fateline
    public let contrast: Float         // 0..1 normalized dark-line contrast vs surrounding skin
    public let continuity: Float       // 0..1 fraction of sample path where the line is present
    public let meanIntensity: Float    // 0..1 mean luminance along the path

    public init(region: String, contrast: Float, continuity: Float, meanIntensity: Float) {
        self.region = region
        self.contrast = contrast
        self.continuity = continuity
        self.meanIntensity = meanIntensity
    }
}

/// Per-frame palm geometry + line-region measurements used for feature extraction.
public struct PalmMetrics: Codable, Equatable, Sendable {
    public let landmarks: [LandmarkPoint]
    public let lineRegions: [LineRegionMetrics]

    public init(landmarks: [LandmarkPoint], lineRegions: [LineRegionMetrics]) {
        self.landmarks = landmarks
        self.lineRegions = lineRegions
    }
}

public struct BestFrameResult: Codable, Equatable, Sendable {
    public let angle: Angle
    public let frameIndex: Int
    public let qualityScores: QualityScores
    public let fileRef: String?
    public let palmMetrics: PalmMetrics?

    public init(angle: Angle, frameIndex: Int, qualityScores: QualityScores, fileRef: String?, palmMetrics: PalmMetrics? = nil) {
        self.angle = angle
        self.frameIndex = frameIndex
        self.qualityScores = qualityScores
        self.fileRef = fileRef
        self.palmMetrics = palmMetrics
    }
}

public struct AngleGateResult: Codable, Equatable, Sendable {
    public let angle: Angle
    public let passed: Bool
    public let failReason: String?

    public init(angle: Angle, passed: Bool, failReason: String?) {
        self.angle = angle
        self.passed = passed
        self.failReason = failReason
    }
}

public struct ScanSessionSummary: Codable, Equatable, Sendable {
    public let sessionId: String
    public let hand: Hand
    public let angleResults: [Angle: BestFrameResult]
    public let overallQualityScore: Int
    public let featureCoverage: Float
    public let totalDurationMs: Int64
    public let totalAttempts: Int

    public init(
        sessionId: String,
        hand: Hand,
        angleResults: [Angle: BestFrameResult],
        overallQualityScore: Int,
        featureCoverage: Float,
        totalDurationMs: Int64,
        totalAttempts: Int
    ) {
        self.sessionId = sessionId
        self.hand = hand
        self.angleResults = angleResults
        self.overallQualityScore = overallQualityScore
        self.featureCoverage = featureCoverage
        self.totalDurationMs = totalDurationMs
        self.totalAttempts = totalAttempts
    }
}

public struct PalmFeatures: Codable, Equatable, Sendable {
    public let headlinePresent: Bool
    public let heartlinePresent: Bool
    public let lifelinePresent: Bool
    public let fatelinePresent: Bool
    public let headlineShape: String
    public let heartlineShape: String
    public let lifelineShape: String
    public let fatelineShape: String
    public let headlineClarity: String
    public let heartlineClarity: String
    public let lifelineClarity: String
    public let fatelineClarity: String
    public let headlineLength: String
    public let fatelineLength: String
    public let venusMountDensity: String
    public let jupiterMountDensity: String
    public let saturnMountDensity: String
    public let minorLineDensity: String

    public init(
        headlinePresent: Bool,
        heartlinePresent: Bool,
        lifelinePresent: Bool,
        fatelinePresent: Bool,
        headlineShape: String,
        heartlineShape: String,
        lifelineShape: String,
        fatelineShape: String,
        headlineClarity: String,
        heartlineClarity: String,
        lifelineClarity: String,
        fatelineClarity: String,
        headlineLength: String,
        fatelineLength: String,
        venusMountDensity: String,
        jupiterMountDensity: String,
        saturnMountDensity: String,
        minorLineDensity: String
    ) {
        self.headlinePresent = headlinePresent
        self.heartlinePresent = heartlinePresent
        self.lifelinePresent = lifelinePresent
        self.fatelinePresent = fatelinePresent
        self.headlineShape = headlineShape
        self.heartlineShape = heartlineShape
        self.lifelineShape = lifelineShape
        self.fatelineShape = fatelineShape
        self.headlineClarity = headlineClarity
        self.heartlineClarity = heartlineClarity
        self.lifelineClarity = lifelineClarity
        self.fatelineClarity = fatelineClarity
        self.headlineLength = headlineLength
        self.fatelineLength = fatelineLength
        self.venusMountDensity = venusMountDensity
        self.jupiterMountDensity = jupiterMountDensity
        self.saturnMountDensity = saturnMountDensity
        self.minorLineDensity = minorLineDensity
    }
}

public struct PalmFeatureResult: Codable, Equatable, Sendable {
    public let features: PalmFeatures
    public let featureCoverage: Float
    public let confidence: String
    public let extractorVersion: String

    public init(features: PalmFeatures, featureCoverage: Float, confidence: String, extractorVersion: String) {
        self.features = features
        self.featureCoverage = featureCoverage
        self.confidence = confidence
        self.extractorVersion = extractorVersion
    }
}

public struct AstroSignal: Codable, Equatable, Sendable {
    public let signalId: String
    public let direction: String
    public let magnitude: Int
    public let confidence: String
    public let safetyTag: String

    public init(signalId: String, direction: String, magnitude: Int, confidence: String, safetyTag: String) {
        self.signalId = signalId
        self.direction = direction
        self.magnitude = magnitude
        self.confidence = confidence
        self.safetyTag = safetyTag
    }
}

public struct AstroResult: Codable, Equatable, Sendable {
    public let calcLevel: CalcLevel
    public let signals: [AstroSignal]
    public let engineVersion: String

    public init(calcLevel: CalcLevel, signals: [AstroSignal], engineVersion: String) {
        self.calcLevel = calcLevel
        self.signals = signals
        self.engineVersion = engineVersion
    }
}

public struct ExplainEntry: Codable, Equatable, Sendable {
    public let signalId: String
    public let mapping: String
    public let contribution: Double

    public init(signalId: String, mapping: String, contribution: Double) {
        self.signalId = signalId
        self.mapping = mapping
        self.contribution = contribution
    }
}

public struct UserContext: Codable, Equatable, Sendable {
    public let dominantHand: Hand
    public let oneHandOnly: Bool

    public init(dominantHand: Hand, oneHandOnly: Bool) {
        self.dominantHand = dominantHand
        self.oneHandOnly = oneHandOnly
    }
}

public struct ScoringInput: Codable, Equatable, Sendable {
    public let palmFeatures: PalmFeatureResult
    public let astroResult: AstroResult
    public let userContext: UserContext
    public let rulesetVersion: String

    public init(palmFeatures: PalmFeatureResult, astroResult: AstroResult, userContext: UserContext, rulesetVersion: String) {
        self.palmFeatures = palmFeatures
        self.astroResult = astroResult
        self.userContext = userContext
        self.rulesetVersion = rulesetVersion
    }
}

public struct ScoringResult: Codable, Equatable, Sendable {
    public let domainScores: [String: Int]
    public let subdimScores: [String: Int]
    public let grade: String
    public let confidence: String
    public let confidenceReasons: [String]
    public let explainability: [ExplainEntry]
    public let matchedBuckets: [String]
    public let rulesetVersion: String

    public init(
        domainScores: [String: Int],
        subdimScores: [String: Int],
        grade: String,
        confidence: String,
        confidenceReasons: [String],
        explainability: [ExplainEntry],
        matchedBuckets: [String],
        rulesetVersion: String
    ) {
        self.domainScores = domainScores
        self.subdimScores = subdimScores
        self.grade = grade
        self.confidence = confidence
        self.confidenceReasons = confidenceReasons
        self.explainability = explainability
        self.matchedBuckets = matchedBuckets
        self.rulesetVersion = rulesetVersion
    }
}

public struct DeltaValue: Codable, Equatable, Sendable {
    public let value: Int
    public let arrow: String

    public init(value: Int, arrow: String) {
        self.value = value
        self.arrow = arrow
    }
}

public struct GradeShift: Codable, Equatable, Sendable {
    public let from: String
    public let to: String

    public init(from: String, to: String) {
        self.from = from
        self.to = to
    }
}

public struct DeltaResult: Codable, Equatable, Sendable {
    public let domainDeltas: [String: DeltaValue]
    public let subdimDeltas: [String: DeltaValue]
    public let gradeShift: GradeShift?
    public let comparabilityScore: Int
    public let comparabilityBucket: ComparabilityBucket
    public let prevMonthKey: String
    public let currentMonthKey: String

    public init(
        domainDeltas: [String: DeltaValue],
        subdimDeltas: [String: DeltaValue],
        gradeShift: GradeShift?,
        comparabilityScore: Int,
        comparabilityBucket: ComparabilityBucket,
        prevMonthKey: String,
        currentMonthKey: String
    ) {
        self.domainDeltas = domainDeltas
        self.subdimDeltas = subdimDeltas
        self.gradeShift = gradeShift
        self.comparabilityScore = comparabilityScore
        self.comparabilityBucket = comparabilityBucket
        self.prevMonthKey = prevMonthKey
        self.currentMonthKey = currentMonthKey
    }
}

public struct ContentInput: Codable, Equatable, Sendable {
    public let scoringResult: ScoringResult
    public let deltaResult: DeltaResult?
    public let tone: Tone
    public let entitlements: Set<String>
    public let calcLevel: CalcLevel
    public let monthKey: String
    /// Kotlin default: "en". Missing key decodes to "en".
    public let language: String

    public init(
        scoringResult: ScoringResult,
        deltaResult: DeltaResult?,
        tone: Tone,
        entitlements: Set<String>,
        calcLevel: CalcLevel,
        monthKey: String,
        language: String = "en"
    ) {
        self.scoringResult = scoringResult
        self.deltaResult = deltaResult
        self.tone = tone
        self.entitlements = entitlements
        self.calcLevel = calcLevel
        self.monthKey = monthKey
        self.language = language
    }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        scoringResult = try c.decode(ScoringResult.self, forKey: .scoringResult)
        deltaResult = try c.decodeIfPresent(DeltaResult.self, forKey: .deltaResult)
        tone = try c.decode(Tone.self, forKey: .tone)
        entitlements = try c.decodeIfPresent(Set<String>.self, forKey: .entitlements) ?? []
        calcLevel = try c.decode(CalcLevel.self, forKey: .calcLevel)
        monthKey = try c.decode(String.self, forKey: .monthKey)
        language = try c.decodeIfPresent(String.self, forKey: .language) ?? "en"
    }
}

/// PRD Appendix B: interpretation = { pattern, trigger, cost }.
public struct Interpretation: Codable, Equatable, Sendable {
    public let pattern: String
    /// Kotlin default: "". Missing key decodes to "".
    public let trigger: String
    /// Kotlin default: "". Missing key decodes to "".
    public let cost: String

    public init(pattern: String, trigger: String = "", cost: String = "") {
        self.pattern = pattern
        self.trigger = trigger
        self.cost = cost
    }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        pattern = try c.decode(String.self, forKey: .pattern)
        trigger = try c.decodeIfPresent(String.self, forKey: .trigger) ?? ""
        cost = try c.decodeIfPresent(String.self, forKey: .cost) ?? ""
    }
}

public struct Observation: Codable, Equatable, Sendable {
    public let signalId: String
    public let displayName: String
    public let evidenceSummary: String

    public init(signalId: String, displayName: String, evidenceSummary: String) {
        self.signalId = signalId
        self.displayName = displayName
        self.evidenceSummary = evidenceSummary
    }
}

public struct ScoreCard: Codable, Equatable, Sendable {
    public let totalScore: Int
    public let grade: String
    public let delta: DeltaValue?
    public let comparabilityScore: Int?
    public let subdims: [String: Int]

    public init(totalScore: Int, grade: String, delta: DeltaValue?, comparabilityScore: Int?, subdims: [String: Int]) {
        self.totalScore = totalScore
        self.grade = grade
        self.delta = delta
        self.comparabilityScore = comparabilityScore
        self.subdims = subdims
    }
}

public struct SemanticPayload: Codable, Equatable, Sendable {
    public let domain: String
    public let monthKey: String
    public let calcLevel: CalcLevel
    public let confidence: String
    /// Kotlin default: emptyList(). Missing key decodes to [].
    public let confidenceReasons: [String]
    /// Kotlin default: "en". Missing key decodes to "en".
    public let language: String
    public let observations: [Observation]
    public let interpretation: Interpretation
    public let blindspot: String
    public let actionToday: String
    public let actionWeek: String
    public let prompt: String
    public let safetyNotes: [String]
    public let explainability: [ExplainEntry]
    public let scoreCard: ScoreCard

    public init(
        domain: String,
        monthKey: String,
        calcLevel: CalcLevel,
        confidence: String,
        confidenceReasons: [String] = [],
        language: String = "en",
        observations: [Observation],
        interpretation: Interpretation,
        blindspot: String,
        actionToday: String,
        actionWeek: String,
        prompt: String,
        safetyNotes: [String],
        explainability: [ExplainEntry],
        scoreCard: ScoreCard
    ) {
        self.domain = domain
        self.monthKey = monthKey
        self.calcLevel = calcLevel
        self.confidence = confidence
        self.confidenceReasons = confidenceReasons
        self.language = language
        self.observations = observations
        self.interpretation = interpretation
        self.blindspot = blindspot
        self.actionToday = actionToday
        self.actionWeek = actionWeek
        self.prompt = prompt
        self.safetyNotes = safetyNotes
        self.explainability = explainability
        self.scoreCard = scoreCard
    }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        domain = try c.decode(String.self, forKey: .domain)
        monthKey = try c.decode(String.self, forKey: .monthKey)
        calcLevel = try c.decode(CalcLevel.self, forKey: .calcLevel)
        confidence = try c.decode(String.self, forKey: .confidence)
        confidenceReasons = try c.decodeIfPresent([String].self, forKey: .confidenceReasons) ?? []
        language = try c.decodeIfPresent(String.self, forKey: .language) ?? "en"
        observations = try c.decode([Observation].self, forKey: .observations)
        interpretation = try c.decode(Interpretation.self, forKey: .interpretation)
        blindspot = try c.decode(String.self, forKey: .blindspot)
        actionToday = try c.decode(String.self, forKey: .actionToday)
        actionWeek = try c.decode(String.self, forKey: .actionWeek)
        prompt = try c.decode(String.self, forKey: .prompt)
        safetyNotes = try c.decode([String].self, forKey: .safetyNotes)
        explainability = try c.decode([ExplainEntry].self, forKey: .explainability)
        scoreCard = try c.decode(ScoreCard.self, forKey: .scoreCard)
    }
}

public struct RenderedReport: Codable, Equatable, Sendable {
    public let domain: String
    public let tone: Tone
    public let text: String

    public init(domain: String, tone: Tone, text: String) {
        self.domain = domain
        self.tone = tone
        self.text = text
    }

    public func copy(text: String) -> RenderedReport {
        RenderedReport(domain: domain, tone: tone, text: text)
    }
}

public struct MonthlyResult: Codable, Equatable, Sendable {
    public let resultId: String
    public let monthKey: String
    public let scanSessionId: String
    public let scoringResult: ScoringResult
    public let semanticPayloads: [String: SemanticPayload]
    public let scanQualityScore: Int
    public let featureCoverage: Float
    public let createdAt: Int64

    public init(
        resultId: String,
        monthKey: String,
        scanSessionId: String,
        scoringResult: ScoringResult,
        semanticPayloads: [String: SemanticPayload],
        scanQualityScore: Int,
        featureCoverage: Float,
        createdAt: Int64
    ) {
        self.resultId = resultId
        self.monthKey = monthKey
        self.scanSessionId = scanSessionId
        self.scoringResult = scoringResult
        self.semanticPayloads = semanticPayloads
        self.scanQualityScore = scanQualityScore
        self.featureCoverage = featureCoverage
        self.createdAt = createdAt
    }
}

// MARK: - Calendar value types (mirror java.time.LocalDate / LocalTime inputs)

/// Calendar date without timezone (mirrors java.time.LocalDate in the Kotlin
/// AstroEngine interface).
public struct CivilDate: Codable, Equatable, Hashable, Sendable {
    public let year: Int
    public let month: Int   // 1..12
    public let day: Int     // 1..31

    public init(year: Int, month: Int, day: Int) {
        self.year = year
        self.month = month
        self.day = day
    }
}

/// Wall-clock time without timezone (mirrors java.time.LocalTime).
public struct CivilTime: Codable, Equatable, Hashable, Sendable {
    public let hour: Int    // 0..23
    public let minute: Int  // 0..59

    public init(hour: Int, minute: Int) {
        self.hour = hour
        self.minute = minute
    }
}
