
# PalmAstro PRD Full v2.0 — App Store + Google Play Launch Source of Truth

**Product:** PalmAstro / 掌紋星象  
**Document type:** Full product requirements document, launch PRD, platform parity spec, privacy/compliance spec, UX spec, engineering execution source of truth  
**Platforms:** Android + iOS  
**Primary launch language:** Traditional Chinese (`zh-Hant` / `zh-TW`)  
**Launch markets:** Taiwan-first, then Hong Kong/Macau/Singapore/Malaysia/US diaspora expansion  
**Date:** 2026-07-09  
**Status:** Launch-readiness upgrade from functional Android prototype to dual-platform App Store + Google Play quality product  
**Audience:** Claude / multi-agent engineering, product, design, privacy, legal, QA, App Store / Play Store launch owners  

---

## 0. How to Use This Document

This document replaces the earlier PRD-only version and becomes the **single Source of Truth** for PalmAstro launch execution.

Use it for:

1. Product scope and launch gating.
2. Android stabilization and Play Store launch.
3. iOS greenfield implementation and App Store launch.
4. UX/design system alignment.
5. Privacy, safety, compliance, and store-review readiness.
6. Claude / multi-agent implementation planning.
7. QA, beta, release, metadata, and operational readiness.

### Claude / Agent Rule

When using this PRD with Claude or sub-agents:

- Do **not** invent new product requirements.
- Do **not** add cloud processing for palm images, biometric-like palm features, journals, or private user insights.
- Do **not** implement health diagnosis, investment advice, or deterministic future claims.
- Do **not** expose incomplete surfaces to store reviewers.
- If something is ambiguous, propose the smallest safe default and label it:  
  **`Assumption (editable): ...`**
- If implementation conflicts with this PRD, this PRD wins unless explicitly overridden by the product owner.

---

# Part I — Executive Product Definition

## 1. Product Vision

PalmAstro is a privacy-first self-growth app that combines **on-device palm analysis** with **tropical astrology** to help users reflect on recurring life patterns across four domains:

1. Career
2. Wealth
3. Family
4. Health / stress / recovery habits

The product uses metaphysical language as a reflective interface, but the experience is structured, transparent, and action-oriented.

PalmAstro is **not** positioned as a medical, financial, legal, or deterministic fortune-telling tool. It is a reflective guidance product with explainable signals, safety boundaries, and monthly tracking.

## 2. Product Promise

> “Scan your palm. Understand your patterns. Track your growth.”

PalmAstro should feel like a fusion of:

- A premium self-reflection app.
- A transparent palm + astrology interpreter.
- A gentle but honest coaching companion.
- A privacy-first on-device product.
- A world-class Apple/Android mobile experience.

## 3. Strategic Differentiation

PalmAstro must be meaningfully different from generic palm-reading / horoscope apps.

Core differentiators:

1. **On-device palm analysis**
   - Camera scan stays on device.
   - Palm features are derived locally.
   - Raw media is controlled by retention policy.

2. **Explainable scoring**
   - Users can open “How was this calculated?”
   - Scores are tied to palm and astrology signals.
   - The product avoids black-box “mystical output only.”

3. **Monthly rescan and delta tracking**
   - Users can see how their scores and signals change over time.
   - Delta is shown with a comparability score.
   - Low-comparability deltas are weakened or blocked.

4. **Self-growth framing**
   - Every interpretation includes action and reflection.
   - The product emphasizes habits, attention, boundaries, risk awareness, recovery, and communication.

5. **Visible safety and privacy**
   - Safety language appears in UI, not buried.
   - The app explains what it does and does not do.
   - Delete-all-data and retention controls are first-class features.

6. **Premium cross-platform UX**
   - Android must feel native to Material 3.
   - iOS must feel native to Apple Human Interface Guidelines.
   - The same product system should express itself through platform-native interaction patterns.

---

# Part II — Current State Baseline

## 4. Current Android State

The current Android implementation is already functional and has reportedly run on a Google Pixel 10 Pro. It includes a modular Kotlin architecture, CameraX, MediaPipe HandLandmarker, Room with SQLCipher, deterministic scoring/content engines, privacy-safe analytics, and a Compose UI.

### 4.1 Current Android Modules

Existing modules:

- `contracts`
- `engine-scan-quality`
- `engine-palm-features`
- `engine-astro`
- `engine-scoring`
- `engine-content`
- `svc-analytics`
- `data-room`
- `app`
- `integration-tests`

### 4.2 Current Implemented Capabilities

Current Android build reportedly includes:

- 6-step onboarding.
- 7-angle palm scan.
- MediaPipe-backed palm landmarking.
- Intensity-based palm line detection.
- 4-domain scoring.
- Multi-paragraph content generation.
- 5-language support.
- Score transparency breakdown.
- Score education card.
- Unicode-hardened safety filter.
- Circular score gauge.
- Journal.
- Home screen widget.
- Share cards.
- Deep links.
- Feature flags.
- Root/tamper/emulator/signature detection.
- Data wipe.
- CI/CD with tests and lint.
- Privacy policy.

### 4.3 Current Known Gaps

Current known gaps must be treated as launch blockers or launch-readiness backlog:

1. Wheel picker auto-resets.
2. Onboarding illustrations may not render on all devices.
3. Daily insights engine is designed but not fully wired.
4. LLM interpretation code exists but requires API key and currently falls back to templates.
5. IAP infrastructure exists but purchase UI is incomplete.
6. Wear OS module is minimal.
7. Some UI screens still use hardcoded English strings instead of Android string resources.

## 5. Launch-State Reframing

The project is no longer in pure product-discovery mode.

The updated goal is:

> Move PalmAstro from a functional Android prototype to a **dual-platform, app-store-launch-quality product** for Google Play and Apple App Store.

This requires:

- Android stabilization.
- iOS implementation.
- Store policy compliance.
- Native UX polish.
- Metadata and privacy declarations.
- QA, beta testing, release operations.
- Platform parity and feature gating.

---

# Part III — Launch Strategy

## 6. Launch Phases

### Phase 0 — Internal Partner Demo

