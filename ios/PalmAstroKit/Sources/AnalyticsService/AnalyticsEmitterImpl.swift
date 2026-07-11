import Foundation
import CoreContracts

/// Privacy-safe analytics wrapper (PRD §29, §51, Appendix C).
/// Port of svc-analytics AnalyticsEmitterImpl.kt — enforcement order:
/// event-name allowlist → property-key allowlist → denylist scan →
/// type validation. Free-form text can never pass: string values must match
/// an enumerated-token shape and are capped in length.
public final class AnalyticsEmitterImpl: AnalyticsEmitter {

    private let sink: (String, [String: Any]) -> Void

    public init(sink: @escaping (String, [String: Any]) -> Void = { _, _ in }) {
        self.sink = sink
    }

    private static let allowlist: Set<String> = [
        "onboarding_start", "onboarding_step_view", "onboarding_submit", "onboarding_complete",
        "demo_start", "demo_step_view", "demo_complete", "demo_skip",
        "scan_start", "scan_angle_prompt_view", "scan_angle_quality_fail", "scan_angle_pass", "scan_complete",
        "inference_start", "inference_success", "inference_fail",
        "results_view", "domain_card_tap", "why_drawer_open", "delta_view", "guidance_view",
        "journal_saved",
        "paywall_view", "purchase_start", "purchase_success", "purchase_fail",
        "restore_start", "restore_success", "restore_fail",
        "settings_view", "tone_change", "reminders_change", "retention_toggle_change",
        "language_change",
        "dominant_hand_change", "delete_all_data_click", "delete_all_data_confirm",
        "app_crash", "performance_sample",
    ]

    /// PRD §51: allowlist properties. Only these keys may ever leave the wrapper.
    private static let propertyAllowlist: Set<String> = [
        "step", "angle", "reason", "domain", "tone", "language", "frequency",
        "enabled", "calc_level", "confidence", "duration_ms", "attempt",
        "quality_bucket", "product_id", "error_code", "screen", "metric", "value",
    ]

    private static let denylistKeyPatterns: [NSRegularExpression] = [
        "^palm_feature_.*",
        "^biometric_.*",
        "^embedding_.*",
        "^journal_text$",
        "^journal_entry$",
        "^reflection_text$",
        "^birthday_value$",
        "^birth_date$",
        "^dob$",
        "^birth_time_value$",
        "^birth_place_value$",
        "^purchase_token$",
        "^receipt.*",
    ].compactMap { try? NSRegularExpression(pattern: $0) }

    private static let denylistValuePatterns: [NSRegularExpression] = [
        ".*/scan/.*",
        ".*/frames/.*",
        ".*/media/.*",
    ].compactMap { try? NSRegularExpression(pattern: $0) }

    /// PRD §51 "No free text": string values must look like enumerated tokens
    /// (lowercase snake/kebab identifiers, locale tags, or product ids), max 64 chars.
    private static let tokenShape = try! NSRegularExpression(pattern: "^[a-z0-9][a-z0-9_.\\-]{0,63}$")

    public func emit(eventName: String, props: [String: Any]) {
        guard Self.allowlist.contains(eventName) else { return }

        let filtered = props.filter { key, value in
            Self.propertyAllowlist.contains(key)
                && !Self.isDeniedKey(key)
                && !Self.isDeniedValue(value)
                && Self.isValidType(value)
        }

        sink(eventName, filtered)
    }

    private static func isDeniedKey(_ key: String) -> Bool {
        denylistKeyPatterns.contains { matchesEntireString($0, key) }
    }

    private static func isDeniedValue(_ value: Any) -> Bool {
        if let string = value as? String,
           denylistValuePatterns.contains(where: { matchesEntireString($0, string) }) {
            return true
        }
        // Numeric vectors (e.g. embeddings/landmarks) may never leave the device.
        if let array = value as? [Any], array.count > 3, array.allSatisfy({ isNumber($0) }) {
            return true
        }
        return false
    }

    private static func isValidType(_ value: Any) -> Bool {
        switch value {
        case is Bool:
            return true
        case is Int, is Int64, is Float, is Double:
            return true
        case let string as String:
            return matchesEntireString(tokenShape, string)
        default:
            return false
        }
    }

    private static func isNumber(_ value: Any) -> Bool {
        value is Int || value is Int64 || value is Float || value is Double
    }

    private static func matchesEntireString(_ regex: NSRegularExpression, _ string: String) -> Bool {
        let range = NSRange(string.startIndex..<string.endIndex, in: string)
        guard let match = regex.firstMatch(in: string, options: [], range: range) else { return false }
        return match.range == range
    }
}
