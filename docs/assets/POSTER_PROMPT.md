# PalmAstro — Hero Poster Image Prompt

Use with any high-fidelity image model (Midjourney v7, DALL·E, Imagen, Flux).
Generate at **1280×640** (GitHub social preview / README hero) and **1024×1536**
(2:3 theatrical one-sheet). Save the README hero as `docs/assets/poster.png`.

---

## Master prompt

> Cinematic movie-theater one-sheet poster for a premium mobile app called
> **"PalmAstro 掌紋星象"**. An open human hand, palm facing the viewer, rises from
> the bottom third of the frame into a vast night sky. The palm's life, head,
> heart, and fate lines glow softly in luminous teal (#4FD1C5) and continue
> seamlessly beyond the fingertips, transforming into constellation lines that
> connect real stars across a deep-space nebula in royal purple and indigo
> (#6B46C1 → #1A1033 gradient). Fine astrological chart geometry — a faint
> zodiac wheel, orbit arcs, and degree ticks in thin elegant hairlines — is
> etched translucently behind the hand, as if the sky itself is an instrument.
> A small, precise crescent moon sits upper right. Subtle particles of stardust
> drift from the palm lines upward.
>
> Mood: serene, intelligent, trustworthy, quietly magical — "mystic but modern,
> calm but precise". NOT spooky, NOT occult, NOT neon cyberpunk.
>
> Lighting: single soft key light from the upper left with gentle rim light on
> the hand's edge; deep atmospheric falloff into the nebula; volumetric but
> restrained. Color grade like a prestige sci-fi film — rich blacks, controlled
> highlights, no crushed shadows.
>
> Composition: strict central vertical symmetry broken only by the crescent
> moon; generous negative space in the upper quarter reserved for the title.
> Rule-of-thirds horizon where hand meets sky. Shallow cinematic depth,
> 65 mm anamorphic feel, extremely fine grain.
>
> Typography (if the model renders type, otherwise composite later): the
> wordmark **PALMASTRO** in a modern geometric sans (Futura/Avenir spirit),
> wide letter-spacing, white with a faint teal glow, centered in the upper
> negative space; beneath it the Chinese title **掌紋星象** in an elegant light
> serif; beneath that, small tagline caps: "SCAN YOUR PALM · UNDERSTAND YOUR
> PATTERNS". Bottom edge: a slim, movie-credit-style compressed text block in
> miniature type reading "ON-DEVICE ANALYSIS · EXPLAINABLE SCORES · PRIVATE BY
> DESIGN".
>
> Quality: ultra-high fidelity, 8K master, photorealistic hand with painterly
> celestial background, museum-grade print quality, flawless skin rendering
> with natural texture, no plastic smoothness.

## Negative prompt

> crystal ball, tarot cards, fortune teller silhouette, hooded figure, skulls,
> candles, red palette, horror mood, cluttered composition, lens flare kitsch,
> extra fingers, deformed hand, watermark, low contrast mush, cartoon style,
> stock-photo look.

## Variants

- **Social preview (1280×640):** same scene reframed landscape; hand enters from
  bottom-center, constellations sweep left→right; title block right-aligned in
  the clear sky area.
- **App Store feature banner:** crop to the palm-to-constellation transition
  zone only, title omitted.
- **Dark-mode README:** identical prompt + "background fades to pure #0B0714 at
  the frame edges" so it blends into GitHub dark theme.

## Brand anchors (keep exact)

| Token | Value |
|---|---|
| Royal purple | `#6B46C1` |
| Calm teal | `#4FD1C5` |
| Night sky base | `#1A1033` → `#0B0714` |
| Type | Geometric sans (EN) + light serif (繁中) |
| Motifs | palm, stars, orbit, shield — never crystal balls |
