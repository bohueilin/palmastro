# PalmAstro — Visual & Video Production Guide

**Status:** Source of truth for every pixel and frame that represents PalmAstro.
**Audience:** design, marketing, contractors, and AI generation tools.
**Companion:** `PalmAstro_PRD_Full_v2_AppStoreLaunch.md` (product), `docs/audit/` (current defects).

---

## 0. The one-paragraph brief

PalmAstro turns a palm scan into a monthly reflection. Its visual identity is **one motif**: *palm lines rising into a constellation*. Everything — app illustrations, loading states, store art, video — is a variation on that single idea. The tone is **calm, premium, honest**. It is not mystical kitsch, not a fortune-teller aesthetic, not glowing crystal balls. The nearest neighbours are Oura, Headspace, and Apple Fitness+ — instruments that happen to be beautiful — not horoscope apps.

**The anti-brief.** No crystal balls. No tarot cards. No robed fortune tellers. No neon "mystic" purple-pink gradients. No countdown timers or urgency. No stock-photo hands with sparkles. No fake UI in marketing that the app cannot actually show.

---

## 1. The visual system (authoritative values)

These are the real values in `app/src/main/kotlin/com/palmastro/app/ui/components/BrandIllustration.kt` and `ios/App/Support/BrandPalette.swift`. Any asset that misses them looks like a different product.

### Core palette

| Token | Hex | Role |
| --- | --- | --- |
| Night sky (top) | `#231A4A` | Illustration panel gradient start |
| Night sky (bottom) | `#140F2E` | Illustration panel gradient end |
| Calm teal | `#4FD1C5` | Palm lines — the "you" element |
| Royal purple | `#6B46C1` | Brand anchor, primary actions |
| Lavender | `#9F7AEA` | Constellation edges — the "sky" element |
| Starlight | `#EDEAFB` | Stars, text on night sky |

### Grade palette (never improvise these)

| Grade | Light | Dark |
| --- | --- | --- |
| Growing | `#2E7D32` | `#81C784` |
| Stable | `#00695C` | `#80CBC4` |
| Building | `#5E35B1` | `#B39DDB` |
| Watch-out | `#B35A00` | `#FFB77C` |

**Watch-out is amber, never red.** Red is reserved for true errors. A cautionary month is coaching, not an alarm — this is a product rule, not a style preference.

### Construction rules for any illustration

1. **Night sky panel**, 20 dp corner radius, vertical gradient `#231A4A → #140F2E`.
2. **Palm lines** as smooth quadratic curves in teal: a 2 dp core at 92% alpha over a 7 dp glow at 16%.
3. **Constellation edges** as 1.1 dp lavender lines at 42% alpha connecting star nodes.
4. **Stars** as starlight dots with a halo at 20% alpha, ~3.2× the core radius.
5. **Palm lines live in the lower half; the constellation rises into the upper half.** Always. That vertical grammar — body below, sky above — is the whole metaphor.
6. **Twinkle** is a 2.6 s sine breath between 72% and 100% alpha. Never a blink, never a flash.

---

## 2. Where visuals belong in the app — and where they do not

The app is already fully code-drawn (`BrandIllustration`, `DomainGlyph`, `ConstellationReveal`, `ScoreGauge`). That was deliberate: raster illustrations cost ~23 MB, broke dark mode, and looked generic.

**Rule: do not add raster or video assets to the app bundle unless they teach something vectors cannot.**

| Surface | Treatment | Why |
| --- | --- | --- |
| Onboarding (8 steps) | Code-drawn `BrandScene` | Crisp at every density, theme-correct, ~0 bytes |
| Empty states | Code-drawn `BrandScene` | Same |
| Post-scan processing | `ConstellationReveal` animation | The signature moment |
| Domain cards | `DomainGlyph` | Consistent, tintable |
| **Scan technique** | **⚠️ The one real gap — see §3** | Motion genuinely teaches this |
| Store listing | Video + screenshots | Outside the bundle |
| Landing page / social | Video | Outside the bundle |

### The one honest case for video *inside* the app

Seven capture angles (front, left tilt, right tilt, close, full hand, tilt up, tilt down) are **motion instructions**. A still frame cannot show "rotate your wrist slowly to the left until the edge of your palm faces the lens." This is the single place where moving image beats vectors.

**Recommended: a silent, ~2 s looping demo per angle, code-drawn, not video.** An animated hand *silhouette* rotating through each pose, drawn with the same vector language as the rest of the app, costs nothing and stays on-brand. Ship real video only if user testing shows the silhouette is insufficient — and if so, use one ~12 s WebM (VP9, ≤ 400 KB, no audio) covering all seven, downloaded on demand alongside the MediaPipe model rather than bundled.

