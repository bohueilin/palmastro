import Foundation
import CoreContracts

// Mirrors engine-content/src/main/kotlin/com/palmastro/content/GuidanceBuilder.kt
// exactly ("Understand your reading" guidance layer, templates v2.1.0).
// Parity fixtures in ios/shared-fixtures/guidance assert both implementations
// agree, including item selection order.

/// One "lean into" or "be mindful of" guidance card. `signalId` is the ruleset
/// signal that backed the card, or nil when it came from the per-domain bucket
/// generic fallback.
public struct GuidanceItem: Codable, Equatable, Sendable {
    public let domain: String
    public let signalId: String?
    public let title: String
    public let body: String
    public let action: String

    public init(domain: String, signalId: String?, title: String, body: String, action: String) {
        self.domain = domain
        self.signalId = signalId
        self.title = title
        self.body = body
        self.action = action
    }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        domain = try c.decode(String.self, forKey: .domain)
        signalId = try c.decodeIfPresent(String.self, forKey: .signalId)
        title = try c.decodeIfPresent(String.self, forKey: .title) ?? ""
        body = try c.decodeIfPresent(String.self, forKey: .body) ?? ""
        action = try c.decodeIfPresent(String.self, forKey: .action) ?? ""
    }
}

/// The composed "Understand your reading" guidance layer for one monthly
/// result: a grade-keyed month theme, up to three strengths to lean into,
/// two-to-three gentle mindful pointers, and one weekly focus line per domain.
public struct Guidance: Codable, Equatable, Sendable {
    public let monthTheme: String
    public let strengths: [GuidanceItem]
    public let mindful: [GuidanceItem]
    public let weekPlan: [String]

    public init(monthTheme: String, strengths: [GuidanceItem], mindful: [GuidanceItem], weekPlan: [String]) {
        self.monthTheme = monthTheme
        self.strengths = strengths
        self.mindful = mindful
        self.weekPlan = weekPlan
    }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        monthTheme = try c.decodeIfPresent(String.self, forKey: .monthTheme) ?? ""
        strengths = try c.decodeIfPresent([GuidanceItem].self, forKey: .strengths) ?? []
        mindful = try c.decodeIfPresent([GuidanceItem].self, forKey: .mindful) ?? []
        weekPlan = try c.decodeIfPresent([String].self, forKey: .weekPlan) ?? []
    }

    public var isEmpty: Bool {
        monthTheme.isBlank && strengths.isEmpty && mindful.isEmpty && weekPlan.isEmpty
    }
}

/// Pure, deterministic guidance composer (PRD §11-§13, §30-§32). All copy
/// comes from `ContentTemplates.guidance`; derivation:
///
/// - strengths: positive explainability contributions sorted descending
///   (ties: domain order, then signalId) -> top `maxStrengths` with distinct
///   domains; short lists are backfilled with the domain bucket generic for
///   the highest-scoring uncovered domains.
/// - mindful: negative contributions sorted by magnitude (same tie-breaks),
///   deduplicated by signalId -> up to `maxMindful`; backfilled to at least
///   `minMindful` with bucket generics, preferring domains not already used
///   as strengths, lowest score first.
/// - weekPlan: one focus line per domain in `Domains.all` order, choosing the
///   "high"/"low" variant by `scoreCard.totalScore >= 65`.
///
/// Tone is positivity-first and never fear-based: mindful items are gentle
/// attention-pointers with a concrete micro-action, never warnings of doom.
///
/// Kotlin's sorts are stable; Swift's is not guaranteed to be, so every
/// comparator ends in an explicit total tie-break (domain index, then
/// signalId) — the same lesson as the scoring engine's explainability sort.
public struct GuidanceBuilder {

    private static let maxStrengths = 3
    private static let maxMindful = 3
    private static let minMindful = 2
    private static let highThreshold = 65
    private static let defaultScore = 50
    private static let defaultGrade = "Stable"
    private static let domainPlaceholder = "{domain}"

    private let templates: ContentTemplates