Purpose: allow internal cross-functional partners and developers to install and test the Android APK.

Scope:

- Sideloadable debug APK.
- Core flow works end-to-end.
- Known unfinished features hidden or clearly disabled.
- No store submission yet.

Exit criteria:

- App installs on Pixel and at least 3 representative Android devices.
- Onboarding completes.
- 7-angle scan completes or safely fails with coaching.
- Results screen loads.
- Delete-all-data works.
- Crash-free internal demo session.

### Phase 1 — Android Closed Testing

Purpose: validate with broader controlled testers using Google Play internal / closed testing.

Scope:

- Release-signed Android build.
- Play Console setup.
- Data Safety draft.
- Privacy policy URL.
- Store listing draft.
- Feature flags configured for launch.
- IAP disabled or fully implemented.

Exit criteria:

- Closed test build approved and installable.
- No P0 crashes.
- Data safety declarations match actual behavior.
- Target SDK and permissions pass Play policy review.

### Phase 2 — iOS TestFlight Alpha

Purpose: implement iOS parity and test with internal users.

Scope:

- Native iOS app.
- Camera scan flow.
- On-device feature pipeline or equivalent iOS-compatible implementation.
- Shared rules/content schemas.
- StoreKit purchase scaffolding.
- Privacy manifest / App Privacy details draft.
- TestFlight build.

Exit criteria:

- TestFlight approval.
- Core flow works on iPhone.
- Camera permission purpose string approved internally.
- No incomplete demo content in App Review build.

### Phase 3 — Public Store Launch

Purpose: launch PalmAstro on Google Play and Apple App Store.

Scope:

- Production Android and iOS builds.
- Final metadata.
- Screenshots and videos.
- Privacy policy.
- Data Safety / App Privacy declarations.
- IAP live.
- Support URL.
- Delete-account / delete-data path.
- Review notes for Apple and Google.

Exit criteria:

- Both stores approve.
- Monitoring dashboards active.
- Rollback plan ready.
- Customer support path ready.

---

# Part IV — Product Scope

## 7. Launch Feature Tiers

## 7.1 Core Free Experience

Free users must receive enough value to trust the product.

Free scope:

1. Onboarding.
2. Palm scan.
3. Basic astrology profile.
4. Four domain scores.
5. Short narrative per domain.
6. Explainability breakdown.
7. Score education.
8. Journal entry per month/domain.
9. Limited delta history.
10. Privacy controls.
11. Language selection.
12. Settings and data deletion.

## 7.2 Paid Experience

Paid products should deepen understanding, not unlock manipulative fear-based predictions.

Paid scope:

1. Career Deep Dive Pack.
2. Wealth Deep Dive Pack.
3. Bundle Pack.
4. Deep delta interpretation.
5. Extended reflection prompts.
6. Monthly action plans.
7. Export/share enhancements.
8. Historical trend interpretation beyond free limit.

## 7.3 Not in Launch

Do not launch with:

1. Cloud LLM interpretations as a default consumer feature.
2. Live human fortune tellers.
3. Wear OS as a core launch promise.
4. Medical health prediction.
5. Investment advice.
6. Child-targeted flows.
7. Ads.
8. Social matchmaking.
9. Open-ended user-generated content sharing inside the app.

---

# Part V — Platform Scope and Parity

## 8. Android Launch Scope

Android should build upon the existing Kotlin/Compose implementation.

### Android Launch Requirements

- `targetSdkVersion`: must meet current Google Play target API requirements.
- `compileSdk`: latest stable at release.
- Minimum supported OS: product decision; current code has minSdk 26, but launch messaging should focus on modern Android devices.
- CameraX scan pipeline.
- MediaPipe or equivalent on-device palm landmarking.
- SQLCipher or equivalent encrypted local storage.
- WorkManager cleanup and reminders.
- Google Play Billing.
- Play Integrity / root/tamper checks as non-blocking risk signals.
- Glance widget may remain if stable.
- No broad photo/video library permissions unless strictly necessary.

### Android Launch Gates

- Build signed AAB.
- Debug APK and release AAB both verified.
- No `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` unless approved and core.
- Camera permission copy explains why the camera is needed.
- Data Safety completed and accurate.
- App content rating completed.
- Closed testing track completed.
- Play Billing purchase and restore verified.
- Crashlytics works if configured, but app does not crash without Firebase config in dev builds.

## 9. iOS Launch Scope

iOS should be native and premium, not a direct Android port.

### iOS Architecture Direction

Preferred:

- Swift.
- SwiftUI.
- MVVM or Composable Architecture-style state management.
- AVFoundation camera pipeline.
- Vision / Core ML / MediaPipe-compatible iOS pipeline for hand landmarks and image processing.
- SQLite/GRDB/SwiftData or encrypted local database, depending on final data requirements.
- Keychain for install ID / cryptographic secrets.
- StoreKit 2 for purchases.
- AppIntents / Widgets only if stable and clearly tied to app functionality.

### iOS Launch Requirements

- Native iOS navigation and gesture patterns.
- HIG-aligned UI.
- Clear camera permission purpose string.
- Privacy Nutrition Labels / App Privacy details.
- StoreKit purchase and restore.
- Delete-all-data.
- No incomplete cloud/LLM path exposed.
- Built-in demo mode if needed for App Review.
- App Review notes explain:
  - self-growth positioning,
  - on-device processing,
  - no medical/financial advice,
  - why the app is meaningfully different from fortune-telling apps.

### iOS Launch Gates

- TestFlight approval.
- No crash on clean install.
- Onboarding complete.
- Scan flow complete.
- Results complete.
- Purchase/restore complete.
- Delete-all-data complete.
- App Review metadata complete.
- App Store privacy details match actual SDK behavior.

## 10. Cross-Platform Parity

The following must be functionally equivalent across Android and iOS:

1. Domain model:
   - Career
   - Wealth
   - Family
   - Health

2. Score semantics:
   - 0–100 score.
   - Domain grade.
   - Confidence.
   - Explainability.

3. Safety boundaries:
   - Health soft-only.
   - Wealth soft-only.
   - No deterministic predictions.
   - No medical / investment advice.

