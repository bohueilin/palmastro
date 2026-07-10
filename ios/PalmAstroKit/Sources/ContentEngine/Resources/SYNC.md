# Resource sync note

`content-templates.json` here is a **placeholder copy** for local development
and tests. The canonical content library lives in the Android module:

    engine-content/src/main/resources/

At integration time the canonical file is copied over this one verbatim.
The Swift loader (`ContentTemplates.loadDefault()`) ignores unknown keys and
resolves languages with exact → primary-subtag → fallback matching, so adding
languages (zh-CN / ja / hi, kept as internal capability per EXECUTION_SPEC)
or fields requires no Swift code change.

This copy ships `en` and `zh-TW` (launch minimum). Copy text must never
contain terms matched by safety-rules.json — the SafetyEngine test suite
composes from these templates and asserts zero violations.
