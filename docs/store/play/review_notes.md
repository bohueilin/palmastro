# Google Play Review Notes — PalmAstro

Paste the block below into Play Console → App content → "App access" notes / testing
instructions, and reuse for policy-team appeals if any. Source: PRD v2 §§34.3 (mirrored
for Play), 35, 69; EXECUTION_SPEC launch decisions.

---

## Notes for the review team

**What the app is.** PalmAstro is a privacy-first self-reflection app. It photographs
the user's palm, analyzes it entirely on the device together with tropical-astrology
signals derived from the user's birthday, and produces explainable, growth-oriented
reports across four life areas (career, wealth habits, family communication,
stress/recovery). It makes no predictive, medical, or financial claims; a rules-based
safety filter blocks such content by design.

**No account is required or supported.** The app has no login, no registration, and no
server backend. Nothing needs to be provisioned to test it.

**Camera usage.** The CAMERA permission is used exclusively to photograph the user's
palm during the guided scan. Frames are analyzed on-device; no image ever leaves the
device. Raw scan photos are auto-deleted within 24 hours by default, and the user can
disable photo retention entirely in Settings.

**Network usage.** The app's only network access is a one-time HTTPS download of the
open-source MediaPipe hand-landmarker model file from storage.googleapis.com the first
time a scan is started (integrity-checked via SHA-256). No user data is transmitted at
any point. Firebase libraries present in the APK are inert: no google-services.json is
packaged and no analytics or crash data is sent.

**Data deletion.** Settings → "Delete all data" wipes the encrypted local database, all
photos and derived features, rotates the local install identifier, and returns the app
to first-run state.

## Steps to test the core flow

1. Launch the app. Complete onboarding: privacy explanation → enter any birthday
   (required) → choose a dominant hand (required) → optional birth time/place (skip is
   fine; the app then uses the simpler L1 astrology mode) → pick a tone
   (Analytical / Gentle / Direct) → language (English or Traditional Chinese).
2. Start a scan. Grant the camera permission when prompted (requested only at this
   point). On first scan, the model file downloads once — an internet connection is
   needed for this single step.
3. Follow the on-screen guide through the seven capture angles (front, left/right tilt,
   close, full hand, tilt up/down). Live quality feedback coaches retries; a poorly lit
   palm will be rejected with a reason rather than producing a fake result.
4. View the results dashboard: four domain cards with score, grade, and confidence.
5. Open any domain → "How was this calculated?" shows the per-signal score breakdown
   (explainability). Each domain also offers a daily/weekly action and a reflection
   prompt with a private on-device journal.
6. Settings: toggle raw-photo retention, view the privacy policy and terms (bundled
   offline), and test "Delete all data" (returns to onboarding with a fresh install ID).
7. Optional: monthly reminders are OFF by default; enabling them triggers the
   POST_NOTIFICATIONS runtime prompt (the permission is requested only then).

## Feature flags (launch defaults)

The build ships with these defaults; no reviewer action needed. Listed for transparency:

| Flag | Default |
|---|---|
| daily_insights_enabled | false |
| llm_interpretations_enabled | false (no cloud/LLM content at launch) |
| iap_enabled | false (free-only launch; no purchase UI is shown) |
| wear_enabled | false |
| widget_enabled | false |
| share_cards_enabled | true |
| strict_safety_enabled | true |
| debug_scan_bypass_enabled | false |
| scan_reminders_enabled | true (reminders themselves are per-user opt-in, default off) |
| l2_astro_enabled | true |
| delta_tracking_enabled | true |

## Policy positioning notes

- The app is positioned and written as self-reflection/entertainment, not fortune
  telling: no "predict your fate/future", no guaranteed outcomes, no medical diagnosis,
  no financial advice anywhere in-app or in the listing (PRD §§30–32 filters enforce
  this at runtime).
- Palm analysis outputs categorical pattern summaries only — it is not biometric
  identification and no biometric template is created.
- Permissions are minimal: CAMERA, INTERNET, POST_NOTIFICATIONS (opt-in). No storage or
  media permissions; the advertising-ID permission is explicitly removed
  (see `permissions.md`).
