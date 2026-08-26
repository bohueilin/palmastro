# Localization status

| Locale | Coverage | Ships at launch |
| --- | --- | --- |
| `values/` (English) | 426 keys — source of truth | yes |
| `values-zh-rTW` (Traditional Chinese) | complete; the 4 unlocalized keys are `translatable="false"` (brand, support address, endonyms) | yes — primary launch language |
| Hindi | 24 of 426 keys | **no** |

## Why Hindi is not in `res/`

`hi-partial-strings.xml` was `app/src/main/res/values-hi/strings.xml`. At 24 of 426 keys,
a Hindi-locale device fell back to English for 94% of the UI — a mixed-language product.

PRD §19 and §44 are explicit: *"launch UI must not expose languages with incomplete
platform localization"* and *"hide incomplete UI language switches"*, with Hindi listed
as a post-launch language. Holding the partial file here keeps the finished work while
Hindi-locale devices get one consistent language.

## To finish Hindi

Restore the file to `app/src/main/res/values-hi/strings.xml`, translate the remaining
keys across every `strings_*.xml`, then add `hi` to the in-app language picker
(`ui/onboarding/AppLanguage.kt` and the Settings language section). Do not restore it
partially — that reintroduces the mixed-language UI.
