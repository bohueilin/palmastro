# Resource sync note

`safety-rules.json` here is a **verbatim copy of the canonical file**
(the frozen cross-platform source of truth). Do NOT edit it here. The
canonical safety vocabulary lives in the Android module:

    engine-content/src/main/resources/safety-rules.json

When the canonical file changes it is copied over this one verbatim.
The Swift schema (`SafetyRules.swift`) mirrors the Kotlin `SafetyRules.kt`
exactly: version + nine categories (medical_diagnosis, treatment,
disease_prediction, investment_advice, guaranteed_money, fear_fate_claims,
self_harm, profanity, identity_attack), each with `zh` and `en` term lists.
Unknown keys are ignored. Every category is enforced on every domain
(strict_safety).

Matching semantics implemented by SafetyFilterImpl (identical on both
platforms): zero-width stripping (U+200B/C/D, U+FEFF, U+00AD), NFC
normalization, fullwidth ASCII folding (U+FF01..FF5E). `zh` terms match as
case-insensitive substrings; `en` terms are regex fragments compiled
case-insensitively and wrapped in explicit ASCII word boundaries —
`(?<![a-zA-Z0-9_])` / `(?![a-zA-Z0-9_])` rather than `\b`, because ICU/Java
treat CJK ideographs as word characters and `\b` would miss EN terms embedded
in Chinese text. Violation strings are formatted `<category_id>: <term>`.
Localized replacement copy for filtered reports comes from the content
template library (`fallback.filteredText` in content-templates.json).
