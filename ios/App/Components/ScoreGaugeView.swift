import SwiftUI

/// Circular score gauge — the iOS mirror of the Android award-polish
/// ScoreGauge. A 270-degree arc sweeps 0–100 through a calm teal-to-purple
/// angular gradient with rounded caps around a centered tabular numeral and
/// grade label. The sweep animates with a gentle spring and renders already
/// settled when Reduce Motion is on (PRD §40, §41). Exposed to assistive
/// technology as ONE element: "<title> score <n> of 100, grade <grade>."
struct ScoreGaugeView: View {

    enum Style {
        case hero
        case compact

        var diameter: CGFloat { self == .hero ? 140 : 96 }
        var lineWidth: CGFloat { self == .hero ? 12 : 8 }

        var scoreFont: Font {
            self == .hero
                ? .system(size: 40, weight: .bold, design: .rounded)
                : .system(size: 26, weight: .bold, design: .rounded)
        }

        var gradeFont: Font { self == .hero ? .subheadline : .caption }
    }

    let score: Int
    let grade: String
    /// Already-localized display name, used only in the accessibility label.
    let titleText: String
    var style: Style = .hero

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var sweep: Double = 0

    /// Fraction of the circle the open ring occupies (270°).
    private static let trackFraction: Double = 0.75
    /// Rotation that centers the ring's opening at the bottom.
    private static let ringRotation: Angle = .degrees(135)

    private var targetFraction: Double {
        Double(min(max(score, 0), 100)) / 100.0
    }

    var body: some View {
        ZStack {
            track
            progressArc
            centerLabel
        }
        .frame(width: style.diameter, height: style.diameter)
        .onAppear { animateSweep() }
        .onChange(of: score) { _, _ in animateSweep() }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(verbatim: accessibilitySummary))
    }

    // MARK: - Layers

    private var track: some View {
        Circle()
            .trim(from: 0, to: Self.trackFraction)
            .stroke(Color.secondary.opacity(0.18), style: strokeStyle)
            .rotationEffect(Self.ringRotation)
    }

    private var progressArc: some View {
        Circle()
            .trim(from: 0, to: Self.trackFraction * sweep)
            .stroke(arcGradient, style: strokeStyle)
            .rotationEffect(Self.ringRotation)
    }

    private var centerLabel: some View {
        VStack(spacing: 2) {
            Text(verbatim: "\(score)")
                .font(style.scoreFont)
                .monospacedDigit()
                .minimumScaleFactor(0.5)
                .lineLimit(1)
            Text(verbatim: gradeDisplayName(grade))
                .font(style.gradeFont)
                .foregroundStyle(.secondary)
                .lineLimit(1)
        }
        .padding(style.lineWidth)
    }

    // MARK: - Styling

    private var strokeStyle: StrokeStyle {
        StrokeStyle(lineWidth: style.lineWidth, lineCap: .round)
    }

    private var arcGradient: AngularGradient {
        AngularGradient(
            gradient: Gradient(colors: [BrandPalette.calmTeal, BrandPalette.royalPurple]),
            center: .center,
            startAngle: .degrees(0),
            endAngle: .degrees(360 * Self.trackFraction)
        )
    }

    // MARK: - Accessibility + motion

    private var accessibilitySummary: String {
        String(
            format: NSLocalizedString("gauge_a11y_format", comment: "Score gauge accessibility summary"),
            titleText, score, gradeDisplayName(grade)
        )
    }

    private func animateSweep() {
        if reduceMotion {
            sweep = targetFraction
            return
        }
        sweep = 0
        withAnimation(.spring(response: 0.9, dampingFraction: 0.85)) {
            sweep = targetFraction
        }
    }
}
