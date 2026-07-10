# Apple App Privacy Labels — PalmAstro

Source: PRD v2 §34 item 3; §§25–29. Fill App Store Connect → App Privacy with the
answers below.

## Headline answer

**Data Not Collected** — for every category. In App Store Connect, answer
"Do you or your third-party partners collect data from this app?" → **No,
we do not collect data from this app.**

Apple's definition of "collect" is transmitting data off the device in a way that is
accessible to the developer or partners for longer than servicing the request. PalmAstro
transmits **no user data at all**: every feature runs on-device, there is no backend, no
account, no analytics or crash reporting egress (Firebase libraries are present but
inert — no config is shipped), and no third-party SDK that phones home. The app's single
network request is a one-time static file download (the MediaPipe hand-landmarker
model); it carries no user data.

**Tracking:** None. No data is used for tracking; App Tracking Transparency is not
required and must not be prompted.

## Per-category rationale (for audit/review defense)

| Apple label category | Data the app touches | Why "Not Collected" |
|---|---|---|
| Contact Info (name, email, phone, address, other) | Optional display name typed in onboarding | Stored only in the on-device encrypted DB; never transmitted. No email/phone ever requested. |
| Health & Fitness | None | Stress/recovery content is reflective text generated on-device; no health data is gathered or transmitted, and the app makes no medical claims. |
| Financial Info | None | No payments at launch, no financial data entry. Wealth-domain content is habit reflection only. |
| Location | None | Location permission never requested. Optional birth *place* is a user-typed historical fact used for local astrology math; it is not device location and never leaves the device. |
| Sensitive Info | Birthday, optional birth time/place | Processed and stored on-device only (SQLCipher-encrypted); never transmitted. |
| Contacts | None | Contacts access never requested. |
| User Content (photos/videos, audio, customer support, other) | Palm photos; journal entries; generated reports | Photos are analyzed on-device, auto-deleted within 24h by default (retention user-disableable). Journal and reports live only in the local encrypted DB. Nothing is uploaded. Support email is user-initiated outside the app. |
| Browsing History | None | No browser; legal pages are bundled HTML rendered offline. |
| Search History | None | No search feature that transmits anything. |
| Identifiers (user ID, device ID) | Random local install ID | Generated on-device, used only to organize local data, never transmitted, rotated on delete-all-data. Not accessible to the developer. |
| Purchases | None | Free-only launch; no IAP, no purchase history. |
| Usage Data (product interaction, advertising data, other) | None transmitted | No analytics egress at launch. |
| Diagnostics (crash data, performance data, other) | None transmitted | No crash reporting egress. (Apple's own opt-in crash reports to developers via Xcode/App Store Connect do not require declaration.) |
| Surroundings / Body (environment scanning, hands, head) | Camera frames of the user's hand | Processed ephemerally on-device for landmark detection; frames are not retained beyond the scan (raw photos ≤24h, user-disableable) and are never transmitted. On-device-only processing is not "collection" under Apple's definition. |
| Other Data | None | — |

## Invariants that keep this label true

Any of these changes invalidates "Data Not Collected" and requires updating the label
(and the privacy policy) **before** release:

1. Activating Firebase Analytics/Crashlytics (→ declare Usage Data + Diagnostics +
   Identifiers, "not linked to you", "no tracking").
2. Adding IAP (→ Purchases may need declaration if receipts/entitlements are processed
   server-side; StoreKit-2 local entitlements generally do not).
3. Adding any cloud/LLM interpretation path (`llm_interpretations_enabled` must remain
   false at launch).
4. Adding any SDK with network egress. Run a network-traffic capture on a release build
   before every submission to verify the only host contacted is
   `storage.googleapis.com` (one-time model download).

## Privacy manifest note (iOS build)

The iOS target must ship a `PrivacyInfo.xcprivacy` consistent with this label:
`NSPrivacyTracking = false`, no tracking domains, no collected data types, and required
Reason API declarations only for the standard SDK reasons actually used
(e.g. UserDefaults CA92.1). Owned by the ios agent — flagged here as an integration
requirement so the label and manifest never diverge.
