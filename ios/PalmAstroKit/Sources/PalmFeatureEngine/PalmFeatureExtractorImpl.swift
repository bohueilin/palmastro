import CoreContracts

/// Palm feature extractor (v2 semantics per EXECUTION_SPEC).
///
/// Primary path: derives categorical, explainable features from the measured
/// `PalmMetrics` (21 normalized landmarks + 4 `LineRegionMetrics`) attached to
/// best frames by the on-device scan layer. No raw imagery is consumed.
///
/// Fallback path: when no frame carries `PalmMetrics`, produces conservative
/// quality-derived features and forces confidence to "low" (EXECUTION_SPEC:
/// "Extractor falls back to conservative low-confidence quality-derived
/// features when null").
///
/// Vocabulary (stable, cross-platform):
/// - clarity: clear | moderate | faint
/// - shape:   smooth | chained | unknown
/// - length:  long | medium | short | unknown
/// - density: dense | med | low | unknown
/// Mount texture and minor-line density are NOT measurable from the four line
/// regions, so the metrics path reports "unknown" for them rather than
/// fabricating values.
public final class PalmFeatureExtractorImpl: PalmFeatureExtractor {

    public static let regionHeadline = "headline"
    public static let regionHeartline = "heartline"
    public static let regionLifeline = "lifeline"
    public static let regionFateline = "fateline"

    private let version: String

    public init(version: String = "2.0.0") {
        self.version = version
    }

    public func extract(bestFrames: [Angle: BestFrameResult], hand: Hand) -> PalmFeatureResult {
        let frames = Array(bestFrames.values)
        let avgQuality: Int
        if frames.isEmpty {
            avgQuality = 0
        } else {
            let total = frames.reduce(0) { $0 + $1.qualityScores.composite }
            avgQuality = total / frames.count
        }

        let regionStats = aggregateRegions(frames: frames)

        let features: PalmFeatures
        let metricsAvailable = !regionStats.isEmpty
        if metricsAvailable {
            features = buildFeatures(from: regionStats)
        } else {
            features = conservativeFallbackFeatures(quality: avgQuality)
        }

        let coverage = computeFeatureCoverage(features)
        let confidence = metricsAvailable
            ? deriveConfidence(scanQuality: avgQuality, featureCoverage: coverage)
            : "low"

        return PalmFeatureResult(
            features: features,
            featureCoverage: coverage,
            confidence: confidence,
            extractorVersion: version
        )
    }

    // MARK: - Region aggregation

    struct RegionStats {
        let contrast: Float
        let continuity: Float
        let meanIntensity: Float
    }

    /// Averages each canonical region's measurements across all frames that
    /// carry palm metrics. Deterministic: iteration order does not matter for
    /// per-region means.
    private func aggregateRegions(frames: [BestFrameResult]) -> [String: RegionStats] {
        var sums: [String: (contrast: Float, continuity: Float, intensity: Float, count: Int)] = [:]
        for frame in frames {
            guard let metrics = frame.palmMetrics else { continue }
            for region in metrics.lineRegions {
                var entry = sums[region.region] ?? (0, 0, 0, 0)
                entry.contrast += region.contrast
                entry.continuity += region.continuity
                entry.intensity += region.meanIntensity
                entry.count += 1
                sums[region.region] = entry
            }
        }
        var stats: [String: RegionStats] = [:]
        for (region, entry) in sums where entry.count > 0 {
            let n = Float(entry.count)
            stats[region] = RegionStats(
                contrast: entry.contrast / n,
                continuity: entry.continuity / n,
                meanIntensity: entry.intensity / n
            )
        }
        return stats
    }

    // MARK: - Categorical mapping (metrics path)

    private func isPresent(_ s: RegionStats?) -> Bool {
        guard let s else { return false }
        return s.continuity >= 0.30 && s.contrast >= 0.15
    }

