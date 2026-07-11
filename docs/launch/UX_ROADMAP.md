# PalmAstro UX Roadmap — Staged Claude Execution Prompts

Source of Truth: `PalmAstro_PRD_Full_v2_AppStoreLaunch.md` (repo root) + `docs/launch/EXECUTION_SPEC.md`
(frozen cross-agent decisions). This document extends PRD Part XX (§§74–77): every item below is a
ready-to-paste prompt for a fresh Claude Code session opened at the repo root.

## How to use this document

1. Work top-down: Stage 1 before Stage 2, etc. Items inside a stage are independent unless noted.
2. Paste one prompt per session. Each prompt is self-contained: it names its Source of Truth, the
   files it owns, what it must not touch, and what tests must pass.
3. After each item, run the repo verification the prompt specifies before merging.

## Global rules (repeated inside every prompt; listed here for reviewers)

- `contracts/` is FROZEN. No prompt may edit or extend it. If an item seems to need a contract
  change, stop and escalate instead.
- Engines stay deterministic (PRD §§18–19, Appendix D3). No cloud LLM paths (PRD §21).
- Voice: calm, positivity-first, action-oriented. "Be mindful of" framing — never fear, doom,
  urgency, or pressure (PRD §11, §12.3, §§30–32). No medical, financial, or deterministic claims.
- Android user-visible strings only via per-screen resource files in `values/` AND `values-zh-rTW/`
  (natural Taiwan Traditional Chinese), per EXECUTION_SPEC "String resources convention". This
  includes `contentDescription` and accessibility announcements.
- Kotlin 1.9.22; Material 3 Compose on Android (PRD §39), SwiftUI-native on iOS (PRD §38).
- Feature-flag anything not launch-committed (PRD §69; flags live in
  `app/src/main/kotlin/com/palmastro/app/config/FeatureFlags.kt` and
  `ios/App/Support/FeatureFlags.swift`).
- `Assumption (editable):` the Guidance surface built for launch lives at
  `app/src/main/kotlin/com/palmastro/app/ui/guidance/` with `strings_guidance.xml`, and
  `ios/App/Results/GuidanceView.swift` on iOS. If the parallel launch agents landed it elsewhere,
  substitute the real paths when pasting prompts that reference it.

## Stage overview

| Stage | Theme | Items | When |
|---|---|---|---|
| 1 | Launch polish | Haptics, motion/reduced-motion, type-scale audit, skeletons, error copy | Before store submission |
| 2 | Retention | Monthly rescan ritual, gentle return, journal prompts, share-card refresh | First 1–2 post-launch releases |
| 3 | Depth | Guided reflection, glossary, delta storytelling, accessibility deep pass | Post-launch quarter |
| 4 | Platform | Widgets, shortcuts, tablet/foldable | Post-launch quarter |
| 5 | Expansion | zh-CN/ja/hi UI, Wear OS, paid packs | When prerequisites land |

Explicitly excluded items are listed at the end (PRD §7.3 compliance).

---

## Stage 1 — Launch polish

### 1.1 Haptics vocabulary for scan capture

- **Why it matters:** PRD §38/§39 require native haptics; PRD §41 says motion (and by extension
  feedback) must "reinforce scan success"; PRD §40 requires non-color-only quality indicators —
  haptics give the quality gate a second, non-visual channel. This is the highest-leverage
  perceived-quality win per line of code (PRD §36 "world-class native app" bar).
- **Effort:** S  **Platform:** Android + iOS

```text
Use PalmAstro_PRD_Full_v2_AppStoreLaunch.md and docs/launch/EXECUTION_SPEC.md as Source of Truth.

Add a small, consistent haptics vocabulary to the scan flow on both platforms. Exactly three
events, mapped identically:
1. Angle capture success -> a single crisp confirmation tick.
2. Quality-gate fail (retry coaching shown) -> a soft double tap. Never a harsh buzz — PRD 12.3
   forbids anything that feels like an alarm; a quality fail is coaching, not an error.
3. Scan complete (all 7 angles) -> a slightly fuller success pattern.

Android: implement in app/src/main/kotlin/com/palmastro/app/ui/scan/ (ScanScreen.kt and a new
ScanHaptics.kt). Use VibrationEffect predefined effects (EFFECT_CLICK / EFFECT_TICK /
createWaveform for completion) via VibratorManager on API 31+, Vibrator fallback below; no new
permissions beyond VIBRATE in AndroidManifest.xml if not already present. Respect the system
"touch feedback" setting; do nothing when unavailable.
iOS: implement in ios/App/Scan/ (ScanView.swift and a new ScanHaptics.swift) with
UINotificationFeedbackGenerator (.success) and UIImpactFeedbackGenerator (.soft) — CoreHaptics
CHHapticEngine only for the completion pattern, with a UIFeedbackGenerator fallback.

Ownership: only the files named above. contracts/ is FROZEN — do not edit it. Do not touch engine
modules, viewmodels' scoring logic, or other agents' UI screens. No user-visible strings are
expected; if any appear, put them in strings_scan.xml (values/ + values-zh-rTW/).

Tests: unit-test the event->effect mapping (Android: a JVM test for ScanHaptics with a fake
Vibrator abstraction under app/src/test/kotlin; iOS: mapping test in ios/PalmAstroKit only if the
mapping lives in shared code — otherwise keep it app-side and compile-check). All existing tests
stay green: ./gradlew :app:testDebugUnitTest detekt, and cd ios/PalmAstroKit && ./test.sh.
```

### 1.2 Motion polish + reduced-motion audit

