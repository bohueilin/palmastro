import UIKit
import CoreHaptics

/// PalmAstro haptic vocabulary — the iOS mirror of the Android award-polish
/// ScanHaptics. Exactly four events, mapped identically across platforms
/// (PRD §38 native haptics; §41 feedback reinforces scan success):
///
/// - `tickCapture` — a single crisp, light tick when an angle is captured.
/// - `thumpQualityPass` — a fuller medium thump when the quality gate passes.
/// - `buzzQualityFail` — the system's gentle warning, used sparingly; a
///   quality fail is coaching, never an alarm (PRD §12.3).
/// - `shimmerReveal` — three softly ascending CoreHaptics transients as the
///   reading is revealed, with a graceful fallback to a plain success
///   notification when CoreHaptics is unavailable.
@MainActor
final class Haptics {

    static let shared = Haptics()

    private let lightImpact = UIImpactFeedbackGenerator(style: .light)
    private let mediumImpact = UIImpactFeedbackGenerator(style: .medium)
    private let notice = UINotificationFeedbackGenerator()
    private var engine: CHHapticEngine?

    private init() {}

    /// Warms the taptic hardware ahead of the scan flow to minimize latency.
    func prepare() {
        lightImpact.prepare()
        mediumImpact.prepare()
        notice.prepare()
    }

    /// Angle capture success — single crisp confirmation tick.
    func tickCapture() {
        lightImpact.impactOccurred()
    }

    /// Quality gate passed / scan session complete — slightly fuller thump.
    func thumpQualityPass() {
        mediumImpact.impactOccurred()
    }

    /// Quality-gate fail with retry coaching. Called at most once per gate
    /// evaluation by the scan flow; kept to the platform's gentlest warning
    /// so it reads as guidance, not error (PRD §12.3).
    func buzzQualityFail() {
        notice.notificationOccurred(.warning)
    }

    /// Reveal shimmer: three ascending transients. Falls back to a plain
    /// success notification when CoreHaptics is unsupported or fails.
    func shimmerReveal() {
        guard let player = makeShimmerPlayer() else {
            notice.notificationOccurred(.success)
            return
        }
        do {
            try player.start(atTime: CHHapticTimeImmediate)
        } catch {
            notice.notificationOccurred(.success)
        }
    }

    // MARK: - CoreHaptics plumbing

    private func makeShimmerPlayer() -> CHHapticPatternPlayer? {
        guard CHHapticEngine.capabilitiesForHardware().supportsHaptics else { return nil }
        do {
            let engine = try runningEngine()
            let events = (0..<3).map { step in
                CHHapticEvent(
                    eventType: .hapticTransient,
                    parameters: [
                        CHHapticEventParameter(parameterID: .hapticIntensity, value: 0.45 + 0.20 * Float(step)),
                        CHHapticEventParameter(parameterID: .hapticSharpness, value: 0.30 + 0.25 * Float(step)),
                    ],
                    relativeTime: 0.12 * Double(step)
                )
            }
            let pattern = try CHHapticPattern(events: events, parameters: [])
            return try engine.makePlayer(with: pattern)
        } catch {
            return nil
        }
    }

    private func runningEngine() throws -> CHHapticEngine {
        if let engine { return engine }
        let created = try CHHapticEngine()
        created.resetHandler = { [weak self] in
            Task { @MainActor in self?.engine = nil }
        }
        created.stoppedHandler = { [weak self] _ in
            Task { @MainActor in self?.engine = nil }
        }
        try created.start()
        engine = created
        return created
    }
}
