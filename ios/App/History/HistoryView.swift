import SwiftUI
import CoreContracts

/// Month-by-month history (PRD §54), the iOS mirror of Android's
/// HistoryScreen. Reached from the Results screen rather than a fourth tab:
/// the tab bar stays at three, matching Android's information architecture.
/// Read-only for launch — tapping a month is deliberately not wired up,
/// because ResultsView renders `latestResult` only.
struct HistoryView: View {

    @EnvironmentObject private var model: AppModel

    /// Domain order shown on every card, matching Results.
    fileprivate static let orderedDomains = [
        Domains.career, Domains.wealth, Domains.family, Domains.health,
    ]

    var body: some View {
        Group {
            if model.history.isEmpty {
                emptyState
            } else {
                monthList
            }
        }
        .navigationTitle(Text("history_title"))
        .navigationBarTitleDisplayMode(.inline)
    }

    private var monthList: some View {
        List {
            if model.history.count == 1 {
                Section {
                    Text("history_single_record")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            ForEach(Array(model.history.enumerated()), id: \.element.resultId) { index, result in
                Section {
                    MonthCard(result: result, deltas: deltas(at: index))
                }
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 12) {
            Image(systemName: "calendar")
                .font(.system(size: 48))
                .foregroundStyle(.secondary)
            Text("history_empty_title")
                .font(.headline)
                .accessibilityAddTraits(.isHeader)
            Text("history_empty_desc")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(32)
    }

    /// Signed change per domain against the next-older record. Recomputed here
    /// rather than read from the stored ScoreCard delta: that one was computed
    /// once at scan time against whatever record happened to be previous then,
    /// so it does not stay consistent down the list. Mirrors HistoryViewModel,
    /// which pairs element i with element i+1 of the newest-first list.
    private func deltas(at index: Int) -> [String: Int] {
        guard index + 1 < model.history.count else { return [:] }
        let previous = model.history[index + 1].scoringResult.domainScores
        return model.history[index].scoringResult.domainScores.reduce(into: [:]) { result, entry in
            guard let previousScore = previous[entry.key] else { return }
            result[entry.key] = entry.value - previousScore
        }
    }
}

/// One month: key, grade chip, per-domain meter with signed delta, confidence.
private struct MonthCard: View {

    let result: MonthlyResult
    let deltas: [String: Int]

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(result.monthKey)
                    .font(.headline)
                    .monospacedDigit()
                Spacer()
                gradeChip
            }
            ForEach(HistoryView.orderedDomains, id: \.self) { domain in
                domainRow(domain)
            }
            HStack(spacing: 6) {
                Text("results_confidence_label").foregroundStyle(.secondary)
                ConfidenceLabel(confidence: result.scoringResult.confidence)
            }
            .font(.caption)
        }
        .padding(.vertical, 4)
    }

    /// Solid chip with the paired on-color: the raw grade color on a low-alpha
    /// tint cannot hold 4.5:1 for text this small (same call Android made).
    private var gradeChip: some View {
        Text(verbatim: gradeDisplayName(result.scoringResult.grade))
            .font(.caption.weight(.medium))
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(gradeColor(result.scoringResult.grade), in: Capsule())
            .foregroundStyle(onGradeColor(result.scoringResult.grade))
    }

    private func domainRow(_ domain: String) -> some View {
        let score = result.scoringResult.domainScores[domain] ?? 0
        let delta = deltas[domain]
        return HStack(spacing: 8) {
            Text(domainNameKey(domain))
                .font(.caption)
                .frame(width: 56, alignment: .leading)
            ProgressView(value: Double(score) / 100.0)
                .tint(gradeColor(result.scoringResult.grade))
            Text(verbatim: "\(score)")
                .font(.caption.weight(.medium).monospacedDigit())
                .frame(width: 32, alignment: .trailing)
            deltaText(delta)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(verbatim: rowAccessibilityLabel(domain: domain, score: score, delta: delta)))
    }

    /// Glyph plus signed number, so direction is never color-only.
    @ViewBuilder
    private func deltaText(_ delta: Int?) -> some View {
        let (text, color): (String, Color) = {
            guard let delta else { return ("", .secondary) }
            if delta > 0 { return ("▲+\(delta)", BrandPalette.deltaPositive) }
            if delta < 0 { return ("▼\(delta)", BrandPalette.deltaNegative) }
            return ("—", BrandPalette.deltaNeutral)
        }()
        Text(verbatim: text)
            .font(.caption.weight(.semibold).monospacedDigit())
            .foregroundStyle(color)
            .frame(width: 48, alignment: .trailing)
    }

    private func rowAccessibilityLabel(domain: String, score: Int, delta: Int?) -> String {
        let name = domainDisplayName(domain)
        var parts = ["\(name) \(score)"]
        if let delta {
            parts.append(String(
                format: NSLocalizedString("history_delta_desc", comment: "Month-over-month change"),
                name, delta
            ))
        }
        return parts.joined(separator: ", ")
    }

    private func domainNameKey(_ domain: String) -> LocalizedStringKey {
        switch domain {
        case Domains.career: return "domain_career"
        case Domains.wealth: return "domain_wealth"
        case Domains.family: return "domain_family"
        case Domains.health: return "domain_health"
        default: return LocalizedStringKey(domain)
        }
    }

    private func domainDisplayName(_ domain: String) -> String {
        switch domain {
        case Domains.career: return NSLocalizedString("domain_career", comment: "")
        case Domains.wealth: return NSLocalizedString("domain_wealth", comment: "")
        case Domains.family: return NSLocalizedString("domain_family", comment: "")
        case Domains.health: return NSLocalizedString("domain_health", comment: "")
        default: return domain
        }
    }
}