- **Why it matters:** PRD §41 (motion must clarify progress, respect reduced motion, never hide
  loading/failures) and PRD §40 (reduced-motion setting is a hard accessibility requirement).
  A calm, consistent motion language is core to the §11 North Star ("Calm", "Premium").
- **Effort:** M  **Platform:** Android + iOS

```text
Use PalmAstro_PRD_Full_v2_AppStoreLaunch.md and docs/launch/EXECUTION_SPEC.md as Source of Truth.

Task 1 — audit: walk every screen (onboarding, scan, results, guidance, domain detail,
explainability, journal, history, settings, share preview) and produce a table in a PR
description (not a repo doc): animation, purpose per PRD 41 (clarify progress / reinforce
success), duration, and behavior under reduced motion. Flag anything decorative-only, longer
than ~400ms, or that blocks content.
Task 2 — fix: (a) gate all non-essential animation behind the platform reduced-motion signal —
Android: Settings.Global.ANIMATOR_DURATION_SCALE == 0 exposed via one small helper in
app/src/main/kotlin/com/palmastro/app/ui/components/MotionPreferences.kt; iOS:
@Environment(\.accessibilityReduceMotion). Under reduced motion, replace movement with opacity
crossfades; progress indicators must remain (PRD 41: never hide loading). (b) Standardize
durations/easing into theme-level constants (app/src/main/kotlin/com/palmastro/app/ui/theme/
Motion.kt; ios/App/Support/Motion.swift). (c) Score/gauge count-up animations on results,
guidance and detail screens must settle fast and skip entirely under reduced motion.

Ownership: the audited UI files' animation code only — do not change layout, copy, navigation, or
business logic. contracts/ is FROZEN — do not edit it. New strings (if any) go in the owning
screen's strings_*.xml (values/ + values-zh-rTW/).

Tests: unit-test MotionPreferences mapping; Compose UI test asserting the results score gauge
renders its final value with animations disabled. ./gradlew :app:testDebugUnitTest detekt green;
cd ios/PalmAstroKit && ./test.sh green.
```

### 1.3 Dynamic Type / font-scale audit (incl. zh line-height)

- **Why it matters:** PRD §37 and §40 make font scaling a launch requirement; PRD §44 requires
  testing with long strings. Traditional Chinese needs looser line-height than Latin at the same
  sp size or dense reflective text becomes cramped — a visible quality miss for the primary
  Taiwan audience (PRD §43 lists zh-TW first).
- **Effort:** M  **Platform:** Android + iOS

```text
Use PalmAstro_PRD_Full_v2_AppStoreLaunch.md and docs/launch/EXECUTION_SPEC.md as Source of Truth.

Audit every screen at Android font scale 1.3 and 2.0 (and display-size large) and iOS Dynamic
Type XL and AX3, in BOTH English and zh-TW. Then fix:
1. Clipped/ellipsized text that hides meaning (scores, confidence labels, action items, guidance
   items) — allow wrapping, never shrink-to-fit safety copy.
2. Fixed-height containers holding text (cards, buttons, list rows) -> min-height + wrap content.
3. zh line-height: define per-language-aware text styles in
   app/src/main/kotlin/com/palmastro/app/ui/theme/Type.kt — body/reading styles get
   lineHeight >= 1.5x fontSize when the locale is Chinese (Latin can stay ~1.4x); on iOS use
   lineSpacing in a shared TextStyle helper (ios/App/Support/Typography.swift). Do not hardcode
   px/dp line heights; keep everything in sp/relative units so scaling composes.
4. Touch targets stay >= 48dp/44pt after text growth (PRD 40).

Ownership: theme/typography files, and minimal layout fixes inside existing screen files. Do not
reword any copy (that is item 1.5). contracts/ is FROZEN — do not edit it. Any new strings go in
the owning screen's strings_*.xml (values/ + values-zh-rTW/).

Tests: screenshot or Compose UI tests for the worst three screens at fontScale=2.0 asserting key
elements exist and are not clipped (use existing TestTags.kt tags; add tags if missing).
./gradlew :app:testDebugUnitTest detekt green; cd ios/PalmAstroKit && ./test.sh green.
```

### 1.4 Empty-state and loading skeletons

- **Why it matters:** PRD §41 — motion must "never hide loading or failures"; PRD §36 premium
  bar. Analysis after a scan takes real time on low-end devices (PRD §60 device matrix); a calm
  skeleton beats a spinner, and first-run empty states (no scan yet, empty journal, empty
  history) are currently the least-designed moments of the app.
- **Effort:** M  **Platform:** Android + iOS

```text
Use PalmAstro_PRD_Full_v2_AppStoreLaunch.md and docs/launch/EXECUTION_SPEC.md as Source of Truth.

Build a shared skeleton/empty-state kit and apply it:
1. Skeleton components (Android: app/src/main/kotlin/com/palmastro/app/ui/components/
   Skeletons.kt with a subtle shimmer that disables under reduced motion; iOS:
   ios/App/Support/SkeletonView.swift using .redacted(reason: .placeholder)). Shapes must mirror
   final layout (card, gauge, text rows) to avoid layout jumps.
2. Apply to: results dashboard while composing a report, guidance screen while its items load,
   domain detail, history list.
3. Empty states with one calm illustration-free layout (icon + title + body + single CTA) for:
   no completed scan yet (CTA: start scan), empty journal (CTA: open this month's prompt), empty
   history (body explains monthly rescans — informative, zero pressure per PRD 12.3).
   Copy is invitational ("When you are ready..."), never urgent.

Ownership: the new component files, the four screens' loading/empty branches, plus
strings_results.xml, strings_detail.xml (values/ + values-zh-rTW/) and a new
strings_empty_states.xml (both locales) for the shared empty-state copy. contracts/ is FROZEN —
do not edit it. Do not alter data loading logic or viewmodels beyond exposing an
isLoading/isEmpty state if missing.

Tests: Compose UI tests asserting skeleton shows while loading state is true and empty-state CTA
navigates (or emits the nav event). ./gradlew :app:testDebugUnitTest detekt green;
cd ios/PalmAstroKit && ./test.sh green.
```

