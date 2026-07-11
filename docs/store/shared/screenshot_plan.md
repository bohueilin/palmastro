# Screenshot Plan — Both Stores

Source: PRD v2 §66 (required set), §67 (video guidance), §§30–32 (copy safety).
Locales: en-US and zh-TW/zh-Hant — capture each shot twice with the device language
switched. All captions below are pre-checked against the safety rules; do not improvise
new caption copy without re-checking.

## Ground rules

- Stage with **demo data** on a debug/`closedTest` build (`debug_scan_bypass_enabled`
  may be used to seed a completed scan on emulators); the release build ships with the
  flag off.
- Never show a real person's palm photo in any screenshot; shot 2 uses the guided
  overlay over a staged hand or illustrative placeholder frame.
- No captions that promise outcomes, predict the future, or mention health/wealth
  results. Scores shown should be mid-range (60s–70s) with visible confidence labels —
  avoid staging suspicious 100s.
- Android (Play): 1080×1920 minimum, phone set; capture 7-in and 10-in tablet variants
  of shots 3–5 if tablet screenshots are provided. iOS: 6.9" and 6.5" iPhone sets
  (iPad only if the iOS app declares iPad support).

## The eight shots (PRD §66)

| # | PRD slot | Actual screen (Android impl) | Staging | Caption en | Caption zh-TW |
|---|---|---|---|---|---|
| 1 | Onboarding | `ui/onboarding/OnboardingScreen.kt` — privacy-promise step | Fresh install, privacy step visible | Your data never leaves your device | 你的資料不會離開你的裝置 |
| 2 | Scan flow | `ui/scan/ScanScreen.kt` — angle guide + quality feedback | Mid-scan, angle 3/7, quality coach visible | A guided 7-angle palm scan with live quality feedback | 七角度掃描引導，即時回饋拍攝品質 |
| 3 | Dashboard | `ui/results/ResultsScreen.kt` — four domain cards | Demo data: 4 domains with score/grade/confidence + month theme + delta indicators visible | Four life areas, scored and explained | 四大生活領域，每個分數都有依據 |
| 4 | Domain detail | `ui/detail/DomainDetailScreen.kt` | Career domain, gauge + today/week actions visible | One action for today, one for the week | 今天一個行動，本週一個練習 |
| 5 | Explainability | Explainability section ("How was this calculated?") | Signal contribution list + confidence row visible | See exactly how every score was calculated | 每個分數怎麼來的，看得清清楚楚 |
| 6 | Journal | `ui/journal/JournalScreen.kt` | One staged reflective entry (neutral text, no personal data) | A private journal, stored only on your phone | 私密日記，只存在你的手機裡 |
| 7 | Settings/privacy | `ui/settings/SettingsScreen.kt` — retention toggle + delete-all | Scroll to privacy section: retention switch + Delete all data | Auto-delete photos in 24h — or keep nothing at all | 照片 24 小時自動刪除，也可以完全不保留 |
| 8 | Paid pack | **HERO — Guidance** `ui/guidance/GuidanceScreen.kt` ("Understand your reading") | Demo data: both groups visible — 2–3 "lean into" items + 1–2 "be mindful of" items, month label on screen | Know what to lean into — and what to be mindful of | 值得發揮的、需要留意的，一目了然 |

Shot 8 note: PRD §66 lists "Paid pack", but the frozen launch decision is free-only
(EXECUTION_SPEC: `iap_enabled=false`, no paid CTA visible). Shipping a paid-pack
screenshot would misrepresent the app to both stores. The slot instead becomes the
**hero shot of the Guidance surface** — the launch differentiator (PRD §34.2
"action-oriented self-growth") that turns each reading into what to lean into and what
to be mindful of. Staging rules for this shot: the "be mindful of" group must read as
gentle awareness (no red styling, no warning icons — PRD §12.3), and no item text may
touch health outcomes or money outcomes (PRD §§31–32). The former substitute
(History/monthly-delta) is covered by the delta indicators staged in shot 3.
Revisit the paid-pack slot when IAP ships.
*(Assumption (editable): Guidance screen path per docs/launch/UX_ROADMAP.md header —
substitute the real path if the launch agents landed it elsewhere.)*

## Caption rendering

Captions are rendered in the marketing frame (not in-app UI), brand font per PRD §37,
one short line, no ALL CAPS, no medical/financial words, no star-rating imagery.

## Order per store

- Play + App Store order: 3, 8, 5, 2, 7, 4, 6, 1 — lead with dashboard and the
  Guidance hero (the "know what to lean into / what to be mindful of" differentiator),
  then explainability; keep onboarding last.
  *(Assumption (editable): PRD fixes the set, not the order.)*

## App preview video (optional, PRD §67)

If produced: 15–25s silent-friendly capture showing scan → results → guidance →
explainability → privacy settings, no overclaiming text, no fast flashing (PRD §41 motion rules,
accessibility). Reuse the captions above as overlay text. Not a launch blocker.
