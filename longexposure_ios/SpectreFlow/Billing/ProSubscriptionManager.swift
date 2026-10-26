import Foundation
import Combine

public protocol ProSubscriptionProtocol: AnyObject {
    var isProUnlocked: Bool { get }
    func purchasePro() async -> Result<Bool, Error>
    func restorePurchases() async -> Result<Bool, Error>
    func setProUnlocked(_ unlocked: Bool)
}

/// Manages SpectreFlow Pro entitlement.
/// Implemented cleanly as an observable service ready for StoreKit 2 Product.purchase() integration.
public final class ProSubscriptionManager: ObservableObject, ProSubscriptionProtocol {
    public static let shared = ProSubscriptionManager()

    @Published public private(set) var isProUnlocked: Bool = false

    private let proKey = "spectreflow_is_pro_unlocked"

    public init() {
        self.isProUnlocked = UserDefaults.standard.bool(forKey: proKey)
    }

    public func purchasePro() async -> Result<Bool, Error> {
        // Ready for StoreKit 2:
        // let result = try await product.purchase()
        setProUnlocked(true)
        return .success(true)
    }

    public func restorePurchases() async -> Result<Bool, Error> {
        // Ready for StoreKit 2:
        // try await AppStore.sync()
        return .success(isProUnlocked)
    }

    public func setProUnlocked(_ unlocked: Bool) {
        self.isProUnlocked = unlocked
        UserDefaults.standard.set(unlocked, forKey: proKey)
    }
}
