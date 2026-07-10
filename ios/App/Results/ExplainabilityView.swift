import SwiftUI
import CoreContracts

/// "Why this reading?" drawer (PRD §13.5): lists every contributing signal
/// with direction and relative weight, plus the calculation level and an
/// honest note about what the app can and cannot know.
struct ExplainabilityView: View {

    @Environment(\.dismiss) private var dismiss

    let payload: SemanticPayload

    var body: some View {
        NavigationStack {
            List {
                Section {
                    HStack {
                        Text("explain_calc_level_label").foregroundStyle(.secondary)
                        Spacer()
                        Text(payload.calcLevel == .L2 ? "explain_calc_level_l2" : "explain_calc_level_l1")
                    }
                    HStack {
                        Text("results_confidence_label").foregroundStyle(.secondary)
                        Spacer()
                        ConfidenceChip(confidence: payload.confidence)
                    }
                }

                if payload.explainability.isEmpty {
                    Section {
                        Text("explain_no_signals")
                            .foregroundStyle(.secondary)
                    }
                } else {
                    Section {
                        ForEach(Array(payload.explainability.enumerated()), id: \.offset) { _, entry in
                            HStack {
                                Image(systemName: entry.contribution >= 0 ? "plus.circle" : "minus.circle")
                                    .foregroundStyle(entry.contribution >= 0 ? .green : .orange)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(entry.signalId)
                                        .font(.subheadline.monospaced())
                                    ContributionBar(contribution: entry.contribution)
                                }
                            }
                            .accessibilityElement(children: .combine)
                            .accessibilityLabel(Text(entry.contribution >= 0 ? "explain_signal_positive_a11y" : "explain_signal_negative_a11y"))
                        }
                    } header: {
                        Text("explain_signals_header")
                    } footer: {
                        Text("explain_footer_disclaimer")
                    }
                }
            }
            .navigationTitle(Text("explain_title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button {
                        dismiss()
                    } label: {
                        Text("common_done")
                    }
                }
            }
        }
    }
}

struct ContributionBar: View {

    let contribution: Double

    var body: some View {
        GeometryReader { geometry in
            let magnitude = min(abs(contribution) / 4.0, 1.0)
            RoundedRectangle(cornerRadius: 2)
                .fill(contribution >= 0 ? Color.green.opacity(0.6) : Color.orange.opacity(0.7))
                .frame(width: max(geometry.size.width * magnitude, 4), height: 4)
        }
        .frame(height: 4)
    }
}
