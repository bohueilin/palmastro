import Testing
import CoreContracts
@testable import PalmFeatureEngine

// Mirrors engine-palm-features/src/test/kotlin/com/palmastro/palm/PalmFeatureExtractorTest.kt.

@Suite struct PalmFeatureExtractorTests {

    private let extractor = PalmFeatureExtractorImpl()

    // Canonical normalized MediaPipe-style landmark layout (right hand, palm up).
    private func canonicalLandmarks() -> [LandmarkPoint] {
        [
            LandmarkPoint(x: 0.50, y: 0.95, z: 0), // 0 wrist
            LandmarkPoint(x: 0.38, y: 0.85, z: 0), // 1 thumb CMC
            LandmarkPoint(x: 0.30, y: 0.75, z: 0), // 2 thumb MCP
            LandmarkPoint(x: 0.24, y: 0.66, z: 0), // 3 thumb IP
            LandmarkPoint(x: 0.20, y: 0.58, z: 0), // 4 thumb tip
            LandmarkPoint(x: 0.34, y: 0.48, z: 0), // 5 index MCP
            LandmarkPoint(x: 0.32, y: 0.36, z: 0), // 6
            LandmarkPoint(x: 0.31, y: 0.27, z: 0), // 7
            LandmarkPoint(x: 0.30, y: 0.19, z: 0), // 8
            LandmarkPoint(x: 0.46, y: 0.46, z: 0), // 9 middle MCP
            LandmarkPoint(x: 0.46, y: 0.32, z: 0), // 10
            LandmarkPoint(x: 0.46, y: 0.22, z: 0), // 11
            LandmarkPoint(x: 0.46, y: 0.13, z: 0), // 12
            LandmarkPoint(x: 0.58, y: 0.47, z: 0), // 13 ring MCP
            LandmarkPoint(x: 0.59, y: 0.34, z: 0), // 14
            LandmarkPoint(x: 0.60, y: 0.25, z: 0), // 15
            LandmarkPoint(x: 0.61, y: 0.17, z: 0), // 16
            LandmarkPoint(x: 0.70, y: 0.52, z: 0), // 17 pinky MCP
            LandmarkPoint(x: 0.72, y: 0.41, z: 0), // 18
            LandmarkPoint(x: 0.73, y: 0.33, z: 0), // 19
            LandmarkPoint(x: 0.74, y: 0.26, z: 0), // 20
        ]
    }

    private func region(_ name: String, _ contrast: Float, _ continuity: Float, meanIntensity: Float = 0.5) -> LineRegionMetrics {
        LineRegionMetrics(region: name, contrast: contrast, continuity: continuity, meanIntensity: meanIntensity)
    }

    private func palmMetrics(
        headline: LineRegionMetrics? = nil,
        heartline: LineRegionMetrics? = nil,
        lifeline: LineRegionMetrics? = nil,
        fateline: LineRegionMetrics? = nil,
        landmarks: [LandmarkPoint]? = nil
    ) -> PalmMetrics {
        PalmMetrics(
            landmarks: landmarks ?? canonicalLandmarks(),
            lineRegions: [
                headline ?? region("headline", 0.70, 0.90),
                heartline ?? region("heartline", 0.70, 0.85),
                lifeline ?? region("lifeline", 0.65, 0.80),
                fateline ?? region("fateline", 0.60, 0.80),
            ]
        )
    }

    private func frames(
        metrics: PalmMetrics?,
        quality: Int = 80,
        coverage: Float = 0.9,
        angles: [Angle] = [.FRONT, .NEAR, .FAR]
    ) -> [Angle: BestFrameResult] {
        var result: [Angle: BestFrameResult] = [:]
        for angle in angles {
            result[angle] = BestFrameResult(
                angle: angle, frameIndex: 0,
                qualityScores: QualityScores(blur: 0.8, glare: 0.8, exposure: 0.8, coverage: coverage, stability: 0.8, composite: quality),
                fileRef: nil, palmMetrics: metrics
            )
        }
        return result
    }

    private func frames(quality: Int = 80) -> [Angle: BestFrameResult] {
        frames(metrics: palmMetrics(), quality: quality)
    }

    @Test func extractReturnsNonIdentifyingCategoricalFeatures() {
        let result = extractor.extract(bestFrames: frames(), hand: .RIGHT)
        #expect(result.features.headlinePresent)
        #expect(!result.features.headlineShape.isEmpty)
        #expect(!result.features.headlineClarity.isEmpty)
    }

    @Test func strongPalmProducesClearLinesAndLongHeadline() {
        let f = extractor.extract(bestFrames: frames(), hand: .RIGHT).features
        #expect(f.headlineClarity == "clear")
        #expect(f.headlineLength == "long")
        #expect(f.heartlineClarity == "clear")
        #expect(f.lifelineClarity == "clear")
        #expect(f.fatelinePresent)
        #expect(f.fatelineClarity == "clear")
    }

