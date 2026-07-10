# Apple App Review Notes — PalmAstro

Source: PRD v2 §§34.1–34.3. Paste the block below into App Store Connect → App Review
Information → Notes. Keep under App Review's practical length limits; trim the flag
table first if needed.

---

## Notes for App Review

**Positioning.** PalmAstro is a privacy-first self-reflection app that uses on-device
palm feature analysis and tropical astrology signals to generate explainable personal
growth reports across career, wealth habits, family communication, and stress/recovery
patterns. It is not fortune telling: it makes no predictions about the future, no
medical or health claims, and no financial advice — a built-in rules-based safety
filter blocks such content at render time.

**No account, no login, no server.** The app has no registration and no backend. No
test account is needed; a fresh install shows the full experience.

**Camera.** The camera is used only to photograph the user's palm during the guided
scan. All analysis runs on the device; no image or derived data is ever uploaded.

**Raw media retention.** Raw palm photos are automatically deleted within 24 hours of
capture by default; the user can disable photo retention entirely in Settings. Derived
categorical features (not a biometric template, not usable for identification) are
stored in a locally encrypted database. Settings → "Delete all data" performs a full
local wipe and returns the app to first-run state.

**Network.** The app's only network request is a one-time HTTPS download of the
open-source MediaPipe hand-landmarker model file (storage.googleapis.com,
integrity-checked) before the first scan. Nothing else is ever transmitted — no
analytics, no crash reports, no cloud AI. There is no incomplete cloud/LLM flow: LLM
interpretations are disabled (`llm_interpretations_enabled=false`) and no such UI is
reachable.

**IAP.** None at launch. The app is fully free with no purchase or restore UI shown, so
there is no restore-purchases location to test. (Entitlement scaffolding exists in code
behind `iap_enabled=false` for a possible future release.)

## How PalmAstro differs from generic fortune-telling apps (4.3 differentiation)

1. **On-device scanning** — a real 7-angle guided camera capture with live quality
   gating, not a canned reading.
2. **Explainable scoring** — every score ships with a "How was this calculated?"
   breakdown listing each palm/astro signal's contribution, plus a confidence level
   derived from scan quality.
3. **Deterministic rules engine** — same inputs, same outputs; no random fortunes.
4. **Monthly delta tracking** — rescans compare against prior months per domain.
5. **Journaling** — a private, on-device journal tied to each domain's reflection
   prompt.
6. **Transparent safety model** — medical/financial/deterministic-fate claims are
   filtered out by design, and the app says so in the UI.
7. **Privacy-first architecture** — no account, no data leaves the device, encrypted
   local storage, user-controlled retention and one-tap full deletion.
8. **Action-oriented self-growth** — each report gives one action for today, one for
   the week, and a reflection prompt, rather than predictions.

## Camera purpose string (NSCameraUsageDescription) — suggested copy

- en: `PalmAstro uses the camera only to photograph your palm during a scan. Photos are analyzed on this device and are never uploaded.`
- zh-Hant: `掌紋星象只會在掃描時使用相機拍攝你的手掌。照片僅在這部裝置上分析，絕不會上傳。`

(Owned by the ios agent's Info.plist/InfoPlist.strings; keep both locales in sync with
this doc.)

## Demo steps (5 minutes)

1. Launch → onboarding: privacy explanation, enter any birthday, pick a dominant hand,
   optionally skip birth time/place, choose a tone (Analytical/Gentle/Direct) and
   language (EN or 繁體中文).
2. Tap Scan → grant camera permission (requested only here) → first run downloads the
   model file once (needs internet for this step only).
3. Follow the seven-angle capture guide with live quality feedback; poor lighting is
   rejected with an explanation instead of a made-up result.
4. Results dashboard: four domain cards (score, grade, confidence, delta when a prior
   month exists).
5. Open a domain → view interpretation, blind spot, today/this-week actions, reflection
   prompt, journal; tap "How was this calculated?" for the per-signal breakdown.
6. Settings → toggle photo retention, open Privacy Policy / Terms (bundled, offline),
   and optionally "Delete all data" to see the full wipe + first-run reset.
   Reminders are off by default; enabling them triggers the notification permission
   prompt (the only time it appears).

## Feature flags (reviewer transparency)

Launch defaults: daily_insights=false, llm_interpretations=false, iap=false, wear=false,
widget=false, share_cards=true, strict_safety=true, debug_scan_bypass=false,
scan_reminders=true (per-user reminders default off), l2_astro=true,
delta_tracking=true. No reviewer action needed.

## Support and policy URLs

- Support URL: hosted support/FAQ page (content: `docs/store/shared/support.md`).
- Privacy Policy URL: hosted copy of `app/src/main/assets/legal/privacy_policy_en.html`
  (zh-Hant: `privacy_policy_zh-TW.html`).