---

## 3. Video strategy

Four assets, in priority order. Do not make all four at once — #1 earns the others.

| # | Asset | Length | Where | Purpose |
| --- | --- | --- | --- | --- |
| 1 | **Store preview** | 20–25 s | Play listing, App Store | Convert the store visitor |
| 2 | **Scan technique loops** | 7 × 2 s | In-app (or on-demand) | Reduce failed captures |
| 3 | **Brand film** | 45–60 s | Landing page, investor deck | Explain the product's soul |
| 4 | **Social cuts** | 6–15 s | Paid acquisition, organic | Top of funnel |

### Platform specs

**Google Play**
- Promo video: a **YouTube URL** (not an upload). 30 s–2 min; Play autoplays it in the listing. No age restriction, no ads on the video.
- Feature graphic: 1024 × 500 PNG/JPG — required, and it is what shows before the video plays.
- Phone screenshots: 2–8, 16:9 or 9:16, min 320 px, max 3840 px.

**App Store**
- App preview: 15–30 s, H.264/HEVC, up to 500 MB, **portrait 886 × 1920 or 1080 × 1920** for 6.5"/6.7". Up to 3 per localisation.
- First frame is the poster frame — it must stand alone.
- Apple rejects previews that show anything the app does not literally do.

**Social**
- 9:16, 1080 × 1920, H.264, ≤ 30 s, **designed to work silently** (85% of feed video is muted). Burn in captions.

---

## 4. Video generation prompts

Written for **Veo 3**, **Sora 2**, **Runway Gen-4**, and **Kling 2.x**. Each prompt is self-contained. Generate 4–6 variants per shot and cut the best.

> **Critical constraint for all AI-generated footage:** AI cannot render the actual app UI convincingly. **Never** ask a model to generate the app's screens — that produces fake UI, which App Store review rejects and which breaks the product's own honesty rule. Use AI for *atmosphere and hands*; composite **real screen recordings** for anything showing the product.

### 4.1 Hero shot — "the hand and the sky"

```
A close-up of an open human palm held up in soft, low evening light against a deep
indigo background. The camera slowly pushes in. Faint teal light traces the natural
creases of the palm, following the existing lines exactly — like light finding a path
that was already there, not drawn on top. The traced lines lift gently off the skin
and rise upward, becoming small points of pale lavender starlight that connect into a
quiet constellation above the hand. Extremely slow, calm motion. Cinematic macro
photography, shallow depth of field, natural skin texture, no jewellery, no text,
no user interface. Colour palette strictly deep indigo #140F2E, soft teal #4FD1C5,
pale lavender #9F7AEA. Serene, contemplative, premium. 24fps, subtle film grain.
```

**Negative prompt:** `neon, glitter, sparkles, magic, wizard, crystal ball, tarot, fortune teller, gaudy, saturated pink, purple haze, text, watermark, UI, screen, fast motion, zoom bursts, lens flare`

### 4.2 Establishing shot — "the quiet morning"

```
A person sits alone by a window in early morning light, holding a phone, looking
thoughtful rather than excited. Muted interior, warm neutral tones, soft natural
light from one side. They are still. The mood is reflective and private — a moment
of checking in with oneself, not of discovery or celebration. Handheld with almost
no movement, shallow depth of field, documentary realism. No visible screen content.
No text, no UI. Calm, unhurried, 24fps.
```

**Negative prompt:** `smiling at camera, excited, celebration, stock photo, group of people, office, bright saturated colours, screen visible, UI, text`

### 4.3 Transition texture — "constellation drift"

```
Abstract slow drift through a field of small pale stars on a deep indigo gradient
background, from #231A4A at the top to #140F2E at the bottom. Thin lavender lines
trace between some stars, forming and dissolving quiet constellation shapes.
Extremely slow parallax. Nothing pulses or flashes. Meditative, minimal, elegant.
Seamlessly loopable. No text, no faces, no objects.
```

Use as a 2–3 s bridge between live-action and screen-recording segments.

### 4.4 Scan-technique reference (for the animator, not the store)

```
A single human hand, palm facing the camera, against a plain dark background, slowly
rotating to show the palm from a specific angle. The motion is deliberate and even,
taking two full seconds, with a brief hold at the end position. Even, diffuse lighting
with no harsh shadows across the palm. Macro lens, neutral skin tone, no jewellery,
no text, no UI. Reference footage quality — clear and instructional.
```

