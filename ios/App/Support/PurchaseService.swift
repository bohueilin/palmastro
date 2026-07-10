import Foundation
import StoreKit
import CoreContracts
import DataStore

/// StoreKit 2 purchase service (PRD §22, §48).
///
/// Launch posture: `iap_enabled=false` (EXECUTION_SPEC free-only launch) —
/// no paid CTA is shown and `loadProducts()` short-circuits, but the full
/// purchase/restore/entitlement scaffolding stays wired so flipping the flag
/// post-launch requires no code change. Entitlements are mirrored into the
/// local DataStore for synchronous, offline-safe checks; StoreKit remains the
/// source of truth.
@MainActor
final class PurchaseService: ObservableObject {

    enum PurchaseError: Error {
        case iapDisabled
        case productNotFound
        case pendingOrCancelled
        case failedVerification
    }

    @Published private(set) var products: [Product] = []
    @Published private(set) var ownedProductIds: Set<String> = []

    private let flags: FeatureFlags
    private let entitlements: EntitlementServiceImpl
    private var transactionListener: Task<Void, Never>?

    init(flags: FeatureFlags = .shared, entitlements: EntitlementServiceImpl) {
        self.flags = flags
        self.entitlements = entitlements
        self.ownedProductIds = entitlements.activeEntitlements()
        guard flags.iapEnabled else { return }
        transactionListener = Task { [weak self] in
            for await update in Transaction.updates {
                await self?.handle(transactionResult: update)
            }
        }
    }

    deinit {
        transactionListener?.cancel()
    }

    // MARK: - Catalog

    func loadProducts() async {
        guard flags.iapEnabled else { return }
        do {
            products = try await Product.products(for: ProductIds.all)
        } catch {
            products = []
        }
    }

    // MARK: - Purchase / restore

    func purchase(productId: String) async throws {
        guard flags.iapEnabled else { throw PurchaseError.iapDisabled }
        guard let product = products.first(where: { $0.id == productId }) else {
            throw PurchaseError.productNotFound
        }
        let result = try await product.purchase()
        switch result {
        case .success(let verification):
            await handle(transactionResult: verification)
        case .pending, .userCancelled:
            throw PurchaseError.pendingOrCancelled
        @unknown default:
            throw PurchaseError.pendingOrCancelled
        }
    }

    /// Restore = sync with the App Store then replay current entitlements.
    func restorePurchases() async throws {
        guard flags.iapEnabled else { throw PurchaseError.iapDisabled }
        try await AppStore.sync()
        await refreshEntitlements()
    }

    /// Rebuilds the local entitlement mirror from StoreKit's
    /// currentEntitlements (called on launch and after restore).
    func refreshEntitlements() async {
        guard flags.iapEnabled else { return }
        var active: Set<String> = []
        for await result in Transaction.currentEntitlements {
            if case .verified(let transaction) = result,
               transaction.revocationDate == nil,
               ProductIds.all.contains(transaction.productID) {
                active.insert(transaction.productID)
                expandBundle(into: &active, productId: transaction.productID)
            }
        }
        persist(active)
    }

    func hasEntitlement(productId: String) -> Bool {
        ownedProductIds.contains(productId)
    }

    // MARK: - Internals

    private func handle(transactionResult: VerificationResult<Transaction>) async {
        guard case .verified(let transaction) = transactionResult else { return }
        if transaction.revocationDate == nil, ProductIds.all.contains(transaction.productID) {
            var active = ownedProductIds
            active.insert(transaction.productID)
            expandBundle(into: &active, productId: transaction.productID)
            persist(active)
        }
        await transaction.finish()
    }

    /// The bundle unlocks both packs (PRD §22).
    private func expandBundle(into active: inout Set<String>, productId: String) {
        if productId == ProductIds.bundle {
            active.insert(ProductIds.careerPack)
            active.insert(ProductIds.wealthPack)
        }
    }

    private func persist(_ active: Set<String>) {
        ownedProductIds = active
        try? entitlements.replaceAll(with: active)
    }
}
