# Local Build & UI/UX Inspection Guide

Everything needed to run PalmAstro locally and walk every screen. Written for
macOS (Apple Silicon); adjust paths for other hosts.

---

## 0. One-time prerequisites

```bash
# JDK 17 (the Gradle wrapper needs JAVA_HOME set — every time, or add to ~/.zshrc)
brew install openjdk@17
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home

# Android SDK — either install Android Studio (easiest) or verify a bare SDK:
#   local.properties must point at it, e.g.  sdk.dir=/Users/<you>/android-sdk
# Required packages: platforms;android-35, build-tools;35.0.0, platform-tools

# Fresh clone on macOS? Gatekeeper may quarantine the wrapper:
xattr -d com.apple.quarantine gradlew 2>/dev/null || true
```

## 1. Android on a physical device (recommended — the camera flow needs a real camera)

1. On the phone: **Settings → About phone → tap "Build number" 7×** to unlock
   Developer options, then enable **USB debugging**. Connect via USB and accept
   the trust prompt.
2. Build + install:

   ```bash
   export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
   ./gradlew :app:assembleDebug
   adb devices                 # confirm the device is listed
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

3. First run notes:
   - The hand-landmark model (~7.8 MB) downloads **once, over Wi-Fi/data**, at
     first scan — verified against a pinned SHA-256. Airplane mode on first scan
     shows the (intentional) download-error UX with retry.
   - Onboarding → 7-angle scan → results. Poor lighting triggers the coaching
     UX — worth inspecting deliberately.

## 2. Android emulator (no camera realism, fine for every non-scan screen)

```bash
SDK=~/android-sdk   # or ~/Library/Android/sdk with Android Studio
$SDK/cmdline-tools/latest/bin/sdkmanager "emulator" "system-images;android-35;google_apis;arm64-v8a"
$SDK/cmdline-tools/latest/bin/avdmanager create avd -n palmastro -k "system-images;android-35;google_apis;arm64-v8a" -d pixel_8
$SDK/emulator/emulator -avd palmastro &
./gradlew :app:assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The emulator's fake camera can complete captures (quality gate may fail
frames — use it to inspect the retry/coaching states, which is a feature tour
of its own).

### Skipping straight to results screens

The scan gates results on a real capture. Fastest inspection loop for
Results / Guidance / Detail / Explainability / History / Journal: complete one
scan of anything palm-like (a printed palm photo works on the emulator's
virtual scene camera), then all downstream screens are populated and reachable.

## 3. UI/UX inspection checklist (what to look at, screen by screen)

| Screen | Inspect |
|---|---|
| Onboarding (10 steps) | privacy promise copy, back navigation, optional-field clarity, wheel-free pickers, language switch applying instantly |
| Scan | pre-scan explainer, per-angle instructions, coaching on deliberate bad frames, permission-denied fallback (deny once), completion state |
| Results | month theme line, "This month" guidance card, delta arrows (needs 2 months of data), scan-quality chip, safety card |
| Guidance | lean-into vs mindful color language (must never read as alarming), week plan, footer disclaimer |
| Domain detail | Pattern/Trigger/Cost sections, journal button, "How was this calculated?" |
| Explainability | signed contribution bars, confidence reasons |
| Settings | language/tone switches, legal viewers (offline HTML), delete-all-data full round trip back to onboarding |
| Dark mode + TalkBack | toggle both — every screen has semantics; spot-check with TalkBack on |
| 繁體中文 | switch in Settings; every launch surface must be fully translated |

## 4. iOS (requires a Mac with Xcode 15.4+ — engines run anywhere)

```bash
# Engines + parity suite (works with Command Line Tools only):
cd ios/PalmAstroKit && ./test.sh          # expect: 163 tests, 0 failures

# Full app (Xcode required):
brew install xcodegen
cd ios && xcodegen generate
open PalmAstro.xcodeproj                   # select a simulator, Run
# Before device/TestFlight builds: set DEVELOPMENT_TEAM in project.yml
```

## 5. Release builds (when you're ready for the store)

```bash
# Signed AAB (keystore/palmastro-upload.jks + keystore.properties must exist — they are gitignored)
./gradlew :app:bundleRelease -PversionCode=2 -PversionName=0.2.0
ls app/build/outputs/bundle/release/app-release.aab
```

## Troubleshooting

| Symptom | Fix |
|---|---|
| `Unable to locate a Java Runtime` | `export JAVA_HOME=...` (see §0) — the wrapper reads the env var |
| `operation not permitted: ./gradlew` | `xattr -d com.apple.quarantine gradlew` |
| `SDK location not found` | create `local.properties` with `sdk.dir=/path/to/android-sdk` |
| First scan stuck on "downloading" | device offline — model downloads once from storage.googleapis.com |
| `swift test` fails in PalmAstroKit | use `./test.sh` (CLT ships Swift Testing outside default search paths) |
