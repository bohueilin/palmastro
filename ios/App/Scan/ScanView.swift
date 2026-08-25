import SwiftUI
import AVFoundation
import CoreContracts

/// Seven-angle scan flow (PRD §13.2): camera preview, per-angle guidance,
/// live quality coaching keys resolved to localized strings, graceful
/// permission-denied fallback, and hand-off to the on-device result pipeline.
struct ScanView: View {

    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss

    @StateObject private var controller = HandPoseScanController()
    @State private var processing = false

    var body: some View {
        ZStack {
            CameraPreviewView(session: controller.session)
                .ignoresSafeArea()

            VStack {
                header
                Spacer()
                guidance
            }
            .padding()

            if controller.permissionDenied {
                permissionFallback
            }
            if processing {
                processingReveal
            }
        }
        .onAppear {
            Haptics.shared.prepare()
            model.analytics.emit(eventName: "scan_start", props: [:])
            controller.begin(hand: model.profile.dominantHand)
        }
        .onDisappear {
            controller.stop()
        }
        .onChange(of: controller.finishedSummary) { _, summary in
            guard let summary else { return }
            // Haptic moment 2 (thumpQualityPass): the quality gate passed the
            // full seven-angle session.
            Haptics.shared.thumpQualityPass()
            processing = true
            model.analytics.emit(eventName: "scan_complete", props: [
                "duration_ms": Int(summary.totalDurationMs),
                "attempt": summary.totalAttempts,
            ])
            model.processScanSession(summary)
            // Dismissal waits for the constellation reveal to finish
            // (instant static frame under Reduce Motion).
        }
        .onChange(of: controller.currentAngleIndex) { _, index in
            // Haptic moment 1 (tickCapture): one angle captured successfully.
            Haptics.shared.tickCapture()
            model.analytics.emit(eventName: "scan_angle_pass", props: ["angle": angleToken(at: index - 1)])
        }
        .onChange(of: controller.qualityFailCount) { _, _ in
            // Haptic moment 3 (buzzQualityFail): gentle, once per gate
            // evaluation — coaching, never an alarm (PRD §12.3).
            Haptics.shared.buzzQualityFail()
        }
    }

    /// Signature processing state: the constellation reveal plays once over a
    /// dimmed camera, then hands off to results. The caption below the canvas
    /// keeps loading visible and screen-reader accessible (PRD §41: motion
    /// never hides loading).
    private var processingReveal: some View {
        ZStack {
            Color.black.opacity(0.6).ignoresSafeArea()
            VStack(spacing: 20) {
                ConstellationRevealView {
                    // Haptic moment 4 (shimmerReveal): the reading is ready.
                    Haptics.shared.shimmerReveal()
                    dismiss()
                }
                .frame(width: 260, height: 260)
                Text("scan_processing")
                    .font(.headline)
                    .foregroundStyle(.white)
            }
        }
    }

    private var header: some View {
        VStack(spacing: 8) {
            HStack {
                Button {
                    controller.stop()
                    dismiss()
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.title)
                        .foregroundStyle(.white.opacity(0.85))
                        .frame(minWidth: 44, minHeight: 44)
                }
                .accessibilityLabel(Text("common_close"))
                Spacer()
            }
            ProgressView(
                value: Double(controller.currentAngleIndex),
                total: Double(HandPoseScanController.angleSequence.count)
            )
            .tint(.white)
        }
    }

    private var guidance: some View {
        VStack(spacing: 12) {
            Text(anglePromptKey(for: controller.currentAngle))
                .font(.title3.bold())
                .foregroundStyle(.white)
                .multilineTextAlignment(.center)

            if let hintKey = controller.lastHintKey {
                Text(LocalizedStringKey(hintKey))
                    .font(.body)
                    .foregroundStyle(.yellow)
                    .multilineTextAlignment(.center)
                    .accessibilityLabel(Text(LocalizedStringKey(hintKey)))
            } else if !controller.isHandDetected {
                Text("coach_hand_not_detected")
                    .font(.body)
                    .foregroundStyle(.yellow)
            }
        }
        .padding()
        .frame(maxWidth: .infinity)
        .background(.black.opacity(0.55), in: RoundedRectangle(cornerRadius: 16))
    }

    private var permissionFallback: some View {
        VStack(spacing: 16) {
            Image(systemName: "camera.badge.ellipsis").font(.largeTitle)
            Text("scan_permission_denied_title").font(.headline)
            Text("scan_permission_denied_body")
                .multilineTextAlignment(.center)
                .font(.subheadline)
            Button {
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(url)
                }
            } label: {
                Text("scan_open_settings").frame(minHeight: 44)
            }
            .buttonStyle(.borderedProminent)
            Button {
                dismiss()
            } label: {
                Text("common_close").frame(minHeight: 44)
            }
        }
        .padding()
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20))
        .padding()
    }

    // Qualified: SwiftUI also exports a top-level `Angle`, so a bare reference
    // here is ambiguous for type lookup.
    private func anglePromptKey(for angle: CoreContracts.Angle) -> LocalizedStringKey {
        switch angle {
        case .FRONT: return "scan_angle_front"
        case .LEFT_TILT: return "scan_angle_left_tilt"
        case .RIGHT_TILT: return "scan_angle_right_tilt"
        case .NEAR: return "scan_angle_near"
        case .FAR: return "scan_angle_far"
        case .UP_TILT: return "scan_angle_up_tilt"
        case .DOWN_TILT: return "scan_angle_down_tilt"
        }
    }

    private func angleToken(at index: Int) -> String {
        guard index >= 0, index < HandPoseScanController.angleSequence.count else { return "unknown" }
        return HandPoseScanController.angleSequence[index].rawValue.lowercased()
    }
}

/// UIKit-bridged AVCaptureSession preview layer.
struct CameraPreviewView: UIViewRepresentable {

    let session: AVCaptureSession

    final class PreviewView: UIView {
        override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }
        var previewLayer: AVCaptureVideoPreviewLayer { layer as! AVCaptureVideoPreviewLayer }
    }

    func makeUIView(context: Context) -> PreviewView {
        let view = PreviewView()
        view.previewLayer.session = session
        view.previewLayer.videoGravity = .resizeAspectFill
        return view
    }

    func updateUIView(_ uiView: PreviewView, context: Context) {}
}
