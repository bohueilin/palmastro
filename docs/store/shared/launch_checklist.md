# Store Submission Checklist — PalmAstro

Source: PRD v2 Appendix F, statuses as of **2026-07-09** (launch-hardening Wave 1 in
flight; owners per `docs/launch/EXECUTION_SPEC.md` file-ownership table). Update
statuses at each integration build; this file is the single tracking sheet for both
stores.

Legend: `[x]` done · `[ ]` open — status notes state owner and blocker.

## Google Play (PRD Appendix F)

- [ ] **Target API compliant** — status: in progress (build-release agent). Verify
      `targetSdk` meets the current Play target-API requirement in the release AAB.
- [ ] **Release AAB signed** — status: in progress (build-release agent). 2026-07-09
      audit P0: signing config was template-only and no AAB path existed; being wired
      now (`keystore.properties` + bundle task). Verify with a local
      `bundleRelease` + `apksigner verify` on the extracted APKs.
- [ ] **Data Safety complete** — status: answers drafted and final in
      `docs/store/play/data_safety_form.md` (No collected / No shared). Console entry
      pending. Re-verify no-egress claim with a traffic capture on the release build.
- [ ] **Privacy policy URL** — status: **blocker — hosting needed.** Final policy HTML
      exists (en + zh-TW) in `app/src/main/assets/legal/` and doubles as the web
      version; publish to a public HTTPS URL and paste into Play Console.
- [ ] **Content rating complete** — status: IARC answers drafted in
      `docs/store/play/content_rating.md`; questionnaire submission pending.
- [x] **Play Billing ready** — status: **N/A at launch by decision** (free-only launch,
      `iap_enabled=false`, no paid CTA — EXECUTION_SPEC). Nothing to configure;
      reopen this item when IAP ships.
- [ ] **Permissions minimized** — status: manifest already minimal (CAMERA, INTERNET,
      POST_NOTIFICATIONS); **open item:** AD_ID `tools:node="remove"` must land in the
      manifest (build-release agent; snippet in `docs/store/play/permissions.md`) and
      be verified in the merged release manifest.
- [ ] **Closed testing complete** — status: not started; blocked on signed AAB.
      PRD Phase 1 gate: ≥14 days closed testing, crash-free ≥99%.
- [ ] **Screenshots** — status: plan final in `docs/store/shared/screenshot_plan.md`
      (8 slots; paid-pack slot N/A-substituted); capture blocked on Wave-2 UI freeze.
- [ ] **Store listing** — status: copy final (en + zh-TW) in
      `docs/store/play/listing.md`; console entry pending hosting/graphics.
- [ ] **No misleading claims** — status: all drafted copy (listing, captions, legal,
      support) written to and checked against PRD §§30–32/65 prohibited lists; final
      pass required over the *built app UI* after Wave-2 string integration.

## Apple (PRD Appendix F)

- [ ] **App binary complete** — status: in progress (ios agent; PalmAstroKit + SwiftUI
      app under construction). 2026-07-09 baseline: iOS app did not exist.
- [ ] **TestFlight tested** — status: not started; blocked on binary. PRD Phase 2 gate.
- [ ] **App Privacy complete** — status: answers final in
      `docs/store/apple/app_privacy_labels.md` ("Data Not Collected", no tracking);
      App Store Connect entry pending; iOS `PrivacyInfo.xcprivacy` must match
      (ios agent).
- [ ] **Privacy policy URL** — status: **blocker — same hosting task as Play** (one
      URL serves both stores; zh-Hant variant linked or auto-negotiated).
- [ ] **Support URL** — status: page content final in
      `docs/store/shared/support.md`; hosting pending (same task).
- [ ] **Camera purpose string** — status: copy final (en + zh-Hant) in
      `docs/store/apple/review_notes.md`; wiring into Info.plist/InfoPlist.strings
      pending (ios agent).
- [x] **StoreKit products ready** — status: **N/A at launch by decision** (free-only
      launch; no StoreKit products, no restore UI). Reopen when IAP ships.
- [ ] **Review notes** — status: final in `docs/store/apple/review_notes.md`; paste
      into App Store Connect at submission.
- [ ] **Screenshots** — status: shared plan final; iOS capture blocked on binary.
- [ ] **No placeholder content** — status: open; verify after Wave-2 UI work that no
      placeholder screens/links remain (2026-07-09 audit flagged unreachable
      explainability/journal surfaces — being fixed by ui-results agent).
- [ ] **No misleading claims** — status: metadata drafted compliant
      (`docs/store/apple/metadata.md`, §34.1 prohibited-phrase check done); final pass
      over built app UI required.

## Cross-store items not in Appendix F but launch-blocking (from 2026-07-09 audit)

- [ ] Backup/encryption hardening: `allowBackup` disabled or scoped rules + SQLCipher
      actually wired (data-privacy agent; was P0 — DB shipped unencrypted).
- [ ] Room migration 1→2 crash fixed + v3 schema (data-privacy agent; P0).
- [ ] Test compilation restored so CI can gate the release (multiple agents; P0).
- [ ] In-app privacy policy/terms surface wired to `assets/legal/*.html`
      (ui-results agent, Wave 2 — files are in place, 4 files, en + zh-TW).
- [ ] Final release-build network capture: only egress is the one-time
      `storage.googleapis.com` model download.