    public init(templates: ContentTemplates) {
        self.templates = templates
    }

    /// Convenience initializer loading the bundled default templates.
    public init() throws {
        self.init(templates: try ContentTemplates.loadDefault())
    }

    public func build(
        payloads: [String: SemanticPayload],
        overallGrade: String,
        language: String
    ) -> Guidance {
        let lang = templates.resolveLanguage(language)
        let candidates = collectCandidates(payloads: payloads)
        let strengths = buildStrengths(candidates: candidates, payloads: payloads, lang: lang)
        let mindful = buildMindful(candidates: candidates, payloads: payloads, strengths: strengths, lang: lang)
        return Guidance(
            monthTheme: monthTheme(overallGrade: overallGrade, lang: lang),
            strengths: strengths,
            mindful: mindful,
            weekPlan: weekPlan(payloads: payloads, lang: lang)
        )
    }

    // MARK: - Candidates

    private struct Candidate {
        let domain: String
        let domainIndex: Int
        let signalId: String
        let contribution: Double
    }

    private func collectCandidates(payloads: [String: SemanticPayload]) -> [Candidate] {
        Domains.all.enumerated().flatMap { index, domain -> [Candidate] in
            (payloads[domain]?.explainability ?? [])
                .filter { $0.mapping.contains(domain) }
                .map { Candidate(domain: domain, domainIndex: index, signalId: $0.signalId, contribution: $0.contribution) }
        }
    }

    // MARK: - Strengths (lean into)

    private func buildStrengths(
        candidates: [Candidate],
        payloads: [String: SemanticPayload],
        lang: String
    ) -> [GuidanceItem] {
        var items: [GuidanceItem] = []
        var usedDomains = Set<String>()
        let positives = candidates.filter { $0.contribution > 0 }.sorted { a, b in
            if a.contribution != b.contribution { return a.contribution > b.contribution }
            if a.domainIndex != b.domainIndex { return a.domainIndex < b.domainIndex }
            return a.signalId < b.signalId
        }
        for candidate in positives {
            if items.count == Self.maxStrengths { break }
            if usedDomains.contains(candidate.domain) { continue }
            guard let copy = templates.guidance.signals[candidate.signalId]?.leanInto else { continue }
            items.append(item(domain: candidate.domain, signalId: candidate.signalId, copy: copy, lang: lang))
            usedDomains.insert(candidate.domain)
        }
        if items.count < Self.maxStrengths {
            let backfill = Domains.all.enumerated()
                .filter { !usedDomains.contains($0.element) && payloads[$0.element] != nil }
                .sorted { a, b in
                    let scoreA = score(payloads: payloads, domain: a.element)
                    let scoreB = score(payloads: payloads, domain: b.element)
                    if scoreA != scoreB { return scoreA > scoreB }
                    return a.offset < b.offset
                }
            for (_, domain) in backfill {
                if items.count == Self.maxStrengths { break }
                guard let copy = bucketCopy(
                    field: templates.guidance.domains[domain]?.strengths,
                    score: score(payloads: payloads, domain: domain)
                ) else { continue }
                items.append(item(domain: domain, signalId: nil, copy: copy, lang: lang))
                usedDomains.insert(domain)
            }
        }
        return items
    }

    // MARK: - Mindful (be mindful of)

