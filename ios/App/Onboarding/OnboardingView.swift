import SwiftUI
import AVFoundation
import CoreContracts

/// Launch onboarding (PRD §13.1) — twelve steps:
/// welcome, privacy promise, name, birthday, dominant hand, relationship,
/// birth time/place, tone, language, summary, camera education, scan entry.
/// Birthday and dominant hand are required; everything else is clearly
/// optional. Uses native pickers and system fonts (Dynamic Type).
struct OnboardingView: View {

    enum Step: Int, CaseIterable {
        case welcome, privacyPromise, name, birthday, dominantHand,
             relationship, birthTimePlace, tone, language, summary,
             cameraEducation, scanEntry
    }

    @EnvironmentObject private var model: AppModel
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    @State private var step: Step = .welcome
    @State private var displayName = ""
    @State private var birthdaySet = false
    @State private var birthdayDate = Calendar.current.date(byAdding: .year, value: -25, to: Date()) ?? Date()
    @State private var dominantHand: Hand?
    @State private var relationshipStatus = "prefer_not_to_say"
    @State private var birthTimeKnown = false
    @State private var birthTimeDate = Date()
    @State private var birthPlaceLatText = ""
    @State private var birthPlaceLonText = ""
    @State private var tone: Tone = .SCIENTIFIC
    @State private var language = "system"
    @State private var showScan = false