4. Content engine:
   - Same semantic payload.
   - Platform-native rendering.
   - Same tone meanings.

5. Data practices:
   - On-device sensitive processing.
   - Raw media retention.
   - Delete-all-data.
   - Analytics restrictions.

6. IAP entitlements:
   - Career Pack.
   - Wealth Pack.
   - Bundle.
   - Restore purchases.

7. Store claims:
   - Same core value proposition.
   - Same safety disclaimers.
   - Same privacy promise, adapted to each store’s required language.

---

# Part VI — User Experience Requirements

## 11. UX North Star

PalmAstro must feel:

- Premium
- Calm
- Trustworthy
- Transparent
- Native
- Reflective
- Not gimmicky
- Not fear-based
- Not exploitative

## 12. UX Principles

### 12.1 Safety is visible

Safety must appear in:

- Onboarding.
- Result screen.
- Health domain.
- Wealth domain.
- Paywall.
- Privacy/About.
- App Review notes.

### 12.2 Transparency beats mystery

Users should understand:

- What was scanned.
- What signals contributed.
- How confidence works.
- Why a result may be low-confidence.
- What is action vs reflection vs entertainment.

### 12.3 Calm by default

Avoid:

- Red panic warnings.
- Countdown pressure.
- “Unlock your fate now” dark patterns.
- Fear-based paywall copy.
- Overly mystical claims.

### 12.4 Native platform excellence

Android should feel like a top-tier Material 3 app.

iOS should feel like a top-tier iOS app.

Do not use identical UI if it violates platform expectations.

## 13. Core UX Flows

## 13.1 Onboarding

Launch onboarding steps:

1. Welcome / value proposition.
2. Privacy promise.
3. Name / optional display name.
4. Birthday.
5. Dominant hand.
6. Relationship status (if retained).
7. Birth time / place optional.
8. Tone selection.
9. Language selection.
10. Summary.
11. Camera permission education.
12. Demo or scan entry.

### Onboarding Acceptance Criteria

- User cannot continue without birthday.
- User cannot continue without dominant hand.
- Optional fields are clearly optional.
- Birth time/place missing causes L1 astrology mode.
- Camera permission is requested only when needed.
- Permission denial produces a graceful fallback.
- No UI auto-resets while scrolling.
- All copy localized through platform resource system.

## 13.2 Scan Flow

Launch scan steps:

1. Explain scan requirements.
2. Show lighting/positioning tips.
3. Capture seven angles:
   - Front
   - Left Tilt
   - Right Tilt
   - Close Up
   - Full Hand
   - Tilt Up
   - Tilt Down
4. Show quality result per angle.
5. Coach retry if needed.
6. Show scan completion.
7. Proceed to analysis.

### Scan Acceptance Criteria

- Camera permission is clear and platform-native.
- User knows why each angle matters.
- Quality gate explains failure reason.
- The app never claims biometric identity.
- Low quality scan can still produce a conservative result or require rescan.
- Battery/thermal performance is acceptable.
- App does not crash on low-memory devices.

## 13.3 Results Dashboard

Each domain card must show:

- Domain name.
- Score.
- Grade.
- Confidence.
- Short insight.
- Delta indicator if available.
- Tap target to domain detail.

Dashboard must include:

- Overall month theme.
- Scan quality summary.
- Privacy/safety card.
- CTA for paid deep dive, if appropriate.

## 13.4 Domain Detail

Each domain detail must include:

1. Score gauge.
2. Score interpretation.
3. “How was this calculated?”
4. Observed signals.
5. Blind spot.
6. Today action.
7. Weekly action.
8. Reflection prompt.
9. Journal entry.
10. Related paid pack CTA, if relevant.

## 13.5 Explainability

Explainability screen must show:

- Baseline score.
- Palm signal contributions.
- Astro signal contributions.
- Confidence.
- Quality factors.
- Safety note.

The user should leave thinking:

> “This app may be reflective rather than scientific, but it is honest about how it reached the result.”

## 13.6 Journal

Journal must be:

- Private.
- Local-first.
- Optional.
- Per domain/month.
- Clear character limit.
- Deletable.

Do not send journal text to analytics or cloud services.

## 13.7 Share Cards

Share cards must:

- Avoid exposing sensitive raw data.
- Avoid showing palm images by default.
- Use a polished visual design.
- Include watermark/brand.
- Avoid medical/financial claims.
- Let users preview before sharing.

## 13.8 Settings

Settings must include:

- Account / purchases.
- Language.
- Tone.
- Notifications.
- Privacy.
- Raw media retention.
- Delete all data.
- About.
- Support.
- Privacy policy.
- Terms.
- App version.
- Build number.
- Diagnostic export for support, excluding sensitive data.

---

# Part VII — Feature Requirements

## 14. Onboarding Requirements

### Required Fields

- Birthday.
- Dominant hand.

### Optional Fields

- Name.
- Gender.
- Relationship status.
- Birth time.
- Birth place.

### Platform Requirements

Android:

- Compose UI.
- Fix wheel picker reset.
- Use string resources.
- Ensure TalkBack labels.

iOS:

- SwiftUI forms/wheels.
- Dynamic Type.
- VoiceOver labels.
- Native date/time pickers where appropriate.

## 15. Scan Requirements

### Inputs

- Camera feed.
- Seven-angle captures.
- MediaPipe / Vision landmarks.
- Quality scoring.

### Outputs

- Scan session.
- Quality scores.
- Best frames.
- Palm features.
- Confidence.

### Retention

- Raw media default retention: 24 hours.
- User can disable raw media retention.
- Derived features stored locally.
- Delete-all-data wipes everything.

## 16. Palm Feature Requirements

Palm feature extraction should produce categorical, explainable signals, not irreversible biometric identity templates.

Signals include:

- Life line clarity.
- Head line clarity.
- Heart line clarity.
- Fate line clarity.
- Curvature.
- Continuity.
- Length category.
- Relative depth category.
- Minor line density.
- Mount texture categories.

Do not claim:

- Identity verification.
- Medical diagnosis.
- Scientific biometric measurement.
- Guaranteed personality truth.

