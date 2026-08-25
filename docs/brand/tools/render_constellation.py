#!/usr/bin/env python3
"""
Render the PalmAstro constellation motif to frames, for store video and social cuts.

Geometry, timeline and palette are copied verbatim from the shipping Compose
source (ui/scan/ConstellationReveal.kt) so marketing motion and in-app motion are
the same animation — not a lookalike an editor rebuilt by eye.

  python3 render_constellation.py --out FRAMEDIR [--width 1080] [--height 1920]
"""
import argparse, math, os
from PIL import Image, ImageDraw, ImageFilter

# ── Palette (BrandIllustration.kt / BrandPalette.swift) ──
NIGHT_TOP, NIGHT_BOTTOM = (0x23, 0x1A, 0x4A), (0x14, 0x0F, 0x2E)
LINE_TEAL, EDGE_LAVENDER, STARLIGHT = (0x4F, 0xD1, 0xC5), (0x9F, 0x7A, 0xEA), (0xED, 0xEA, 0xFB)

# ── Timeline (ConstellationReveal.kt), as fractions of the reveal ──
REVEAL_S = 2.0
LINE_STAGGER, LINE_SPAN = 0.09, 0.33
NODE_START, NODE_STAGGER, NODE_SPAN = 0.52, 0.02, 0.14
EDGE_START, EDGE_STAGGER, EDGE_SPAN = 0.62, 0.045, 0.20
AMBIENT_START, AMBIENT_END = 0.50, 0.90
TWINKLE_S, TWINKLE_MIN = 1.4, 0.65

HEART = [(0.16, 0.66), (0.38, 0.58), (0.62, 0.57), (0.82, 0.62)]
HEAD  = [(0.18, 0.74), (0.44, 0.72), (0.68, 0.73), (0.80, 0.77)]
LIFE  = [(0.24, 0.70), (0.30, 0.80), (0.38, 0.90), (0.50, 0.96)]
FATE  = [(0.55, 0.96), (0.53, 0.84), (0.52, 0.72), (0.50, 0.60)]
PALM_LINES = [HEART, HEAD, LIFE, FATE]
SKY = [(0.26, 0.22), (0.50, 0.10), (0.74, 0.20)]
AMBIENT = [(0.12, 0.34), (0.34, 0.14), (0.65, 0.28), (0.88, 0.38)]
NODES = [p for ln in PALM_LINES for p in (ln[0], ln[-1])] + SKY
PALM_NODE_COUNT = len(PALM_LINES) * 2
EDGES = [(HEART[0], SKY[0]), (SKY[0], SKY[1]), (SKY[1], SKY[2]), (SKY[2], HEART[-1]), (FATE[-1], SKY[1])]


def window(t, a, b):
    return max(0.0, min(1.0, (t - a) / (b - a)))


def ease_in_out(t):  # FastOutSlowIn, close enough for export
    return t * t * (3 - 2 * t)


