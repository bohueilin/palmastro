# Cross-platform parity fixtures

Fixtures asserting that the iOS engines (PalmAstroKit) produce the same
deterministic outputs as the Android engines (the reference implementation)
for identical inputs — PRD §10 (Cross-Platform Parity) and Workstream C.

## Layout

```
shared-fixtures/
├── scoring/   *.json — ScoringEngine fixtures
├── content/   *.json — ContentComposer fixtures
└── guidance/  *.json — GuidanceBuilder fixtures
```

`ParityTests` (in `ios/PalmAstroKit/Tests/ParityTests`) skips a category with
an explanatory message while its directory has no fixtures. Real fixtures are
generated from the Android side at integration time (a small JVM harness
dumping engine inputs/outputs as JSON) and committed here so both CI pipelines
assert against the same files.

One hand-computed smoke fixture ships now
(`scoring/smoke_low_confidence_l1.json`: low-confidence palm + empty L1 astro
-> all-baseline scores) to keep the harness itself exercised; replace/extend
it with Android-generated fixtures at integration.

## Fixture format

Every file is one JSON object: `{ "input": ..., "expected": ... }`.

### scoring/*.json

- `input`: a `ScoringInput` — `palmFeatures` (`PalmFeatureResult`),
  `astroResult` (`AstroResult`), `userContext`, `rulesetVersion`.
- `expected`: the `ScoringResult` produced by the Android
  `ScoringEngineImpl` with the canonical `default-ruleset.json`.

Compared: `domainScores`, `grade`, `confidence`, `confidenceReasons`,
`rulesetVersion`, and `explainability` (signal order, mapping strings,
contributions to 1e-9).

### content/*.json

- `input`: a `ContentInput` — `scoringResult`, optional `deltaResult`,
  `tone`, `entitlements`, `calcLevel`, `monthKey`, `language`.
- `expected`: the `Map<String, SemanticPayload>` produced by the Android
  composer with the canonical `content-templates.json`.

Compared: full payload text fields, safety notes, score cards, observations,
language, calcLevel and confidence per domain.

### guidance/*.json

- `input`: `{ "payloads": Map<String, SemanticPayload>, "overallGrade": String,
  "language": String }` — the exact `GuidanceBuilder.build` arguments.
- `expected`: the `Guidance` produced by the Android engine-content
  `GuidanceBuilder` with the canonical `content-templates.json` (>= 2.1.0):
  `{ "monthTheme": String, "strengths": [GuidanceItem], "mindful":
  [GuidanceItem], "weekPlan": [String] }` where `GuidanceItem` is
  `{ domain, signalId?, title, body, action }` (`signalId` null/absent for
  bucket-generic fallback items).

Compared: monthTheme, weekPlan, and full strengths/mindful item lists
including selection order.

## Conventions

- JSON field names are the Kotlin property names (kotlinx.serialization
  defaults); enums serialize as their Kotlin names (`"FRONT"`, `"L1"`,
  `"SCIENTIFIC"`).
- Fixtures must be generated with the same resource versions the apps ship
  (ruleset 2.0.0, content templates 2.1.0 — the guidance-enabled library).
  Bump fixtures together with resource versions.
- Keep fixtures small and named by scenario, e.g.
  `scoring/high_quality_l2_zh.json`, `content/low_scores_l1_en.json`.
