import SwiftUI
import CoreContracts

/// Settings (PRD §13.8): language, tone (Analytical/Gentle/Direct display
/// names, PRD §45), raw-media retention toggle, opt-in reminders, legal
/// viewers, and delete-all-data with double confirmation. No paid CTA at
/// launch (iap_enabled=false).
struct SettingsView: View {

    @EnvironmentObject private var model: AppModel

    @State private var showDeleteConfirm = false
    @State private var showDeleteFinalConfirm = false
    @State private var showDeletedToast = false

    var body: some View {
        List {
            Section {
                Picker("settings_language", selection: languageBinding) {
                    Text("language_system").tag("system")
                    Text("language_english").tag("en")
                    Text("language_traditional_chinese").tag("zh-TW")
                }
                Picker("settings_tone", selection: toneBinding) {
                    Text("tone_analytical").tag(Tone.SCIENTIFIC)
                    Text("tone_gentle").tag(Tone.HEALING)
                    Text("tone_direct").tag(Tone.ROAST_SAFE)
                }
                Picker("settings_dominant_hand", selection: handBinding) {
                    Text("hand_left").tag(Hand.LEFT)
                    Text("hand_right").tag(Hand.RIGHT)
                }
            } header: {
                Text("settings_section_preferences")
            }

            Section {
                Toggle("settings_retain_raw_media", isOn: retentionBinding)
                Toggle("settings_reminders", isOn: remindersBinding)
            } header: {
                Text("settings_section_privacy")
            } footer: {
                Text("settings_retention_footer")
            }

            Section {
                NavigationLink {
                    LegalViewer(
                        titleKey: "settings_privacy_policy",
                        resourceName: "privacy-policy",
                        language: model.contentLanguage
                    )
                } label: {
                    Text("settings_privacy_policy")
                }
                NavigationLink {
                    LegalViewer(
                        titleKey: "settings_terms",
                        resourceName: "terms-of-service",
                        language: model.contentLanguage
                    )
                } label: {
                    Text("settings_terms")
                }
            } header: {
                Text("settings_section_legal")
            }

            Section {
                Button(role: .destructive) {
                    model.analytics.emit(eventName: "delete_all_data_click", props: [:])
                    showDeleteConfirm = true
                } label: {
                    Text("settings_delete_all").frame(minHeight: 44)
                }
            } footer: {
                Text("settings_delete_all_footer")
            }
        }
        .navigationTitle(Text("settings_title"))
        .onAppear {
            model.analytics.emit(eventName: "settings_view", props: [:])
        }
        .confirmationDialog("settings_delete_confirm_title", isPresented: $showDeleteConfirm, titleVisibility: .visible) {
            Button("settings_delete_confirm_continue", role: .destructive) {
                showDeleteFinalConfirm = true
            }
            Button("common_cancel", role: .cancel) {}
        } message: {
            Text("settings_delete_confirm_message")
        }
        .alert("settings_delete_final_title", isPresented: $showDeleteFinalConfirm) {
            Button("settings_delete_final_confirm", role: .destructive) {
                model.deleteAllData()
                showDeletedToast = true
            }
            Button("common_cancel", role: .cancel) {}
        } message: {
            Text("settings_delete_final_message")
        }
        .alert("settings_deleted_title", isPresented: $showDeletedToast) {
            Button("common_done", role: .cancel) {}
        }
    }

    // MARK: - Bindings

    private var languageBinding: Binding<String> {
        Binding(
            get: { model.profile.language },
            set: { newValue in
                model.profile.language = newValue
                model.saveProfile()
                model.analytics.emit(eventName: "language_change", props: ["language": newValue.lowercased()])
            }
        )
    }

    private var toneBinding: Binding<Tone> {
        Binding(
            get: { model.profile.tone },
            set: { newValue in
                model.profile.tone = newValue
                model.saveProfile()
                model.analytics.emit(eventName: "tone_change", props: ["tone": newValue.rawValue.lowercased()])
            }
        )
    }

    private var handBinding: Binding<Hand> {
        Binding(
            get: { model.profile.dominantHand },
            set: { newValue in
                model.profile.dominantHand = newValue
                model.saveProfile()
                model.analytics.emit(eventName: "dominant_hand_change", props: [:])
            }
        )
    }

