import SwiftUI
import CoreContracts

/// Results dashboard (PRD §13.3): four domain cards with score, grade, delta
/// arrow, confidence chip, and entry points to detail + a new scan.
struct ResultsView: View {

    @EnvironmentObject private var model: AppModel
    @State private var showScan = false

    var body: some View {
        Group {
            if let result = model.latestResult {
                resultList(result)
            } else {
                emptyState
            }
        }
        .navigationTitle(Text("results_title"))
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button {
                    showScan = true
                } label: {
                    Label("results_new_scan", systemImage: "camera.viewfinder")
                }
            }
        }
        .fullScreenCover(isPresented: $showScan) {
            ScanView()
        }
        .onAppear {
            model.analytics.emit(eventName: "results_view", props: [:])
        }
    }

    private func resultList(_ result: MonthlyResult) -> some View {
        List {
            Section {
                HStack {
                    Text("results_month_label").foregroundStyle(.secondary)
                    Spacer()
                    Text(result.monthKey).monospacedDigit()
                }
                HStack {
                    Text("results_confidence_label").foregroundStyle(.secondary)
                    Spacer()
                    ConfidenceChip(confidence: result.scoringResult.confidence)
                }
            }

            // Guidance entry card (mirrors the Android placement between the
            // summary section and the domain cards; hidden when the guidance
            // library yields nothing, matching Android).
            if let guidance = model.guidance(for: result), !guidance.isEmpty {
                Section {
                    GuidanceEntryCard(guidance: guidance, result: result)
                }
            }

            Section {
                ForEach(Domains.all, id: \.self) { domain in
                    if let payload = result.semanticPayloads[domain] {
                        NavigationLink(value: domain) {
                            DomainCard(payload: payload)
                        }
                    }
                }
            } footer: {
                Text("results_disclaimer")
                    .font(.footnote)
            }
        }
        .navigationDestination(for: String.self) { domain in
            if let payload = model.latestResult?.semanticPayloads[domain] {
                DomainDetailView(payload: payload)
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 16) {
            Image(systemName: "hand.raised.fingers.spread")
                .font(.system(size: 56))
                .foregroundStyle(.secondary)
            Text("results_empty_title").font(.headline)
            Text("results_empty_body")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Button {
                showScan = true
            } label: {
                Text("results_start_first_scan").frame(minHeight: 44)
            }
            .buttonStyle(.borderedProminent)
        }
        .padding()
    }
}

struct DomainCard: View {

    let payload: SemanticPayload

    var body: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(domainNameKey(payload.domain)).font(.headline)
                Text(payload.scoreCard.grade).font(.subheadline).foregroundStyle(.secondary)
            }
            Spacer()
            if let delta = payload.scoreCard.delta, delta.value != 0 {
                Label {
                    Text(verbatim: "\(abs(delta.value))")
                } icon: {
                    Image(systemName: delta.arrow == "up" ? "arrow.up" : "arrow.down")
                }
                .font(.subheadline)
                .foregroundStyle(delta.arrow == "up" ? .green : .orange)
                .accessibilityLabel(Text(delta.arrow == "up" ? "results_delta_up_a11y" : "results_delta_down_a11y"))
            }
            Text(verbatim: "\(payload.scoreCard.totalScore)")
                .font(.title2.bold().monospacedDigit())
        }
        .padding(.vertical, 6)
        .accessibilityElement(children: .combine)
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

struct ConfidenceChip: View {

    let confidence: String

    var body: some View {
        Text(labelKey)
            .font(.caption.bold())
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(color.opacity(0.18), in: Capsule())
            .foregroundStyle(color)
    }

    private var labelKey: LocalizedStringKey {
        switch confidence {
        case "high": return "confidence_high"
        case "med": return "confidence_med"
        default: return "confidence_low"
        }
    }

    private var color: Color {
        switch confidence {
        case "high": return .green
        case "med": return .blue
        default: return .orange
        }
    }
}
