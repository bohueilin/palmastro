import Foundation
import Testing
import CoreContracts
@testable import AnalyticsService

@Suite struct AnalyticsEmitterTests {

    private final class Capture: @unchecked Sendable {
        var events: [(name: String, props: [String: Any])] = []
    }

    private func makeEmitter() -> (AnalyticsEmitterImpl, Capture) {
        let capture = Capture()
        let emitter = AnalyticsEmitterImpl { name, props in
            capture.events.append((name, props))
        }
        return (emitter, capture)
    }

    @Test func allowlistedEventWithValidPropsPasses() {
        let (emitter, capture) = makeEmitter()
        emitter.emit(eventName: "scan_angle_pass", props: ["angle": "front", "attempt": 2])
        #expect(capture.events.count == 1)
        #expect(capture.events[0].name == "scan_angle_pass")
        #expect(capture.events[0].props["angle"] as? String == "front")
        #expect(capture.events[0].props["attempt"] as? Int == 2)
    }

    @Test func unknownEventDropped() {
        let (emitter, capture) = makeEmitter()
        emitter.emit(eventName: "totally_new_event", props: [:])
        #expect(capture.events.isEmpty)
    }

    @Test func nonAllowlistedPropertyDropped() {
        let (emitter, capture) = makeEmitter()
        emitter.emit(eventName: "results_view", props: ["domain": "career", "secret_field": "x"])
        #expect(capture.events.count == 1)
        #expect(capture.events[0].props["secret_field"] == nil)
        #expect(capture.events[0].props["domain"] as? String == "career")
    }

    @Test func freeTextStringDropped() {
        let (emitter, capture) = makeEmitter()
        emitter.emit(eventName: "journal_saved", props: ["reason": "I feel great about my career today!"])
        #expect(capture.events.count == 1)
        // Free text must not pass the token shape.
        #expect(capture.events[0].props.isEmpty)
    }

    @Test func mediaPathValueDropped() {
        let (emitter, capture) = makeEmitter()
        emitter.emit(eventName: "scan_complete", props: ["reason": "/data/scan/frame1.jpg"])
        #expect(capture.events.count == 1)
        #expect(capture.events[0].props.isEmpty)
    }

    @Test func numericVectorDropped() {
        let (emitter, capture) = makeEmitter()
        emitter.emit(eventName: "inference_success", props: ["value": [0.1, 0.2, 0.3, 0.4, 0.5]])
        #expect(capture.events.count == 1)
        // Landmark/embedding vectors may not leave the device.
        #expect(capture.events[0].props.isEmpty)
    }

    @Test func tokenShapeAcceptsIdentifiersAndLocaleTags() {
        let (emitter, capture) = makeEmitter()
        emitter.emit(eventName: "language_change", props: ["language": "zh-tw"])
        emitter.emit(eventName: "purchase_start", props: ["product_id": "palmastro.pack.career"])
        #expect(capture.events.count == 2)
        #expect(capture.events[0].props["language"] as? String == "zh-tw")
        #expect(capture.events[1].props["product_id"] as? String == "palmastro.pack.career")
    }

    @Test func booleanAndNumberTypesPass() {
        let (emitter, capture) = makeEmitter()
        emitter.emit(eventName: "retention_toggle_change", props: ["enabled": false, "duration_ms": 1234])
        #expect(capture.events.count == 1)
        #expect(capture.events[0].props["enabled"] as? Bool == false)
        #expect(capture.events[0].props["duration_ms"] as? Int == 1234)
    }

    @Test func overlongStringDropped() {
        let (emitter, capture) = makeEmitter()
        let long = String(repeating: "a", count: 80)
        emitter.emit(eventName: "settings_view", props: ["screen": long])
        #expect(capture.events.count == 1)
        #expect(capture.events[0].props.isEmpty)
    }
}
