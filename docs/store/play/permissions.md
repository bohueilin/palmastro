# Android Permissions — Declarations and Justifications

Source: PRD v2 §35.2; current `app/src/main/AndroidManifest.xml`. This doc is the
canonical justification text for Play Console permission declarations and policy
responses.

## Declared permissions

### `android.permission.CAMERA`

- **Why:** Capturing the user's palm during the guided seven-angle scan. This is the
  app's core feature; `<uses-feature android:name="android.hardware.camera"
  android:required="true"/>` is declared because the app is not usable without it.
- **Scope:** The camera is opened only on the scan screen, only after an explicit
  user action, with the standard runtime permission prompt shown at that moment (not at
  app start). Frames are analyzed on-device by MediaPipe; no frame or photo is ever
  transmitted. Raw photos auto-delete within 24 hours by default; retention can be
  disabled entirely.
- **Console declaration text:** "Camera is used solely to photograph the user's palm
  for on-device analysis. Images never leave the device and are auto-deleted within
  24 hours by default."

### `android.permission.INTERNET`

- **Why:** A single purpose — the one-time HTTPS download of the MediaPipe
  hand-landmarker model file (pinned URL on `storage.googleapis.com`, SHA-256
  verified) before the first scan, and re-download if the file is corrupted.
- **Scope:** No other network calls exist. Analytics transmit nothing (Firebase inert —
  no `google-services.json`). Network security config permits HTTPS only (no cleartext).

### `android.permission.POST_NOTIFICATIONS`

- **Why:** Optional monthly rescan reminders, scheduled locally.
- **Scope:** Strictly opt-in. The profile default is reminders **off**; the runtime
  permission is requested only at the moment the user enables reminders in Settings
  (EXECUTION_SPEC launch decision). Denial simply leaves reminders off — no nagging.

### `android.permission.VIBRATE`

- **Why:** Haptic feedback in the scan and reveal flows (capture tick, quality-gate
  pass/fail, guidance reveal) — `HapticPlayer` in
  `app/src/main/kotlin/com/palmastro/app/haptics/Haptics.kt`, called from
  `ScanScreen.kt` and `GuidanceScreen.kt`.
- **Scope:** Short low-amplitude effects only; no-ops when the device has no vibrator
  or the system touch-feedback setting is off. Collects and transmits nothing. Normal
  install-time permission — no Play Console declaration form applies.

## Permissions intentionally NOT used

- **No photo/video/storage permissions** (`READ_MEDIA_IMAGES`,
  `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, etc.): scans come from the camera
  only; there is no gallery import at launch. If import is ever added, use the Android
  Photo Picker (permission-free) per PRD §35.2 — do not add broad media permissions.
- **No location, contacts, microphone, phone, or Bluetooth permissions.**
- Share cards are exported via `FileProvider` + the system share sheet, which needs no
  storage permission.

## Advertising ID — must be removed at the manifest level

Firebase dependencies can merge `com.google.android.gms.permission.AD_ID` into the
manifest even though we use no ads and transmit no analytics. The Data Safety form
declares no ad-ID collection, so the merged permission must be stripped:

```xml
<uses-permission
    android:name="com.google.android.gms.permission.AD_ID"
    tools:node="remove" />
```

**Integration note:** `AndroidManifest.xml` is owned by the build-release agent; this
snippet (plus `xmlns:tools="http://schemas.android.com/tools"` on the `<manifest>`
element) must land there before the release build. Verify in the final merged manifest
of the release AAB (`Merged Manifest` view or `aapt dump permissions`) that `AD_ID` is
absent.

In Play Console → App content → **Advertising ID**, declare: **No, the app does not use
an advertising ID.**

## Pre-submission verification checklist

- [ ] `aapt dump permissions` on the release AAB lists exactly: `CAMERA`, `INTERNET`,
      `POST_NOTIFICATIONS`, `VIBRATE` (plus any OS-implied ones such as
      `RECEIVE_BOOT_COMPLETED`/`SCHEDULE_EXACT_ALARM` only if WorkManager/reminders
      actually require them — audit anything unexpected).
- [ ] `com.google.android.gms.permission.AD_ID` absent from the merged manifest.
- [ ] Camera runtime prompt appears only on the scan screen; notifications prompt only
      when enabling reminders.
- [ ] Localized permission-rationale strings exist in `values/` and `values-zh-rTW/`
      (owned by the UI agents per the string-resources convention).
