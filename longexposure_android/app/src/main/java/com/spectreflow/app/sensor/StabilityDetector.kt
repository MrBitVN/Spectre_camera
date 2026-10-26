package com.spectreflow.app.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

enum class StabilityState {
    STABLE,
    SHAKY
}

/**
 * Real-time IMU 6-DoF Stability Detector using Accelerometer & Gyroscope.
 * Detects whether the device is handheld or on a tripod/resting surface.
 * When STABLE, allows the camera pipeline to skip CPU-intensive frame alignment.
 */
class StabilityDetector(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val _stabilityState = MutableStateFlow(StabilityState.SHAKY)
    val stabilityState: StateFlow<StabilityState> = _stabilityState.asStateFlow()

    private var currentGyroNorm: Float = 0f
    private val accelHistory = FloatArray(16)
    private var accelIndex = 0

    // Thresholds: Below this angular velocity (rad/s), device is considered tripod stable
    private val gyroStableThreshold = 0.045f
    private val accelVarianceThreshold = 0.025f

    fun startListening() {
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        gyroscope?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                val gx = event.values[0]
                val gy = event.values[1]
                val gz = event.values[2]
                currentGyroNorm = sqrt((gx * gx + gy * gy + gz * gz).toDouble()).toFloat()
                evaluateStability()
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val ax = event.values[0]
                val ay = event.values[1]
                val az = event.values[2]
                val mag = sqrt((ax * ax + ay * ay + az * az).toDouble()).toFloat()
                accelHistory[accelIndex % accelHistory.size] = mag
                accelIndex++
                evaluateStability()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun evaluateStability() {
        // Calculate accelerometer variance over recent samples
        var sum = 0f
        val count = accelHistory.size
        for (v in accelHistory) sum += v
        val mean = sum / count

        var varianceSum = 0f
        for (v in accelHistory) {
            val diff = v - mean
            varianceSum += diff * diff
        }
        val variance = varianceSum / count

        val isStable = currentGyroNorm < gyroStableThreshold && variance < accelVarianceThreshold
        _stabilityState.value = if (isStable) StabilityState.STABLE else StabilityState.SHAKY
    }
}