    @Test func canonicalHandGeometryShapes() {
        // Curved head and heart lines, straight fateline.
        let f = extractor.extract(bestFrames: frames(), hand: .RIGHT).features
        #expect(f.headlineShape == "curved")
        #expect(f.heartlineShape == "curved")
        #expect(f.fatelineShape == "straight")
    }

    // MARK: - Clarity buckets

    @Test func midContinuityWithStrongContrastReadsBroken() {
        let m = palmMetrics(headline: region("headline", 0.55, 0.50))
        let f = extractor.extract(bestFrames: frames(metrics: m), hand: .RIGHT).features
        #expect(f.headlinePresent)
        #expect(f.headlineClarity == "broken")
    }

    @Test func midContinuityWithWeakContrastReadsFaint() {
        let m = palmMetrics(fateline: region("fateline", 0.30, 0.50))
        let f = extractor.extract(bestFrames: frames(metrics: m), hand: .RIGHT).features
        #expect(f.fatelineClarity == "faint")
    }

    @Test func heartlineWithHighContinuityButShallowContrastReadsThin() {
        let m = palmMetrics(heartline: region("heartline", 0.30, 0.80))
        let f = extractor.extract(bestFrames: frames(metrics: m), hand: .RIGHT).features
        #expect(f.heartlineClarity == "thin")
    }

    @Test func nonHeartlineWithHighContinuityButShallowContrastReadsFaint() {
        let m = palmMetrics(lifeline: region("lifeline", 0.30, 0.70))
        let f = extractor.extract(bestFrames: frames(metrics: m), hand: .RIGHT).features
        #expect(f.lifelineClarity == "faint")
    }

    @Test func moderateContrastWithGoodContinuityReadsMedium() {
        let m = palmMetrics(headline: region("headline", 0.45, 0.70))
        let f = extractor.extract(bestFrames: frames(metrics: m), hand: .RIGHT).features
        #expect(f.headlineClarity == "medium")
    }

    @Test func lowContinuityMeansLineAbsentAndUnclear() {
        let m = palmMetrics(fateline: region("fateline", 0.70, 0.20))
        let f = extractor.extract(bestFrames: frames(metrics: m), hand: .RIGHT).features
        #expect(!f.fatelinePresent)
        #expect(f.fatelineClarity == "unclear")
        #expect(f.fatelineShape == "unclear")
    }

    // MARK: - Mounts + minor lines

    @Test func denseMinorLinesFromHighResidualContrast() {
        let m = palmMetrics(
            headline: region("headline", 0.80, 0.50),
            heartline: region("heartline", 0.80, 0.50),
            lifeline: region("lifeline", 0.80, 0.50),
            fateline: region("fateline", 0.80, 0.50)
        )
        let f = extractor.extract(bestFrames: frames(metrics: m), hand: .RIGHT).features
        #expect(f.minorLineDensity == "high")
    }

    @Test func cleanContinuousLinesGiveLowMinorLineDensity() {
        let m = palmMetrics(
            headline: region("headline", 0.50, 0.90),
            heartline: region("heartline", 0.50, 0.90),
            lifeline: region("lifeline", 0.50, 0.90),
            fateline: region("fateline", 0.50, 0.90)
        )
        let f = extractor.extract(bestFrames: frames(metrics: m), hand: .RIGHT).features
        #expect(f.minorLineDensity == "low")
    }

    @Test func mountDensityReflectsDarknessAndContrastOfAdjacentRegion() {
        let dark = palmMetrics(lifeline: region("lifeline", 0.65, 0.80, meanIntensity: 0.25))
        #expect(extractor.extract(bestFrames: frames(metrics: dark), hand: .RIGHT).features.venusMountDensity == "high")
        let bright = palmMetrics(lifeline: region("lifeline", 0.40, 0.80, meanIntensity: 0.95))
        #expect(extractor.extract(bestFrames: frames(metrics: bright), hand: .RIGHT).features.venusMountDensity == "low")
    }

    // MARK: - Determinism + input sensitivity

