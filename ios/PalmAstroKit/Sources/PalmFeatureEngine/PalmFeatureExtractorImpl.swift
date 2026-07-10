import Foundation
import CoreContracts

// Mirrors engine-palm-features/src/main/kotlin/com/palmastro/palm/PalmFeatureExtractorImpl.kt
// exactly — thresholds, aggregation, and vocabulary must not drift so both
// platforms emit identical PalmFeatures for identical PalmMetrics.

/// Palm feature extractor v2 (PRD section 16).
///
/// Derives categorical, explainable, non-biometric features from real
/// per-frame measurements (`PalmMetrics`): 21 normalized hand landmarks plus
/// per-line-region intensity statistics (contrast / continuity / meanIntensity,
/// all 0..1) for headline, heartline, lifeline and fateline.
///
/// Aggregation: metrics are median-aggregated across all captured angles that
/// carry palmMetrics (landmarks per coordinate, region stats per field). The
/// across-angle contrast spread (max - min) is kept as a brokenness proxy: a
/// line whose measured contrast varies strongly between angles reads as
/// chained/interrupted rather than uniformly faint.
///
/// When no frame carries palmMetrics the extractor falls back to conservative
/// neutral features with confidence "low" (PRD 13.2: a low quality scan may
/// still produce a conservative result).
///
/// Vocabulary (aligned with SignalResolver in ScoringEngine):
/// - clarity: "clear" | "medium" | "faint" | "broken" | "thin" (heartline
///   only) | "unclear" (absent lines)
/// - shape:   "curved" | "straight" | "unclear"
/// - length:  "long" | "medium" | "short"
/// - density: "high" | "med" | "low"
///
/// Output values are categorical strings/booleans only - never raw
/// measurements - and are deterministic for identical inputs.
public final class PalmFeatureExtractorImpl: PalmFeatureExtractor {

    private let version: String

    public init(version: String = "2.0.0") {
        self.version = version
    }

    public func extract(bestFrames: [Angle: BestFrameResult], hand: Hand) -> PalmFeatureResult {
        let avgQuality: Int
        if bestFrames.isEmpty {
            avgQuality = 0
        } else {
            let total = bestFrames.values.reduce(0.0) { $0 + Double($1.qualityScores.composite) }
            avgQuality = Int(total / Double(bestFrames.count))
        }

        // Deterministic frame order regardless of map implementation.
        let frameMetrics = Angle.allCases
            .compactMap { bestFrames[$0]?.palmMetrics }
            .filter { $0.landmarks.count == Self.landmarkCount }

        if frameMetrics.isEmpty {
            return PalmFeatureResult(
                features: Self.conservativeFallbackFeatures(),
                featureCoverage: Self.fallbackCoverage,
                confidence: "low",
                extractorVersion: version
            )
        }

        let landmarks = medianLandmarks(frames: frameMetrics)
        let geometry = PalmGeometry.from(landmarks)
        var regions: [String: AggregatedRegion] = [:]
        for region in Self.regions {
            regions[region] = aggregateRegion(frames: frameMetrics, region: region)
        }

        let headline = lineFeatures(
            region: regions[Self.regionHeadline],
            curved: geometry.headlineCurvature > Self.mcpArcCurvedThreshold,
            pathRatio: geometry.headlinePathRatio, isHeartline: false
        )
        let heartline = lineFeatures(
            region: regions[Self.regionHeartline],
            curved: geometry.heartlineCurvature > Self.mcpArcCurvedThreshold,
            pathRatio: geometry.heartlinePathRatio, isHeartline: true
        )
        let lifeline = lineFeatures(
            region: regions[Self.regionLifeline],
            curved: geometry.lifelineSweep > Self.lifelineSweepCurvedThreshold,
            pathRatio: geometry.lifelinePathRatio, isHeartline: false
        )
        let fateline = lineFeatures(
            region: regions[Self.regionFateline],
            curved: geometry.fatelineSlant >= Self.fatelineSlantThreshold,
            pathRatio: geometry.fatelinePathRatio, isHeartline: false
        )

        let features = PalmFeatures(
            headlinePresent: headline.present,
            heartlinePresent: heartline.present,
            lifelinePresent: lifeline.present,
            fatelinePresent: fateline.present,
            headlineShape: headline.shape,
            heartlineShape: heartline.shape,
            lifelineShape: lifeline.shape,
            fatelineShape: fateline.shape,
            headlineClarity: headline.clarity,
            heartlineClarity: heartline.clarity,
            lifelineClarity: lifeline.clarity,
            fatelineClarity: fateline.clarity,
            headlineLength: headline.length,
            fatelineLength: fateline.length,
            venusMountDensity: mountDensity(regions[Self.regionLifeline]),
            jupiterMountDensity: mountDensity(regions[Self.regionHeartline]),
            saturnMountDensity: mountDensity(regions[Self.regionFateline]),
            minorLineDensity: minorLineDensity(Self.regions.compactMap { regions[$0] })
        )

        let featureCoverage = computeFeatureCoverage(features: features, regions: regions)
        let confidence = deriveConfidence(scanQuality: avgQuality, featureCoverage: featureCoverage)

        return PalmFeatureResult(
            features: features,
            featureCoverage: featureCoverage,
            confidence: confidence,
            extractorVersion: version
        )
    }

