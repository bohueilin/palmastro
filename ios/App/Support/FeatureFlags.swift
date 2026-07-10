import Foundation

/// Launch feature flags (PRD §69, EXECUTION_SPEC defaults), persisted in
/// UserDefaults so QA can flip them in debug builds. Names match the Android
/// SharedPreferences-backed flags one-to-one.
final class FeatureFlags {

    static let shared = FeatureFlags()

    private let defaults: UserDefaults

    /// PRD §69 flag names → launch defaults (EXECUTION_SPEC).
    private let launchDefaults: [String: Bool] = [
        "daily_insights_enabled": false,
        "llm_interpretations_enabled": false,
        "iap_enabled": false,
        "wear_enabled": false,
        "widget_enabled": false,
        "share_cards_enabled": true,
        "strict_safety_enabled": true,
        "debug_scan_bypass_enabled": false,
        "scan_reminders_enabled": true,
    ]

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func isEnabled(_ name: String) -> Bool {
        if defaults.object(forKey: "flag_\(name)") != nil {
            return defaults.bool(forKey: "flag_\(name)")
        }
        return launchDefaults[name] ?? false
    }

    var dailyInsightsEnabled: Bool { isEnabled("daily_insights_enabled") }
    var llmInterpretationsEnabled: Bool { isEnabled("llm_interpretations_enabled") }
    var iapEnabled: Bool { isEnabled("iap_enabled") }
    var shareCardsEnabled: Bool { isEnabled("share_cards_enabled") }
    var strictSafetyEnabled: Bool { isEnabled("strict_safety_enabled") }
    var debugScanBypassEnabled: Bool { isEnabled("debug_scan_bypass_enabled") }
    var scanRemindersEnabled: Bool { isEnabled("scan_reminders_enabled") }
}
