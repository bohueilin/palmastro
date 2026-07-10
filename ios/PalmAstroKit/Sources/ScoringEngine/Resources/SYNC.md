# Resource sync note

`default-ruleset.json` here is a **placeholder copy** for local development
and tests. The canonical file lives in the Android module:

    engine-scoring/src/main/resources/default-ruleset.json

At integration time the canonical file is copied over this one verbatim
(same JSON schema on both platforms: `version`, `signals[]` with
`signalId/source/direction/magnitude/minConfidence/domainWeights/safetyTag`,
`gradeThresholds`, `confidenceMultipliers`). The Swift loader
(`Ruleset.loadDefault()`) ignores unknown keys, so schema-compatible updates
require no code change.

This copy is written as ruleset version 2.0.0: the Appendix A1 palm signal set
including negative signals, plus the v2 astro signal set
(`ASTRO_SUN_<ELEMENT>/<MODALITY>`, `ASTRO_MOON_<ELEMENT>`, `ASTRO_ASC_<ELEMENT>`).