def smooth_points(pts, steps=140):
    """Quadratics through segment midpoints — mirrors DrawScope.palmPath."""
    out = [pts[0]]
    for i in range(1, len(pts) - 1):
        p, n = pts[i], pts[i + 1]
        mid = ((p[0] + n[0]) / 2, (p[1] + n[1]) / 2)
        a = out[-1]
        for s in range(1, steps // len(pts) + 1):
            u = s / (steps // len(pts))
            x = (1 - u) ** 2 * a[0] + 2 * (1 - u) * u * p[0] + u ** 2 * mid[0]
            y = (1 - u) ** 2 * a[1] + 2 * (1 - u) * u * p[1] + u ** 2 * mid[1]
            out.append((x, y))
    out.append(pts[-1])
    return out


def background(w, h):
    img = Image.new("RGB", (w, h))
    d = ImageDraw.Draw(img)
    for y in range(h):
        u = y / max(1, h - 1)
        d.line([(0, y), (w, y)], fill=tuple(round(NIGHT_TOP[i] + (NIGHT_BOTTOM[i] - NIGHT_TOP[i]) * u) for i in range(3)))
    return img


def draw_frame(w, h, t, twinkle):
    """t: 0..1 reveal progress. twinkle: 0.65..1 breathing alpha."""
    base = background(w, h)
    glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    core = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    gd, cd = ImageDraw.Draw(glow), ImageDraw.Draw(core)
    # The motif is authored for a roughly 4:5 canvas (its proportions in the app). On a
    # taller export frame, map it into a centred 4:5 box so it keeps its shape and the
    # night sky simply extends around it.
    mw = min(w, h * 0.8)
    mh = mw * 1.25
    ox, oy = (w - mw) / 2, (h - mh) / 2
    px = lambda p: (ox + p[0] * mw, oy + p[1] * mh)
    unit = mw / 400.0

    for i, line in enumerate(PALM_LINES):
        f = window(t, i * LINE_STAGGER, i * LINE_STAGGER + LINE_SPAN)
        if f <= 0:
            continue
        pts = smooth_points(line)
        n = max(2, int(len(pts) * f))
        xy = [px(p) for p in pts[:n]]
        gd.line(xy, fill=LINE_TEAL + (60,), width=max(2, int(7 * unit)), joint="curve")
        cd.line(xy, fill=LINE_TEAL + (235,), width=max(1, int(2 * unit)), joint="curve")

    for i, (a, b) in enumerate(EDGES):
        f = window(t, EDGE_START + i * EDGE_STAGGER, EDGE_START + i * EDGE_STAGGER + EDGE_SPAN)
        if f <= 0:
            continue
        ax, ay = px(a); bx, by = px(b)
        cd.line([(ax, ay), (ax + (bx - ax) * f, ay + (by - ay) * f)],
                fill=EDGE_LAVENDER + (int(255 * 0.42 * f),), width=max(1, int(1.1 * unit)))

    for i, node in enumerate(NODES):
        f = window(t, NODE_START + i * NODE_STAGGER, NODE_START + i * NODE_STAGGER + NODE_SPAN)
        if f <= 0:
            continue
        cx, cy = px(node)
        boost = 1.35 if i >= PALM_NODE_COUNT else 1.0
        halo = 6.5 * unit * boost * (0.6 + 0.4 * f)
        gd.ellipse([cx - halo, cy - halo, cx + halo, cy + halo], fill=STARLIGHT + (int(255 * 0.20 * f * twinkle),))
        r = 2.2 * unit * boost * f
        cd.ellipse([cx - r, cy - r, cx + r, cy + r], fill=STARLIGHT + (int(255 * f * (0.7 + 0.3 * twinkle)),))

    amb = window(t, AMBIENT_START, AMBIENT_END)
    for i, s in enumerate(AMBIENT):
        if amb <= 0:
            break
        phase = twinkle if i % 2 == 0 else (TWINKLE_MIN + 1.0 - twinkle)
        cx, cy = px(s); r = 1.2 * unit
        cd.ellipse([cx - r, cy - r, cx + r, cy + r], fill=STARLIGHT + (int(255 * 0.35 * amb * phase),))

    base.paste(Image.alpha_composite(
        glow.filter(ImageFilter.GaussianBlur(radius=max(2, int(3 * unit)))), core
    ).convert("RGBA"), (0, 0), Image.alpha_composite(
        glow.filter(ImageFilter.GaussianBlur(radius=max(2, int(3 * unit)))), core))
    return base


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--width", type=int, default=1080)
    ap.add_argument("--height", type=int, default=1920)
    ap.add_argument("--fps", type=int, default=30)
    ap.add_argument("--seconds", type=float, default=6.0)
    a = ap.parse_args()
    os.makedirs(a.out, exist_ok=True)
    total = int(a.fps * a.seconds)
    for i in range(total):
        secs = i / a.fps
        t = ease_in_out(min(1.0, secs / REVEAL_S))
        tw = TWINKLE_MIN + (1 - TWINKLE_MIN) * (0.5 + 0.5 * math.sin(2 * math.pi * secs / (TWINKLE_S * 2)))
        draw_frame(a.width, a.height, t, tw).save(os.path.join(a.out, f"f{i:05d}.png"))
        if i % 30 == 0:
            print(f"  {i}/{total}")
    print(f"rendered {total} frames -> {a.out}")


if __name__ == "__main__":
    main()