## 17. Astrology Requirements

System:

- Tropical zodiac.

Input:

- Birthday required.
- Birth time/place optional.

Modes:

- L1: birthday only.
- L2: birthday + time + place.

L1 must not show:

- Ascendant.
- Houses.
- House-based interpretations.

L2 may show:

- More detailed signals if accuracy is sufficient.

## 18. Scoring Requirements

Each domain score:

- 0–100.
- Grade.
- Confidence.
- Explanation.

Scoring must be deterministic.

Inputs:

- Palm signals.
- Astro signals.
- Scan quality.
- Confidence.
- Ruleset.

Output:

- Score card.
- Domain detail.
- Explainability.

## 19. Content Requirements

Content must be:

- Signal-aware.
- Localized.
- Tone-rendered.
- Safety-filtered.
- Non-deterministic in language.
- Action-oriented.

Supported launch languages:

- Traditional Chinese
- English

Supported post-launch languages:

- Simplified Chinese
- Japanese
- Hindi

If Android currently supports 5 content languages, maintain internal capability, but launch UI must not expose languages with incomplete platform localization.

## 20. Daily Insights

Daily Insights are a launch candidate but not required for first store launch unless fully wired.

If launched:

- Must be deterministic or rules-based.
- Must use safe content filter.
- Must not require cloud LLM.
- Must respect notification preferences.
- Must support language/tone.

If not ready:

- Hide behind feature flag.

## 21. LLM Interpretations

Cloud LLM interpretations must be disabled for public launch unless all of the following are true:

- Explicit user consent.
- No raw palm image upload.
- No journal text upload by default.
- No sensitive user data in prompts unless disclosed and consented.
- Privacy policy updated.
- Store privacy declarations updated.
- Safety filter gates all generated text.
- Fallback to deterministic templates.
- Failure mode is graceful.

Default launch recommendation:

> Template/rules engine only. Keep LLM internal/experimental.

## 22. IAP Requirements

Products:

- Career Pack.
- Wealth Pack.
- Bundle.

Required behavior:

- Product list loads.
- Purchase works.
- Restore works.
- Offline access after purchase.
- Graceful failure on network/billing failure.
- No paywall dark patterns.
- No fear-based paywall copy.

Android:

- Google Play Billing.

iOS:

- StoreKit 2.

## 23. Notifications

Notification types:

- Monthly scan reminder.
- Optional daily insight.
- Optional journal reminder.

Rules:

- Opt-in.
- Clear value.
- No sensitive text in notification body.
- Not required for app function.
- Easy opt-out.

## 24. Widgets

Android widget may launch if stable.

iOS widget should be considered post-launch unless core app is stable.

Widget rules:

- No sensitive details by default.
- Tapping widget deep links into app.
- Widget content must be tied to app functionality.
- Widget must not contain ads or IAP prompts.

---

# Part VIII — Privacy, Security, Safety

## 25. Privacy Principles

1. Process sensitive data on device.
2. Minimize data collection.
3. Store only what is needed.
4. Delete by design.
5. Make data practices visible.
6. No sensitive analytics.
7. No advertising tracking at launch.

## 26. Sensitive Data Classification

### Sensitive / High-risk

- Palm raw image/video.
- Palm feature summaries.
- Birthday.
- Birth time/place.
- Journal text.
- Generated insights.
- Purchase status.
- Install ID.
- Camera frames.
- Crash reports that may include metadata.

### Lower-risk

- App version.
- Locale.
- Feature flag state.
- Non-sensitive event names.
- Device model.
- OS version.

## 27. Storage Requirements

Android:

- Room + SQLCipher or equivalent.
- Android Keystore.
- Internal storage only.
- WorkManager cleanup.

iOS:

- Encrypted local storage.
- Keychain for secrets.
- File protection.
- No iCloud backup for sensitive raw media unless explicitly designed and disclosed.

## 28. Data Deletion Requirements

Delete-all-data must:

- Clear local profile.
- Clear scans.
- Clear raw media.
- Clear features.
- Clear insights.
- Clear journal.
- Clear install ID.
- Clear cached content.
- Rotate install ID.
- Return app to first-run state.

Do not delete purchase entitlements in a way that prevents restore.

## 29. Analytics Requirements

Allowed analytics:

- Screen views.
- Flow completion.
- Scan quality failure reason category.
- Purchase funnel.
- Crash diagnostics.
- Performance metrics.

Forbidden analytics:

- Raw images.
- Palm feature vectors.
- Journal text.
- Full birthday.
- Birth time/place.
- Generated private insights.
- User-entered free text.

## 30. Safety Filter Requirements

All generated or rendered content must pass safety filters.

Safety filters must block:

- Medical diagnosis.
- Disease prediction.
- Drug/treatment advice.
- Investment advice.
- Guaranteed money claims.
- Fear-based fate claims.
- Self-harm encouragement.
- Identity attacks.
- Profanity in roast mode.
- Bypass via Unicode variants.

## 31. Health Domain Policy

Allowed:

- Stress.
- Recovery.
- Rest.
- Boundaries.
- Habit awareness.
- “Consider talking to a professional” if user has concerns.

Forbidden:

- “You have X disease.”
- “You will get sick.”
- “This line means illness.”
- Treatment or medication advice.

## 32. Wealth Domain Policy

Allowed:

- Budgeting habit.
- Risk awareness.
- Impulse control.
- Planning.
- Reflection prompts.

Forbidden:

- Buy/sell recommendations.
- Investment timing.
- Guaranteed returns.
- “You will become rich/poor.”
- Financial diagnosis.

## 33. Security Requirements

Android:

- R8/proguard.
- Release signing.
- Play Integrity where appropriate.
- Root/tamper detection as non-blocking risk signal.
- No hardcoded production secrets.
- No API key for consumer LLM in public build unless privacy-reviewed.

iOS:

- No hardcoded secrets.
- Keychain storage.
- App Attest / DeviceCheck optional for abuse mitigation.
- StoreKit receipt validation or serverless StoreKit 2 entitlement handling.

Both:

- Dependency scanning.
- Secrets scanning.
- Crash reporting configured safely.
- Privacy audit before release.

