# PalmAstro Launch Execution Spec (frozen 2026-07-09)

Source of Truth: `PalmAstro_PRD_Full_v2_AppStoreLaunch.md`. This spec fixes cross-agent decisions.
Assumptions are labeled per the PRD's Claude/Agent Rule.

## Frozen contract decisions (already applied in `contracts/`)

- `SemanticPayload` fields are language-neutral: `interpretation: Interpretation(pattern, trigger, cost)`,
  `blindspot`, `actionToday`, `actionWeek`, `prompt`, `safetyNotes`, plus new `language` and
  `confidenceReasons` (defaults). `Observation(signalId, displayName, evidenceSummary)`,
  `ExplainEntry(signalId, mapping, contribution)`, `RenderedReport(domain, tone, text)`.
- `ContentInput.language: String = "en"` — composer must honor it.
- `BestFrameResult.palmMetrics: PalmMetrics?` — 21 normalized `LandmarkPoint`s + 4
  `LineRegionMetrics` (regions: `headline|heartline|lifeline|fateline`; contrast/continuity/meanIntensity 0..1),
  computed on-device by the app scan layer. Extractor falls back to conservative low-confidence
  quality-derived features when null.
- `Domains.ALL = [career, wealth, family, health]`; `ProductIds` = `palmastro.pack.career|wealth|bundle`.

## Launch decisions

- **IAP: free-only launch.** `Assumption (editable):` Play Billing deferred to post-launch;
  no paid CTA visible; `iap_enabled=false`. Entitlement scaffolding stays.
- **Languages:** UI exposes English + Traditional Chinese only. `resConfigs("en","zh-rTW")`.
  Content engine keeps en/zh-TW/zh-CN/ja/hi capability internally in JSON.
- **L2 astrology:** real math only — Meeus low-precision moon longitude (sign-accurate),
  standard ascendant formula (LST + obliquity + latitude). Fabricated planetary-strength
  signals are removed. L1 = sun sign/element/modality only.
- **Tones:** display names Analytical / Gentle / Direct (PRD §45); enum names unchanged.
- **Reminders:** opt-in — profile default `reminders="off"`; POST_NOTIFICATIONS requested
  only when the user enables reminders.
- **Feature flags (PRD §69 names, SharedPreferences-backed, consumed for real):**
  `daily_insights_enabled=false`, `llm_interpretations_enabled=false`, `iap_enabled=false`,
  `wear_enabled=false`, `widget_enabled=false`, `share_cards_enabled=true`,
  `strict_safety_enabled=true`, `debug_scan_bypass_enabled=false`, `scan_reminders_enabled=true`.
- **Safety pipeline (enforced):** compose → `validate()` each payload; on violation replace that
  payload with the engine-provided safe fallback payload; render via `ToneRenderer` → `filter()`.
- **DB:** schema v3, `exportSchema=true` (schemas committed). v3 = journal WITHOUT profile
  fields; user_profile gains `language` (default "system"); reminders default "off".
  MIGRATION_1_2 fixed to what v2 shipped; MIGRATION_2_3 normalizes. SQLCipher wired via
  existing `DatabaseKeyManager`. Wipe: clear tables + files + share_audit.log + rotate install
  id + recreate DB key cleanly (key prefs handled so post-wipe DB reopens).
- **Model download:** pin MediaPipe hand_landmarker to the 0.10.9 float16 asset URL with its
  known SHA-256; guarded HandLandmarker init; corrupt model → delete + re-download path.

## String resources convention

Per-screen resource files to avoid merge conflicts: `strings_onboarding.xml`,
`strings_scan.xml`, `strings_results.xml`, `strings_detail.xml`, `strings_settings.xml`,
`strings_share.xml` in BOTH `values/` and `values-zh-rTW/`. Never edit another agent's file.
Every user-visible string (incl. contentDescription / announceForAccessibility) via resources.

## File ownership (Wave 1)

| Agent | Owns |
|---|---|
| build-release | root+app gradle files, proguard, keystore, .github/, AndroidManifest.xml, res/xml/* |
| data-privacy | data-room/*, app DatabaseModule/DatabaseKeyManager, workers, PalmAstroApp.kt |
| engine-content | engine-content/* |
| engines | engine-astro/*, engine-palm-features/*, engine-scoring/*, engine-scan-quality/* |
| store-docs | docs/store/*, app assets/legal/* |
| ios | ios/* |

Wave 2 (after integration build): scan-pipeline (ScanViewModel/ImageQualityAnalyzer/ModelManager),
ui-core (Onboarding/Scan screens, navigation, MainActivity, FeatureFlags), ui-results
(Results/Detail/History/Explainability/Journal wiring/Settings/Share).