### 1.5 Error-state copy audit vs PRD §42

- **Why it matters:** PRD §42 enumerates nine required error states, each needing "what happened /
  why it matters / what to do next". Error moments are where fear-based tone accidentally creeps
  in; auditing copy against §§30–32 and 12.3 protects the safety posture the whole launch rests
  on (and is checked by store reviewers per §§34–35).
- **Effort:** S  **Platform:** Android + iOS (copy shared)

```text
Use PalmAstro_PRD_Full_v2_AppStoreLaunch.md (especially sections 42, 30-32, 12.3) and
docs/launch/EXECUTION_SPEC.md as Source of Truth.

Audit and fix every error state against PRD 42's list: no camera permission, poor lighting, hand
not detected, low confidence, missing birth time/place, purchase unavailable (should be unreachable
at launch — iap_enabled=false — verify and leave a comment, don't build UI), network unavailable
(model download), storage wipe failed, safety-filter-blocked content.
For each: verify the state is reachable and renders (1) what happened, (2) why it matters,
(3) what to do next — in en AND zh-TW. Rewrite copy that is blaming ("You failed to..."),
alarming, or dead-endy. Rules: calm and specific; the next step is always actionable; low
confidence is framed as honesty about evidence (PRD 12.2), not as user error; safety-filter
fallback copy must never hint at what was blocked. zh-TW must be natural Taiwan phrasing, not
literal translation.

Ownership: string resources only — strings_scan_errors.xml, strings_scan.xml, strings_results.xml,
strings_detail.xml, strings_settings.xml (values/ + values-zh-rTW/) — plus minimal wiring where an
error state exists but shows a hardcoded or missing string. Do not restructure error handling.
contracts/ is FROZEN — do not edit it.

Tests: extend existing unit tests that assert error mapping -> string resource ids for the nine
PRD 42 states (add a table-driven test if none exists). ./gradlew :app:testDebugUnitTest detekt
green; cd ios/PalmAstroKit && ./test.sh green. Also paste the before/after copy table into the PR
description for safety review.
```

---

## Stage 2 — Retention (gentle, pressure-free)

### 2.1 Monthly rescan ritual — calendar-anchored reminder + "your month is ready" moment

- **Why it matters:** Monthly delta tracking is a core differentiator (PRD §34.2) and reports are
  per-month by design (PRD §52–53). A ritual anchored to the month boundary turns the existing
  `ScanReminderWorker` into the retention loop — while PRD §23 constrains it: opt-in, clear
  value, no sensitive text in the notification body, easy opt-out.
- **Effort:** M  **Platform:** Android first (worker exists), iOS follow-up

```text
Use PalmAstro_PRD_Full_v2_AppStoreLaunch.md (sections 23, 34.2, 12.3, 52-53) and
docs/launch/EXECUTION_SPEC.md as Source of Truth. Reminders are opt-in (profile default "off";
POST_NOTIFICATIONS only requested when enabling) — keep that behavior exactly.

Upgrade the monthly rescan into a ritual:
1. Calendar anchoring: extend app/src/main/kotlin/com/palmastro/app/worker/ScanReminderWorker.kt
   scheduling so the opt-in reminder fires at the start of a new month (user-configurable day
   1-7, default day 1, at a quiet default hour) instead of a rolling interval. Deterministic
   scheduling logic in a pure, testable class (e.g. worker/ReminderScheduling.kt).
2. Notification copy (strings via a new strings_reminders.xml in values/ + values-zh-rTW/):
   invitation, not pressure — e.g. "A new month — rescan when you're ready." Forbidden: streaks,
   "don't lose", "you missed", countdowns, emoji alarm imagery, any reading content in the body
   (PRD 23: no sensitive text).
3. "Your month is ready" moment: when the app opens in a month with no scan yet but a previous
   month's report exists, results dashboard shows one calm inline card ("Start this month's
   reading") instead of a modal. Dismissible; never blocks last month's report — old data stays
   fully readable (PRD 12.3).
4. Settings: reminder day picker + preview of the notification text under the existing
   notifications section (strings_settings.xml both locales).

Ownership: worker/*, the new strings files, the dashboard card in ui/results/ResultsScreen.kt,
the settings rows in ui/settings/SettingsScreen.kt. contracts/ is FROZEN — do not edit it. Flag:
reuse scan_reminders_enabled; no new flags.

Tests: unit tests for ReminderScheduling (month rollover incl. Dec->Jan, day-7 clamp, timezone
change, opt-out cancels work); Compose test for the inline card visibility logic.
./gradlew :app:testDebugUnitTest detekt green.
```

### 2.2 Streak-free gentle return mechanics

- **Why it matters:** PRD §12.3 explicitly forbids countdowns and pressure; §11 forbids
  exploitative mechanics. Retention must come from warmth and continuity, not loss-aversion.
  This item defines the pattern once so later features don't each invent their own (risking a
  compliance slip).
- **Effort:** S  **Platform:** Android + iOS

