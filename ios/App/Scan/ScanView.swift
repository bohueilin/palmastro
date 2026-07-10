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
                ProgressView("scan_processing")
                    .padding()
                    .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 12))
            }
        }
        .onAppear {
            model.analytics.emit(eventName: "scan_start", props: [:])
            controller.begin(hand: model.profile.dominantHand)
        }
        .onDisappear {
            controller.stop()
        }
        .onChange(of: controller.finishedSummary) { _, summary in
            guard let summary else { return }
            processing = true
            model.analytics.emit(eventName: "scan_complete", props: [
                "duration_ms": Int(summary.totalDurationMs),
                "attempt": summary.totalAttempts,
            ])
            model.processScanSession(summary)
            processing = false
            dismiss()
        }
        .onChange(of: controller.currentAngleIndex) { _, index in
            model.analytics.emit(eventName: "scan_angle_pass", props: ["angle": angleToken(at: index - 1)])
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

    private func anglePromptKey(for angle: Angle) -> LocalizedStringKey {
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
