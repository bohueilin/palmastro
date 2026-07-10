import Foundation
import AVFoundation
import Vision
import CoreContracts
import ScanQualityEngine

/// Camera + hand-landmark pipeline for the seven-angle scan flow (PRD §13.2,
/// §15). AVFoundation feeds frames into Vision's
/// `VNDetectHumanHandPoseRequest`; detected joints are mapped onto the
/// 21-point MediaPipe landmark contract, frames are quality-gated by the
/// shared `QualityGateImpl`, and passing frames carry `PalmMetrics`
/// (landmarks + line-region statistics) for the feature extractor.
final class HandPoseScanController: NSObject, ObservableObject {

    static let angleSequence: [Angle] = [.FRONT, .LEFT_TILT, .RIGHT_TILT, .NEAR, .FAR, .UP_TILT, .DOWN_TILT]

    /// Frames a single angle must collect before best-frame selection.
    static let framesPerAngle = 12

    @Published private(set) var currentAngleIndex = 0
    @Published private(set) var lastHintKey: String?
    @Published private(set) var isHandDetected = false
    @Published private(set) var finishedSummary: ScanSessionSummary?
    @Published var permissionDenied = false

    let session = AVCaptureSession()

    private let gate = QualityGateImpl()
    private let videoQueue = DispatchQueue(label: "com.palmastro.scan.video")
    private let handPoseRequest: VNDetectHumanHandPoseRequest = {
        let request = VNDetectHumanHandPoseRequest()
        request.maximumHandCount = 1
        return request
    }()

    private var hand: Hand = .RIGHT
    private var startedAt = Date()
    private var totalAttempts = 0
    private var candidateFrames: [(scores: QualityScores, metrics: PalmMetrics?)] = []
    private var bestFrames: [Angle: BestFrameResult] = [:]
    private var previousCentroid: (x: Float, y: Float)?

    var currentAngle: Angle {
        Self.angleSequence[min(currentAngleIndex, Self.angleSequence.count - 1)]
    }

    // MARK: - Lifecycle

    func begin(hand: Hand) {
        self.hand = hand
        startedAt = Date()
        currentAngleIndex = 0
        totalAttempts = 0
        candidateFrames = []
        bestFrames = [:]
        finishedSummary = nil
        requestCameraAccess()
    }

    func stop() {
        videoQueue.async { [session] in
            if session.isRunning { session.stopRunning() }
        }
    }