    private func buildMindful(
        candidates: [Candidate],
        payloads: [String: SemanticPayload],
        strengths: [GuidanceItem],
        lang: String
    ) -> [GuidanceItem] {
        var items: [GuidanceItem] = []
        var usedSignals = Set<String>()
        var usedDomains = Set<String>()
        let negatives = candidates.filter { $0.contribution < 0 }.sorted { a, b in
            let magA = abs(a.contribution)
            let magB = abs(b.contribution)
            if magA != magB { return magA > magB }
            if a.domainIndex != b.domainIndex { return a.domainIndex < b.domainIndex }
            return a.signalId < b.signalId
        }
        for candidate in negatives {
            if items.count == Self.maxMindful { break }
            if usedSignals.contains(candidate.signalId) { continue }
            guard let copy = templates.guidance.signals[candidate.signalId]?.mindfulOf else { continue }
            items.append(item(domain: candidate.domain, signalId: candidate.signalId, copy: copy, lang: lang))
            usedSignals.insert(candidate.signalId)
            usedDomains.insert(candidate.domain)
        }
        if items.count < Self.minMindful {
            let strengthDomains = Set(strengths.map(\.domain))
            let backfill = Domains.all.enumerated()
                .filter { !usedDomains.contains($0.element) && payloads[$0.element] != nil }
                .sorted { a, b in
                    // Prefer domains not already used as strengths, then
                    // lowest score, then canonical domain order.
                    let inStrengthsA = strengthDomains.contains(a.element)
                    let inStrengthsB = strengthDomains.contains(b.element)
                    if inStrengthsA != inStrengthsB { return !inStrengthsA }
                    let scoreA = score(payloads: payloads, domain: a.element)
                    let scoreB = score(payloads: payloads, domain: b.element)
                    if scoreA != scoreB { return scoreA < scoreB }
                    return a.offset < b.offset
                }
            for (_, domain) in backfill {
                if items.count >= Self.minMindful { break }
                guard let copy = bucketCopy(
                    field: templates.guidance.domains[domain]?.mindful,
                    score: score(payloads: payloads, domain: domain)
                ) else { continue }
                items.append(item(domain: domain, signalId: nil, copy: copy, lang: lang))
                usedDomains.insert(domain)
            }
        }
        return items
    }

    // MARK: - Week plan / month theme

    private func weekPlan(payloads: [String: SemanticPayload], lang: String) -> [String] {
        Domains.all.compactMap { domain -> String? in
            guard let payload = payloads[domain] else { return nil }
            let key = payload.scoreCard.totalScore >= Self.highThreshold ? "high" : "low"
            guard let text = templates.guidance.domains[domain]?.monthPlan[key] else { return nil }
            let line = templates.localized(text, language: lang)
            guard !line.isBlank else { return nil }
            return line.replacingOccurrences(
                of: Self.domainPlaceholder,
                with: displayName(domain: domain, lang: lang)
            )
        }
    }

    private func monthTheme(overallGrade: String, lang: String) -> String {
        let theme = templates.guidance.monthTheme[overallGrade]
            ?? templates.guidance.monthTheme[Self.defaultGrade]
            ?? [:]
        return templates.localized(theme, language: lang)
    }

    // MARK: - Helpers

    private func item(domain: String, signalId: String?, copy: GuidanceCopy, lang: String) -> GuidanceItem {
        let name = displayName(domain: domain, lang: lang)
        func resolve(_ text: LocalizedText) -> String {
            templates.localized(text, language: lang)
                .replacingOccurrences(of: Self.domainPlaceholder, with: name)
        }
        return GuidanceItem(
            domain: domain,
            signalId: signalId,
            title: resolve(copy.title),
            body: resolve(copy.body),
            action: resolve(copy.action)
        )
    }

    private func displayName(domain: String, lang: String) -> String {
        let name = templates.domains[domain]
            .map { templates.localized($0.displayName, language: lang) } ?? ""
        return name.isBlank ? domain : name
    }

    private func score(payloads: [String: SemanticPayload], domain: String) -> Int {
        payloads[domain]?.scoreCard.totalScore ?? Self.defaultScore
    }

    /// Bucket whose range contains `score`. Kotlin scans entries in template
    /// JSON order; Swift dictionaries are unordered, so the sorted-key scan
    /// from `ContentTemplates.bucketValue` is used — bucket ranges within one
    /// field are disjoint in the canonical data, so at most one entry matches
    /// and both implementations agree.
    private func bucketCopy(field: [String: GuidanceCopy]?, score: Int) -> GuidanceCopy? {
        guard let field else { return nil }
        return templates.bucketValue(field, score: score)
    }
}
