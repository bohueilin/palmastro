import Testing
import CoreContracts
@testable import PalmFeatureEngine

@Suite struct PalmFeatureExtractorTests {

    private let extractor = PalmFeatureExtractorImpl()

    private func quality(_ composite: Int) -> QualityScores {
        let component = Float(composite) / 100.0
        return QualityScores(
            blur: component, glare: component, exposure: component,
            coverage: component, stability: component, composite: composite
        )
    }

    private func frame(
        angle: Angle,
        composite: Int,
        metrics: PalmMetrics?
    ) -> BestFrameResult {
        BestFrameResult(
            angle: angle, frameIndex: 0,
            qualityScores: quality(composite), fileRef: nil, palmMetrics: metrics
        )
    }

    private func metrics(
        headline: (Float, Float, Float),
        heartline: (Float, Float, Float),
        lifeline: (Float, Float, Float),
        fateline: (Float, Float, Float)
    ) -> PalmMetrics {
        PalmMetrics(
            landmarks: (0..<21).map { LandmarkPoint(x: Float($0) / 21.0, y: 0.5, z: 0) },
            lineRegions: [
                LineRegionMetrics(region: "headline", contrast: headline.0, continuity: headline.1, meanIntensity: headline.2),
                LineRegionMetrics(region: "heartline", contrast: heartline.0, continuity: heartline.1, meanIntensity: heartline.2),
                LineRegionMetrics(region: "lifeline", contrast: lifeline.0, continuity: lifeline.1, meanIntensity: lifeline.2),
                LineRegionMetrics(region: "fateline", contrast: fateline.0, continuity: fateline.1, meanIntensity: fateline.2),
            ]
        )
    }

    @Test func clearLinesProduceClearCategoricalFeatures() {
        let strong = metrics(
            headline: (0.7, 0.85, 0.35),
            heartline: (0.65, 0.8, 0.4),
            lifeline: (0.6, 0.75, 0.4),
            fateline: (0.6, 0.7, 0.45)
        )
        let result = extractor.extract(bestFrames: [.FRONT: frame(angle: .FRONT, composite: 85, metrics: strong)], hand: .RIGHT)
        let f = result.features

        #expect(f.headlinePresent)
        #expect(f.headlineClarity == "clear")
        #expect(f.headlineLength == "long")
        #expect(f.headlineShape == "smooth")
        #expect(f.heartlineClarity == "clear")
        #expect(f.fatelinePresent)
        #expect(result.extractorVersion == "2.0.0")
    }

    @Test func brokenLowContinuityLineIsChained() {
        let broken = metrics(
            headline: (0.6, 0.38, 0.5),   // present but fragmented
            heartline: (0.6, 0.8, 0.4),
            lifeline: (0.6, 0.8, 0.4),
            fateline: (0.5, 0.35, 0.5)
        )
        let result = extractor.extract(bestFrames: [.FRONT: frame(angle: .FRONT, composite: 80, metrics: broken)], hand: .LEFT)
        #expect(result.features.headlinePresent)
        #expect(result.features.headlineShape == "chained")
        #expect(result.features.fatelineShape == "chained")
    }

    @Test func weakSignalsAreFaintOrAbsentNotFabricated() {
        let weak = metrics(
            headline: (0.05, 0.1, 0.8),
            heartline: (0.1, 0.2, 0.8),
            lifeline: (0.05, 0.15, 0.8),
            fateline: (0.02, 0.05, 0.9)
        )
        let result = extractor.extract(bestFrames: [.FRONT: frame(angle: .FRONT, composite: 75, metrics: weak)], hand: .RIGHT)
        let f = result.features
        #expect(!f.headlinePresent)
        #expect(!f.fatelinePresent)
        #expect(f.headlineClarity == "faint")
        #expect(f.headlineShape == "unknown")
        // Mounts are not measurable from line regions — must stay unknown.
        #expect(f.venusMountDensity == "unknown")
        #expect(f.minorLineDensity == "unknown")
    }

    @Test func fallbackWithoutMetricsIsConservativeAndLowConfidence() {
        let result = extractor.extract(
            bestFrames: [
                .FRONT: frame(angle: .FRONT, composite: 90, metrics: nil),
                .NEAR: frame(angle: .NEAR, composite: 88, metrics: nil),
            ],
            hand: .RIGHT
        )
        // Metrics-free extraction must be low confidence per EXECUTION_SPEC.
        #expect(result.confidence == "low")
        #expect(!result.features.fatelinePresent)
        #expect(result.features.headlineShape == "unknown")
        #expect(result.features.headlineClarity == "moderate")
    }

    @Test func metricsAveragedAcrossFrames() {
        let sharp = metrics(
            headline: (0.9, 0.9, 0.3),
            heartline: (0.9, 0.9, 0.3),
            lifeline: (0.9, 0.9, 0.3),
            fateline: (0.9, 0.9, 0.3)
        )
        let dull = metrics(
            headline: (0.3, 0.5, 0.6),
            heartline: (0.3, 0.5, 0.6),
            lifeline: (0.3, 0.5, 0.6),
            fateline: (0.3, 0.5, 0.6)
        )
        let result = extractor.extract(
            bestFrames: [
                .FRONT: frame(angle: .FRONT, composite: 80, metrics: sharp),
                .LEFT_TILT: frame(angle: .LEFT_TILT, composite: 70, metrics: dull),
            ],
            hand: .RIGHT
        )
        // Averages: contrast 0.6, continuity 0.7 -> clear.
        #expect(result.features.headlineClarity == "clear")
        #expect(result.features.headlineShape == "smooth")
    }

    @Test func deterministicForSameInput() {
        let m = metrics(
            headline: (0.5, 0.6, 0.5),
            heartline: (0.4, 0.5, 0.5),
            lifeline: (0.45, 0.55, 0.5),
            fateline: (0.2, 0.35, 0.6)
        )
        let frames: [Angle: BestFrameResult] = [.FRONT: frame(angle: .FRONT, composite: 72, metrics: m)]
        let a = extractor.extract(bestFrames: frames, hand: .RIGHT)
        let b = extractor.extract(bestFrames: frames, hand: .RIGHT)
        #expect(a == b)
    }
}
