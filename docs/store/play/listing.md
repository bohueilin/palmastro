# Google Play Store Listing — PalmAstro

Source: PRD v2 §§35.3, 43, 63–65; safety rules §§30–32. Free-only launch (no IAP), EN + zh-TW.

## App name (max 30 chars)

| Locale | Name |
|---|---|
| Default / `en-US` | `PalmAstro` |
| `zh-TW` (Taiwan listing) | `掌紋星象 PalmAstro` |

Per PRD §63: the Taiwan listing uses the zh-Hant name; the English listing uses `PalmAstro`.

## Category and contact

- Category: Lifestyle. *(Assumption (editable): "Lifestyle" over "Entertainment" — it better matches the self-reflection positioning and avoids the fortune-telling shelf.)*
- Email: support@palmastro.app
- Website / privacy policy / terms URLs: hosted copies of `app/src/main/assets/legal/*` (see `docs/store/shared/launch_checklist.md` — hosting is a launch blocker; Play requires a public privacy policy URL).
- Contains ads: **No**. In-app purchases: **None** at launch (do not declare IAP price range).

## Short description (max 80 chars)

**en-US** (63 chars, PRD §64 verbatim):

> Privacy-first palm and astrology insights for self-reflection.

**zh-TW** (24 chars, PRD §64 verbatim):

> 以掌紋與星象探索自我模式，隱私優先、清楚可解釋。

## Full description — en-US (max 4000 chars)

```
PalmAstro is a privacy-first self-reflection app. It combines on-device palm feature analysis with tropical astrology signals to build explainable, growth-oriented reports across four areas of life: career, wealth habits, family communication, and stress & recovery patterns.

PalmAstro is not fortune telling. It does not predict your future, diagnose your health, or give financial advice. It turns patterns into prompts: things to notice, one action to try today, one for this week, and a question worth journaling about.

HOW IT WORKS
• Scan your palm with a guided seven-angle capture and live quality feedback.
• On-device analysis extracts categorical line features (clarity, continuity, depth) — never a biometric identity template. Nothing is uploaded.
• Add your birthday — and optionally birth time and place — for tropical astrology signals, computed locally.
• A deterministic, rules-based engine scores each domain 0–100 with a grade and a confidence level.

EXPLAINABLE, NOT MYSTICAL
• Every score has a "How was this calculated?" view showing each signal's contribution.
• Confidence reflects scan quality and available inputs — the app tells you when the evidence is weak instead of pretending certainty.

TRACK AND REFLECT
• Monthly rescans show how each domain changed since last time.
• A private, on-device journal per domain and month.
• Pick the voice that fits you: Analytical, Gentle, or Direct.
• Share polished summary cards if you choose — palm photos are never included.

PRIVACY BY DESIGN
• All analysis happens on your device. Palm photos, birth details, journal entries, and reports are never uploaded.
• No account. No ads. No trackers. No analytics transmitted.
• Local database encrypted (SQLCipher), key protected by the Android Keystore.
• Raw palm photos auto-delete within 24 hours by default — or turn photo retention off entirely.
• Delete all data anytime in Settings: a full local wipe that returns the app to its first-run state.
• The app's only network use is a one-time download of the open-source hand-detection model file. No personal data is sent.

WHAT PALMASTRO IS NOT
• Not medical advice — no diagnosis, no illness prediction. If you have health concerns, talk to a professional.
• Not financial advice — no investment tips, no promises about money.
• Not deterministic — palm lines and star signs are reflective lenses, not verdicts. You stay in charge.

PRICE
PalmAstro is free, with no in-app purchases and no ads at launch. If optional paid deep-dive packs arrive later, today's free experience stays free.

Languages: English and Traditional Chinese (繁體中文).

Questions? support@palmastro.app
```

## Full description — zh-TW (max 4000 chars)

```
掌紋星象 PalmAstro 是一款隱私優先的自我探索 App。它在你的裝置上分析掌紋特徵，結合回歸黃道星象訊號，針對四個生活領域——事業、財富習慣、家庭溝通、壓力與恢復——產生清楚可解釋、以成長為導向的報告。

掌紋星象不是算命。它不預測你的未來、不診斷健康、也不提供理財建議。它把觀察到的模式化為引導：值得留意的傾向、今天可以嘗試的一個行動、本週的一個練習，以及一個值得寫進日記的問題。

運作方式
• 依照引導完成七個角度的手掌掃描，即時回饋拍攝品質。
• 掌紋分析完全在裝置上進行，只產出分類型的線條特徵（清晰度、連續度、深淺）——不是生物辨識模板，也不會上傳任何影像。
• 輸入生日（出生時間與地點可選填），星象訊號同樣在本機計算。
• 確定性的規則引擎為每個領域評分 0–100，並附上等級與信心水準。

可解釋，不故弄玄虛
• 每個分數都有「這是怎麼算出來的？」頁面，逐項列出各訊號的貢獻。
• 信心水準反映掃描品質與可用資訊——證據不足時，App 會誠實告訴你，而不是假裝篤定。

追蹤與反思
• 每月重新掃描，看看每個領域相較上次的變化。
• 依領域與月份書寫的私密日記，只存在裝置上。
• 三種語氣任你選：理性分析、溫柔陪伴、直接坦率。
• 想分享時，可以輸出設計精美的摘要卡片——絕不會包含手掌照片。

隱私至上
• 所有分析都在你的裝置上完成。手掌照片、出生資訊、日記與報告絕不上傳。
• 不用帳號、沒有廣告、沒有追蹤器、不傳送任何分析數據。
• 本機資料庫以 SQLCipher 加密，金鑰由 Android Keystore 保護。
• 原始手掌照片預設 24 小時內自動刪除，也可以完全關閉照片保留。
• 隨時可在「設定」中刪除所有資料：完整清除本機資料，App 回到初次使用狀態。
• 唯一的網路連線，是一次性下載開放原始碼的手部偵測模型檔案，過程不傳送任何個人資料。

掌紋星象不是什麼
• 不是醫療建議——不診斷疾病、不預言健康狀況。若有健康疑慮，請諮詢專業人員。
• 不是財務建議——不報明牌、不承諾財富。
• 不是宿命論——掌紋與星象只是自我反思的視角，不是定論。決定權永遠在你手上。

價格
掌紋星象目前完全免費，沒有內購、沒有廣告。未來若推出選購的深度解析內容，現在免費的功能仍然免費。

支援語言：繁體中文、English。

有任何問題？support@palmastro.app
```

## Copy safety check (PRD §§30–32, 65)

Both descriptions were checked against the prohibited list: no guaranteed predictions, no health/disease claims, no wealth promises, no "know your future" / "100% accurate" phrasing, no medical or financial advice; disclaimers included; IAP status stated (free, none at launch). Do not edit the copy without re-running this check.

## Not part of this listing

- No "Contains ads" flag, no ad SDKs, no news declarations.
- Do not enroll in Google Play Families / Teacher Approved (see `content_rating.md` — target audience 18+).
