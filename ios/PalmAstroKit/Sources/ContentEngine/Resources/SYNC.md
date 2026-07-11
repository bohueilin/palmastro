# Resource sync note

`content-templates.json` here is a **verbatim copy of the canonical file**
(the frozen cross-platform source of truth). Do NOT edit it here. The
canonical content library lives in the Android module:

    engine-content/src/main/resources/content-templates.json

When the canonical file changes it is copied over this one verbatim (last
synced at version 2.1.0, the guidance-enabled library). The Swift schema
(`ContentTemplates.swift`) mirrors the Kotlin `ContentTemplates.kt` exactly:
version / defaultLanguage / languages / buckets / domains / observations /
observationFallbackEvidence / fallback / tones / labels / guidance. Unknown
keys are ignored; language resolution is exact membership in `languages`
with fallback to `defaultLanguage` ("en").

The v2.1.0 `guidance` section ("Understand your reading", PRD §11-§13,
§30-§32) is `signals.<id>.leanInto/.mindfulOf {title, body, action}`,
`domains.<domain>.strengths/.mindful` keyed by the five score buckets plus
`domains.<domain>.monthPlan` keyed "high"/"low", and `monthTheme.<grade>`.
Guidance copy ships complete in en + zh-TW; zh-CN/ja/hi fall back per-field
to en. `GuidanceBuilder.swift` mirrors the Kotlin `GuidanceBuilder.kt`
derivation exactly; the ParityTests golden-snapshot test replays the Android
`GuidanceGoldenSnapshotTest` input against
`engine-content/src/test/resources/golden/guidance_{en,zh-TW}.json`.

Bucketed fields resolve through the shared score buckets: interpretation
pattern uses the five buckets (peak / rising / transition / building /
attention); trigger, cost, blindspot, actions, and prompt use the high
(>= 65) / low (<= 64) split.

The file ships all five launch languages (en, zh-TW, zh-CN, ja, hi). Copy
text must never contain terms matched by safety-rules.json — the
SafetyEngine test suite composes from these templates and asserts zero
violations.