Generate seven takes, substituting the end pose: `flat and facing the lens` · `rotated slightly left so the thumb edge lifts` · `rotated slightly right so the little-finger edge lifts` · `moved closer to fill the frame` · `moved back so the whole hand and wrist are visible` · `tilted so the fingertips angle toward the lens` · `tilted so the heel of the palm angles toward the lens`.

**Use these as rotoscoping reference for the vector silhouettes** described in §2 — not as shipped footage.

### 4.5 Prompting notes per model

| Model | Strength here | Watch for |
| --- | --- | --- |
| **Veo 3** | Best photoreal hands and skin; native audio | Ask for silence explicitly, or you get stock music |
| **Sora 2** | Strongest at slow, coherent camera moves | Drifts toward cinematic drama — keep repeating "calm, still, unhurried" |
| **Runway Gen-4** | Best image-to-video; feed it a still you already like | Weaker at hand anatomy — generate hands elsewhere |
| **Kling 2.x** | Cheapest iteration, good motion | Over-saturates; state hex values and "muted" |

**Hands are still the hardest subject for every model.** Budget 6–10 generations per hand shot, and consider filming real hands — a palm on a dark background with one soft light is a 30-minute phone shoot, and it will beat AI on anatomy every time.

---

## 5. Store preview — the 22-second edit

The structure that converts. Screen recordings are **real**, from a real device.

| Time | Content | Source | On-screen text |
| --- | --- | --- | --- |
| 0–3 s | Hero: palm, lines lift into stars | AI (§4.1) | *Your patterns, not your fortune* |
| 3–6 s | Real capture flow, brackets, coaching | Screen recording | *Scan in about a minute* |
| 6–9 s | `ConstellationReveal` playing | Screen recording | *Everything stays on your device* |
| 9–14 s | Results dashboard, scroll to a domain card | Screen recording | *Four areas, scored and explained* |
| 14–18 s | "How was this calculated?" → explainability | Screen recording | *See exactly how* |
| 18–22 s | History, month-over-month deltas | Screen recording | *Track how you change* |
| 22 s | Wordmark on night sky | Vector | *PalmAstro* |

**Rules.** Silent-first — every claim readable without audio. Cuts on the beat of the app's own motion, never faster than the app actually moves. If music is used: ambient, no percussion, no build. Text on screen ≥ 4.5:1 against its background, minimum 32 px at 1080 wide.

**Capture the screen recordings with:**

```bash
adb shell screenrecord --size 1080x1920 --bit-rate 12000000 /sdcard/seg.mp4
```

---

## 6. Screenshot set (8 frames)

Order matters — most users only see the first two.

1. **Results dashboard** — the payoff, first. Caption: *Four areas. One monthly reading.*
2. **Constellation reveal** — the signature. Caption: *Analysed entirely on your device.*
3. **Domain detail with gauge** — depth. Caption: *A score, and what it means.*
4. **Explainability** — the differentiator. Caption: *See every signal behind it.*
5. **Guidance** — utility. Caption: *What to lean into this month.*
6. **History with deltas** — retention. Caption: *Watch the pattern change.*
7. **Journal** — intimacy. Caption: *Private reflections, stored only here.*
8. **Privacy/settings** — trust. Caption: *Delete everything, any time.*

Frame each on the night-sky gradient with the caption above the device, 64 px from the top. Never crop the status bar. Use a clean device frame; show a realistic time and full battery.

---

## 7. Production pipeline

```
1. Script + storyboard        →  this document
2. Screen recordings          →  adb screenrecord on a real Pixel, 1080x1920
3. AI atmosphere shots        →  §4 prompts, 4–6 variants each
4. Assembly                   →  cut to 22 s, silent-first
5. Export                     →  H.264, CRF 18, yuv420p, faststart
6. Verify                     →  mute it and watch; if it doesn't land, rewrite the text
```

**Export command:**

```bash
ffmpeg -i edit.mov -c:v libx264 -crf 18 -preset slow -pix_fmt yuv420p -movflags +faststart preview_1080x1920.mp4
```

**Loopable in-app WebM (only if §2's exception applies):**

```bash
ffmpeg -i angle.mov -c:v libvpx-vp9 -crf 40 -b:v 0 -an -vf scale=720:-2 angle.webm
```

---

## 8. Definition of done

- [ ] Every colour traced to §1 — no improvised hexes
- [ ] Palm lines below, constellation above, in every frame
- [ ] Watch-out amber `#B35A00`, never red
- [ ] No fabricated UI anywhere; every screen is a real recording
- [ ] Readable and complete with sound off
- [ ] No urgency, scarcity, countdowns, or deterministic claims
- [ ] Store text matches what the app literally does
- [ ] Text ≥ 4.5:1 on its background
- [ ] Motion no faster than the app's own