---

# Part IX — Store Launch Requirements

## 34. Apple App Store Readiness

Apple launch must address:

1. App completeness.
2. Meaningfully different experience from generic fortune telling.
3. Privacy labels.
4. Camera purpose string.
5. IAP purchase/restore.
6. Support URL.
7. Privacy policy URL.
8. App Review notes.
9. No placeholder screens.
10. No broken links.
11. No incomplete cloud LLM flow.
12. No medical/financial claims.
13. Native iOS UX.

### 34.1 Apple Review Positioning

PalmAstro should be described as:

> A privacy-first self-reflection app that uses on-device palm feature analysis and tropical astrology signals to generate explainable personal growth reports across career, wealth habits, family communication, and stress/recovery patterns.

Avoid:

- “Predict your fate.”
- “Know your future.”
- “Guaranteed accurate palm reading.”
- “Health diagnosis.”
- “Financial prediction.”

### 34.2 Apple 4.3 Spam Mitigation

Because fortune-telling apps are a saturated category, PalmAstro must highlight:

- On-device scanning.
- Explainable scoring.
- Monthly delta tracking.
- Journaling.
- Transparent safety model.
- Privacy-first architecture.
- Action-oriented self-growth.

### 34.3 App Review Notes

App Review notes should include:

- Test account if login required.
- Demo mode if needed.
- Explanation that camera is used only for palm scan.
- Raw media retention details.
- No medical/financial advice.
- IAP restore location.
- Feature flags for reviewer.
- Steps to test core flow.

## 35. Google Play Readiness

Google Play launch must address:

1. Target API requirement.
2. Data Safety form.
3. Privacy policy.
4. Camera permission.
5. Photo/video permissions avoided unless needed.
6. Play Billing.
7. Content rating.
8. Store listing.
9. Closed/open testing.
10. App signing.
11. Crash and ANR monitoring.
12. SDK compliance.

### 35.1 Google Play Data Safety

Declare accurately:

- Whether analytics data is collected.
- Whether crash diagnostics are collected.
- Whether purchase data is collected.
- Whether data is encrypted in transit.
- Whether users can request deletion.
- Whether data is shared with third parties.

If Firebase Crashlytics/Analytics is included, Data Safety must reflect any transmitted diagnostics/usage data.

### 35.2 Google Play Permissions

Required:

- Camera.

Avoid:

- Broad photo/video permissions unless absolutely necessary.

If importing images from library is supported:

- Use Android Photo Picker where possible.
- Avoid broad media permissions.

### 35.3 Google Play Policy Positioning

PalmAstro must avoid deceptive claims.

Store copy should emphasize:

- Self-reflection.
- Entertainment/personal growth.
- Transparency.
- On-device privacy.
- No medical/financial advice.

---

# Part X — Design System

## 36. Design Quality Bar

PalmAstro must meet “world-class native app” quality.

Design must be:

- Premium.
- Native.
- Accessible.
- Clear.
- Calm.
- Trustworthy.
- Delightful without gimmicks.

## 37. Brand System

Working brand attributes:

- Mystic but modern.
- Calm but precise.
- Premium but accessible.
- Reflective but not heavy.
- Honest but not cynical.

Color direction:

- Purple as mystical intelligence.
- Teal as calm clarity.
- Dark mode as premium night-sky experience.
- Avoid aggressive red except true error states.

Typography:

- Platform-native typography scales.
- Support Dynamic Type / font scaling.
- Clear hierarchy.

Iconography:

- Thin-line premium icons.
- Avoid cliché crystal-ball overload.
- Use palm, stars, orbit, journal, signal, shield motifs.

## 38. iOS Design Requirements

iOS should use:

- SwiftUI-native structure.
- Large titles where appropriate.
- Native sheets.
- Native permission prompts.
- Native haptics.
- SF Symbols where appropriate.
- NavigationStack.
- Dynamic Type.
- VoiceOver.

Avoid:

- Android-style top app bars copied directly.
- Overly dense cards.
- Non-native wheel behavior unless justified.
- Custom controls that break accessibility.

## 39. Android Design Requirements

Android should use:

- Material 3.
- Compose.
- Dynamic color consideration where appropriate.
- Proper back behavior.
- Haptic feedback.
- Edge-to-edge layout where appropriate.
- TalkBack semantics.
- Adaptive layouts for large screens if feasible.

## 40. Accessibility Requirements

Must support:

- Screen reader labels.
- Dynamic font scaling.
- Color contrast.
- Touch target minimums.
- Reduced motion setting.
- Keyboard/focus order where applicable.
- Descriptive labels for scan progress.
- Non-color-only quality indicators.

## 41. Motion Requirements

Motion should:

- Clarify progress.
- Reinforce scan success.
- Avoid long delays.
- Respect reduced motion.
- Never hide loading or failures.

## 42. Error States

Every major flow requires:

- No camera permission.
- Poor lighting.
- Hand not detected.
- Low confidence.
- Missing birth time/place.
- Purchase unavailable.
- Network unavailable.
- Storage wipe failed.
- Content safety filter blocked output.

Each error must include:

- What happened.
- Why it matters.
- What to do next.

---

# Part XI — Localization

## 43. Launch Localization Strategy

Launch UI languages:

1. Traditional Chinese.
2. English.

Optional later:

- Simplified Chinese.
- Japanese.
- Hindi.

If content engine supports more languages than UI resources, hide incomplete UI language switches.

## 44. Localization Requirements

- No hardcoded strings in launch screens.
- Use platform string systems.
- Localize permissions.
- Localize store metadata.
- Localize safety disclaimers.
- Test with long strings.
- Test right-to-left only if supporting such languages.

## 45. Tone Names

Recommended:

- Scientific → “Analytical”
- Healing → “Gentle”
- Straight Talk / Roast Safe → “Direct”

Avoid “roast” in public UX if it feels unsafe.

---

# Part XII — Architecture

## 46. Shared Product Contracts

Android and iOS must share:

- Signal schema.
- Ruleset JSON schema.
- Content payload schema.
- Safety rules.
- Store product IDs mapping.
- Localization keys.
- Domain taxonomy.
- Event taxonomy.

