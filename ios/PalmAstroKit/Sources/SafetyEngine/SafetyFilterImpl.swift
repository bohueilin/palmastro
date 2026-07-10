import Foundation
import CoreContracts
import ContentEngine

// Mirrors engine-content/src/main/kotlin/com/palmastro/content/SafetyFilterImpl.kt.

/// Rule-driven safety filter (PRD §30-§32). Rules load from the versioned
/// canonical `safety-rules.json`; every category is enforced on every domain
/// (strict_safety): medical/investment/guaranteed-money claims are blocked
/// cross-domain, and self-harm / identity attacks / fear-fate claims /
/// profanity are always blocked.
///
/// Matching: NFC normalization + zero-width stripping + fullwidth folding,
/// then zh terms as case-insensitive substrings and en terms as regex patterns
/// wrapped in explicit ASCII word boundaries (kills "cure"-in-"secure" and
/// "you have"-in-"you haven't" false positives). ASCII lookarounds —
/// `(?<![a-zA-Z0-9_])` / `(?![a-zA-Z0-9_])` — are used instead of `\b`
/// because ICU/Java treat CJK ideographs as word characters, so `\b` would
/// miss EN terms embedded in Chinese text ("damn you的態度").
public final class SafetyFilterImpl: SafetyFilter {

    private struct CompiledCategory {
        let id: String
        let zhTerms: [String]
        let enPatterns: [(raw: String, regex: NSRegularExpression)]
    }

    private let rules: SafetyRules
    private let templates: ContentTemplates
    private let compiled: [CompiledCategory]

    public init(rules: SafetyRules, templates: ContentTemplates) {
        self.rules = rules
        self.templates = templates
        self.compiled = rules.categories.map { category in
            CompiledCategory(
                id: category.id,
                zhTerms: category.zh.map(Self.normalize),
                enPatterns: category.en.compactMap { pattern in
                    let bounded = "(?<![a-zA-Z0-9_])(?:\(pattern))(?![a-zA-Z0-9_])"
                    guard let regex = try? NSRegularExpression(pattern: bounded, options: [.caseInsensitive]) else {
                        assertionFailure("safety-rules.json en pattern failed to compile: \(pattern)")
                        return nil
                    }
                    return (pattern, regex)
                }
            )
        }
    }

    /// Convenience initializer loading the bundled canonical rules + templates.
    public convenience init() throws {
        self.init(rules: try SafetyRules.loadDefault(), templates: try ContentTemplates.loadDefault())
    }

    // MARK: - SafetyFilter

    /// Scans every text field of the payload (Appendix B fields + observations + notes).
    public func validate(payload: SemanticPayload) -> SafetyCheckResult {
        var fields: [String] = [
            payload.interpretation.pattern,
            payload.interpretation.trigger,
            payload.interpretation.cost,
            payload.blindspot,
            payload.actionToday,
            payload.actionWeek,
            payload.prompt,
        ]
        for observation in payload.observations {
            fields.append(observation.displayName)
            fields.append(observation.evidenceSummary)
        }
        fields.append(contentsOf: payload.safetyNotes)

        var seen = Set<String>()
        var violations: [String] = []
        for violation in fields.flatMap(scan) where seen.insert(violation).inserted {
            violations.append(violation)
        }
        return SafetyCheckResult(passed: violations.isEmpty, violations: violations)
    }

    public func filter(rendered: RenderedReport) -> RenderedReport {
        filter(rendered: rendered, language: templates.defaultLanguage)
    }

    /// Language-aware overload: replaces a violating report with the localized
    /// safe fallback text from the template library. The interface method
    /// delegates here with the default language; the app pipeline should pass
    /// the payload's language.
    public func filter(rendered: RenderedReport, language: String) -> RenderedReport {
        guard !scan(rendered.text).isEmpty else { return rendered }
        let lang = templates.resolveLanguage(language)
        return rendered.copy(text: templates.localized(templates.fallback.filteredText, language: lang))
    }

    // MARK: - Matching

    private func scan(_ text: String) -> [String] {
        if text.isBlank { return [] }
        let normalized = Self.normalize(text)
        var violations: [String] = []
        for category in compiled {
            for term in category.zhTerms {
                if normalized.range(of: term, options: [.caseInsensitive]) != nil {
                    violations.append("\(category.id): \(term)")
                }
            }
            let nsRange = NSRange(normalized.startIndex..<normalized.endIndex, in: normalized)
            for (raw, regex) in category.enPatterns {
                if regex.firstMatch(in: normalized, options: [], range: nsRange) != nil {
                    violations.append("\(category.id): \(raw)")
                }
            }
        }
        return violations
    }

    // MARK: - Normalization (Kotlin parity: no lowercasing — matching is
    // case-insensitive at comparison time)

    private static let zeroWidthScalars: Set<Unicode.Scalar> = [
        "\u{200B}", "\u{200C}", "\u{200D}", "\u{FEFF}", "\u{00AD}",
    ]

    static func normalize(_ text: String) -> String {
        // 1. Strip zero-width / invisible characters.
        let stripped = String(String.UnicodeScalarView(
            text.unicodeScalars.filter { !zeroWidthScalars.contains($0) }
        ))
        // 2. NFC normalization.
        let nfc = stripped.precomposedStringWithCanonicalMapping
        // 3. Fullwidth ASCII folding (U+FF01..U+FF5E -> U+0021..U+007E).
        var folded = String.UnicodeScalarView()
        for scalar in nfc.unicodeScalars {
            if (0xFF01...0xFF5E).contains(scalar.value),
               let ascii = Unicode.Scalar(scalar.value - 0xFEE0) {
                folded.append(ascii)
            } else {
                folded.append(scalar)
            }
        }
        return String(folded)
    }
}

private extension String {
    /// Kotlin `isBlank()` parity: empty or whitespace-only.
    var isBlank: Bool { allSatisfy { $0.isWhitespace } }
}
