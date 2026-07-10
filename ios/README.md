# PalmAstro iOS

Native iOS app (PRD v2 §9, §48): SwiftUI + MVVM app layer over
**PalmAstroKit**, a platform-neutral Swift package containing the same
deterministic engines as the Android app (scan quality, palm features,
astrology, scoring, content, safety, analytics, storage). Engines share JSON
resources and signal vocabulary with the Android modules for cross-platform
parity (PRD §10, §49).

```
ios/
├── PalmAstroKit/       Swift package — engines + contracts + tests (builds on macOS)
│   ├── Sources/…       CoreContracts, ScanQualityEngine, PalmFeatureEngine,
│   │                   AstroEngine, ScoringEngine, ContentEngine, SafetyEngine,
│   │                   AnalyticsService, DataStore
│   ├── Tests/…         per-engine suites + ParityTests
│   └── test.sh         test runner (handles CLT-only machines)
├── App/                SwiftUI app sources (compiled via the Xcode project)
├── shared-fixtures/    cross-platform parity fixtures (see its README)
├── project.yml         XcodeGen spec
└── README.md
```

## Engine tests (no Xcode required)

The Kit compiles and tests on macOS — engines import Foundation only
(no UIKit/SwiftUI/AVFoundation):

```sh
cd ios/PalmAstroKit
./test.sh                 # wraps `swift test`
```

With full Xcode installed, plain `swift test` works. On Command Line
Tools-only machines `test.sh` adds the framework/rpath flags Swift Testing
needs (see the comments in the script and Package.swift).

`Tests/ParityTests` consumes `ios/shared-fixtures/{scoring,content}` and
skips (with an explanatory message) any category whose fixture directory is
empty; fixtures are generated from the Android engines at integration time.

## Building the app

1. Install [XcodeGen](https://github.com/yonaskolb/XcodeGen) (`brew install xcodegen`)
   and Xcode 15.4+ (iOS 17 SDK).
2. Generate and open the project:

   ```sh
   cd ios
   xcodegen generate
   open PalmAstro.xcodeproj
   ```

3. Select the `PalmAstro` scheme, set your `DEVELOPMENT_TEAM` (project.yml
   comment shows where), and run on an iOS 17 device. The scan flow needs a
   physical device (camera + Vision hand pose).

Resource sync: the JSON files under `PalmAstroKit/Sources/*/Resources/` are
placeholder copies; canonical versions live in the Android engine resources
and are copied over at integration (see each `Resources/SYNC.md`). Legal
documents (`privacy-policy.md`, `terms-of-service.md`) are bundled into the
app target at integration from `docs/store/`.

## Launch posture (EXECUTION_SPEC)

- **Free-only launch**: `iap_enabled=false` — no paid CTA. The StoreKit 2
  scaffolding (`App/Support/PurchaseService.swift`: purchase, restore,
  entitlement persistence) stays wired for a post-launch flip.
- **Languages**: UI + content ship en + zh-Hant (zh-TW content). The content
  engine keeps additional languages as internal JSON capability.
- **Reminders**: opt-in; notification permission is requested only when the
  user enables them.
- **Privacy**: all inference on-device; raw scan frames retained ≤24h (user
  can disable retention entirely); delete-all-data wipes the JSON store, raw
  media, preferences, and rotates the Keychain install id.

## TestFlight checklist (PRD §56)

- [ ] `cd ios/PalmAstroKit && ./test.sh` — all engine suites green.
- [ ] `xcodegen generate` produces a project that archives in Release
      (`Product ▸ Archive`) with a valid team.
- [ ] Clean install: onboarding completes; birthday + dominant hand enforced;
      optional fields skippable; L1 mode messaging appears when birth time is
      omitted.
- [ ] Camera permission prompt shows the localized purpose string (en +
      zh-Hant); denial leads to the graceful fallback screen, not a crash.
- [ ] Full 7-angle scan on a physical device produces a monthly reading;
      low-light/glare coaching hints appear and are localized.
- [ ] Results, domain detail, explainability drawer, journal save, settings
      (language/tone/retention) all function with Dynamic Type at largest
      accessibility sizes.
- [ ] Delete-all-data: double confirmation; after wiping, the app returns to
      onboarding and no prior data is visible.
- [ ] Purchase/restore: with `iap_enabled=false` nothing paid is visible
      (launch posture). Before enabling IAP post-launch: sandbox purchase,
      restore on reinstall, and entitlement persistence must be tested.
- [ ] App Privacy details match actual behavior (no tracking, no data
      collection off-device; camera data processed on-device only).
- [ ] App Review notes attached: self-growth positioning, on-device
      processing, no medical/financial advice, difference from
      fortune-telling apps (PRD §34).
- [ ] No incomplete UI reachable; crash-free on clean install.