```text
Use PalmAstro_PRD_Full_v2_AppStoreLaunch.md (sections 11, 12.3, 2) and
docs/launch/EXECUTION_SPEC.md as Source of Truth.

Implement gentle return mechanics with a hard NO-list (no streak counters, no "X days since",
no badges for frequency, no red dots, no guilt copy, no countdowns):
1. Returning-user greeting on the results dashboard: if the user has a report for the current
   month, greet with continuity ("Welcome back — your [month] reading") instead of generic copy.
2. If the user returns after 30+ days with no current-month report, the 2.1 inline card gets a
   softer variant: acknowledge time passed WITHOUT counting it ("It's been a while. Your last
   reading is still here whenever you want to look back."). Absence is always framed as fine.
3. Journal continuity: when opening a domain the user journaled about in a previous month,
   offer a one-tap "re-read last month's entry" link (read-only) before writing.
Document the NO-list as a comment block in the greeting component so future contributors see it.

Ownership: ui/results/ResultsScreen.kt (greeting + card variant), ui/journal/JournalScreen.kt
(continuity link), strings_results.xml + strings_journal.xml — create strings_journal.xml if the
journal currently borrows from another file — in values/ + values-zh-rTW/. contracts/ is FROZEN —
do not edit it. No notifications in this item (2.1 owns those); no new flags.

Tests: unit tests for the greeting/variant selection logic (fresh user / current-month report /
stale >30d) as a pure function; assert the 30+ day variant string contains no digits (guards
against "it's been 43 days" regressions). ./gradlew :app:testDebugUnitTest detekt green;
cd ios/PalmAstroKit && ./test.sh green if shared logic lands in PalmAstroKit.
```

### 2.3 Journal writing prompts from the reading

- **Why it matters:** The content engine already emits a per-domain reflection `prompt`
  (EXECUTION_SPEC `SemanticPayload.prompt`; PRD §13.4 item 8) but the journal is a blank
  textarea. Piping the reading's own prompt into the journal makes journaling — a §7.1 core
  free feature and §34.2 differentiator — dramatically easier to start.
- **Effort:** S  **Platform:** Android + iOS

```text
Use PalmAstro_PRD_Full_v2_AppStoreLaunch.md (sections 13.6, 7.1, 12.2) and
docs/launch/EXECUTION_SPEC.md as Source of Truth.

Surface the reading's reflection prompt inside the journal flow:
1. When the journal editor opens for a domain+month that has a report, show that domain's
   reflection prompt (SemanticPayload.prompt, already tone-rendered and safety-filtered by the
   engine pipeline) above the text field as a quiet, dismissible card with an "Use as starting
   point" action that inserts the prompt as a quoted first line.
2. If a guidance "be mindful of" item exists for the domain, offer it as a second optional
   starting point with mindful framing intact — copy the engine text verbatim; do NOT compose
   new interpretive sentences in the app layer (deterministic content stays in the engine).
3. Journal privacy rules unchanged: local-only, never analytics, deletable (PRD 13.6). The
   prompt card must not imply the entry is analyzed.

Ownership: ui/journal/JournalScreen.kt, its viewmodel wiring in viewmodel/ (read-only pass-through
of existing report data), strings_journal.xml (values/ + values-zh-rTW/) for chrome copy only —
prompt TEXT comes from the engine, never from string resources. contracts/ is FROZEN — do not
edit it. Do not modify engine-content templates in this item.

Tests: viewmodel unit test — prompt shown when report exists, hidden otherwise; insertion
prepends verbatim engine text; journal save path unchanged (existing tests stay green).
./gradlew :app:testDebugUnitTest detekt green.
```

### 2.4 Share-card refresh featuring guidance quotes

- **Why it matters:** Share cards are the only built-in growth loop (`share_cards_enabled=true`,
  PRD §13.7). A card that quotes one "lean into" guidance line is more shareable — and safer —
  than scores alone, and showcases the launch Guidance surface. §13.7 rules: no sensitive raw
  data, no palm photos, watermark, preview before share.
- **Effort:** M  **Platform:** Android (renderer exists), iOS follow-up

```text
Use PalmAstro_PRD_Full_v2_AppStoreLaunch.md (section 13.7, 30-32, 37) and
docs/launch/EXECUTION_SPEC.md as Source of Truth.

Refresh the share card to feature guidance:
1. New card layout in app/src/main/kotlin/com/palmastro/app/share/ShareCardRenderer.kt: month +
   domain, grade (not raw score by default), ONE "lean into" guidance line quoted from the
   engine output, brand watermark. Never include: "be mindful of" items (out-of-context mindful
   copy reads as a warning on social media), palm imagery, birth data, confidence internals,
   journal text.
2. Card copy comes verbatim from already-safety-filtered engine output; the renderer adds no
   interpretive text. Static chrome strings in strings_share.xml (values/ + values-zh-rTW/).
3. Preview dialog (ui/results/SharePreviewDialog.kt) lets the user pick between the existing
   score card and the new guidance card; guidance card is default when guidance exists.
4. Keep the share_audit.log behavior and share_cards_enabled flag gating unchanged.

Ownership: share/ShareCardRenderer.kt, share/ShareHelper.kt (only if the entry point needs the
variant param), ui/results/SharePreviewDialog.kt, strings_share.xml both locales. contracts/ is
FROZEN — do not edit it.

Tests: renderer unit test — guidance variant contains the guidance string, watermark present,
and NEVER contains blindspot/"be mindful" text or raw score when grade-only; preview default
selection test. ./gradlew :app:testDebugUnitTest detekt green.
```

---

## Stage 3 — Depth

### 3.1 Guided reflection mode (one mindful item → 3 journal micro-prompts)

- **Why it matters:** The "be mindful of" half of Guidance needs a constructive outlet or it's
  just information. Guided reflection converts a mindful item into three small journal questions
  — pure §2 Product Promise territory ("reflection", "action") and deepens the §7.1 journal
  without any new claims. Everything stays deterministic (PRD §19).
