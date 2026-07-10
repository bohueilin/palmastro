import Foundation
import Security

/// Random install identifier stored in the Keychain (PRD §48: Keychain for
/// install ID). Not tied to the Apple ID or device hardware; rotated by
/// delete-all-data so post-wipe analytics cannot be joined to pre-wipe data.
enum KeychainInstallId {

    private static let service = "com.palmastro.install-id"
    private static let account = "install-id"

    /// Returns the current install id, creating one on first access.
    static func currentId() -> String {
        if let existing = read() {
            return existing
        }
        let fresh = UUID().uuidString.lowercased()
        write(fresh)
        return fresh
    }

    /// Deletes the stored id and mints a new one (delete-all-data, PRD §28).
    @discardableResult
    static func rotate() -> String {
        delete()
        let fresh = UUID().uuidString.lowercased()
        write(fresh)
        return fresh
    }

    // MARK: - Keychain plumbing

    private static var baseQuery: [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
        ]
    }

    private static func read() -> String? {
        var query = baseQuery
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        guard status == errSecSuccess, let data = result as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }

    private static func write(_ value: String) {
        var query = baseQuery
        query[kSecValueData as String] = Data(value.utf8)
        // Device-only, never synced or backed up to another device.
        query[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        let status = SecItemAdd(query as CFDictionary, nil)
        if status == errSecDuplicateItem {
            SecItemUpdate(
                baseQuery as CFDictionary,
                [kSecValueData as String: Data(value.utf8)] as CFDictionary
            )
        }
    }

    private static func delete() {
        SecItemDelete(baseQuery as CFDictionary)
    }
}
