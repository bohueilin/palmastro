// Mirrors contracts/src/main/kotlin/com/palmastro/contracts/Enums.kt exactly.
// Raw values MUST equal the Kotlin enum names so JSON payloads and fixtures
// are byte-compatible across platforms (kotlinx.serialization encodes names).

public enum Hand: String, Codable, CaseIterable, Equatable, Sendable {
    case LEFT
    case RIGHT
}

public enum Angle: String, Codable, CaseIterable, Equatable, Sendable {
    case FRONT
    case LEFT_TILT
    case RIGHT_TILT
    case NEAR
    case FAR
    case UP_TILT
    case DOWN_TILT
}

/// Ensures `[Angle: X]` dictionaries encode as JSON objects keyed by the raw
/// value (matching kotlinx `Map<Angle, X>`), not as flat arrays.
extension Angle: CodingKeyRepresentable {}

public enum CalcLevel: String, Codable, CaseIterable, Equatable, Sendable {
    case L1
    case L2
}

public enum Tone: String, Codable, CaseIterable, Equatable, Sendable {
    case SCIENTIFIC
    case HEALING
    case ROAST_SAFE
}

public enum ComparabilityBucket: String, Codable, CaseIterable, Equatable, Sendable {
    case HIGH
    case MED
    case LOW
}
