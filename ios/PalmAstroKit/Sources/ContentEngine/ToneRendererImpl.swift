import Foundation
import CoreContracts

/// Tone-aware plain-text renderer. Section labels and tone prefixes come from
/// content-templates.json for the payload's language — the engine emits no
/// hardcoded display strings. Tones keep their contract names
/// (SCIENTIFIC/HEALING/ROAST_SAFE); display names are a UI concern (PRD §45).
public final class ToneRendererImpl: Renderer {

    private let templates: ContentTemplates

    public init(templates: ContentTemplates) {
        self.templates = templates
    }

    public convenience init() throws {
        self.init(templates: try ContentTemplates.loadDefault())
    }

    public func render(payload: SemanticPayload, tone: Tone) -> RenderedReport {
        let (_, bundle) = templates.resolveLanguage(payload.language)
        let domainName = bundle.domains[payload.domain]?.displayName ?? payload.domain

        let tonePrefix = bundle.tonePrefixes[tone.rawValue] ?? ""
        let blindspotLabel = bundle.toneBlindspotLabels[tone.rawValue]
            ?? bundle.labels["blindspot"] ?? "blindspot"

        var lines: [String] = []
        lines.append("\(domainName) — \(payload.scoreCard.totalScore) / 100 — \(payload.scoreCard.grade)")
        lines.append("")
        lines.append("\(tonePrefix)\(payload.interpretation.pattern)")
        if !payload.interpretation.trigger.isEmpty {
            lines.append(payload.interpretation.trigger)
        }
        if !payload.interpretation.cost.isEmpty {
            lines.append(payload.interpretation.cost)
        }
        if !payload.blindspot.isEmpty {
            lines.append("")
            lines.append("\(blindspotLabel): \(payload.blindspot)")
        }
        lines.append("")
        lines.append("\(bundle.labels["actionToday"] ?? "today"): \(payload.actionToday)")
        lines.append("\(bundle.labels["actionWeek"] ?? "week"): \(payload.actionWeek)")
        lines.append("")
        lines.append("\(bundle.labels["prompt"] ?? "prompt"): \(payload.prompt)")
        for note in payload.safetyNotes {
            lines.append("\(bundle.labels["safety"] ?? "note"): \(note)")
        }

        return RenderedReport(domain: payload.domain, tone: tone, text: lines.joined(separator: "\n"))
    }
}