    // MARK: - Aggregation

    struct AggregatedRegion {
        let contrast: Float
        let continuity: Float
        let meanIntensity: Float
        let contrastSpread: Float
    }

    private func aggregateRegion(frames: [PalmMetrics], region: String) -> AggregatedRegion? {
        let samples = frames.flatMap { frame in frame.lineRegions.filter { $0.region == region } }
        if samples.isEmpty { return nil }
        let contrasts = samples.map(\.contrast)
        return AggregatedRegion(
            contrast: median(contrasts),
            continuity: median(samples.map(\.continuity)),
            meanIntensity: median(samples.map(\.meanIntensity)),
            contrastSpread: contrasts.max()! - contrasts.min()!
        )
    }

    private func medianLandmarks(frames: [PalmMetrics]) -> [LandmarkPoint] {
        (0..<Self.landmarkCount).map { i in
            LandmarkPoint(
                x: median(frames.map { $0.landmarks[i].x }),
                y: median(frames.map { $0.landmarks[i].y }),
                z: median(frames.map { $0.landmarks[i].z })
            )
        }
    }

    func median(_ values: [Float]) -> Float {
        let sorted = values.sorted()
        let mid = sorted.count / 2
        return sorted.count % 2 == 1 ? sorted[mid] : (sorted[mid - 1] + sorted[mid]) / 2
    }

    // MARK: - Geometry (curvature / length proxies from the 21 landmarks)

    /// Curvature and path-length proxies derived from MediaPipe landmark
    /// geometry, normalized by palm width (distance index-MCP..pinky-MCP).
    struct PalmGeometry {
        let headlineCurvature: Float
        let heartlineCurvature: Float
        let lifelineSweep: Float
        let fatelineSlant: Float
        let headlinePathRatio: Float
        let heartlinePathRatio: Float
        let lifelinePathRatio: Float
        let fatelinePathRatio: Float

        static func from(_ l: [LandmarkPoint]) -> PalmGeometry {
            let palmWidth = max(distance(l[5], l[17]), minPalmWidth)
            // Curvature proxies: perpendicular deviation of the ring/middle
            // MCPs from the index-MCP..pinky-MCP chord (the "MCP arc").
            let headCurve = perpendicularDistance(l[13], l[5], l[17]) / palmWidth
            let heartCurve = perpendicularDistance(l[9], l[5], l[17]) / palmWidth
            // Lifeline sweep: how far the thumb MCP swings from the
            // wrist..index-MCP midline (wide thenar arc = curved lifeline).
            let lifeSweep = distance(l[2], midpoint(l[0], l[5])) / palmWidth
            // Fateline slant: deviation of the middle MCP from the
            // wrist..palm-center axis (aligned = straight fate line).
            let center = centroid([l[0], l[5], l[9], l[13], l[17]])
            let fateSlant = perpendicularDistance(l[9], l[0], center) / palmWidth
            // Path-length ratios of the canonical sampling paths (see
            // integration spec) relative to palm width.
            let headlineStart = midpoint(l[5], l[9])
            let headlineEnd = towards(l[17], l[0], 0.12)
            let heartlineStart = towards(l[17], l[0], 0.12)
            let heartlineEnd = towards(l[5], l[0], 0.12)
            let lifelineStart = midpoint(l[1], l[2])
            return PalmGeometry(
                headlineCurvature: headCurve,
                heartlineCurvature: heartCurve,
                lifelineSweep: lifeSweep,
                fatelineSlant: fateSlant,
                headlinePathRatio: distance(headlineStart, headlineEnd) / palmWidth,
                heartlinePathRatio: distance(heartlineStart, heartlineEnd) / palmWidth,
                lifelinePathRatio: distance(lifelineStart, l[0]) / palmWidth,
                fatelinePathRatio: distance(l[0], l[9]) / palmWidth
            )
        }

