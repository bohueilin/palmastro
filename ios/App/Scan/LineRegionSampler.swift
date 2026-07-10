import Foundation
import CoreVideo
import CoreContracts

/// Samples luminance statistics along the four canonical palm-line regions
/// from a camera frame + the 21 normalized landmarks, producing the
/// cross-platform `LineRegionMetrics` contract (contrast / continuity /
/// meanIntensity, all 0..1). No raw imagery leaves this function.
///
/// Region paths are defined in landmark space (MediaPipe indexing):
/// - heartline: arc under the finger MCPs (index 5 → pinky 17)
/// - headline:  across the mid-palm (thumb-side below index MCP → ulnar side)
/// - lifeline:  curve around the thumb base (between thumb CMC 1 and wrist 0)
/// - fateline:  vertical from wrist 0 toward middle MCP 9
enum LineRegionSampler {

    static let sampleCount = 48

    /// Extracts metrics from a BGRA pixel buffer. Landmarks are normalized
    /// (0..1, top-left origin) matching the contract coordinate space.
    static func sample(pixelBuffer: CVPixelBuffer, landmarks: [LandmarkPoint]) -> [LineRegionMetrics]? {
        guard landmarks.count == 21 else { return nil }

        CVPixelBufferLockBaseAddress(pixelBuffer, .readOnly)
        defer { CVPixelBufferUnlockBaseAddress(pixelBuffer, .readOnly) }

        guard let base = CVPixelBufferGetBaseAddress(pixelBuffer) else { return nil }
        let width = CVPixelBufferGetWidth(pixelBuffer)
        let height = CVPixelBufferGetHeight(pixelBuffer)
        let bytesPerRow = CVPixelBufferGetBytesPerRow(pixelBuffer)
        let buffer = base.assumingMemoryBound(to: UInt8.self)

        func luma(atNormalizedX x: Float, y: Float) -> Float? {
            guard x >= 0, x < 1, y >= 0, y < 1 else { return nil }
            let px = min(Int(x * Float(width)), width - 1)
            let py = min(Int(y * Float(height)), height - 1)
            let offset = py * bytesPerRow + px * 4
            let b = Float(buffer[offset])
            let g = Float(buffer[offset + 1])
            let r = Float(buffer[offset + 2])
            return (0.114 * b + 0.587 * g + 0.299 * r) / 255.0
        }

        func point(_ index: Int) -> LandmarkPoint { landmarks[index] }

        func lerp(_ a: LandmarkPoint, _ b: LandmarkPoint, _ t: Float) -> (x: Float, y: Float) {
            (a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
        }

        /// Samples along a polyline (in landmark space), comparing each
        /// on-path sample with a perpendicular-offset skin sample to measure
        /// dark-line contrast.
        func metrics(region: String, path: [(Float, Float)], normalOffset: Float) -> LineRegionMetrics {
            var onPath: [Float] = []
            var offPath: [Float] = []
            var presentSamples = 0
            var totalSamples = 0

            for i in 0..<sampleCount {
                let t = Float(i) / Float(sampleCount - 1)
                let scaled = t * Float(path.count - 1)
                let segment = min(Int(scaled), path.count - 2)
                let localT = scaled - Float(segment)
                let (x0, y0) = path[segment]
                let (x1, y1) = path[segment + 1]
                let x = x0 + (x1 - x0) * localT
                let y = y0 + (y1 - y0) * localT

                // Perpendicular direction for the skin reference sample.
                let dx = x1 - x0, dy = y1 - y0
                let length = max(sqrtf(dx * dx + dy * dy), 1e-5)
                let nx = -dy / length, ny = dx / length

                guard let lineLuma = luma(atNormalizedX: x, y: y),
                      let skinLuma = luma(atNormalizedX: x + nx * normalOffset, y: y + ny * normalOffset) else {
                    continue
                }
                totalSamples += 1
                onPath.append(lineLuma)
                offPath.append(skinLuma)
                // A palm line is darker than the surrounding skin.
                if skinLuma - lineLuma > 0.04 { presentSamples += 1 }
            }

            guard totalSamples > 0 else {
                return LineRegionMetrics(region: region, contrast: 0, continuity: 0, meanIntensity: 0)
            }
            let meanLine = onPath.reduce(0, +) / Float(onPath.count)
            let meanSkin = offPath.reduce(0, +) / Float(offPath.count)
            let contrast = min(max((meanSkin - meanLine) * 6.0, 0), 1)
            let continuity = Float(presentSamples) / Float(totalSamples)
            return LineRegionMetrics(
                region: region,
                contrast: contrast,
                continuity: continuity,
                meanIntensity: min(max(meanLine, 0), 1)
            )
        }

        let wrist = point(0)
        let thumbCMC = point(1)
        let indexMCP = point(5)
        let middleMCP = point(9)
        let ringMCP = point(13)
        let pinkyMCP = point(17)

        // Push the finger-MCP arc toward the palm center for the heart line.
        func towardWrist(_ p: LandmarkPoint, _ amount: Float) -> (Float, Float) {
            lerp(p, wrist, amount)
        }

        let heartPath = [
            towardWrist(indexMCP, 0.20),
            towardWrist(middleMCP, 0.18),
            towardWrist(ringMCP, 0.18),
            towardWrist(pinkyMCP, 0.20),
        ]
        let headPath = [
            towardWrist(indexMCP, 0.38),
            towardWrist(middleMCP, 0.40),
            towardWrist(ringMCP, 0.42),
            towardWrist(pinkyMCP, 0.45),
        ]
        let lifePath = [
            lerp(thumbCMC, indexMCP, 0.35),
            lerp(thumbCMC, middleMCP, 0.30),
            lerp(thumbCMC, wrist, 0.35),
            lerp(thumbCMC, wrist, 0.75),
        ]
        let fatePath = [
            lerp(wrist, middleMCP, 0.15),
            lerp(wrist, middleMCP, 0.45),
            lerp(wrist, middleMCP, 0.75),
        ]

        return [
            metrics(region: "heartline", path: heartPath, normalOffset: 0.03),
            metrics(region: "headline", path: headPath, normalOffset: 0.03),
            metrics(region: "lifeline", path: lifePath, normalOffset: 0.03),
            metrics(region: "fateline", path: fatePath, normalOffset: 0.03),
        ]
    }
}
