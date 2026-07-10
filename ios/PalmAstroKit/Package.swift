// swift-tools-version: 6.0
// PalmAstroKit — cross-platform engine layer for the PalmAstro iOS app.
// Engines are platform-neutral (no UIKit/SwiftUI/AVFoundation imports) so the
// whole package compiles and tests on macOS with the Command Line Tools alone
// (tools-version 6 is required for Swift Testing; language mode stays 5).
import PackageDescription

// NOTE (CLT-only machines): with Xcode installed, a plain `swift test` works.
// With Command Line Tools only, Swift Testing lives outside the default
// search paths — run `./test.sh` (same directory), which passes the required
// framework/rpath flags. Keep flags out of the manifest: unsafeFlags would
// taint the package for consumers, and manifest-injected paths make a plain
// `swift test` silently skip the run phase (false green).
func engineTestTarget(_ name: String, dependencies: [Target.Dependency]) -> Target {
    .testTarget(name: name, dependencies: dependencies)
}

let package = Package(
    name: "PalmAstroKit",
    defaultLocalization: "en",
    platforms: [
        .macOS(.v14),
        .iOS(.v17),
    ],
    products: [
        .library(
            name: "PalmAstroKit",
            targets: [
                "CoreContracts",
                "ScanQualityEngine",
                "PalmFeatureEngine",
                "AstroEngine",
                "ScoringEngine",
                "ContentEngine",
                "SafetyEngine",
                "AnalyticsService",
                "DataStore",
            ]
        ),
    ],
    targets: [
        // MARK: Sources
        .target(name: "CoreContracts"),
        .target(name: "ScanQualityEngine", dependencies: ["CoreContracts"]),
        .target(name: "PalmFeatureEngine", dependencies: ["CoreContracts"]),
        .target(name: "AstroEngine", dependencies: ["CoreContracts"]),
        .target(
            name: "ScoringEngine",
            dependencies: ["CoreContracts"],
            resources: [.process("Resources")]
        ),
        .target(
            name: "ContentEngine",
            dependencies: ["CoreContracts"],
            resources: [.process("Resources")]
        ),
        // SafetyEngine mirrors the Kotlin engine-content module, where the
        // safety filter draws its localized replacement copy from the content
        // template library — hence the ContentEngine dependency.
        .target(
            name: "SafetyEngine",
            dependencies: ["CoreContracts", "ContentEngine"],
            resources: [.process("Resources")]
        ),
        .target(name: "AnalyticsService", dependencies: ["CoreContracts"]),
        .target(name: "DataStore", dependencies: ["CoreContracts"]),

        // MARK: Tests
        engineTestTarget("CoreContractsTests", dependencies: ["CoreContracts"]),
        engineTestTarget("ScanQualityEngineTests", dependencies: ["ScanQualityEngine", "CoreContracts"]),
        engineTestTarget("PalmFeatureEngineTests", dependencies: ["PalmFeatureEngine", "CoreContracts"]),
        engineTestTarget("AstroEngineTests", dependencies: ["AstroEngine", "CoreContracts"]),
        engineTestTarget("ScoringEngineTests", dependencies: ["ScoringEngine", "CoreContracts"]),
        engineTestTarget("ContentEngineTests", dependencies: ["ContentEngine", "ScoringEngine", "CoreContracts"]),
        engineTestTarget("SafetyEngineTests", dependencies: ["SafetyEngine", "ContentEngine", "CoreContracts"]),
        engineTestTarget("AnalyticsServiceTests", dependencies: ["AnalyticsService", "CoreContracts"]),
        engineTestTarget("DataStoreTests", dependencies: ["DataStore", "CoreContracts"]),
        engineTestTarget(
            "ParityTests",
            dependencies: ["CoreContracts", "ScoringEngine", "ContentEngine", "SafetyEngine", "AstroEngine"]
        ),
    ],
    swiftLanguageModes: [.v5]
)