## 47. Android Architecture

Android keeps the modular Kotlin architecture:

- `contracts`
- `engine-scan-quality`
- `engine-palm-features`
- `engine-astro`
- `engine-scoring`
- `engine-content`
- `svc-analytics`
- `data-room`
- `app`
- `integration-tests`

Required upgrades:

- Stabilize picker.
- Convert hardcoded strings.
- Hide incomplete surfaces.
- Complete IAP UI.
- Verify target SDK.
- Verify Data Safety.
- Verify release signing.
- Add partner/release flavors if needed.

## 48. iOS Architecture

Recommended modules:

- `PalmAstroApp`
- `CoreContracts`
- `ScanQualityEngine`
- `PalmFeatureEngine`
- `AstroEngine`
- `ScoringEngine`
- `ContentEngine`
- `SafetyEngine`
- `DataStore`
- `PurchaseService`
- `AnalyticsService`
- `DesignSystem`

Recommended technologies:

- SwiftUI.
- AVFoundation.
- Vision/CoreML/MediaPipe iOS where appropriate.
- StoreKit 2.
- Keychain.
- SQLite/GRDB/SwiftData with file protection.
- XCTest / XCUITest.
- Swift Package modules.

## 49. Cross-Platform Ruleset

Rulesets must be:

- JSON.
- Versioned.
- Deterministic.
- Testable.
- Backward compatible.
- Validated on app startup.
- Signed or checksummed if remotely updated later.

## 50. Content Library

Content must be:

- Versioned.
- Localized.
- Safety-tagged.
- Tone-renderable.
- Deterministic.
- Test-covered with snapshot tests.

## 51. Analytics Architecture

Use a privacy-safe event wrapper.

All events must pass:

- Allowlist event name.
- Allowlist properties.
- Denylist scan.
- Type validation.
- No free text.
- No sensitive payloads.

---

# Part XIII — Data Model

## 52. Core Entities

Required conceptual entities:

- `UserProfile`
- `BirthProfile`
- `ScanSession`
- `ScanAngleCapture`
- `PalmFeatureSummary`
- `AstroProfile`
- `MonthlyResult`
- `DomainScore`
- `ExplainabilityItem`
- `DeltaResult`
- `JournalEntry`
- `Entitlement`
- `InstallId`
- `PrivacySettings`
- `FeatureFlags`
- `ContentVersion`
- `RulesetVersion`

## 53. Key Data Rules

- `InstallId` rotates on delete-all-data.
- `Birthday` stays local.
- `Birth time/place` stay local.
- `JournalEntry.text` never leaves device.
- Raw scan media expires by policy.
- Entitlements can be restored from store.
- Purchase tokens/receipts handled securely.

---

# Part XIV — Acceptance Criteria

## 54. Launch Acceptance Criteria — Product

### P0 Product

- User can install app from store.
- User can complete onboarding.
- User can grant camera permission.
- User can complete scan or recover from scan failure.
- User can view dashboard.
- User can open domain detail.
- User can view explainability.
- User can write journal.
- User can delete all data.
- User can restore purchases.
- User sees privacy and safety information.

### P1 Product

- User can share card.
- User can receive reminders.
- User can view history.
- User can change language.
- User can change tone.

## 55. Launch Acceptance Criteria — Android

- Release AAB builds.
- Target API meets Google Play requirement.
- No prohibited permissions.
- Data Safety completed.
- Privacy policy URL live.
- Billing purchase/restore tested.
- Closed testing passed.
- Crash-free target met.

## 56. Launch Acceptance Criteria — iOS

- Release archive builds.
- TestFlight build approved.
- App Store privacy details completed.
- Camera purpose string clear.
- StoreKit purchase/restore tested.
- App Review demo flow works.
- No incomplete UI.
- Crash-free target met.

## 57. Launch Acceptance Criteria — Safety

- Health content contains no diagnosis.
- Wealth content contains no investment advice.
- Safety filter blocks prohibited content.
- LLM disabled or fully reviewed.
- Roast/direct tone contains no profanity/identity attack.
- Safety tests pass.

## 58. Launch Acceptance Criteria — Privacy

- Raw media retention works.
- Delete-all-data works.
- Analytics emits no sensitive fields.
- Crash logs sanitized.
- Privacy policy matches actual behavior.
- Store privacy declarations match actual SDKs.

## 59. Launch Acceptance Criteria — UX

- No major layout clipping.
- Dynamic text works.
- VoiceOver/TalkBack labels pass.
- Dark mode works.
- Loading states exist.
- Error states exist.
- Paywall is non-manipulative.
- Store screenshots match production UI.

---

# Part XV — QA and Testing

## 60. Test Matrix

### Devices

Android:

- Pixel flagship.
- Samsung Galaxy flagship.
- Mid-tier Android.
- Low-memory Android.
- Foldable/tablet if supported.

iOS:

- Current iPhone Pro.
- Standard iPhone.
- Older supported iPhone.
- iPad compatibility if supporting.

### OS Versions

Android:

- Minimum supported OS.
- Current OS.
- Upcoming beta if feasible.

iOS:

- Minimum supported iOS.
- Current iOS.
- Upcoming beta if feasible.

## 61. Test Types

- Unit tests.
- Snapshot tests.
- Integration tests.
- UI tests.
- Camera pipeline tests.
- Safety adversarial tests.
- Localization tests.
- Accessibility tests.
- Performance tests.
- Purchase sandbox tests.
- Store-review dry run.

## 62. Release Quality Metrics

Targets:

- Crash-free sessions: 99.5%+ during beta.
- P0 bugs: zero before submission.
- P1 bugs: product owner approval required.
- Scan completion rate: target threshold to be set from beta.
- Onboarding completion rate: target threshold to be set from beta.
- Delete-all-data success: 100% in QA.
- Purchase restore success: 100% in sandbox tests.

---

# Part XVI — Store Assets and Metadata

## 63. App Name

Options:

- PalmAstro
- PalmAstro: 掌紋星象
- 掌紋星象 PalmAstro

