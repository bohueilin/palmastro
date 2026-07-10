import CoreContracts

/// Maps categorical palm features / astro signals onto ruleset signal
/// definitions. Deterministic; resolution rules use only the stable
/// cross-platform vocabulary produced by PalmFeatureExtractorImpl.
public enum SignalResolver {

    public static func resolvePalmSignals(features: PalmFeatureResult, ruleset: Ruleset) -> [SignalDefinition] {
        let f = features.features
        var matchedIds: [String] = []

        // Positive signals (Appendix A1).
        if f.headlinePresent && f.headlineClarity == "clear" && f.headlineLength == "long" {
            matchedIds.append("PALM_HEADLINE_LONG_CLEAR")
        }
        if f.fatelinePresent && f.fatelineShape == "smooth" && (f.fatelineClarity == "clear" || f.fatelineClarity == "moderate") {
            matchedIds.append("PALM_FATELINE_STRONG")
        }
        if f.heartlinePresent && f.heartlineClarity == "clear" {
            matchedIds.append("PALM_HEARTLINE_DEEP")
        }
        if f.lifelinePresent && (f.lifelineClarity == "clear" || f.lifelineClarity == "moderate") {
            matchedIds.append("PALM_LIFELINE_CLEAR")
        }

        // Negative signals (Appendix A1).
        if f.headlinePresent && f.headlineShape == "chained" {
            matchedIds.append("PALM_HEADLINE_CHAINED")
        }
        if f.fatelinePresent && f.fatelineShape == "chained" {
            matchedIds.append("PALM_FATELINE_BREAKS")
        }
        if f.heartlinePresent && f.heartlineClarity == "faint" {
            matchedIds.append("PALM_HEARTLINE_THIN")
        }
        if f.lifelinePresent && f.lifelineClarity == "faint" {
            matchedIds.append("PALM_LIFELINE_FAINT")
        }
        if f.venusMountDensity == "dense" {
            matchedIds.append("PALM_VENUS_TEXTURE_DENSE")
        }
        if f.minorLineDensity == "dense" {
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
