# Support Page — PalmAstro

Source: PRD v2 §71 (support readiness). This file is the content for the hosted
support/FAQ page (the "Support URL" both stores require) and the canned-answer base for
support@palmastro.app. Publish in both languages; the zh-TW strings follow each entry.

## Contact

- Support email: **support@palmastro.app** (target first response: 2 business days)
- Privacy policy: hosted copies of `app/src/main/assets/legal/privacy_policy_en.html` /
  `privacy_policy_zh-TW.html`
- Terms of service: hosted copies of `terms_en.html` / `terms_zh-TW.html`

## FAQ (10 questions)

### 1. What is PalmAstro?

PalmAstro is a privacy-first self-reflection app. It analyzes photos of your palm on
your device, combines them with tropical astrology signals from your birthday, and
generates explainable reports across four life areas — career, wealth habits, family
communication, and stress & recovery — each with suggested actions and a journaling
prompt.

**zh-TW：掌紋星象是什麼？** 掌紋星象是一款隱私優先的自我探索 App。它在你的裝置上分析手掌
照片，結合由生日推算的星象訊號，針對事業、財富習慣、家庭溝通、壓力與恢復四個領域，產生
清楚可解釋的報告、行動建議與日記引導。

### 2. What is PalmAstro NOT?

It is not fortune telling and it does not predict the future. It is not medical advice —
it never diagnoses or predicts illness. It is not financial advice — it never recommends
investments or promises money outcomes. Its reports are reflective interpretations to
think with, not verdicts about you.

**zh-TW：掌紋星象不是什麼？** 它不是算命，也不預測未來。它不是醫療建議——絕不診斷或預言
疾病；也不是理財建議——不推薦投資、不承諾財富。報告是幫助你思考的反思素材，不是對你的
定論。

### 3. Does my palm photo ever leave my phone?

No. All analysis runs on your device. Palm photos, derived features, your birthday,
journal, and reports are never uploaded anywhere. There is no server behind the app.

**zh-TW：手掌照片會離開我的手機嗎？** 不會。所有分析都在你的裝置上進行。手掌照片、掌紋
特徵、生日、日記與報告都不會上傳到任何地方。這個 App 背後沒有伺服器。

### 4. Why does the app need internet once?

Before your first scan, the app downloads the open-source hand-detection model file it
needs (a one-time, integrity-checked HTTPS download from Google Cloud Storage). No
personal data is sent, and the app makes no other network requests.

**zh-TW：為什麼第一次使用需要網路？** 第一次掃描前，App 需要下載開放原始碼的手部偵測
模型檔案（一次性、經完整性驗證的 HTTPS 下載）。過程不會傳送任何個人資料，之後 App 也
不會再發出其他網路連線。

### 5. How long are my palm photos kept?

By default, raw palm photos are automatically deleted within 24 hours of capture. You
can also turn photo retention off entirely in Settings, so photos are not kept at all
after analysis. The derived reports stay until you delete them.

**zh-TW：手掌照片會保留多久？** 預設情況下，原始手掌照片會在拍攝後 24 小時內自動刪除。
你也可以在「設定」中完全關閉照片保留，分析完成後就不留任何照片。產生的報告則會保留到你
刪除為止。

### 6. How do I delete all my data?

Settings → "Delete all data". This permanently erases your profile, scans, photos,
features, reports, journal, and settings, and replaces the app's internal install
identifier with a new one — the app returns to its first-run state. This cannot be
undone, and because there is no cloud copy, we cannot restore it either. Uninstalling
the app also removes all its data.

**zh-TW：如何刪除所有資料？** 「設定」→「刪除所有資料」。這會永久清除個人資料、掃描紀錄、
照片、特徵、報告、日記與設定，並更換 App 內部的安裝識別碼，App 會回到初次使用狀態。此
操作無法復原；因為沒有雲端備份，我們也無法為你還原。解除安裝 App 同樣會移除所有資料。

### 7. Do I need an account?

No. There is no sign-up, no login, no email required — and no ads or trackers either.

**zh-TW：需要註冊帳號嗎？** 不需要。沒有註冊、沒有登入、不需要電子郵件，也沒有廣告或
追蹤器。

### 8. How are the scores calculated? Can I trust them?

Scores come from a deterministic rules engine: categorical palm-line features (clarity,
continuity, depth) plus astrology signals, weighted by scan quality. Every score has a
"How was this calculated?" view listing each signal's contribution and a confidence
level. Treat scores as honest, explainable reflection prompts — not scientific
measurements of who you are.

**zh-TW：分數是怎麼算的？可以相信嗎？** 分數來自確定性的規則引擎：分類型的掌紋特徵
（清晰度、連續度、深淺）加上星象訊號，並依掃描品質加權。每個分數都有「這是怎麼算出來
的？」頁面，列出各訊號的貢獻與信心水準。請把分數當作誠實、可解釋的反思引導，而不是對
你這個人的科學測量。

### 9. Can I change the language or the report's tone?

Yes. The app supports English and Traditional Chinese, and three voices — Analytical,
Gentle, and Direct — switchable anytime in Settings. Direct is candid but always passes
the same safety rules as the other tones.

**zh-TW：可以更改語言或報告語氣嗎？** 可以。App 支援繁體中文與英文，並提供三種語氣——
理性分析、溫柔陪伴、直接坦率——隨時可在「設定」中切換。「直接坦率」語氣直率但仍套用與
其他語氣相同的安全規則。

### 10. How do refunds work?

PalmAstro is completely free at launch — there is nothing to purchase, so refunds don't
apply. If optional paid content is introduced in the future, purchases and refunds will
be handled by Google Play / the App Store under their standard refund policies, and
this page will be updated with the exact steps.

**zh-TW：如何退款？** 掌紋星象目前完全免費，沒有任何付費項目，因此不涉及退款。未來若推
出選購的付費內容，購買與退款將依 Google Play／App Store 的標準退款政策辦理，屆時本頁會
更新詳細步驟。

## Known issues (beta/launch) — template

Maintain this section on the hosted page during closed testing and staged rollout
(PRD §71 "Known issues page for beta"). Format per entry:

| Field | Content |
|---|---|
| Issue ID | KI-YYYYMMDD-nn |
| Affected versions | e.g. 1.0.0 (build N) |
| Affected devices/OS | e.g. Android 12 on low-RAM devices |
| Symptom | One sentence, user language, no internal jargon |
| Workaround | Steps, or "none" |
| Status | Investigating / Fix in next release / Fixed in x.y.z |

Current entries: *(none yet — populate from closed-testing triage)*

## Support macros (email)

- Data deletion request → point to FAQ 6; note there is no server-side data to delete.
- "Is this accurate / will X happen to me?" → FAQ 2 + 8 language; never confirm
  predictive readings; if the user raises health worries, gently suggest talking to a
  qualified professional (allowed phrasing per PRD §31).
- Crash reports → ask the user to send **Settings → Legal & Support → Diagnostic
  report**, which previews and then emails build, locale, device and feature-flag state
  (nothing from a reading). Device model, OS version and app version (Settings → About)
  are the manual fallback; remind users not to email palm photos; no diagnostic data
  reaches us automatically.
