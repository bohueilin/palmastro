# Google Play Data Safety Form — PalmAstro

Source: PRD v2 §§25–29, 35.1; EXECUTION_SPEC launch decisions. Fill the Play Console
"Data safety" section exactly as below for the launch build.

## Ground truth the answers rest on

- All palm/astro/journal/report processing is on-device. Nothing is transmitted.
- Firebase dependencies are present in the build but **inert**: no `google-services.json`
  is packaged, so Analytics/Crashlytics initialize nothing and transmit nothing.
- The only network egress is a one-time HTTPS download of the MediaPipe hand-landmarker
  model file from `storage.googleapis.com` (a plain file GET; no user data attached).
- No accounts, no ads, no ad IDs (`com.google.android.gms.permission.AD_ID` is removed
  via manifest `tools:node="remove"` — see `permissions.md`).
- Local DB is SQLCipher-encrypted; delete-all-data wipes everything and rotates the
  install ID.

Play's definition of "collected" is data transmitted off the device. On-device-only
processing and data that never leaves the device are not "collected". Camera frames are
processed ephemerally on-device. Therefore:

## Launch answers (enter exactly)

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |

That single "No" completes the questionnaire. The store listing will display
**"No data collected"** and **"No data shared"**.

If the console version in use still shows the follow-up questions, answer:

| Question | Answer |
|---|---|
| Is all of the user data collected by your app encrypted in transit? | N/A (no data collected) — if a Yes/No is forced, answer **Yes** (the only network call is HTTPS) |
| Do you provide a way for users to request that their data is deleted? | N/A (no data collected) — note: the app has in-app "Delete all data" regardless |

## Account deletion requirement

Play's account-deletion policy applies to apps that support account creation.
PalmAstro has **no accounts**, so the "Account deletion" URL section is **not applicable**.
If a reviewer asks: all user data is local; Settings → Delete all data performs a full
local wipe and install-ID rotation.

## Why the model download is not "collection"

The one-time model fetch is a static file download. The request carries no user
identifiers or content. (As with any HTTPS request, Google's file host transiently sees
an IP address; this is service-provider infrastructure, not app data collection, and is
disclosed in the privacy policy.)

## Conditional answer set — ONLY if Firebase Analytics/Crashlytics is enabled post-launch

Do not use this at launch. If a future release ships `google-services.json` and turns on
Analytics and/or Crashlytics, resubmit the form as follows (and update the privacy
policy + this doc first):

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **Yes** |

Data types to declare (all: Collected = Yes, Shared = No*):

| Category | Data type | Collected | Shared* | Ephemeral | Required or optional | Purposes |
|---|---|---|---|---|---|---|
| App activity | App interactions (screen views, event names per PRD Appendix C) | Yes | No | No | Optional (gate behind an in-app opt-in toggle) | Analytics |
| App info and performance | Crash logs | Yes | No | No | Optional | Analytics |
| App info and performance | Diagnostics | Yes | No | No | Optional | Analytics |
| Device or other IDs | Device or other IDs (Firebase installation ID / app-instance ID) | Yes | No | No | Optional | Analytics |

\* Firebase acts as a service provider processing on the developer's behalf, which Play
exempts from "sharing" disclosure. Declare Shared = No unless that arrangement changes.

Follow-ups in the conditional case:

| Question | Answer |
|---|---|
| Is all of the user data collected by your app encrypted in transit? | **Yes** |
| Do you provide a way for users to request that their data is deleted? | **Yes** (in-app Delete all data rotates local IDs; users can email support@palmastro.app for Firebase-side deletion requests) |

Hard limits that must stay true even then (PRD §29): never transmit raw images, palm
feature vectors, journal text, full birthday, birth time/place, generated insights, or
any user-entered free text. Event taxonomy is restricted to PRD Appendix C.

## Re-check triggers

Re-review this form whenever: a new SDK is added, any network call is added, Firebase is
activated, IAP/Play Billing is added (purchase-history declaration), or ads are ever
introduced (they must not be).
