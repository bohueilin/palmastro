import Foundation
import CoreContracts

// Mirrors engine-content/src/main/kotlin/com/palmastro/content/ToneRenderer.kt.

/// Renders a `SemanticPayload` to a plain-text report (no HTML) in the
/// payload's language, applying tone prefixes/labels from the versioned
/// template library (PRD §19, §45, §50). Tones keep their contract names
/// (SCIENTIFIC/HEALING/ROAST_SAFE); display names are a UI concern.
public final class ToneRendererImpl: Renderer {

    private let templates: ContentTemplates

    public init(templates: ContentTemplates) {
        self.templates = templates
    }

    public convenience init() throws {
        self.init(templates: try ContentTemplates.loadDefault())
    }

    public func render(payload: SemanticPayload, tone: Tone) -> RenderedReport {
        let lang = templates.resolveLanguage(payload.language)
        let toneTemplate = templates.tones[tone.rawValue]
        let prefix = toneTemplate.map { templates.localized($0.interpretationPrefix, language: lang) } ?? ""
        let blindspotLabel = toneTemplate.map { templates.localized($0.blindspotLabel, language: lang) } ?? ""
        let labels = templates.labels
        let domainName = templates.domains[payload.domain]
            .map { templates.localized($0.displayName, language: lang) }
            .flatMap { $0.isBlank ? nil : $0 }
            ?? payload.domain
        let grade = labels.grades[payload.scoreCard.grade]
            .map { templates.localized($0, language: lang) }
            .flatMap { $0.isBlank ? nil : $0 }
            ?? payload.scoreCard.grade

        var lines: [String] = []
        if grade.isBlank {
            lines.append("\(domainName) — \(payload.scoreCard.totalScore)/100")
        } else {
            lines.append("\(domainName) — \(payload.scoreCard.totalScore)/100 · \(grade)")
        }
        lines.append("")
        lines.append("\(prefix)\(payload.interpretation.pattern)")
        if !payload.interpretation.trigger.isBlank { lines.append(payload.interpretation.trigger) }
        if !payload.interpretation.cost.isBlank { lines.append(payload.interpretation.cost) }
        if !payload.blindspot.isBlank {
            lines.append("")
            lines.append("\(blindspotLabel)\(payload.blindspot)")
        }
        lines.append("")
        lines.append("\(templates.localized(labels.actionToday, language: lang))\(payload.actionToday)")
        lines.append("\(templates.localized(labels.actionWeek, language: lang))\(payload.actionWeek)")
        lines.append("")
        lines.append("\(templates.localized(labels.prompt, language: lang))\(payload.prompt)")
        for note in payload.safetyNotes {
            lines.append("")
            lines.append("\(templates.localized(labels.safetyNote, language: lang))\(note)")
        }

        return RenderedReport(domain: payload.domain, tone: tone, text: lines.joined(separator: "\n"))
    }
}
