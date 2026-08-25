# PalmAstro — Enhancement Roadmap

**Written:** 2026-08-25, after a full-codebase audit (60 confirmed defects, see `docs/audit/`).
**Status:** proposal. Items marked **[beyond PRD]** extend `PalmAstro_PRD_Full_v2_AppStoreLaunch.md` and need product-owner sign-off; the PRD's own rule is that agents must not invent requirements.

---

## The one problem worth solving first

**PalmAstro is a monthly app with no reason to open it in between.**

Everything else here is secondary. The product's value — month-over-month pattern tracking — requires a user to come back in 30 days and repeat a 7-angle scan. Today nothing invites that: the reminder scheduler is broken (both options fire the same 30-day job immediately on opt-in), there is no stale-month prompt, and a returning user in September sees August's reading looking perfectly current.

A reflection product that is opened once a month is opened once, then never again. **Fix the loop before adding anything.**

---

## Tier 1 — Make the core loop survive (do these first)

### 1.1 Close the retention loop
*Cost: S · Impact: existential · PRD-sanctioned (§23)*

Three fixes, already specified in `docs/audit/confirmed-findings.json`:
- Repair the reminder scheduler so "1st of each month" is actually monthly and never fires on opt-in.
- Add a stale-month banner on Results when the newest reading is not the current month — gated so History and deep links are unaffected.
- Surface the comparability score next to the delta, so month-over-month change is honest rather than a bare arrow.

### 1.2 Weekly actions become checkable
*Cost: M · Impact: high · **[beyond PRD]***

The engine already generates `actionToday`, `actionWeek`, and a 7-step `weekPlan` per reading. Today they are inert paragraphs a user reads once.

Make them **tickable**, stored locally alongside the journal. This is the smallest honest change that gives someone a reason to open the app on a Tuesday: not a horoscope, not a notification treadmill, but the reflective commitments *they already received* in their monthly reading.

It also produces the one signal the product currently lacks — did the person act? — which is exactly the input a "how did your month go" prompt needs at rescan time.

**Guardrails:** no streaks, no badges, no guilt copy, no push nagging. Unchecked items must never be framed as failure. That would violate the calm-by-default principle (PRD §12.3) and turn a reflection tool into a habit-shaming app.

### 1.3 Rescan is not destructive
*Cost: S · Impact: high · PRD-sanctioned (§13.3)*

Re-scanning in the same month silently overwrites the existing reading with no confirmation and no undo — including readings a user has journalled against. Confirm before replacing, and say plainly what will be lost.

---

## Tier 2 — Reduce the cost of the first reading

### 2.1 Sample reading before commitment
*Cost: M · Impact: high · PRD-sanctioned (§13.1 step 12 — "Demo or scan entry", currently unbuilt)*

Today a user must complete 10 onboarding steps *and* a 7-angle palm scan before seeing anything the product does. That is an enormous ask from a cold install.

Ship a clearly-labelled **sample reading** reachable from onboarding and from the empty state — real UI, real engine output, from fixed demo inputs, watermarked "Sample" on every surface so it can never be mistaken for the user's own.

**Integrity requirement:** it must run the real engines on stated demo data, never hardcoded prose. A fabricated reading dressed as a real one is exactly the dishonesty this product's positioning forbids.

### 2.2 Progressive capture
*Cost: L · Impact: high · **[beyond PRD]***

Seven angles is the single highest-friction moment in the product, performed one-handed while holding a phone. Any abandonment there yields *nothing* — no reading, no account value, no reason to return.

Allow a **partial scan** to produce a genuinely lower-confidence reading: capture the front angle plus two others, get an L1-confidence result labelled honestly, with a standing invitation to complete the remaining angles to raise confidence. The scoring engine already models confidence and the quality gate already scores per angle, so the machinery exists.

This converts an abandoned funnel into a returning user with something to improve.

### 2.3 Scan technique demo
*Cost: M · Impact: medium · PRD-sanctioned (§13.2 step 2)*

Each angle instruction is a *motion* ("rotate your wrist slowly to the left"). Static text cannot teach it. Add a small looping vector silhouette per angle — see `docs/brand/VISUAL_AND_VIDEO_GUIDE.md` §2 for why this should be code-drawn rather than shipped video.

Expected effect: fewer quality-gate rejections, which is the difference between a 60-second scan and a frustrating three-minute one.

---

## Tier 3 — Deepen the reflection (the actual product promise)

### 3.1 Journal ↔ score reflection
*Cost: M · Impact: high · **[beyond PRD]***

The journal exists, and monthly scores exist, and they never meet. The reflective payoff of this product is *"here is what you wrote in July, and here is how your July score moved."*

At rescan, show last month's journal entry beside the new delta and ask one question. That single screen is arguably the product's whole thesis, and it costs one composable over data already stored.

### 3.2 Explainability earlier
*Cost: S · Impact: medium · PRD-sanctioned (§12.2)*

Explainability is the strategic differentiator — and it is two taps deep, behind a domain card. A first-time user forms their trust judgment on the Results screen and may never find it.

Surface one honest line of provenance on the dashboard itself ("Built from 6 palm signals and 3 astrology signals — see how"), linking through.

### 3.3 Data export
*Cost: S · Impact: medium · **[beyond PRD]***

A privacy-first product should let people take their data with them, not just delete it. A local JSON export of profile, readings, and journal — written straight to a user-chosen file — costs little, strengthens thecore positioning, and pre-empts data-portability questions from reviewers and regulators.

---

## Tier 4 — Deliberately not now

| Idea | Why not |
| --- | --- |
| Daily insights | PRD §7.3 defers it. It would also convert a considered monthly instrument into a horoscope app — the exact category the product differentiates from. |
| LLM interpretations | Flag exists, engine does not. Non-deterministic output cannot be explained by the explainability screen, so it breaks the core promise. |
| Social / friend comparison | Turns self-reflection into ranking. Directly opposed to the product's tone. |
| Streaks and badges | Habit-shaming machinery in a calm-by-default product. |
| Cloud sync | Contradicts the on-device promise, which is the primary differentiator. |
| Widgets / Wear | Fine eventually; no value until the monthly loop demonstrably works. |

---

## Sequencing

```
Now      →  Tier 1 (loop survives) + the 60 audit fixes
Next     →  2.1 sample reading, 2.3 scan demo      ← cheapest conversion wins
Then     →  1.2 checkable actions, 3.1 journal↔score  ← the retention thesis
Later    →  2.2 progressive capture, 3.2, 3.3
Never    →  Tier 4, unless the positioning changes
```

**The single highest-leverage item:** §1.2, checkable weekly actions. It is the only proposal that creates a between-scan reason to open the app, and it does so entirely from content the engine already produces — no new content pipeline, no new model, no compromise of the product's honesty.