        private static func distance(_ a: LandmarkPoint, _ b: LandmarkPoint) -> Float {
            let dx = a.x - b.x
            let dy = a.y - b.y
            return (dx * dx + dy * dy).squareRoot()
        }

        private static func midpoint(_ a: LandmarkPoint, _ b: LandmarkPoint) -> LandmarkPoint {
            LandmarkPoint(x: (a.x + b.x) / 2, y: (a.y + b.y) / 2, z: (a.z + b.z) / 2)
        }

        private static func towards(_ from: LandmarkPoint, _ to: LandmarkPoint, _ fraction: Float) -> LandmarkPoint {
            LandmarkPoint(
                x: from.x + (to.x - from.x) * fraction,
                y: from.y + (to.y - from.y) * fraction,
                z: from.z + (to.z - from.z) * fraction
            )
        }

        private static func centroid(_ points: [LandmarkPoint]) -> LandmarkPoint {
            LandmarkPoint(
                x: points.map(\.x).reduce(0, +) / Float(points.count),
                y: points.map(\.y).reduce(0, +) / Float(points.count),
                z: points.map(\.z).reduce(0, +) / Float(points.count)
            )
        }

        /// Perpendicular distance of `p` from the line through `a` and `b`.
        private static func perpendicularDistance(_ p: LandmarkPoint, _ a: LandmarkPoint, _ b: LandmarkPoint) -> Float {
            let cx = b.x - a.x
            let cy = b.y - a.y
            let chordLength = max((cx * cx + cy * cy).squareRoot(), minPalmWidth)
            let wx = p.x - a.x
            let wy = p.y - a.y
            return abs(cx * wy - cy * wx) / chordLength
        }
    }

    // MARK: - Per-line categorical buckets

    struct LineFeatures {
        let present: Bool
        let clarity: String
        let shape: String
        let length: String
    }

    func lineFeatures(region: AggregatedRegion?, curved: Bool, pathRatio: Float, isHeartline: Bool) -> LineFeatures {
        guard let region, region.continuity > Self.presenceContinuity else {
            return LineFeatures(present: false, clarity: "unclear", shape: "unclear", length: "short")
        }
        return LineFeatures(
            present: true,
            clarity: clarityBucket(region: region, isHeartline: isHeartline),
            shape: curved ? "curved" : "straight",
            length: lengthBucket(continuity: region.continuity, pathRatio: pathRatio)
        )
    }

    /// Clarity vocabulary (aligned with SignalResolver in ScoringEngine):
    /// "clear" | "medium" | "faint" | "broken" | "thin" (heartline only) |
    /// "unclear" (absent lines only).
    ///
    /// Broken family: mid continuity with either strong contrast (a dark line
    /// that keeps disappearing) or high across-angle contrast spread.
    func clarityBucket(region: AggregatedRegion, isHeartline: Bool) -> String {
        if region.continuity <= Self.brokenContinuity {
            if region.contrast >= Self.brokenContrast || region.contrastSpread > Self.brokenSpread {
                return "broken"
            }
            return "faint"
        }
        if isHeartline && region.contrast < Self.thinContrast { return "thin" }
        if region.contrast >= Self.clearContrast && region.continuity > Self.clearContinuity { return "clear" }
        if region.contrast >= Self.mediumContrast { return "medium" }
        return "faint"
    }

    /// Effective length = continuity x (sampling path length / palm width).
    func lengthBucket(continuity: Float, pathRatio: Float) -> String {
        let normalizedLength = continuity * pathRatio
        if normalizedLength >= Self.lengthLong { return "long" }
        if normalizedLength >= Self.lengthMedium { return "medium" }
        return "short"
    }

    // MARK: - Mounts + minor lines

    /// Mount texture density from the intensity statistics of the adjacent
    /// line region: darker skin (low mean intensity) with strong local
    /// contrast reads as densely textured.
    func mountDensity(_ region: AggregatedRegion?) -> String {
        guard let region else { return "low" }
        let texture = (1 - region.meanIntensity) * 0.6 + region.contrast * 0.4
        if texture >= Self.mountHigh { return "high" }
        if texture >= Self.mountMed { return "med" }
        return "low"
    }

