import Foundation
import Testing

/// Locks in the App-layer launch resources that App Review depends on
/// (audit P0): the four bundled legal documents under App/Resources/Legal
/// (converted from the canonical Android HTML assets) and the monthly
/// reminder notification strings in both locales.
///
/// Files are located relative to this source file (the same pattern as
/// ParityTests) because the App target is not part of this package.
@Suite struct AppResourcesTests {

    /// ios/App/Resources, located relative to this source file:
    /// .../ios/PalmAstroKit/Tests/AppResourcesTests/AppResourcesTests.swift
    static var appResourcesURL: URL {
        URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()  // AppResourcesTests/
            .deletingLastPathComponent()  // Tests/
            .deletingLastPathComponent()  // PalmAstroKit/
            .deletingLastPathComponent()  // ios/
            .appendingPathComponent("App/Resources", isDirectory: true)
    }

    static func legalText(_ fileName: String) throws -> String {
        let url = appResourcesURL.appendingPathComponent("Legal/\(fileName)")
        return try String(contentsOf: url, encoding: .utf8)
    }

    // MARK: - Legal documents (P0: broken privacy policy link)

    @Test(arguments: [
        ("privacy-policy_en.md", "# PalmAstro Privacy Policy"),
        ("privacy-policy_zh-TW.md", "隱私權政策"),
        ("terms-of-service_en.md", "# PalmAstro Terms of Service"),
        ("terms-of-service_zh-TW.md", "服務條款"),
    ])
    func legalDocumentExistsAndIsSubstantial(_ fixture: (String, String)) throws {
        let (fileName, marker) = fixture
        let text = try Self.legalText(fileName)
        #expect(text.contains(marker), "\(fileName) must contain \(marker)")
        #expect(text.contains("support@palmastro.app"), "\(fileName) must keep the contact address")
        #expect(text.count > 1_000, "\(fileName) must be a full document, not a stub")
    }

    @Test func privacyPolicyKeepsAllTwelveCanonicalSections() throws {
        for file in ["privacy-policy_en.md", "privacy-policy_zh-TW.md"] {
            #expect(try Self.sectionHeadings(in: file).count == 12, "\(file) must keep 12 sections")
        }
    }

    @Test func termsKeepAllFourteenCanonicalSections() throws {
        for file in ["terms-of-service_en.md", "terms-of-service_zh-TW.md"] {
            #expect(try Self.sectionHeadings(in: file).count == 14, "\(file) must keep 14 sections")
        }
    }

    /// LegalViewer falls back to `<resourceName>_en.md`; that fallback must
    /// exist for every resource name SettingsView links to.
    @Test(arguments: ["privacy-policy", "terms-of-service"])
    func englishFallbackExistsForViewerResource(_ resourceName: String) {
        let url = Self.appResourcesURL.appendingPathComponent("Legal/\(resourceName)_en.md")
        #expect(FileManager.default.fileExists(atPath: url.path), "missing \(resourceName)_en.md")
    }

    /// XcodeGen gives .md files no build phase by default; the project spec
    /// must keep the explicit resources phase or the bundle ships empty.
    @Test func projectSpecBundlesLegalDocumentsAsResources() throws {
        let projectYml = Self.appResourcesURL
            .deletingLastPathComponent()  // App/
            .deletingLastPathComponent()  // ios/
            .appendingPathComponent("project.yml")
        let spec = try String(contentsOf: projectYml, encoding: .utf8)
        #expect(spec.contains("App/Resources/Legal"))
        #expect(spec.contains("buildPhase: resources"))
    }

    // MARK: - Reminder notification strings (both locales)

    @Test(arguments: ["en.lproj", "zh-Hant.lproj"])
    func reminderNotificationStringsExist(_ locale: String) throws {
        let url = Self.appResourcesURL.appendingPathComponent("\(locale)/Localizable.strings")
        let table = try String(contentsOf: url, encoding: .utf8)
        #expect(table.contains("\"reminder_notification_title\""), "\(locale) missing reminder title")
        #expect(table.contains("\"reminder_notification_body\""), "\(locale) missing reminder body")
    }

    // MARK: - Helpers

    private static func sectionHeadings(in fileName: String) throws -> [String] {
        try legalText(fileName)
            .components(separatedBy: .newlines)
            .filter { $0.hasPrefix("## ") }
    }
}