    private func clarity(_ s: RegionStats?) -> String {
        guard let s, isPresent(s) else { return "faint" }
        if s.contrast >= 0.55 && s.continuity >= 0.65 { return "clear" }
        if s.contrast >= 0.35 && s.continuity >= 0.45 { return "moderate" }
        return "faint"
    }

    private func shape(_ s: RegionStats?) -> String {
        guard let s, isPresent(s) else { return "unknown" }
        return s.continuity < 0.50 ? "chained" : "smooth"
    }

    private func lengthCategory(_ s: RegionStats?) -> String {
        guard let s, isPresent(s) else { return "unknown" }
        if s.continuity >= 0.75 { return "long" }
        if s.continuity >= 0.50 { return "medium" }
        return "short"
    }

    private func buildFeatures(from stats: [String: RegionStats]) -> PalmFeatures {
        let head = stats[Self.regionHeadline]
        let heart = stats[Self.regionHeartline]
        let life = stats[Self.regionLifeline]
        let fate = stats[Self.regionFateline]

        return PalmFeatures(
            headlinePresent: isPresent(head),
            heartlinePresent: isPresent(heart),
            lifelinePresent: isPresent(life),
            fatelinePresent: isPresent(fate),
            headlineShape: shape(head),
            heartlineShape: shape(heart),
            lifelineShape: shape(life),
            fatelineShape: shape(fate),
            headlineClarity: clarity(head),
            heartlineClarity: clarity(heart),
            lifelineClarity: clarity(life),
            fatelineClarity: clarity(fate),
            headlineLength: lengthCategory(head),
            fatelineLength: lengthCategory(fate),
            venusMountDensity: "unknown",
            jupiterMountDensity: "unknown",
            saturnMountDensity: "unknown",
            minorLineDensity: "unknown"
        )
    }

    // MARK: - Conservative fallback (no metrics)

    private func conservativeFallbackFeatures(quality: Int) -> PalmFeatures {
        let usable = quality >= 60
        return PalmFeatures(
            headlinePresent: usable,
            heartlinePresent: usable,
            lifelinePresent: usable,
            fatelinePresent: false,
            headlineShape: "unknown",
            heartlineShape: "unknown",
            lifelineShape: "unknown",
            fatelineShape: "unknown",
            headlineClarity: usable ? "moderate" : "faint",
            heartlineClarity: usable ? "moderate" : "faint",
            lifelineClarity: usable ? "moderate" : "faint",
            fatelineClarity: "faint",
            headlineLength: "unknown",
            fatelineLength: "unknown",
            venusMountDensity: "unknown",
            jupiterMountDensity: "unknown",
            saturnMountDensity: "unknown",
            minorLineDensity: "unknown"
        )
    }

    // MARK: - Coverage and confidence

    /// Fraction of the 18 contract fields carrying a determinate value.
    /// "faint"/"unknown"/"unclear" string values do not count as covered
    /// (same convention as the Android extractor).
    private func computeFeatureCoverage(_ f: PalmFeatures) -> Float {
        let indeterminate: Set<String> = ["unknown", "unclear", "faint"]
        var present = 0
        for flag in [f.headlinePresent, f.heartlinePresent, f.lifelinePresent, f.fatelinePresent] where flag {
            present += 1
        }
        let stringFields = [
            f.headlineShape, f.heartlineShape, f.lifelineShape, f.fatelineShape,
            f.headlineClarity, f.heartlineClarity, f.lifelineClarity, f.fatelineClarity,
            f.headlineLength, f.fatelineLength,
            f.venusMountDensity, f.jupiterMountDensity, f.saturnMountDensity, f.minorLineDensity,
        ]
        for value in stringFields where !indeterminate.contains(value) {
            present += 1
        }
        return min(max(Float(present) / 18.0, 0), 1)
    }

    private func deriveConfidence(scanQuality: Int, featureCoverage: Float) -> String {
        if scanQuality < 40 || featureCoverage < 0.40 { return "low" }
        if scanQuality >= 70 && featureCoverage >= 0.70 { return "high" }
        return "med"
    }
}
