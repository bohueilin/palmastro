import Foundation
import CoreContracts

/// Safety filter (PRD §30, §57; EXECUTION_SPEC safety pipeline).
///
/// Text normalization before matching (adversarial-input hardening, parity
/// with the Android SafetyFilterImpl):
/// 1. zero-width characters stripped (U+200B/C/D, U+FEFF, U+00AD),
/// 2. Unicode NFC normalization,
/// 3. fullwidth ASCII folding (U+FF01..U+FF5E -> ASCII),
/// 4. lowercasing for case-insensitive comparison.
///
/// Matching: ASCII terms match on word boundaries ("cure" does not match
/// "secure"); CJK terms match as substrings.
public final class SafetyFilterImpl: SafetyFilter {

    private let rules: SafetyRules
    private let asciiRegexCache: [String: NSRegularExpression]

    public init(rules: SafetyRules) {
        self.rules = rules
        var cache: [String: NSRegularExpression] = [:]
        for category in rules.categories {
            for term in category.terms where Self.isASCIITerm(term) {
                let normalized = Self.normalize(term)
                if cache[normalized] == nil {
                    let pattern = "\\b" + NSRegularExpression.escapedPattern(for: normalized) + "\\b"
                    cache[normalized] = try? NSRegularExpression(pattern: pattern, options: [])
                }
            }
        }
        self.asciiRegexCache = cache
    }

    /// Convenience initializer loading the bundled default rules.
    public convenience init() throws {
        self.init(rules: try SafetyRules.loadDefault())
    }

    // MARK: - SafetyFilter

    public func validate(payload: SemanticPayload) -> SafetyCheckResult {
        let rawText = [
            payload.interpretation.pattern,
            payload.interpretation.trigger,
            payload.interpretation.cost,
            payload.blindspot,
            payload.actionToday,
            payload.actionWeek,
            payload.prompt,
        ].joined(separator: " ")
        let text = Self.normalize(rawText)

        var violations: [String] = []
        for category in rules.categories {
            let applies = category.crossDomain || category.appliesToDomains.contains(payload.domain)
            guard applies else { continue }
            for term in category.terms where matches(term: term, inNormalizedText: text) {
                violations.append("\(category.id): \(term)")
            }
        }
        return SafetyCheckResult(passed: violations.isEmpty, violations: violations)
    }

    public func filter(rendered: RenderedReport) -> RenderedReport {
        filter(rendered: rendered, language: rules.fallbackLanguage)
    }

    /// Language-aware variant: replacement copy is drawn from the rules file
    /// for the given language.
    public func filter(rendered: RenderedReport, language: String) -> RenderedReport {
        let text = Self.normalize(rendered.text)
        for category in rules.categories {
            let applies = category.crossDomain || category.appliesToDomains.contains(rendered.domain)
            guard applies else { continue }
            for term in category.terms where matches(term: term, inNormalizedText: text) {
                return rendered.copy(text: rules.resolvedFallbackText(language: language))
            }
        }
        return rendered
    }

    /// Engine-provided safe replacement payload (EXECUTION_SPEC: on violation
    /// the composed payload is swapped for this one before rendering).
    public func safeFallbackPayload(
        domain: String,
        monthKey: String,
        calcLevel: CalcLevel,
        language: String,
        scoreCard: ScoreCard
    ) -> SemanticPayload {
        let text = rules.resolvedFallbackPayload(language: language)
        return SemanticPayload(
            domain: domain,
            monthKey: monthKey,
            calcLevel: calcLevel,
            confidence: "low",
            confidenceReasons: ["safety_fallback"],
            language: language,
            observations: [],
            interpretation: Interpretation(pattern: text?.pattern ?? ""),
            blindspot: text?.blindspot ?? "",
            actionToday: text?.actionToday ?? "",
            actionWeek: text?.actionWeek ?? "",
            prompt: text?.prompt ?? "",
            safetyNotes: [],
            explainability: [],
            scoreCard: scoreCard
        )
    }

    // MARK: - Matching

    private func matches(term: String, inNormalizedText text: String) -> Bool {
        let normalizedTerm = Self.normalize(term)
        guard !normalizedTerm.isEmpty else { return false }
        if Self.isASCIITerm(term) {
            guard let regex = asciiRegexCache[normalizedTerm] else { return false }
            let range = NSRange(text.startIndex..<text.endIndex, in: text)
            return regex.firstMatch(in: text, options: [], range: range) != nil
        }
        return text.contains(normalizedTerm)
    }

    static func isASCIITerm(_ term: String) -> Bool {
        term.unicodeScalars.allSatisfy { $0.isASCII }
    }

    // MARK: - Normalization

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
        // 4. Case-insensitive comparisons via lowercasing.
        return String(folded).lowercased()
    }
}