    @Test func deterministicForSameInput() {
        let input = frames()
        #expect(extractor.extract(bestFrames: input, hand: .RIGHT)
                == extractor.extract(bestFrames: input, hand: .RIGHT))
    }

    @Test func inputSensitiveDifferentMetricsGiveDifferentFeatures() {
        let strong = extractor.extract(bestFrames: frames(metrics: palmMetrics()), hand: .RIGHT)
        let weak = extractor.extract(
            bestFrames: frames(metrics: palmMetrics(
                headline: region("headline", 0.55, 0.50),
                heartline: region("heartline", 0.30, 0.80),
                lifeline: region("lifeline", 0.30, 0.70),
                fateline: region("fateline", 0.70, 0.20)
            )),
            hand: .RIGHT
        )
        #expect(strong.features != weak.features)
    }

    // MARK: - Aggregation across angles

    @Test func medianAcrossAnglesIsRobustToOneOutlierFrame() {
        let good = palmMetrics()
        let outlier = palmMetrics(headline: region("headline", 0.05, 0.05))
        var bestFrames = frames(metrics: good, angles: [.FRONT, .FAR])
        bestFrames[.NEAR] = BestFrameResult(
            angle: .NEAR, frameIndex: 0,
            qualityScores: QualityScores(blur: 0.8, glare: 0.8, exposure: 0.8, coverage: 0.9, stability: 0.8, composite: 80),
            fileRef: nil, palmMetrics: outlier
        )
        let f = extractor.extract(bestFrames: bestFrames, hand: .RIGHT).features
        #expect(f.headlinePresent)
        #expect(f.headlineClarity == "clear")
    }

    @Test func highContrastSpreadAcrossAnglesMarksLineBroken() {
        // Median contrast 0.35 (below the broken-contrast bar) but the
        // across-angle spread 0.40 exceeds the spread proxy.
        var bestFrames: [Angle: BestFrameResult] = [:]
        for (angle, contrast) in [(Angle.FRONT, Float(0.20)), (.NEAR, 0.35), (.FAR, 0.60)] {
            bestFrames[angle] = BestFrameResult(
                angle: angle, frameIndex: 0,
                qualityScores: QualityScores(blur: 0.8, glare: 0.8, exposure: 0.8, coverage: 0.9, stability: 0.8, composite: 80),
                fileRef: nil,
                palmMetrics: palmMetrics(headline: region("headline", contrast, 0.50))
            )
        }
        let f = extractor.extract(bestFrames: bestFrames, hand: .RIGHT).features
        #expect(f.headlineClarity == "broken")
    }

    // MARK: - Fallback

    @Test func allPalmMetricsNilFallsBackToConservativeLowConfidence() {
        let result = extractor.extract(bestFrames: frames(metrics: nil), hand: .RIGHT)
        #expect(result.confidence == "low")
        let f = result.features
        // Conservative: no category that resolves a negative signal.
        #expect(!["broken", "faint", "thin"].contains(f.headlineClarity))
        #expect(!["broken", "faint", "thin"].contains(f.heartlineClarity))
        #expect(!["broken", "faint", "thin"].contains(f.lifelineClarity))
        #expect(f.minorLineDensity != "high")
        #expect(!f.fatelinePresent)
    }

    @Test func malformedLandmarkListIsTreatedAsMissingMetrics() {
        let bad = PalmMetrics(
            landmarks: Array(canonicalLandmarks().prefix(5)),
            lineRegions: palmMetrics().lineRegions
        )
        let result = extractor.extract(bestFrames: frames(metrics: bad), hand: .RIGHT)
        #expect(result.confidence == "low")
    }

    @Test func emptyFrameMapFallsBack() {
        let result = extractor.extract(bestFrames: [:], hand: .RIGHT)
        #expect(result.confidence == "low")
        #expect(result.featureCoverage >= 0 && result.featureCoverage <= 1)
    }

    // MARK: - Confidence tiers

    @Test func confidenceHighWhenQualityAtLeast70AndRichMetrics() {
        #expect(extractor.extract(bestFrames: frames(quality: 80), hand: .RIGHT).confidence == "high")
    }

    @Test func confidenceMedWhenQualityBetween40And70() {
        #expect(extractor.extract(bestFrames: frames(quality: 55), hand: .RIGHT).confidence == "med")
    }

    @Test func confidenceLowWhenQualityBelow40() {
        #expect(extractor.extract(bestFrames: frames(quality: 30), hand: .RIGHT).confidence == "low")
    }

    // MARK: - Coverage

    @Test func featureCoverageReflectsExtractableFeatures() {
        let rich = extractor.extract(bestFrames: frames(), hand: .RIGHT)
        #expect(rich.featureCoverage >= 0 && rich.featureCoverage <= 1)
        let fallback = extractor.extract(bestFrames: frames(metrics: nil), hand: .RIGHT)
        #expect(rich.featureCoverage > fallback.featureCoverage)
    }

    @Test func missingRegionLowersCoverage() {
        let noFateline = PalmMetrics(
            landmarks: canonicalLandmarks(),
            lineRegions: palmMetrics().lineRegions.filter { $0.region != "fateline" }
        )
        let partial = extractor.extract(bestFrames: frames(metrics: noFateline), hand: .RIGHT)
        let full = extractor.extract(bestFrames: frames(), hand: .RIGHT)
        #expect(partial.featureCoverage < full.featureCoverage)
        #expect(!partial.features.fatelinePresent)
    }

    @Test func extractorVersionIsSemver() {
        let result = extractor.extract(bestFrames: frames(), hand: .RIGHT)
        #expect(result.extractorVersion == "2.0.0")
        #expect(result.extractorVersion.range(of: #"^\d+\.\d+\.\d+$"#, options: .regularExpression) != nil)
    }
}