Recommendation:

- Taiwan listing: `掌紋星象 PalmAstro`
- English listing: `PalmAstro`

## 64. Short Description

Example:

> Privacy-first palm and astrology insights for self-reflection.

Chinese:

> 以掌紋與星象探索自我模式，隱私優先、清楚可解釋。

## 65. Full Description Requirements

Must include:

- What the app does.
- On-device privacy.
- Explainable scores.
- Four domains.
- Monthly tracking.
- Safety boundaries.
- No medical/financial advice.
- IAP details.

Must avoid:

- Guaranteed predictions.
- Health claims.
- Wealth promises.
- “Know your future.”
- “100% accurate.”

## 66. Screenshot Set

Required screenshots:

1. Onboarding.
2. Scan flow.
3. Dashboard.
4. Domain detail.
5. Explainability.
6. Journal.
7. Settings/privacy.
8. Paid pack.

## 67. App Preview Video

Optional but recommended.

Should show:

- Scan.
- Results.
- Explainability.
- Privacy controls.

Avoid:

- Overclaiming.
- Medical/financial claims.
- Fast flashing animations.

---

# Part XVII — Release Operations

## 68. Build Types

Android:

- `debug`
- `partnerDemo`
- `closedTest`
- `release`

iOS:

- `Debug`
- `Internal`
- `TestFlight`
- `Release`

## 69. Feature Flags

Launch flags:

- `daily_insights_enabled`
- `llm_interpretations_enabled`
- `iap_enabled`
- `wear_enabled`
- `share_cards_enabled`
- `widget_enabled`
- `strict_safety_enabled`
- `debug_scan_bypass_enabled`

Production defaults:

- `llm_interpretations_enabled=false`
- `strict_safety_enabled=true`
- `debug_scan_bypass_enabled=false`

## 70. Rollout Plan

Android:

1. Internal.
2. Closed test.
3. Open test optional.
4. Production staged rollout.

iOS:

1. Internal TestFlight.
2. External TestFlight.
3. App Review.
4. Release manually after approval.

## 71. Support Readiness

Required:

- Support email.
- FAQ.
- Privacy policy.
- Terms.
- Data deletion explanation.
- Refund guidance.
- Known issues page for beta.

---

# Part XVIII — Compliance and Policy References

## 72. Official References

Apple:

- App Review Guidelines: https://developer.apple.com/app-store/review/guidelines/
- Human Interface Guidelines: https://developer.apple.com/design/human-interface-guidelines
- App Privacy Details: https://developer.apple.com/app-store/app-privacy-details/
- StoreKit: https://developer.apple.com/storekit/

Google:

- Google Play Developer Policy Center: https://play.google.com/about/developer-content-policy/
- Google Play Data Safety: https://support.google.com/googleplay/android-developer/answer/10787469
- Google Play Target API Requirement: https://developer.android.com/google/play/requirements/target-sdk
- Photo and Video Permissions: https://support.google.com/googleplay/android-developer/answer/16935362
- Android Photo Picker / selected photo access: https://developer.android.com/about/versions/14/changes/partial-photo-video-access

---

# Part XIX — Updated Roadmap

## 73. Immediate Roadmap

### Workstream A — Android Launch Stabilization

1. Fix wheel picker reset.
2. Fix illustration visibility.
3. Convert hardcoded UI strings.
4. Hide or complete IAP UI.
5. Hide LLM public surface.
6. Verify deletion and retention.
7. Build release AAB.
8. Prepare Play Console.

### Workstream B — iOS MVP Build

1. Create iOS architecture.
2. Implement onboarding.
3. Implement camera scan.
4. Implement feature extraction.
5. Implement astrology.
6. Implement scoring.
7. Implement content rendering.
8. Implement results.
9. Implement local storage.
10. Implement StoreKit.
11. Implement privacy settings.
12. Prepare TestFlight.

### Workstream C — Shared Contracts

1. Finalize signal schema.
2. Finalize ruleset schema.
3. Finalize content payload.
4. Finalize safety rules.
5. Finalize localization keys.
6. Add cross-platform fixtures.

### Workstream D — Store Launch

1. Metadata.
2. Screenshots.
3. Privacy forms.
4. Review notes.
5. Support site.
6. Beta.
7. Launch.

---

# Part XX — Claude Execution Prompts

## 74. Claude Prompt — Current State Audit

Use this prompt after uploading this PRD:

```text
I uploaded "PalmAstro_PRD_Full_v2_AppStoreLaunch.md". Treat it as the single Source of Truth.

We are upgrading PalmAstro from a functional Android prototype to a dual-platform Android + iOS App Store / Google Play launch-quality product.

Do not generate code yet.

Please produce a Current State Launch Audit with:
1. What Android already has.
2. What Android must fix before Google Play launch.
3. What iOS must build from scratch.
4. Cross-platform contracts required.
5. Store policy risks.
6. Top 10 launch blockers.
7. Recommended execution order.

Keep output structured and concise.
```

## 75. Claude Prompt — Android Launch Stabilization

```text
Use the uploaded PRD v2 as Source of Truth.

Focus only on Android launch stabilization.

Output:
1. P0/P1/P2 bug list.
2. Exact fixes for wheel picker, illustration visibility, hardcoded strings, IAP incomplete UI, LLM disabled path.
3. Play Store launch checklist.
4. Build/release commands.
5. QA checklist.
6. Files likely touched.

Do not redesign iOS yet.
```

## 76. Claude Prompt — iOS Architecture

```text
Use the uploaded PRD v2 as Source of Truth.

Focus only on iOS architecture.

Output:
1. Swift module structure.
2. Data model mapping.
3. Camera scan pipeline.
4. Palm feature pipeline options.
5. StoreKit 2 entitlement model.
6. Local privacy/storage model.
7. SwiftUI screen map.
8. TestFlight readiness checklist.

Do not write implementation code yet.
```

## 77. Claude Prompt — Store Readiness