    var body: some View {
        VStack(spacing: 0) {
            ProgressView(value: Double(step.rawValue + 1), total: Double(Step.allCases.count))
                .padding(.horizontal)
                .padding(.top, 8)
                .accessibilityLabel(Text("onboarding_progress_a11y"))

            ScrollView {
                stepContent
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            navigationBar
        }
        .onAppear {
            model.analytics.emit(eventName: "onboarding_start", props: [:])
        }
        .onChange(of: step) { _, newStep in
            model.analytics.emit(eventName: "onboarding_step_view", props: ["step": "step_\(newStep.rawValue)"])
        }
        .fullScreenCover(isPresented: $showScan) {
            ScanView()
        }
    }

    // MARK: - Steps

    @ViewBuilder
    private var stepContent: some View {
        switch step {
        case .welcome:
            VStack(alignment: .leading, spacing: 16) {
                Text("onboarding_welcome_title").font(.largeTitle.bold())
                Text("onboarding_welcome_body")
                Text("onboarding_welcome_disclaimer").font(.footnote).foregroundStyle(.secondary)
            }
        case .privacyPromise:
            VStack(alignment: .leading, spacing: 16) {
                Text("onboarding_privacy_title").font(.title.bold())
                Label("onboarding_privacy_on_device", systemImage: "iphone")
                Label("onboarding_privacy_retention", systemImage: "clock.arrow.circlepath")
                Label("onboarding_privacy_no_upload", systemImage: "icloud.slash")
                Label("onboarding_privacy_delete_all", systemImage: "trash")
            }
        case .name:
            VStack(alignment: .leading, spacing: 16) {
                Text("onboarding_name_title").font(.title.bold())
                Text("onboarding_optional_hint").font(.footnote).foregroundStyle(.secondary)
                TextField("onboarding_name_placeholder", text: $displayName)
                    .textFieldStyle(.roundedBorder)
                    .textContentType(.nickname)
            }
        case .birthday:
            VStack(alignment: .leading, spacing: 16) {
                Text("onboarding_birthday_title").font(.title.bold())
                Text("onboarding_birthday_why").font(.footnote).foregroundStyle(.secondary)
                DatePicker(
                    "onboarding_birthday_label",
                    selection: $birthdayDate,
                    in: ...Date(),
                    displayedComponents: .date
                )
                .datePickerStyle(.wheel)
                .labelsHidden()
                .onChange(of: birthdayDate) { _, _ in birthdaySet = true }
                Toggle("onboarding_birthday_confirm", isOn: $birthdaySet)
            }
        case .dominantHand:
            VStack(alignment: .leading, spacing: 16) {
                Text("onboarding_hand_title").font(.title.bold())
                Text("onboarding_hand_why").font(.footnote).foregroundStyle(.secondary)
                Picker("onboarding_hand_title", selection: $dominantHand) {
                    Text("hand_left").tag(Hand?.some(.LEFT))
                    Text("hand_right").tag(Hand?.some(.RIGHT))
                }
                .pickerStyle(.segmented)
            }
        case .relationship:
            VStack(alignment: .leading, spacing: 16) {
                Text("onboarding_relationship_title").font(.title.bold())
                Text("onboarding_optional_hint").font(.footnote).foregroundStyle(.secondary)
                Picker("onboarding_relationship_title", selection: $relationshipStatus) {
                    Text("relationship_prefer_not").tag("prefer_not_to_say")
                    Text("relationship_single").tag("single")
                    Text("relationship_partnered").tag("partnered")
                    Text("relationship_married").tag("married")
                }
                .pickerStyle(.menu)
            }
        case .birthTimePlace:
            VStack(alignment: .leading, spacing: 16) {
                Text("onboarding_birthtime_title").font(.title.bold())
                Text("onboarding_birthtime_why").font(.footnote).foregroundStyle(.secondary)
                Toggle("onboarding_birthtime_known", isOn: $birthTimeKnown)
                if birthTimeKnown {
                    DatePicker("onboarding_birthtime_label", selection: $birthTimeDate, displayedComponents: .hourAndMinute)
                    TextField("onboarding_birthplace_lat", text: $birthPlaceLatText)
                        .keyboardType(.numbersAndPunctuation)
                        .textFieldStyle(.roundedBorder)
                    TextField("onboarding_birthplace_lon", text: $birthPlaceLonText)
                        .keyboardType(.numbersAndPunctuation)
                        .textFieldStyle(.roundedBorder)
                } else {
                    Text("onboarding_birthtime_l1_note").font(.footnote).foregroundStyle(.secondary)
                }
            }
        case .tone:
            VStack(alignment: .leading, spacing: 16) {
                Text("onboarding_tone_title").font(.title.bold())
                // Display names per PRD §45: Analytical / Gentle / Direct.
                Picker("onboarding_tone_title", selection: $tone) {
                    Text("tone_analytical").tag(Tone.SCIENTIFIC)
                    Text("tone_gentle").tag(Tone.HEALING)
                    Text("tone_direct").tag(Tone.ROAST_SAFE)
                }
                .pickerStyle(.inline)
            }
        case .language:
            VStack(alignment: .leading, spacing: 16) {
                Text("onboarding_language_title").font(.title.bold())
                Picker("onboarding_language_title", selection: $language) {
                    Text("language_system").tag("system")
                    Text("language_english").tag("en")
                    Text("language_traditional_chinese").tag("zh-TW")
                }
                .pickerStyle(.inline)
            }
        case .summary:
            VStack(alignment: .leading, spacing: 12) {
                Text("onboarding_summary_title").font(.title.bold())
                summaryRow(labelKey: "onboarding_name_title", value: displayName.isEmpty ? nil : displayName)
                summaryRow(labelKey: "onboarding_birthday_title", value: birthdaySet ? birthdayDate.formatted(date: .long, time: .omitted) : nil)
                summaryRow(labelKey: "onboarding_hand_title", value: dominantHand.map { $0 == .LEFT ? String(localized: "hand_left") : String(localized: "hand_right") })
                summaryRow(labelKey: "onboarding_birthtime_title", value: birthTimeKnown ? birthTimeDate.formatted(date: .omitted, time: .shortened) : String(localized: "onboarding_summary_l1_mode"))
                Text("onboarding_summary_note").font(.footnote).foregroundStyle(.secondary)
            }
        case .cameraEducation:
            VStack(alignment: .leading, spacing: 16) {
                Text("onboarding_camera_title").font(.title.bold())
                Label("onboarding_camera_local", systemImage: "cpu")
                Label("onboarding_camera_angles", systemImage: "camera.viewfinder")
                Label("onboarding_camera_permission", systemImage: "hand.raised")
                Text("onboarding_camera_denied_note").font(.footnote).foregroundStyle(.secondary)
            }
        case .scanEntry:
            VStack(alignment: .leading, spacing: 16) {
                Text("onboarding_scan_entry_title").font(.title.bold())
                Text("onboarding_scan_entry_body")
                Button {
                    completeOnboarding()
                    showScan = true
                } label: {
                    Text("onboarding_start_scan")
                        .frame(maxWidth: .infinity, minHeight: 44)
                }
                .buttonStyle(.borderedProminent)

                Button {
                    completeOnboarding()
                } label: {
                    Text("onboarding_scan_later")
                        .frame(maxWidth: .infinity, minHeight: 44)
                }
                .buttonStyle(.bordered)
            }
        }
    }

    private func summaryRow(labelKey: LocalizedStringKey, value: String?) -> some View {
        HStack {
            Text(labelKey).foregroundStyle(.secondary)
            Spacer()
            if let value {
                Text(value)
            } else {
                Text("onboarding_summary_not_set").foregroundStyle(.tertiary)
            }
        }
    }

    // MARK: - Navigation

    private var canContinue: Bool {
        switch step {
        case .birthday: return birthdaySet             // required (PRD §14)
        case .dominantHand: return dominantHand != nil // required (PRD §14)
        default: return true
        }
    }

    private var navigationBar: some View {
        HStack {
            if step != .welcome {
                Button {
                    withAnimation(reduceMotion ? nil : .default) { step = Step(rawValue: step.rawValue - 1) ?? .welcome }
                } label: {
                    Text("common_back").frame(minHeight: 44)
                }
            }
            Spacer()
            if step != .scanEntry {
                Button {
                    withAnimation(reduceMotion ? nil : .default) { step = Step(rawValue: step.rawValue + 1) ?? .scanEntry }
                } label: {
                    Text("common_next").frame(minWidth: 88, minHeight: 44)
                }
                .buttonStyle(.borderedProminent)
                .disabled(!canContinue)
            }
        }
        .padding()
    }

    /// Free-text coordinates are only trusted inside the real geographic
    /// range. Anything else stays nil, which keeps the reading at L1 rather
    /// than letting the ascendant formula silently clamp a typo to ±89.9.
    private static func coordinate(_ text: String, limit: Double) -> Double? {
        guard let value = Double(text), value >= -limit, value <= limit else { return nil }
        return value
    }

    private func completeOnboarding() {
        var profile = model.profile
        profile.displayName = displayName
        let components = Calendar.current.dateComponents([.year, .month, .day], from: birthdayDate)
        if let year = components.year, let month = components.month, let day = components.day {
            profile.birthday = CivilDate(year: year, month: month, day: day)
        }
        profile.dominantHand = dominantHand ?? .RIGHT
        profile.relationshipStatus = relationshipStatus
        let latitude = Self.coordinate(birthPlaceLatText, limit: 90)
        let longitude = Self.coordinate(birthPlaceLonText, limit: 180)
        if birthTimeKnown {
            let time = Calendar.current.dateComponents([.hour, .minute], from: birthTimeDate)
            profile.birthTime = CivilTime(hour: time.hour ?? 0, minute: time.minute ?? 0)
            profile.birthPlaceLat = latitude
            profile.birthPlaceLon = longitude
        }
        profile.tone = tone
        profile.language = language
        profile.onboardingComplete = true
        model.profile = profile
        model.saveProfile()
        model.analytics.emit(eventName: "onboarding_complete", props: [
            "calc_level": (birthTimeKnown && latitude != nil && longitude != nil) ? "l2" : "l1",
            "tone": tone.rawValue.lowercased(),
            "language": language.lowercased(),
        ])
    }
}