- **Effort:** M  **Platform:** Android first, iOS follow-up

```text
Use PalmAstro_PRD_Full_v2_AppStoreLaunch.md (sections 2, 12.3, 13.6, 19, 30-32) and
docs/launch/EXECUTION_SPEC.md as Source of Truth.

Build "Guided reflection": from a "be mindful of" item on the guidance/detail surface, a "Reflect
on this" action opens a 3-step flow (one question per step, one shared text field appended per
step, saved as a single journal entry).
1. The 3 micro-prompts come from the deterministic content engine: add a reflectionSteps template
   family to engine-content templates keyed by the existing signal/blindspot ids, en + zh-TW (+
   internal zh-CN/ja/hi capability like other templates), routed through the same validate() ->
   safe-fallback -> ToneRenderer -> filter() pipeline (EXECUTION_SPEC safety pipeline). Question
   shapes: (1) where do you notice this pattern, (2) what has it cost/given you lately, (3) one
   small thing to try this week. Never "why are you like this" phrasing; always agency-forward.
2. UI: a new ui/reflection/GuidedReflectionScreen.kt (Compose, Material 3), entry points from the
   guidance surface and domain detail. Progress dots, back-navigation preserves text, exit
   confirms discard. Strings for chrome in a new strings_reflection.xml (values/ + values-zh-rTW/);
   the questions themselves come from the engine, not string resources.
3. Saved entry is a normal journal row (local-only, deletable, PRD 13.6) prefixed with the
   mindful item title.

Ownership: engine-content template JSON + its tests, ui/reflection/*, journal viewmodel wiring,
strings_reflection.xml both locales, navigation/AppNavigation.kt route addition. contracts/ is
FROZEN — do not edit it (reflection steps ride on existing payload/journal shapes; if that seems
impossible, stop and escalate rather than touching contracts/).

Tests: engine golden tests for reflectionSteps (en + zh-TW, all three tones) incl. safety-filter
pass; UI state-machine unit test (step advance, text retention, save). ./gradlew
:engine-content:test :app:testDebugUnitTest detekt green.
```

### 3.2 Reading glossary / education center

- **Why it matters:** PRD §12.2 "Transparency beats mystery" — users should understand what was
  scanned and what signals mean. A glossary ("What is a head line?", "How confidence works",
  "What astrology inputs we use") turns mystique into trust, supports the §34.2 anti-spam
  positioning, and doubles as §7.1 "score education".
- **Effort:** M  **Platform:** Android + iOS (content shared)

```text
Use PalmAstro_PRD_Full_v2_AppStoreLaunch.md (sections 12.2, 7.1, 16-18, Appendix A) and
docs/launch/EXECUTION_SPEC.md as Source of Truth.

Build a small education center ("About your reading"):
1. Content: one entry per palm line region (headline, heartline, lifeline, fateline — matching
   EXECUTION_SPEC LineRegionMetrics regions), plus entries for: how scoring works, how confidence
   works, what astrology inputs are used (L1 vs L2 per EXECUTION_SPEC), what the app never claims
   (medical/financial/deterministic — reuse the framing of PRD 30-32). Each entry: what it is,
   what the app measures from it (categorical features only — never biometric identity, PRD 13.2),
   what it does NOT mean. Neutral-reflective register, no mystical promises.
2. Store entries as structured static content (JSON asset or Kotlin object) with en + zh-TW,
   rendered in a new ui/education/EducationScreen.kt (list -> detail). Entry points: settings
   "About your reading" row, "What's this?" affordances on explainability signal rows and
   guidance items (link by signalId where possible).
3. All UI chrome strings in a new strings_education.xml (values/ + values-zh-rTW/); entry bodies
   may live in the structured content with both locales inline.

Ownership: ui/education/*, the static content asset, strings_education.xml both locales, one row
in ui/settings/SettingsScreen.kt + strings_settings.xml, small tap-target additions in
explainability/guidance rows, navigation/AppNavigation.kt route. contracts/ is FROZEN — do not
edit it. No engine changes.

Tests: content-lint unit test — every entry has both locales, non-empty "what it does NOT mean"
section, and body text passes the same prohibited-terms list the safety filter uses (import or
mirror the term list; if mirroring, add a comment pointing at the canonical list). Navigation
test for signalId deep-link. ./gradlew :app:testDebugUnitTest detekt green.
```

### 3.3 Delta storytelling — "what changed since last month"

