import SwiftUI
import CoreContracts
import DataStore

/// Domain detail (PRD §13.4): tone-rendered interpretation, observations,
/// blindspot, actions, reflection prompt, safety notes, an explainability
/// drawer ("Why this reading?", PRD §13.5) and a journal entry field.
struct DomainDetailView: View {

    @EnvironmentObject private var model: AppModel

    let payload: SemanticPayload

    @State private var showExplainability = false
    @State private var journalText = ""
    @State private var journalSaved = false

    var body: some View {
        List {
            Section {
                HStack {
                    Text(verbatim: "\(payload.scoreCard.totalScore)")
                        .font(.system(size: 44, weight: .bold).monospacedDigit())
                    VStack(alignment: .leading) {
                        Text(payload.scoreCard.grade).font(.headline)
                        ConfidenceChip(confidence: payload.confidence)
                    }
                    Spacer()
                    if let delta = payload.scoreCard.delta, delta.value != 0 {
                        Label {
                            Text(verbatim: "\(abs(delta.value))")
                        } icon: {
                            Image(systemName: delta.arrow == "up" ? "arrow.up" : "arrow.down")
                        }
                        .foregroundStyle(delta.arrow == "up" ? .green : .orange)
                    }
                }
                .accessibilityElement(children: .combine)

                if !payload.confidenceReasons.isEmpty {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("detail_confidence_reasons_title").font(.footnote.bold())
                        ForEach(payload.confidenceReasons, id: \.self) { reason in
                            Text(confidenceReasonKey(reason)).font(.footnote).foregroundStyle(.secondary)
                        }
                    }
                }
            }

            Section {
                Text(payload.interpretation.pattern)
                if !payload.interpretation.trigger.isEmpty {
                    Text(payload.interpretation.trigger).foregroundStyle(.secondary)
                }
                if !payload.interpretation.cost.isEmpty {
                    Text(payload.interpretation.cost).foregroundStyle(.secondary)
                }
            } header: {
                Text("detail_interpretation_header")
            }

            if !payload.observations.isEmpty {
                Section {
                    ForEach(payload.observations, id: \.signalId) { observation in
                        VStack(alignment: .leading, spacing: 2) {
                            Text(observation.displayName)
                            Text(observation.evidenceSummary).font(.footnote).foregroundStyle(.secondary)
                        }
                    }
                } header: {
                    Text("detail_observations_header")
                }
            }

            Section {
                Text(payload.blindspot)
            } header: {
                Text("detail_blindspot_header")
            }

            Section {
                Label(payload.actionToday, systemImage: "sun.max")
                Label(payload.actionWeek, systemImage: "calendar")
            } header: {
                Text("detail_actions_header")
            }

            Section {
                Text(payload.prompt).italic()
                TextField("detail_journal_placeholder", text: $journalText, axis: .vertical)
                    .lineLimit(3...6)
                Button {
                    saveJournalEntry()
                } label: {
                    Text(journalSaved ? "detail_journal_saved" : "detail_journal_save")
                        .frame(minHeight: 44)
                }
                .disabled(journalText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || journalSaved)
            } header: {
                Text("detail_prompt_header")
            }

            if !payload.safetyNotes.isEmpty {
                Section {
                    ForEach(payload.safetyNotes, id: \.self) { note in
                        Label(note, systemImage: "info.circle")
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }
                }
            }

            Section {
                Button {
                    model.analytics.emit(eventName: "why_drawer_open", props: ["domain": payload.domain])
                    showExplainability = true
                } label: {
                    Label("detail_why_button", systemImage: "questionmark.circle")
                        .frame(minHeight: 44)
                }
            }
        }
        .navigationTitle(Text(domainNameKey(payload.domain)))
        .sheet(isPresented: $showExplainability) {
            ExplainabilityView(payload: payload)
                .presentationDetents([.medium, .large])
        }
        .onAppear {
            model.analytics.emit(eventName: "domain_card_tap", props: ["domain": payload.domain])
        }
    }

    private func saveJournalEntry() {
        let text = journalText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return }
        let entry = JournalEntry(
            entryId: UUID().uuidString,
            monthKey: payload.monthKey,
            domain: payload.domain,
            text: text,
            createdAt: Int64(Date().timeIntervalSince1970 * 1000)
        )
        try? model.journalRepository.save(entry)
        journalSaved = true
        // Metadata only — journal text never enters analytics (Appendix C).
        model.analytics.emit(eventName: "journal_saved", props: ["domain": payload.domain])
    }

    private func confidenceReasonKey(_ reason: String) -> LocalizedStringKey {
        switch reason {
        case "scan_quality_low": return "confidence_reason_scan_quality_low"
        case "low_feature_coverage": return "confidence_reason_low_feature_coverage"
        case "missing_birth_time": return "confidence_reason_missing_birth_time"
        case "safety_fallback": return "confidence_reason_safety_fallback"
        default: return LocalizedStringKey(reason)
        }
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
}
