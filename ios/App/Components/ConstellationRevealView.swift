import SwiftUI

/// The signature processing moment — the iOS mirror of the Android
/// award-polish constellation reveal. Four glowing palm lines (heart, head,
/// life, fate) draw themselves across a night-sky card, then key points
/// along them ignite as constellation stars joined by faint threads. Calm
/// smoothstep easing throughout, played once, no loops and no urgency.
///
/// Under Reduce Motion the final frame renders immediately as a static
/// illustration and `onFinished` fires after a short hold, so progress is
/// clarified but never performed (PRD §40, §41). The canvas itself is
/// decorative: it is hidden from assistive tech — the sibling
/// "Reading your palm…" text in ScanView carries the meaning.
struct ConstellationRevealView: View {

    /// Called once, after the reveal completes (or after a short hold under
    /// Reduce Motion). ScanView uses this to fire the reveal haptic and
    /// hand off to the results surface.
    var onFinished: (() -> Void)?

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var startDate = Date()

    /// Full choreography length; lines finish ~60% in, stars after.
    private static let duration: TimeInterval = 2.8
    /// Static-frame hold under Reduce Motion before finishing.
    private static let reducedMotionHold: TimeInterval = 0.8

    var body: some View {
        Group {
            if reduceMotion {
                revealCanvas(progress: 1)
            } else {
                TimelineView(.animation) { timeline in
                    let elapsed = timeline.date.timeIntervalSince(startDate)
                    revealCanvas(progress: min(max(elapsed / Self.duration, 0), 1))
                }
            }
        }
        .background(nightSkyCard)
        .accessibilityHidden(true)
        .onAppear { startDate = Date() }
        .task {
            let seconds = reduceMotion ? Self.reducedMotionHold : Self.duration + 0.2
            try? await Task.sleep(nanoseconds: UInt64(seconds * 1_000_000_000))
            onFinished?()
        }
    }

    // MARK: - Card + canvas

    private var nightSkyCard: some View {
        RoundedRectangle(cornerRadius: 24, style: .continuous)
            .fill(LinearGradient(
                colors: [BrandPalette.nightSkyHigh, BrandPalette.nightSky],
                startPoint: .top,
                endPoint: .bottom
            ))
    }

    private func revealCanvas(progress: Double) -> some View {
        Canvas { context, size in
            drawPalmLines(context: context, size: size, progress: progress)
            drawConstellation(context: context, size: size, progress: progress)
        }
        .padding(14)
    }

    // MARK: - Palm lines

    private func drawPalmLines(context: GraphicsContext, size: CGSize, progress: Double) {
        for (index, line) in Self.palmLines.enumerated() {
            let reveal = Self.smoothstep((progress - 0.10 * Double(index)) / 0.38)
            guard reveal > 0 else { continue }
            let path = Self.linePath(line, in: size).trimmedPath(from: 0, to: reveal)

            var glow = context
            glow.addFilter(.blur(radius: 5))
            glow.stroke(
                path,
                with: .color(BrandPalette.calmTeal.opacity(0.55 * reveal)),
                style: StrokeStyle(lineWidth: 5, lineCap: .round)
            )
            context.stroke(
                path,
                with: .linearGradient(
                    Gradient(colors: [BrandPalette.calmTeal, BrandPalette.royalPurple]),
                    startPoint: Self.scaled(line.start, size),
                    endPoint: Self.scaled(line.end, size)
                ),
                style: StrokeStyle(lineWidth: 2, lineCap: .round)
            )
        }
    }

    // MARK: - Constellation