- **Why it matters:** Monthly delta tracking is the app's Apple-4.3 differentiator (PRD §34.2)
  and free tier includes "limited delta history" (§7.1). Today deltas are numbers; a one-line
  deterministic narrative per domain ("Career steadied this month — your consistency signals
  strengthened") makes the rescan ritual (2.1) emotionally worth repeating.
- **Effort:** M/L  **Platform:** Android first, iOS follow-up

```text
Use PalmAstro_PRD_Full_v2_AppStoreLaunch.md (sections 34.2, 7.1, 18-19, 30-32, 12.3) and
docs/launch/EXECUTION_SPEC.md as Source of Truth.

Add deterministic delta narratives:
1. In engine-content, add a deltaStory template family: input = previous vs current domain score
   band + confidence band + top contributing signal delta; output = ONE sentence per domain plus
   one whole-reading month summary line. Language rules: a score drop is NEVER decline/warning
   language — frame as "shifted", "asks for more attention", with the changed signal named
   (transparency, PRD 12.2). Confidence-drop cases must say the evidence changed, not the person.
   All five internal languages like other templates; en + zh-TW user-facing. Same validate() ->
   fallback -> ToneRenderer -> filter() pipeline as all content (EXECUTION_SPEC).
2. Surface: history screen (ui/history/HistoryScreen.kt) gets the month summary line at top;
   each domain card's delta chip on the results dashboard gets the domain sentence as its detail/
   expanded text. No red styling for negative deltas — use the neutral brand palette (PRD 37:
   red only for true errors).
3. First month (no previous report) shows nothing — no placeholder hype.

Ownership: engine-content templates + tests, ui/history/HistoryScreen.kt, the delta chip
expansion in ui/results/, strings_results.xml additions (chrome only, both locales). contracts/
is FROZEN — do not edit it; delta inputs must be computable from data already exposed to the app
layer — if not, stop and escalate.

Tests: engine golden tests covering up/down/flat/confidence-drop cases in en + zh-TW and all
three tones; a prohibited-language assertion (no "worse", "declined", "warning", "risk" in
output); UI test that first-month renders no delta story. ./gradlew :engine-content:test
:app:testDebugUnitTest detekt green.
```

### 3.4 Accessibility deep pass — TalkBack/VoiceOver walkthrough scripts

- **Why it matters:** PRD §40 is a launch acceptance area (§59) but ad-hoc labeling doesn't
  guarantee coherent flows. A scripted, repeatable screen-reader walkthrough of scan (the
  hardest surface: live camera + quality coaching) and guidance/results (the value surface)
  is what "world-class" (§36) actually requires.
- **Effort:** M  **Platform:** Android + iOS

```text
Use PalmAstro_PRD_Full_v2_AppStoreLaunch.md (sections 40, 41, 13.2, 12.2) and
docs/launch/EXECUTION_SPEC.md as Source of Truth.

Accessibility deep pass in two parts:
1. Write walkthrough scripts as docs/launch/a11y/talkback_walkthrough.md and
   docs/launch/a11y/voiceover_walkthrough.md: step-by-step expected announcements for
   (a) onboarding, (b) full 7-angle scan incl. per-angle quality result and retry coaching,
   (c) results dashboard -> guidance -> domain detail -> explainability, (d) journal entry,
   (e) settings data deletion. Each step: user gesture, expected focus target, expected
   announcement text (reference string resource ids, both locales).
2. Fix what the scripts expose. Likely: live-region announcements for scan quality changes
   (announceForAccessibility with strings from strings_scan.xml — every announcement via
   resources per EXECUTION_SPEC), meaningful contentDescription for score gauges ("Career, 68
   out of 100, grade B, confidence medium" — resource with placeholders), guidance list
   semantics ("lean into" vs "be mindful of" group headings announced), focus order on results,
   escape/dismiss actions for the share preview dialog.

Ownership: the two new docs, semantics/contentDescription changes across ui/*, string additions
to the owning screens' strings_*.xml (values/ + values-zh-rTW/ — announcements are user-visible
strings). contracts/ is FROZEN — do not edit it. No visual or behavioral changes beyond focus
order and semantics.

Tests: Compose semantics tests asserting the gauge/guidance descriptions resolve with
placeholders filled; lint check (or unit test) that no announceForAccessibility call sites use
raw string literals. ./gradlew :app:testDebugUnitTest detekt green; cd ios/PalmAstroKit &&
./test.sh green.
```

---

## Stage 4 — Platform integration

### 4.1 Home-screen widgets — Android Glance + iOS WidgetKit

- **Why it matters:** PRD §24 allows widgets under strict rules (no sensitive details, deep link
  into app, no ads/IAP). A "monthly theme" widget is the perfect fit: ambient, calm brand
  presence that supports the monthly ritual without notifications. Flag `widget_enabled` already
  exists (EXECUTION_SPEC §69 flags).
- **Effort:** L  **Platform:** Android + iOS

```text
Use PalmAstro_PRD_Full_v2_AppStoreLaunch.md (section 24, 12.3, 37) and
docs/launch/EXECUTION_SPEC.md as Source of Truth.

Build a monthly-theme widget behind widget_enabled (default stays false until QA):
1. Content — STRICTLY non-sensitive (PRD 24): current month's overall theme line (already
   non-sensitive dashboard copy) OR, if no report this month, a neutral "New month" state.
   NEVER: scores, grades, domain names with values, guidance items, birth data, streak-like
   counters, or any call-to-urgency. Tap deep-links to the results dashboard via the existing
   navigation/DeepLinkHandler.kt route (add one if missing).
2. Android: Glance app widget in a new app/src/main/kotlin/com/palmastro/app/widget/ package +
   res/xml widget provider info; sizes 2x2 and 4x2; night-sky brand styling per PRD 37; updates
   via WorkManager on report save and month rollover — no polling.
3. iOS: WidgetKit extension target (small + medium) reading a shared App Group snapshot written
   by the app containing ONLY {monthLabel, themeLine} — define the snapshot writer in the app
   layer, nothing sensitive in the App Group.
4. Widget strings in a new strings_widget.xml (values/ + values-zh-rTW/); iOS localized
   strings likewise.

Ownership: app widget/ package, res/xml provider file, DeepLinkHandler.kt route, strings_widget.xml
both locales, FeatureFlags read site; iOS widget extension + project.yml target + snapshot writer.
contracts/ is FROZEN — do not edit it.

Tests: unit test for the snapshot builder (theme present / absent month) asserting the snapshot
type contains no score/domain/birth fields (compile-time shape + runtime assertion); deep link
route test. ./gradlew :app:testDebugUnitTest detekt green; cd ios/PalmAstroKit && ./test.sh green.
```

### 4.2 App Shortcuts (Android) + AppIntents (iOS)

- **Why it matters:** PRD §12.4 native platform excellence: long-press shortcuts ("Start scan",
  "Open journal") and Siri/Spotlight intents are cheap native-quality signals both stores' human
  reviewers notice, and they deepen the §34.2 "real utility, not spam" positioning.
- **Effort:** S  **Platform:** Android + iOS

```text
Use PalmAstro_PRD_Full_v2_AppStoreLaunch.md (sections 12.4, 38-39, 24 rules by analogy) and
docs/launch/EXECUTION_SPEC.md as Source of Truth.

Add launcher/system shortcuts with zero sensitive content:
1. Android static shortcuts (res/xml/shortcuts.xml + AndroidManifest.xml meta-data): "Start scan",
   "This month's reading", "Journal" -> existing routes via navigation/DeepLinkHandler.kt.
   Shortcut labels in a new strings_shortcuts.xml (values/ + values-zh-rTW/). Thin-line icons per
   PRD 37 (palm / stars / journal motifs), added under res/drawable.
2. iOS AppIntents: OpenScanIntent, OpenReadingIntent, OpenJournalIntent with App Shortcuts
   phrases ("Scan my palm in PalmAstro") — navigation only; intents return no reading data to
   Siri/Spotlight (nothing sensitive leaves the app, PRD 25-27).
3. No dynamic shortcuts that embed report content.

Ownership: res/xml/shortcuts.xml, AndroidManifest.xml shortcut meta-data only, strings_shortcuts.xml
both locales, new drawable icons, DeepLinkHandler.kt additions; ios/App intents files + project.yml
if a target change is needed. contracts/ is FROZEN — do not edit it.

Tests: DeepLinkHandler unit tests for the three routes; manifest/xml lint passes. ./gradlew
:app:testDebugUnitTest detekt green; cd ios/PalmAstroKit && ./test.sh green.
```

### 4.3 Tablet / foldable adaptive layouts

- **Why it matters:** PRD §39 "adaptive layouts for large screens if feasible"; Play surfaces
  large-screen quality ratings, and the screenshot plan already anticipates 7"/10" tablet
  captures. Reading-heavy screens (results, guidance, detail, journal) benefit most from a
  two-pane treatment.
- **Effort:** L  **Platform:** Android first (foldables), iPad only if iOS declares support

```text
Use PalmAstro_PRD_Full_v2_AppStoreLaunch.md (sections 39, 36, 60 device matrix) and
docs/launch/EXECUTION_SPEC.md as Source of Truth.

Make the reading surfaces adaptive:
1. Adopt WindowSizeClass (material3-window-size-class) in MainActivity/AppNavigation and pass it
   down; DO NOT fork screens — each screen adapts internally.
2. Expanded width: results dashboard becomes a 2-column card grid; results/guidance + domain
   detail become list-detail two-pane (selection state survives rotation/fold); journal editor
   gets a max content width (~640dp) centered; scan stays single-pane full-bleed but repositions
   the quality coach beside the preview in landscape.
3. Foldables: table-top posture (half-open) on the scan screen puts camera preview on the upper
   half, guidance/coaching on the lower (Jetpack WindowManager fold info).
4. No new strings expected; if any, owning screen's strings_*.xml both locales.

Ownership: MainActivity/AppNavigation wiring, internal layout branches of ui/results, ui/guidance
(see roadmap header assumption for path), ui/detail, ui/journal, ui/scan layout code. No
viewmodel/data changes. contracts/ is FROZEN — do not edit it.

Tests: Compose tests at compact vs expanded widths asserting two-pane vs single-pane composition
(TestTags for pane containers); existing phone-width tests stay green. ./gradlew
:app:testDebugUnitTest detekt green.
```

---

## Stage 5 — Post-launch expansion

### 5.1 Additional UI languages — zh-CN, ja, hi

- **Why it matters:** PRD §43 names these as the "optional later" set and the content engine
  already carries all three internally (EXECUTION_SPEC languages decision) — UI resources are
  the only missing layer. §43 rule: never expose a language switch while its platform
  localization is incomplete.
- **Effort:** L  **Platform:** Android + iOS

```text
Use PalmAstro_PRD_Full_v2_AppStoreLaunch.md (sections 43-44, 19) and
docs/launch/EXECUTION_SPEC.md as Source of Truth.

Add one language at a time (separate session per language; start with zh-CN):
1. Create values-zh-rCN/ mirrors of EVERY strings_*.xml (same keys, no missing entries), plus the
   iOS .strings/.stringsdict counterparts. Register the locale in resConfigs and the language
   picker ONLY once 100% of keys exist — add a unit test that fails the build if any key is
   missing for an exposed language (PRD 43: hide incomplete languages).
2. Translation register: zh-CN follows the same calm register as zh-TW but with mainland
   conventions (App/软件 terms, 简体 punctuation); ja: polite です/ます, no fortune-telling
   register (占い is acceptable descriptively; avoid 運命 determinism); hi: Devanagari with
   everyday vocabulary. Re-verify safety phrasing per PRD 30-32 in each language — translated
   copy must not reintroduce prediction/medical/financial connotations.
3. Verify engine content: the content engine already emits this language internally — wire
   ContentInput.language pass-through for the new locale and snapshot-test one full report.
4. Update store metadata for the new locale only when the app language ships (docs/store/*).

Ownership: new values-* resource trees, language picker list (ui/settings + onboarding language
step), the key-parity unit test, iOS localization files; engine-content snapshot test additions.
contracts/ is FROZEN — do not edit it.

Tests: key-parity test green for all exposed languages; engine snapshot for the new language;
long-string layout spot-check at fontScale 1.3 for the three densest screens. ./gradlew
:engine-content:test :app:testDebugUnitTest detekt green; cd ios/PalmAstroKit && ./test.sh green.
```

### 5.2 Wear OS companion (ambient month theme + journal nudge)

- **Why it matters:** PRD §7.3 only forbids Wear OS "as a core launch promise" — post-launch it
  extends the calm-ambient positioning (same content rules as widgets, PRD §24). Flag
  `wear_enabled` already exists. Strictly scoped: a glanceable tile, not a reading experience.
- **Effort:** L  **Platform:** Android (Wear OS)

```text
Use PalmAstro_PRD_Full_v2_AppStoreLaunch.md (sections 24 by analogy, 12.3, 25-27, 7.3) and
docs/launch/EXECUTION_SPEC.md as Source of Truth.

Build a minimal Wear OS tile behind wear_enabled (default false):
1. New :wear module (do not touch existing module build files beyond settings.gradle.kts
   inclusion). Tile shows: month label + overall theme line only — the same non-sensitive
   snapshot contract as the 4.1 widget. No scores, no guidance items, no notifications from the
   watch, no standalone scanning.
2. Tap opens the phone app (remote intent). If no phone report exists: neutral empty state.
3. Data via Wearable Data Layer carrying ONLY {monthLabel, themeLine}; nothing else syncs to the
   watch (PRD 25-27 sensitive data stays on phone).
4. Strings in the wear module's own strings.xml pair (values/ + values-zh-rTW/).

Ownership: new :wear module, settings.gradle.kts one-line inclusion, phone-side snapshot sender in
app widget/wear sync package, FeatureFlags read site. contracts/ is FROZEN — do not edit it.

Tests: snapshot payload unit test (shape contains only the two fields); flag-off means no data
layer writes. ./gradlew :wear:testDebugUnitTest :app:testDebugUnitTest detekt green.
```

### 5.3 Paid deep-dive packs (when IAP lands)

- **Why it matters:** PRD §7.2 defines the paid tier; EXECUTION_SPEC froze launch as free-only
  with entitlement scaffolding retained and `ProductIds` already in contracts. The UX risk is
  §12.3: the paywall must be the industry's calmest — depth-framed, never fear-framed. Store
  docs (listing/metadata/data-safety/content-rating) must be updated in the same release.
- **Effort:** L  **Platform:** Android (Play Billing) + iOS (StoreKit 2)

```text
Use PalmAstro_PRD_Full_v2_AppStoreLaunch.md (sections 7.2, 22, 12.3, 13.3-13.4, 30-32) and
docs/launch/EXECUTION_SPEC.md as Source of Truth. Precondition: business decision to enable IAP;
ProductIds are FROZEN in contracts (palmastro.pack.career|wealth|bundle) — build against them,
never modify contracts/.

Ship paid deep-dive packs behind iap_enabled:
1. Paywall UX (new ui/paywall/ package + strings_paywall.xml both locales): depth-framed copy —
   what the pack ADDS (deeper delta interpretation, extended reflection prompts, monthly action
   plans per PRD 7.2). FORBIDDEN: countdowns, "unlock your fate", fear copy, fake discounts,
   preselected bundles, any implication free results are less true (PRD 12.3; safety visibility
   on the paywall per PRD 12.1 — the disclaimer strip appears here too).
2. Free tier untouched: everything listed in PRD 7.1 stays free; paid CTA appears only on
   dashboard/detail as the PRD 13.3/13.4 "if appropriate" slot, dismissible, max one per screen.
3. Billing: Play Billing + StoreKit 2 against existing entitlement scaffolding; restore purchases
   in Settings (PRD 13.8); "purchase unavailable" error state per PRD 42.
4. Same-release doc updates: docs/store/play/listing.md + data_safety_form.md + content_rating.md,
   docs/store/apple/metadata.md + app_privacy_labels.md, screenshot_plan.md shot 8 note (paid-pack
   shot becomes real per PRD 66).

Ownership: ui/paywall/*, billing integration in di/ + a new billing package, settings restore row,
strings_paywall.xml + strings_settings.xml additions both locales, the store docs listed.
contracts/ is FROZEN — do not edit it.

Tests: entitlement gating unit tests (flag off -> zero paid UI; flag on + not entitled -> CTA;
entitled -> content); paywall copy test asserting no prohibited terms (reuse safety-filter term
list); restore flow test with fake billing client. ./gradlew :app:testDebugUnitTest detekt green;
cd ios/PalmAstroKit && ./test.sh green.
```

---

## Explicitly excluded (do not schedule)

Per PRD §7.3 and §12.3, the following stay off this roadmap regardless of user requests or
engagement data. Any prompt output proposing them should be rejected in review:

1. Cloud LLM interpretations as a default consumer feature (PRD §7.3.1, §21).
2. Live human fortune tellers / chat (PRD §7.3.2).
3. Medical or health prediction features of any kind — including "health risk" widgets or
   notifications (PRD §7.3.4, §31).
4. Investment/financial advice features (PRD §7.3.5, §32).
5. Ads or ad SDKs (PRD §7.3.7).
6. Social matchmaking, compatibility-with-strangers, or friend feeds (PRD §7.3.8).
7. Open-ended user-generated content sharing inside the app (PRD §7.3.9).
8. Child-targeted flows (PRD §7.3.6; content rating remains 18+ per docs/store/play/content_rating.md).
9. Streaks, countdown timers, loss-framed re-engagement, fear-based paywalls, red-dot pressure
   (PRD §12.3, §11 "not exploitative") — this is why Stage 2 is explicitly "streak-free".