    private var retentionBinding: Binding<Bool> {
        Binding(
            get: { model.profile.retainRawMedia },
            set: { newValue in
                model.profile.retainRawMedia = newValue
                model.saveProfile()
                if !newValue {
                    try? FileManager.default.removeItem(at: AppModel.rawMediaURL())
                }
                model.analytics.emit(eventName: "retention_toggle_change", props: ["enabled": newValue])
            }
        )
    }

    /// Reminders are opt-in (EXECUTION_SPEC): notification permission is
    /// requested only when the user turns this on. Enabling schedules the
    /// repeating monthly reminder; disabling cancels it; denial reverts the
    /// toggle gracefully.
    private var remindersBinding: Binding<Bool> {
        Binding(
            get: { model.profile.reminders == "monthly" },
            set: { newValue in
                model.profile.reminders = newValue ? "monthly" : "off"
                model.saveProfile()
                model.analytics.emit(eventName: "reminders_change", props: ["frequency": newValue ? "monthly" : "off"])
                if newValue {
                    enableReminders()
                } else {
                    ReminderScheduler.disable()
                }
            }
        )
    }

    private func enableReminders() {
        ReminderScheduler.enable(lastScanDate: lastScanDate) { granted in
            if !granted {
                model.profile.reminders = "off"
                model.saveProfile()
            }
        }
    }

    /// Anchor for the monthly reminder: when the latest result was created.
    private var lastScanDate: Date? {
        model.latestResult.map { Date(timeIntervalSince1970: Double($0.createdAt) / 1000) }
    }
}

/// Renders a bundled legal document (PRD §13.8 legal viewers). The canonical
/// Markdown documents live in App/Resources/Legal (converted from the Android
/// assets); the viewer resolves `<resourceName>_<language>.md` with an
/// English fallback so the policy is always reachable.
struct LegalViewer: View {

    let titleKey: LocalizedStringKey
    let resourceName: String
    let language: String

    private enum Block {
        case title(String)
        case heading(String)
        case bullet(String)
        case paragraph(String)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                ForEach(Array(blocks.enumerated()), id: \.offset) { _, block in
                    blockView(block)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding()
            .textSelection(.enabled)
        }
        .navigationTitle(Text(titleKey))
        .navigationBarTitleDisplayMode(.inline)
    }

    private var blocks: [Block] {
        Self.parse(markdown: loadedText)
    }

    private var loadedText: String {
        for name in ["\(resourceName)_\(language)", "\(resourceName)_en"] {
            for ext in ["md", "txt"] {
                if let url = Bundle.main.url(forResource: name, withExtension: ext),
                   let text = try? String(contentsOf: url, encoding: .utf8) {
                    return text
                }
            }
        }
        return String(localized: "legal_missing_placeholder")
    }

    /// Line-oriented Markdown: the bundled documents keep one block per line.
    private static func parse(markdown: String) -> [Block] {
        markdown
            .components(separatedBy: .newlines)
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty }
            .map { line -> Block in
                if line.hasPrefix("# ") { return .title(String(line.dropFirst(2))) }
                if line.hasPrefix("## ") { return .heading(String(line.dropFirst(3))) }
                if line.hasPrefix("- ") { return .bullet(String(line.dropFirst(2))) }
                return .paragraph(line)
            }
    }

    @ViewBuilder
    private func blockView(_ block: Block) -> some View {
        switch block {
        case .title(let text):
            inlineText(text).font(.title2.bold()).padding(.top, 4)
        case .heading(let text):
            inlineText(text).font(.headline).padding(.top, 8)
        case .bullet(let text):
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                Text(verbatim: "•")
                inlineText(text)
            }
        case .paragraph(let text):
            inlineText(text)
        }
    }

    /// Renders inline emphasis (`**bold**` etc.); falls back to the raw line.
    private func inlineText(_ raw: String) -> Text {
        if let attributed = try? AttributedString(markdown: raw) {
            return Text(attributed)
        }
        return Text(raw)
    }
}
