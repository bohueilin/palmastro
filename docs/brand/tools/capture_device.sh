#!/usr/bin/env bash
# Capture PalmAstro store screenshots and screen recordings from a connected device.
#
#   ./capture_device.sh shots [outdir]     # one PNG per screen, light + dark
#   ./capture_device.sh record <name> [s]  # screen recording for the store video
#   ./capture_device.sh a11y               # large-font + reduced-motion sanity pass
#
# Requires: adb on PATH, the app installed, device unlocked with the screen ON.
set -euo pipefail

PKG=com.palmastro.app
ACT="$PKG/.MainActivity"
OUT="${2:-./device-capture}"

need_device() {
  local n
  n=$(adb devices | grep -cw device || true)
  if [ "$n" -eq 0 ]; then
    echo "No device. Plug in the phone, unlock it, and accept the USB-debugging prompt." >&2
    exit 1
  fi
  # A locked or sleeping screen silently yields all-black screencaps.
  adb shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
  if adb shell dumpsys window 2>/dev/null | grep -q "isKeyguardShowing=true"; then
    echo "Device is locked — unlock it and re-run (screenshots would come out black)." >&2
    exit 1
  fi
}

shot() { # shot <name>  — settle, then capture
  sleep "${SETTLE:-2}"
  adb exec-out screencap -p > "$OUT/$1.png"
  echo "  $1.png"
}

set_theme() { # set_theme light|dark
  adb shell "cmd uimode night ${1/light/no}" >/dev/null 2>&1 || true
  sleep 1
}

case "${1:-shots}" in
  shots)
    need_device; mkdir -p "$OUT"
    for theme in light dark; do
      echo "== $theme =="
      set_theme "$theme"
      adb shell am force-stop $PKG; adb shell am start -n "$ACT" >/dev/null; SETTLE=4 shot "${theme}_01_launch"
      # Deep links skip the manual walk; see navigation/DeepLinkHandler.kt for the scheme.
      # Valid hosts only — see DeepLinkHandler.parse: results, domain, scan, history, settings.
      # "scan" is skipped: it opens the camera and needs a real hand in frame.
      for d in results history settings; do
        adb shell am start -a android.intent.action.VIEW -d "palmastro://$d" >/dev/null 2>&1 || true
        shot "${theme}_$d"
      done
    done
    set_theme light
    echo "Wrote $(ls -1 "$OUT"/*.png | wc -l | tr -d ' ') screenshots to $OUT"
    ;;

  record)
    need_device
    name="${2:-segment}"; secs="${3:-20}"
    echo "Recording ${secs}s -> $name.mp4 — drive the app now."
    adb shell screenrecord --size 1080x1920 --bit-rate 12000000 --time-limit "$secs" /sdcard/_rec.mp4
    adb pull /sdcard/_rec.mp4 "$name.mp4" >/dev/null && adb shell rm /sdcard/_rec.mp4
    echo "Wrote $name.mp4"
    ;;

  a11y)
    need_device; mkdir -p "$OUT"
    echo "== large font (2.0x) + reduced motion =="
    adb shell settings put system font_scale 2.0
    adb shell settings put global animator_duration_scale 0.0
    adb shell am force-stop $PKG; adb shell am start -n "$ACT" >/dev/null; SETTLE=4 shot "a11y_fontscale2_launch"
    for d in results settings; do
      adb shell am start -a android.intent.action.VIEW -d "palmastro://$d" >/dev/null 2>&1 || true
      shot "a11y_fontscale2_$d"
    done
    adb shell settings put system font_scale 1.0
    adb shell settings put global animator_duration_scale 1.0
    echo "Restored font scale and animation scale."
    ;;

  *) echo "usage: $0 {shots|record|a11y}" >&2; exit 2 ;;
esac