    private func drawConstellation(context: GraphicsContext, size: CGSize, progress: Double) {
        drawThreads(context: context, size: size, progress: progress)
        for (index, star) in Self.starPoints.enumerated() {
            let ignite = Self.smoothstep((progress - 0.55 - 0.03 * Double(index)) / 0.18)
            guard ignite > 0 else { continue }
            let center = Self.scaled(star, size)
            let radius = 2.0 + 1.6 * ignite
            let dot = CGRect(
                x: center.x - radius, y: center.y - radius,
                width: radius * 2, height: radius * 2
            )

            var glow = context
            glow.addFilter(.blur(radius: 4))
            glow.fill(
                Path(ellipseIn: dot.insetBy(dx: -2.5, dy: -2.5)),
                with: .color(BrandPalette.royalPurple.opacity(0.70 * ignite))
            )
            context.fill(Path(ellipseIn: dot), with: .color(BrandPalette.starlight.opacity(ignite)))
        }
    }

    /// Faint threads joining stars across lines — the constellation forms
    /// only after the stars have ignited.
    private func drawThreads(context: GraphicsContext, size: CGSize, progress: Double) {
        let alpha = 0.35 * Self.smoothstep((progress - 0.80) / 0.15)
        guard alpha > 0 else { return }
        for (from, to) in Self.threads {
            var path = Path()
            path.move(to: Self.scaled(Self.starPoints[from], size))
            path.addLine(to: Self.scaled(Self.starPoints[to], size))
            context.stroke(
                path,
                with: .color(BrandPalette.starlight.opacity(alpha)),
                style: StrokeStyle(lineWidth: 0.8)
            )
        }
    }

    // MARK: - Geometry (normalized 0…1 coordinates)

    private struct PalmLine {
        let start: CGPoint
        let control: CGPoint
        let end: CGPoint
    }

    /// Heart, head, life, fate — stylized, not anatomical (thin-line premium
    /// iconography per PRD §37).
    private static let palmLines: [PalmLine] = [
        PalmLine(start: CGPoint(x: 0.12, y: 0.32), control: CGPoint(x: 0.48, y: 0.16), end: CGPoint(x: 0.86, y: 0.30)),
        PalmLine(start: CGPoint(x: 0.14, y: 0.48), control: CGPoint(x: 0.50, y: 0.46), end: CGPoint(x: 0.82, y: 0.58)),
        PalmLine(start: CGPoint(x: 0.32, y: 0.24), control: CGPoint(x: 0.14, y: 0.60), end: CGPoint(x: 0.40, y: 0.90)),
        PalmLine(start: CGPoint(x: 0.58, y: 0.90), control: CGPoint(x: 0.54, y: 0.55), end: CGPoint(x: 0.62, y: 0.20)),
    ]

    /// Three stars per line: near-start, midpoint, near-end.
    private static let starPoints: [CGPoint] = palmLines.flatMap { line in
        [bezierPoint(line, 0.06), bezierPoint(line, 0.5), bezierPoint(line, 0.94)]
    }

    /// Star-index pairs joined by faint threads (indices into `starPoints`).
    private static let threads: [(Int, Int)] = [(2, 11), (1, 4), (5, 10), (6, 0)]

    private static func linePath(_ line: PalmLine, in size: CGSize) -> Path {
        var path = Path()
        path.move(to: scaled(line.start, size))
        path.addQuadCurve(to: scaled(line.end, size), control: scaled(line.control, size))
        return path
    }

    private static func bezierPoint(_ line: PalmLine, _ t: CGFloat) -> CGPoint {
        let mt = 1 - t
        return CGPoint(
            x: mt * mt * line.start.x + 2 * mt * t * line.control.x + t * t * line.end.x,
            y: mt * mt * line.start.y + 2 * mt * t * line.control.y + t * t * line.end.y
        )
    }

    private static func scaled(_ point: CGPoint, _ size: CGSize) -> CGPoint {
        CGPoint(x: point.x * size.width, y: point.y * size.height)
    }

    /// Clamped smoothstep — the calm ease used for every phase.
    private static func smoothstep(_ x: Double) -> Double {
        let t = min(max(x, 0), 1)
        return t * t * (3 - 2 * t)
    }
}
