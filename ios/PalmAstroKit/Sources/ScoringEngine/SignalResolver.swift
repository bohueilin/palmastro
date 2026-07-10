import CoreContracts

// Mirrors engine-scoring/src/main/kotlin/com/palmastro/scoring/SignalResolver.kt
// exactly — matched-id order and match conditions must stay identical so
// explainability entry order is bit-for-bit comparable across platforms.

/// Maps categorical `PalmFeatures` (extractor v2 vocabulary) and `AstroResult`
/// signals onto ruleset signal definitions.
///
/// Palm clarity vocabulary (must stay aligned with the Kotlin
/// PalmFeatureExtractorImpl): "clear" | "medium" | "faint" | "broken" |
/// "thin" (heartline only) | "unclear" (absent lines).
/// Densities: "high" | "med" | "low".
public enum SignalResolver {

    private static let positiveClarities: Set<String> = ["clear", "medium"]

    public static func resolvePalmSignals(features: PalmFeatureResult, ruleset: Ruleset) -> [SignalDefinition] {
        let f = features.features
        var matchedIds: [String] = []

        // Positive signals.
        if f.headlinePresent && f.headlineClarity == "clear" && f.headlineLength == "long" {
            matchedIds.append("PALM_HEADLINE_LONG_CLEAR")
        }
        if f.heartlinePresent && positiveClarities.contains(f.heartlineClarity) {
            matchedIds.append("PALM_HEARTLINE_STRONG")
        }
        if f.lifelinePresent && positiveClarities.contains(f.lifelineClarity) {
            matchedIds.append("PALM_LIFELINE_CLEAR")
        }
        if f.fatelinePresent && f.fatelineClarity == "clear" {
            matchedIds.append("PALM_FATELINE_STRONG")
        }

        // Negative signals (ruleset v2).
        if f.headlinePresent && f.headlineClarity == "broken" {
            matchedIds.append("PALM_HEADLINE_CHAINED")
        }
        if f.fatelinePresent && f.fatelineClarity == "broken" {
            matchedIds.append("PALM_FATELINE_BREAKS")
        }
        if f.heartlinePresent && f.heartlineClarity == "thin" {
            matchedIds.append("PALM_HEARTLINE_THIN")
        }
        if f.lifelinePresent && f.lifelineClarity == "faint" {
            matchedIds.append("PALM_LIFELINE_FAINT")
        }
        if f.minorLineDensity == "high" {
            matchedIds.append("PALM_MINOR_LINES_DENSE")
        }

        return matchedIds.compactMap { id in
            ruleset.signals.first { $0.signalId == id }
        }
    }

    public static func resolveAstroSignals(astro: AstroResult, ruleset: Ruleset) -> [(SignalDefinition, AstroSignal)] {
        astro.signals.compactMap { signal in
            guard let def = ruleset.signals.first(where: { $0.signalId == signal.signalId }) else {
                return nil
            }
            return (def, signal)
        }
    }
}