```text
Use the uploaded PRD v2 as Source of Truth.

Prepare a dual-store readiness package:
1. Apple App Review notes.
2. Google Play review notes.
3. App Store metadata.
4. Play Store metadata.
5. Data Safety draft.
6. App Privacy draft.
7. Screenshot plan.
8. Known limitation language.
9. Privacy policy delta.
10. Launch blocker checklist.

Do not add product features.
```

---

# Part XXI — Final Launch Definition

PalmAstro is launch-ready only when:

1. Android production build is approved by Google Play.
2. iOS production build is approved by Apple App Store.
3. Both apps deliver the same core product promise.
4. Privacy declarations match actual behavior.
5. Safety filters are active and tested.
6. Purchase/restore works.
7. Delete-all-data works.
8. No incomplete feature surfaces are visible.
9. App metadata avoids misleading claims.
10. Partners can demo the app without caveats that undermine trust.

---

# Appendix A — Signal Table Examples

## A1. Palm Signals

Example launch signals:

| ID | Name | Domain | Direction | Safety |
|---|---|---:|---:|---|
| PALM_HEADLINE_LONG_CLEAR | Head line long and clear | Career | + | Career |
| PALM_HEADLINE_CHAINED | Head line chained/broken | Career/Health | - | Health soft-only |
| PALM_FATELINE_STRONG | Fate line strong | Career | + | Career |
| PALM_FATELINE_BREAKS | Fate line breaks | Career | - | Career |
| PALM_HEARTLINE_DEEP | Heart line deep/curved | Family | + | Family |
| PALM_HEARTLINE_THIN | Heart line thin/straight | Family | - | Family |
| PALM_LIFELINE_CLEAR | Life line clear | Health habits | + | Health soft-only |
| PALM_LIFELINE_FAINT | Life line faint | Health habits | - | Health soft-only |
| PALM_VENUS_TEXTURE_DENSE | Venus mount texture dense | Family/Health | mixed | General |
| PALM_MINOR_LINES_DENSE | Minor lines dense | Health/Career | - | Health soft-only |

## A2. Astro Signals

Example launch signals:

| ID | Name | Calc | Domain | Safety |
|---|---|---|---:|---|
| ASTRO_SUN_EARTH | Sun in earth sign | L1 | Career/Wealth | General |
| ASTRO_SUN_AIR | Sun in air sign | L1 | Career/Family | General |
| ASTRO_MOON_WATER | Moon in water sign | L1 | Family/Health | Health soft-only |
| ASTRO_MERCURY_SATURN_HARD | Mercury Saturn hard aspect | L1 | Career/Health | Health soft-only |
| ASTRO_MARS_STRONG | Mars strong | L1 | Career | Career |
| ASTRO_VENUS_URANUS_HARD | Venus Uranus hard aspect | L1 | Wealth | Wealth soft-only |
| ASTRO_JUPITER_STRONG | Jupiter strong | L1 | Wealth/Career | Wealth soft-only |
| ASTRO_SATURN_STRONG | Saturn strong | L1 | Career/Wealth | General |
| ASTRO_ASC_FIRE | Fire ascendant | L2 | Career | Career |
| ASTRO_2ND_HOUSE | Second house emphasis | L2 | Wealth | Wealth soft-only |

---

# Appendix B — Content Payload Schema

```yaml
semantic_payload:
  domain: career|wealth|family|health
  month_key: "YYYY-MM"
  calc_level: L1|L2
  confidence:
    level: high|medium|low
    reasons: []
  observations:
    - signal_id: ""
      display_name: ""
      evidence_summary: ""
  interpretation:
    pattern: ""
    trigger: ""
    cost: ""
  blindspot: ""
  actions:
    today: ""
    week: ""
  prompt: ""
  safety_notes: []
  explainability:
    - mapping: ""
  score_card:
    total_score: 0
    grade: ""
    delta:
      value: 0
      arrow: up|down|flat
      comparability_score: 0
    subdims: []
```

---

# Appendix C — Privacy-Safe Event Taxonomy

Allowed event groups:

- onboarding
- demo
- scan
- inference
- results
- delta
- journal metadata only
- purchase
- restore
- settings
- delete all data
- crash/performance

Forbidden event payloads:

- raw media
- palm features
- journal text
- birthday
- birth time/place
- full generated insight text
- purchase token raw value

---

# Appendix D — Cross-Platform Architecture Contract

## D1. Core Engines

- Scan Quality
- Palm Feature Extraction
- Astrology
- Scoring
- Content
- Safety
- Analytics
- Storage
- Billing

## D2. Shared Contracts

- `UserProfile`
- `ScanSession`
- `PalmFeatureSummary`
- `AstroProfile`
- `ScoringInput`
- `ScoringResult`
- `SemanticPayload`
- `RenderedReport`
- `DeltaResult`
- `Entitlement`

## D3. Interface Requirements

Each engine must expose:

- deterministic input
- deterministic output
- version number
- error type
- confidence
- test fixture support

---

# Appendix E — Launch Blocker Taxonomy

## E1. P0 Blockers

- App fails to install.
- App crashes on launch.
- Onboarding cannot complete.
- Camera permission flow broken.
- Scan cannot complete or fail gracefully.
- Results cannot load.
- Delete-all-data fails.
- Purchase/restore broken if IAP visible.
- Privacy declarations mismatch behavior.
- Store metadata contains prohibited claims.

## E2. P1 Blockers

- Localization incomplete.
- Accessibility incomplete.
- Share cards broken.
- Widget stale.
- Daily insights partially wired.
- Non-critical animation/layout issues.

## E3. P2

- Cosmetic polish.
- Additional languages.
- Wear OS.
- LLM internal experiments.

---

# Appendix F — Store Submission Checklist

## Apple

- App binary complete.
- TestFlight tested.
- App Privacy complete.
- Privacy policy URL.
- Support URL.
- Camera purpose string.
- StoreKit products ready.
- Review notes.
- Screenshots.
- No placeholder content.
- No misleading claims.

## Google Play

- Target API compliant.
- Release AAB signed.
- Data Safety complete.
- Privacy policy URL.
- Content rating complete.
- Play Billing ready.
- Permissions minimized.
- Closed testing complete.
- Screenshots.
- Store listing.
- No misleading claims.

---

# End of PRD v2.0
