import Foundation
import CoreMotion
import Combine

/// Real-time 6-DoF Motion Stability Classifier using CoreMotion.
/// Reads IMU Gyroscope rotation rates and user acceleration at 60 Hz to distinguish
/// handheld tremor from solid tripod stabilization.
public final class MotionStabilityDetector: ObservableObject {
    private let motionManager = CMMotionManager()
    private let queue = OperationQueue()

    @Published public private(set) var stabilityState: StabilityState = .shaky

    private var accelHistory: [Double] = Array(repeating: 0, count: 16)
    private var accelIndex = 0

    private let gyroStableThreshold = 0.045
    private let accelVarianceThreshold = 0.025

    public init() {
        queue.qualityOfService = .userInteractive
    }

    public func startMonitoring() {
        guard motionManager.isDeviceMotionAvailable else {
            DispatchQueue.main.async { self.stabilityState = .shaky }
            return
        }

        motionManager.deviceMotionUpdateInterval = 1.0 / 60.0
        motionManager.startDeviceMotionUpdates(to: queue) { [weak self] motion, error in
            guard let self = self, let motion = motion else { return }

            // Gyroscope rotation rate norm (rad/s)
            let rx = motion.rotationRate.x
            let ry = motion.rotationRate.y
            let rz = motion.rotationRate.z
            let gyroNorm = sqrt(rx * rx + ry * ry + rz * rz)

            // Linear user acceleration magnitude
            let ax = motion.userAcceleration.x
            let ay = motion.userAcceleration.y
            let az = motion.userAcceleration.z
            let accelMag = sqrt(ax * ax + ay * ay + az * az)

            self.accelHistory[self.accelIndex % self.accelHistory.count] = accelMag
            self.accelIndex += 1

            let mean = self.accelHistory.reduce(0, +) / Double(self.accelHistory.count)
            let variance = self.accelHistory.reduce(0) { $0 + ($1 - mean) * ($1 - mean) } / Double(self.accelHistory.count)

            let isStable = gyroNorm < self.gyroStableThreshold && variance < self.accelVarianceThreshold
            let newState: StabilityState = isStable ? .stable : .shaky

            if self.stabilityState != newState {
                DispatchQueue.main.async {
                    self.stabilityState = newState
                }
            }
        }
    }

    public func stopMonitoring() {
        motionManager.stopDeviceMotionUpdates()
    }

    deinit {
        stopMonitoring()
    }
}
