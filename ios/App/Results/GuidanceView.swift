import SwiftUI
import CoreContracts
import ContentEngine

/// "Understand your reading" (launch guidance layer, PRD §11-§13, §30-§32):
/// a calm month-theme hero, "Lean into" strength cards, "Be mindful of" cards
/// in calm tertiary styling — never alarm-red — a gentle week list, and a
/// safety footer. All copy comes from the reviewed template library via the
/// deterministic GuidanceBuilder: positivity-first, action-oriented, no fear,
/// no medical/financial/deterministic claims. Mirrors the Android
/// GuidanceScreen structure and strings.
struct GuidanceView: View {

    @EnvironmentObject private var model: AppModel

    let result: MonthlyResult

    var body: some View {
        Group {
            if let guidance = model.guidance(for: result), !guidance.isEmpty {
                guidanceList(guidance)
            } else {
                emptyState
            }
        }
        .navigationTitle(Text("guidance_title"))
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            model.analytics.emit(eventName: "guidance_view", props: [:])
        }
    }

    private func guidanceList(_ guidance: Guidance) -> some View {
        List {
            if !guidance.monthTheme.isEmpty {
                Section {
                    VStack(alignment: .leading, spacing: 8) {
                        Label {
                            Text("guidance_theme_label")
                                .font(.caption.bold())
                                .foregroundStyle(.tint)
                        } icon: {
                            Image(systemName: "sparkles")
                                .foregroundStyle(.tint)
                                .accessibilityHidden(true)
                        }
                        Text(guidance.monthTheme)
                            .font(.title3.weight(.semibold))
                            .fixedSize(horizontal: false, vertical: true)
                        Text(result.monthKey)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .monospacedDigit()
                    }
                    .padding(.vertical, 8)
                    .accessibilityElement(children: .combine)
                }
            }

            if !guidance.strengths.isEmpty {
                Section {
                    ForEach(Array(guidance.strengths.enumerated()), id: \.offset) { _, item in
                        GuidanceCard(
                            item: item, icon: "leaf",
                            accent: BrandPalette.gradeGrowing, kindKey: "guidance_lean_into_header"
                        )
                    }
                } header: {
                    Text("guidance_lean_into_header")
                } footer: {
                    Text("guidance_lean_into_footer")
                        .font(.footnote)
                }
            }

            if !guidance.mindful.isEmpty {
                // Calm tertiary styling by design: soft indigo + secondary
                // text, never alarm-red (PRD §12.3).
                Section {
                    ForEach(Array(guidance.mindful.enumerated()), id: \.offset) { _, item in
                        GuidanceCard(
                            item: item, icon: "moon.stars",
                            accent: BrandPalette.gradeBuilding, kindKey: "guidance_mindful_header"
                        )
                    }
                } header: {
                    Text("guidance_mindful_header")
                } footer: {
                    Text("guidance_mindful_footer")
                        .font(.footnote)
                }
            }

            if !guidance.weekPlan.isEmpty {
                Section {
                    ForEach(Array(guidance.weekPlan.enumerated()), id: \.offset) { _, line in
                        Label {
                            Text(line)
                                .font(.subheadline)
                                .fixedSize(horizontal: false, vertical: true)
                        } icon: {
                            Image(systemName: "calendar")
                                .foregroundStyle(.secondary)
                        }
                        .accessibilityElement(children: .combine)
                    }
                } header: {
                    Text("guidance_week_header")
                } footer: {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("guidance_week_footer")
                        Text("guidance_footer_reflection")
                        Text("guidance_disclaimer")
                    }
                    .font(.footnote)
                }
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "sparkles")
                .font(.system(size: 44))
                .foregroundStyle(.secondary)
            Text("guidance_not_found")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Text("guidance_disclaimer")
                .font(.footnote)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding()
    }
}

/// One guidance card: title, calm body copy, and a small "Try this" action.
private struct GuidanceCard: View {

    let item: GuidanceItem
    let icon: String
    let accent: Color
    /// VoiceOver prefix ("Lean into" / "Be mindful of").
    let kindKey: LocalizedStringKey

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Label {
                Text(item.title).font(.headline)
            } icon: {
                Image(systemName: icon)
                    .foregroundStyle(accent)
                    .accessibilityHidden(true)
            }
            Text(item.body)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
            if !item.action.isEmpty {
                HStack(alignment: .firstTextBaseline, spacing: 6) {
                    Image(systemName: "arrow.turn.down.right")
                        .font(.caption)
                        .foregroundStyle(accent)
                        .accessibilityHidden(true)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("guidance_action_label")
                            .font(.caption.bold())
                            .foregroundStyle(accent)
                        Text(item.action)
                            .font(.footnote)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
                .padding(.top, 2)
            }
        }
        .padding(.vertical, 4)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(Text(kindKey))
        .accessibilityValue(Text(verbatim: "\(item.title). \(item.body) \(item.action)"))
    }
}

/// Entry card on the results dashboard (mirrors the Android placement between
/// the summary section and the domain cards): month theme plus the first
/// strength and first mindful item, navigating to the full guidance screen.
struct GuidanceEntryCard: View {

    let guidance: Guidance
    let result: MonthlyResult

    var body: some View {
        NavigationLink {
            GuidanceView(result: result)
        } label: {
            VStack(alignment: .leading, spacing: 4) {
                Text("guidance_entry_title")
                    .font(.caption.bold())
                    .foregroundStyle(.tint)
                if !guidance.monthTheme.isEmpty {
                    Text(guidance.monthTheme)
                        .font(.subheadline.weight(.semibold))
                        .lineLimit(2)
                }
                if let strength = guidance.strengths.first?.title, !strength.isEmpty {
                    Text(String(format: NSLocalizedString("guidance_entry_strength", comment: ""), strength))
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
                if let mindful = guidance.mindful.first?.title, !mindful.isEmpty {
                    Text(String(format: NSLocalizedString("guidance_entry_mindful", comment: ""), mindful))
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }
            .padding(.vertical, 4)
            .accessibilityElement(children: .combine)
            .accessibilityHint(Text("guidance_entry_open"))
        }
    }
}