    /// Minor line density from residual contrast: contrast that is not
    /// explained by continuous major lines (high contrast + low continuity
    /// along the canonical paths implies many small crossing lines).
    func minorLineDensity(_ regions: [AggregatedRegion]) -> String {
        if regions.isEmpty { return "med" }
        let avgContrast = Float(regions.map { Double($0.contrast) }.reduce(0, +) / Double(regions.count))
        let avgContinuity = Float(regions.map { Double($0.continuity) }.reduce(0, +) / Double(regions.count))
        let residual = avgContrast * (1.25 - avgContinuity)
        if residual >= Self.minorHigh { return "high" }
        if residual >= Self.minorMed { return "med" }
        return "low"
    }

    // MARK: - Coverage / confidence / fallback

    private func computeFeatureCoverage(features f: PalmFeatures, regions: [String: AggregatedRegion]) -> Float {
        var present = 0
        if f.headlinePresent { present += 1 }
        if f.heartlinePresent { present += 1 }
        if f.lifelinePresent { present += 1 }
        if f.fatelinePresent { present += 1 }
        for shape in [f.headlineShape, f.heartlineShape, f.lifelineShape, f.fatelineShape] where shape != "unclear" {
            present += 1
        }
        for clarity in [f.headlineClarity, f.heartlineClarity, f.lifelineClarity, f.fatelineClarity] where clarity != "unclear" {
            present += 1
        }
        if f.headlinePresent { present += 1 } // headlineLength informative
        if f.fatelinePresent { present += 1 } // fatelineLength informative
        if regions[Self.regionLifeline] != nil { present += 1 }  // venus
        if regions[Self.regionHeartline] != nil { present += 1 } // jupiter
        if regions[Self.regionFateline] != nil { present += 1 }  // saturn
        if !regions.isEmpty { present += 1 }                     // minor lines
        return min(max(Float(present) / Self.totalFeatures, 0), 1)
    }

    private func deriveConfidence(scanQuality: Int, featureCoverage: Float) -> String {
        if scanQuality < 40 || featureCoverage < 0.5 { return "low" }
        if scanQuality < 70 || featureCoverage < 0.8 { return "med" }
        return "high"
    }

    /// Neutral conservative features: nothing here matches a negative signal
    /// and the mandatory "low" confidence keeps the resolver's positive
    /// matches out of scoring (palm signals require at least "med").
    static func conservativeFallbackFeatures() -> PalmFeatures {
        PalmFeatures(
            headlinePresent: true,
            heartlinePresent: true,
            lifelinePresent: true,
            fatelinePresent: false,
            headlineShape: "unclear",
            heartlineShape: "unclear",
            lifelineShape: "unclear",
            fatelineShape: "unclear",
            headlineClarity: "medium",
            heartlineClarity: "medium",
            lifelineClarity: "medium",
            fatelineClarity: "unclear",
            headlineLength: "medium",
            fatelineLength: "short",
            venusMountDensity: "med",
            jupiterMountDensity: "med",
            saturnMountDensity: "med",
            minorLineDensity: "med"
        )
    }

    // MARK: - Constants (Kotlin companion-object parity)

    private static let landmarkCount = 21
    private static let totalFeatures: Float = 18
    private static let fallbackCoverage: Float = 0.4
    static let minPalmWidth: Float = 1e-4

    public static let regionHeadline = "headline"
    public static let regionHeartline = "heartline"
    public static let regionLifeline = "lifeline"
    public static let regionFateline = "fateline"
    static let regions = [regionHeadline, regionHeartline, regionLifeline, regionFateline]

    // Presence / clarity thresholds (0..1 region metrics).
    static let presenceContinuity: Float = 0.35
    static let brokenContinuity: Float = 0.6
    static let brokenContrast: Float = 0.5
    static let brokenSpread: Float = 0.15
    static let thinContrast: Float = 0.35
    static let clearContrast: Float = 0.55
    static let clearContinuity: Float = 0.75
    static let mediumContrast: Float = 0.35

    // Geometry thresholds (normalized by palm width).
    static let mcpArcCurvedThreshold: Float = 0.055
    static let lifelineSweepCurvedThreshold: Float = 0.30
    static let fatelineSlantThreshold: Float = 0.18

    // Length buckets (continuity x path/palm-width ratio).
    static let lengthLong: Float = 0.70
    static let lengthMedium: Float = 0.45

    // Mount texture buckets.
    static let mountHigh: Float = 0.55
    static let mountMed: Float = 0.35

    // Minor line residual-contrast buckets.
    static let minorHigh: Float = 0.40
    static let minorMed: Float = 0.22
}
