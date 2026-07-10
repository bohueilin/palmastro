# Resource sync note

`safety-rules.json` here is a **placeholder copy** for local development and
tests. The canonical safety vocabulary lives in the Android module:

    engine-content/src/main/resources/

At integration time the canonical file is copied over this one verbatim.
The Swift loader (`SafetyRules.loadDefault()`) ignores unknown keys.

Matching semantics implemented by SafetyFilterImpl (identical on both
platforms): NFC normalization, zero-width stripping (U+200B/C/D, U+FEFF,
U+00AD), fullwidth ASCII folding (U+FF01..FF5E), lowercasing; ASCII terms
match on word boundaries, CJK terms match as substrings. The term lists in
this copy mirror the Android SafetyFilterImpl vocabulary.