    private func requestCameraAccess() {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            configureAndStart()
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
                DispatchQueue.main.async {
                    if granted {
                        self?.configureAndStart()
                    } else {
                        self?.permissionDenied = true
                    }
                }
            }
        default:
            permissionDenied = true
        }
    }

    private func configureAndStart() {
        videoQueue.async { [weak self] in
            guard let self else { return }
            self.session.beginConfiguration()
            self.session.sessionPreset = .hd1280x720
            self.session.inputs.forEach(self.session.removeInput)
            self.session.outputs.forEach(self.session.removeOutput)

            guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back),
                  let input = try? AVCaptureDeviceInput(device: device),
                  self.session.canAddInput(input) else {
                self.session.commitConfiguration()
                DispatchQueue.main.async { self.permissionDenied = true }
                return
            }
            self.session.addInput(input)

            let output = AVCaptureVideoDataOutput()
            output.videoSettings = [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA]
            output.alwaysDiscardsLateVideoFrames = true
            output.setSampleBufferDelegate(self, queue: self.videoQueue)
            if self.session.canAddOutput(output) {
                self.session.addOutput(output)
            }
            self.session.commitConfiguration()
            self.session.startRunning()
        }
    }

    // MARK: - Frame processing

    private func process(pixelBuffer: CVPixelBuffer) {
        totalAttempts += 1

        let handler = VNImageRequestHandler(cvPixelBuffer: pixelBuffer, orientation: .right)
        try? handler.perform([handPoseRequest])

        guard let observation = handPoseRequest.results?.first else {
            DispatchQueue.main.async {
                self.isHandDetected = false
                self.lastHintKey = CoachingHints.keyFor(failReason: "hand_not_detected")
            }
            return
        }

        guard let landmarks = Self.mapToMediaPipeLandmarks(observation) else {
            return
        }

        let quality = measureQuality(pixelBuffer: pixelBuffer, landmarks: landmarks, confidence: observation.confidence)
        let regionMetrics = LineRegionSampler.sample(pixelBuffer: pixelBuffer, landmarks: landmarks)
        let palmMetrics = regionMetrics.map { PalmMetrics(landmarks: landmarks, lineRegions: $0) }
        candidateFrames.append((quality, palmMetrics))

        DispatchQueue.main.async { self.isHandDetected = true }

        guard candidateFrames.count >= Self.framesPerAngle else { return }

        let scores = candidateFrames.map(\.scores)
        let bestIndex = gate.selectBestFrame(frames: scores)
        let gateResult = gate.evaluateAngle(angle: currentAngle, bestScore: scores[bestIndex])

        if gateResult.passed {
            let best = candidateFrames[bestIndex]
            bestFrames[currentAngle] = BestFrameResult(
                angle: currentAngle,
                frameIndex: bestIndex,
                qualityScores: best.scores,
                fileRef: nil,          // raw media persisted only when retention is on
                palmMetrics: best.metrics
            )
            candidateFrames = []
            advanceOrFinish()
        } else {
            candidateFrames = []
            DispatchQueue.main.async {
                self.lastHintKey = CoachingHints.keyFor(failReason: gateResult.failReason ?? "")
            }
        }
    }

    private func advanceOrFinish() {
        DispatchQueue.main.async {
            self.lastHintKey = nil
            if self.currentAngleIndex + 1 < Self.angleSequence.count {
                self.currentAngleIndex += 1
            } else {
                self.finish()
            }
        }
    }

    private func finish() {
        stop()
        let overall = bestFrames.values.map(\.qualityScores.composite)
        let overallScore = overall.isEmpty ? 0 : overall.reduce(0, +) / overall.count
        let coverage = Float(bestFrames.count) / Float(Self.angleSequence.count)
        finishedSummary = ScanSessionSummary(
            sessionId: UUID().uuidString,
            hand: hand,
            angleResults: bestFrames,
            overallQualityScore: overallScore,
            featureCoverage: coverage,
            totalDurationMs: Int64(Date().timeIntervalSince(startedAt) * 1000),
            totalAttempts: totalAttempts
        )
    }

    // MARK: - Quality measurement

    /// Derives the five gate components (0..1 each) from the frame and hand
    /// pose: blur (local luma variance proxy), glare (near-saturated pixel
    /// fraction), exposure (mean luma window), coverage (hand bounding box
    /// area), stability (centroid movement between frames).
    private func measureQuality(pixelBuffer: CVPixelBuffer, landmarks: [LandmarkPoint], confidence: Float) -> QualityScores {
        var meanLuma: Float = 0.5
        var saturatedFraction: Float = 0
        var localVariance: Float = 0

        CVPixelBufferLockBaseAddress(pixelBuffer, .readOnly)
        if let base = CVPixelBufferGetBaseAddress(pixelBuffer) {
            let width = CVPixelBufferGetWidth(pixelBuffer)
            let height = CVPixelBufferGetHeight(pixelBuffer)
            let bytesPerRow = CVPixelBufferGetBytesPerRow(pixelBuffer)
            let buffer = base.assumingMemoryBound(to: UInt8.self)

            var sum: Float = 0, sumSq: Float = 0, saturated = 0
            let grid = 24
            var previous: Float?
            var diffSum: Float = 0, diffCount = 0
            for gy in 0..<grid {
                for gx in 0..<grid {
                    let px = (gx * width) / grid + width / (grid * 2)
                    let py = (gy * height) / grid + height / (grid * 2)
                    let offset = py * bytesPerRow + px * 4
                    let b = Float(buffer[offset]), g = Float(buffer[offset + 1]), r = Float(buffer[offset + 2])
                    let luma = (0.114 * b + 0.587 * g + 0.299 * r) / 255.0
                    sum += luma
                    sumSq += luma * luma
                    if luma > 0.97 { saturated += 1 }
                    if let previousLuma = previous {
                        diffSum += abs(luma - previousLuma)
                        diffCount += 1
                    }
                    previous = luma
                }
            }
            let n = Float(grid * grid)
            meanLuma = sum / n
            saturatedFraction = Float(saturated) / n
            localVariance = diffCount > 0 ? diffSum / Float(diffCount) : 0
        }
        CVPixelBufferUnlockBaseAddress(pixelBuffer, .readOnly)

        // blur: sharper frames show more local luma variation.
        let blur = min(localVariance * 12.0, 1)
        // glare: penalize saturated highlight patches.
        let glare = max(0, 1 - saturatedFraction * 12.0)
        // exposure: best around mid luma.
        let exposure = max(0, 1 - abs(meanLuma - 0.55) * 2.5)

        // coverage: normalized hand bounding-box area, scaled so a
        // well-framed palm (~35% of frame) scores 1.
        var minX: Float = 1, maxX: Float = 0, minY: Float = 1, maxY: Float = 0
        for p in landmarks {
            minX = min(minX, p.x); maxX = max(maxX, p.x)
            minY = min(minY, p.y); maxY = max(maxY, p.y)
        }
        let area = max(0, maxX - minX) * max(0, maxY - minY)
        let coverage = min(area / 0.35, 1)

        // stability: centroid movement vs the previous frame + pose confidence.
        let cx = (minX + maxX) / 2, cy = (minY + maxY) / 2
        var stability = min(max(confidence, 0), 1)
        if let previousCentroid {
            let movement = sqrtf(powf(cx - previousCentroid.x, 2) + powf(cy - previousCentroid.y, 2))
            stability = min(stability, max(0, 1 - movement * 20.0))
        }
        previousCentroid = (cx, cy)

        return gate.scoreFrame(blur: blur, glare: glare, exposure: exposure, coverage: coverage, stability: stability)
    }

    // MARK: - Vision → MediaPipe landmark mapping

    /// Maps Vision hand-pose joints onto the 21-point MediaPipe order used by
    /// the cross-platform contract:
    /// 0 wrist; 1-4 thumb CMC/MP/IP/tip; 5-8 index MCP/PIP/DIP/tip;
    /// 9-12 middle; 13-16 ring; 17-20 little.
    /// Vision uses a bottom-left origin; the contract uses top-left, so y is
    /// flipped. Vision provides no depth: z = 0.
    static func mapToMediaPipeLandmarks(_ observation: VNHumanHandPoseObservation) -> [LandmarkPoint]? {
        let order: [VNHumanHandPoseObservation.JointName] = [
            .wrist,
            .thumbCMC, .thumbMP, .thumbIP, .thumbTip,
            .indexMCP, .indexPIP, .indexDIP, .indexTip,
            .middleMCP, .middlePIP, .middleDIP, .middleTip,
            .ringMCP, .ringPIP, .ringDIP, .ringTip,
            .littleMCP, .littlePIP, .littleDIP, .littleTip,
        ]
        guard let points = try? observation.recognizedPoints(.all) else { return nil }
        var landmarks: [LandmarkPoint] = []
        landmarks.reserveCapacity(order.count)
        for joint in order {
            guard let point = points[joint], point.confidence > 0.2 else { return nil }
            landmarks.append(LandmarkPoint(
                x: Float(point.location.x),
                y: Float(1.0 - point.location.y),
                z: 0
            ))
        }
        return landmarks
    }
}

// MARK: - AVCaptureVideoDataOutputSampleBufferDelegate

extension HandPoseScanController: AVCaptureVideoDataOutputSampleBufferDelegate {

    func captureOutput(
        _ output: AVCaptureOutput,
        didOutput sampleBuffer: CMSampleBuffer,
        from connection: AVCaptureConnection
    ) {
        guard finishedSummary == nil,
              let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }
        process(pixelBuffer: pixelBuffer)
    }
}
