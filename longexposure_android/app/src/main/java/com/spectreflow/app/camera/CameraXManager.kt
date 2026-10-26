package com.spectreflow.app.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * CameraX Pipeline Manager.
 * Orchestrates camera lifecycle, live preview, sensor locks (AF/AE/AWB),
 * and burst frame streaming via ImageAnalysis without dropping frames or leaking memory.
 */
class CameraXManager(private val context: Context) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageAnalysis: ImageAnalysis? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val isCapturing = AtomicBoolean(false)
    private var captureJob: Job? = null
    private var captureStartTimeMs: Long = 0L
    private val scope = CoroutineScope(Dispatchers.Default)

    var onFrameAvailable: ((Bitmap) -> Unit)? = null
    var onPreviewFrameAvailable: ((Bitmap) -> Unit)? = null
    var onBurstProgress: ((progress: Float, elapsedSeconds: Float) -> Unit)? = null
    var onBurstComplete: (() -> Unit)? = null
    var onSupportedLensesChanged: ((List<Float>) -> Unit)? = null

    var isFrontCamera = false
        private set
    var currentZoom = 1.0f
        private set
    var supportedLenses: List<Float> = listOf(1.0f)
        private set

    private var lastPreviewAnalysisMs = 0L

    fun initialize(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        onReady: () -> Unit
    ) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            cameraProvider = future.get()
            bindCamera(lifecycleOwner, surfaceProvider)
            onReady()
        }, ContextCompat.getMainExecutor(context))
    }

    fun flipCamera(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        if (isCapturing.get()) return
        isFrontCamera = !isFrontCamera
        bindCamera(lifecycleOwner, surfaceProvider)
    }

    fun setZoomRatio(ratio: Float) {
        camera?.cameraControl?.setZoomRatio(ratio)
        currentZoom = ratio
    }

    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider
    ) {
        val provider = cameraProvider ?: return
        provider.unbindAll()

        val preview = Preview.Builder()
            .build()
            .also {
                it.surfaceProvider = surfaceProvider
            }

        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageProxy(imageProxy)
                }
            }

        val cameraSelector = if (isFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        try {
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )

            camera?.cameraInfo?.zoomState?.observe(lifecycleOwner) { zoomState ->
                val minZoom = zoomState.minZoomRatio
                val maxZoom = zoomState.maxZoomRatio
                val candidates = listOf(0.5f, 1.0f, 2.0f, 3.0f)
                val filtered = candidates.filter { it in minZoom..maxZoom }
                supportedLenses = if (filtered.isEmpty()) listOf(1.0f) else filtered
                onSupportedLensesChanged?.invoke(supportedLenses)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        val now = SystemClock.elapsedRealtime()
        if (!isCapturing.get()) {
            if (now - lastPreviewAnalysisMs > 350L && onPreviewFrameAvailable != null) {
                lastPreviewAnalysisMs = now
                try {
                    val bitmap = imageProxy.toBitmap()
                    onPreviewFrameAvailable?.invoke(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            imageProxy.close()
            return
        }

        try {
            val bitmap = imageProxy.toBitmap()
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val rotatedBitmap = if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }

            onFrameAvailable?.invoke(rotatedBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            imageProxy.close()
        }
    }

    /**
     * Locks AF/AE/AWB sensor values so burst frames maintain consistent luminance,
     * then streams frames until release or until maxDurationSeconds is reached.
     */
    fun startBurst(maxDurationSeconds: Int) {
        if (isCapturing.get()) return

        lockSensorSettings()
        isCapturing.set(true)
        captureStartTimeMs = SystemClock.elapsedRealtime()

        val totalDurationMs = maxDurationSeconds * 1000L
        val intervalMs = 50L

        captureJob = scope.launch {
            while (isCapturing.get()) {
                delay(intervalMs)
                val elapsedMs = SystemClock.elapsedRealtime() - captureStartTimeMs
                val progress = (elapsedMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)
                val elapsedSec = elapsedMs / 1000f
                onBurstProgress?.invoke(progress, elapsedSec)

                if (elapsedMs >= totalDurationMs) {
                    // Auto-finish when maximum duration cap is reached
                    if (isCapturing.compareAndSet(true, false)) {
                        unlockSensorSettings()
                        onBurstComplete?.invoke()
                    }
                    break
                }
            }
        }
    }

    /**
     * Stops the burst capture upon user shutter release.
     * Returns total elapsed hold time in milliseconds.
     */
    fun stopBurst(): Long {
        if (!isCapturing.compareAndSet(true, false)) return 0L
        val elapsedMs = SystemClock.elapsedRealtime() - captureStartTimeMs
        captureJob?.cancel()
        unlockSensorSettings()
        return elapsedMs
    }

    /**
     * Cancels capture and discards state (e.g. finger dragged away or app backgrounded).
     */
    fun cancelBurst() {
        if (!isCapturing.compareAndSet(true, false)) return
        captureJob?.cancel()
        unlockSensorSettings()
    }

    private fun lockSensorSettings() {
        val ctrl = camera?.cameraControl ?: return
        try {
            // Lock center point autofocus and autoexposure without auto-canceling
            val factory = SurfaceOrientedMeteringPointFactory(1f, 1f)
            val centerPoint = factory.createPoint(0.5f, 0.5f)
            val action = FocusMeteringAction.Builder(
                centerPoint,
                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
            ).disableAutoCancel().build()

            ctrl.startFocusAndMetering(action)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun unlockSensorSettings() {
        val ctrl = camera?.cameraControl ?: return
        try {
            ctrl.cancelFocusAndMetering()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shutdown() {
        cancelBurst()
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()
    }
}
