package com.spectreflow.app

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spectreflow.app.ai.HeuristicSceneDetector
import com.spectreflow.app.alignment.FrameAligner
import com.spectreflow.app.billing.BillingService
import com.spectreflow.app.billing.StubBillingService
import com.spectreflow.app.blender.FrameBlenderEngine
import com.spectreflow.app.blender.LongExposureMode
import com.spectreflow.app.sensor.StabilityDetector
import com.spectreflow.app.sensor.StabilityState
import com.spectreflow.app.settings.AppSettingsManager
import com.spectreflow.app.settings.AppSettingsState
import com.spectreflow.app.settings.CaptureDuration
import com.spectreflow.app.storage.MediaSaver
import com.spectreflow.app.video.CompanionVideoRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicReference

enum class CaptureState {
    IDLE,
    CAPTURING,
    PROCESSING,
    RESULT,
    ERROR
}

enum class AppScreen {
    CAMERA,
    PROCESSING,
    RESULT,
    SETTINGS,
    ONBOARDING
}

data class UiState(
    val currentScreen: AppScreen = AppScreen.CAMERA,
    val isCapturing: Boolean = false,
    val captureProgress: Float = 0f,
    val elapsedSeconds: Float = 0f,
    val hintMessage: String? = null,
    val processingProgress: Float = 0f,
    val statusMessage: String = "",
    val selectedMode: LongExposureMode = LongExposureMode.AUTO,
    val detectedMode: LongExposureMode = LongExposureMode.MOTION_BLUR,
    val selectedDuration: CaptureDuration = CaptureDuration.SEC_3,
    val settings: AppSettingsState = AppSettingsState(),
    val resultBitmap: Bitmap? = null,
    val keyframes: List<Bitmap> = emptyList(),
    val companionVideoUri: Uri? = null,
    val companionVideoFile: File? = null,
    val supportedLenses: List<Float> = listOf(1.0f),
    val currentZoom: Float = 1.0f,
    val isFrontCamera: Boolean = false,
    val savedUri: Uri? = null,
    val showPermissionDialog: Boolean = false,
    val isLimitedAccess: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val settingsManager = AppSettingsManager(application)
    val billingService: BillingService = StubBillingService(settingsManager)
    private val mediaSaver = MediaSaver(application)
    private val blenderEngine = FrameBlenderEngine()
    private val stabilityDetector = StabilityDetector(application)
    private val sceneDetector = HeuristicSceneDetector()

    private var videoRecorder: CompanionVideoRecorder? = null
    private var tempVideoFile: File? = null
    private val burstKeyframes = mutableListOf<Bitmap>()
    private var effectiveModeAtCapture: LongExposureMode = LongExposureMode.MOTION_BLUR

    private val captureState = AtomicReference(CaptureState.IDLE)
    private var frameChannel: Channel<Bitmap>? = null
    private var frameConsumerJob: Job? = null

    private val initialSettings = settingsManager.loadSettings()
    private val _uiState = MutableStateFlow(
        UiState(
            settings = initialSettings,
            currentScreen = if (!initialSettings.hasSeenOnboarding) AppScreen.ONBOARDING else AppScreen.CAMERA
        )
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val stabilityState: StateFlow<StabilityState> = stabilityDetector.stabilityState

    private var frameAligner: FrameAligner? = null
    private var isFirstFrame = true

    init {
        stabilityDetector.startListening()
    }

    fun onPermissionGranted(isLimited: Boolean = false) {
        _uiState.value = _uiState.value.copy(
            showPermissionDialog = false,
            isLimitedAccess = isLimited
        )
    }

    fun onPermissionDenied() {
        _uiState.value = _uiState.value.copy(showPermissionDialog = true)
    }

    fun onDismissOnboarding() {
        val updated = _uiState.value.settings.copy(hasSeenOnboarding = true)
        settingsManager.saveSettings(updated)
        _uiState.value = _uiState.value.copy(
            settings = updated,
            currentScreen = AppScreen.CAMERA
        )
    }

    fun onOpenOnboarding() {
        _uiState.value = _uiState.value.copy(currentScreen = AppScreen.ONBOARDING)
    }

    fun onPreviewFrameCaptured(bitmap: Bitmap) {
        if (_uiState.value.isCapturing) return
        viewModelScope.launch(Dispatchers.Default) {
            val stats = sceneDetector.analyzeFrame(bitmap)
            val detected = sceneDetector.classifyScene(stats)
            if (_uiState.value.detectedMode != detected) {
                _uiState.value = _uiState.value.copy(detectedMode = detected)
            }
        }
    }

    fun onSupportedLensesChanged(lenses: List<Float>) {
        _uiState.value = _uiState.value.copy(supportedLenses = lenses)
    }

    fun onZoomChanged(zoom: Float) {
        _uiState.value = _uiState.value.copy(currentZoom = zoom)
    }

    fun onFrontCameraChanged(isFront: Boolean) {
        _uiState.value = _uiState.value.copy(isFrontCamera = isFront)
    }

    fun onStartBurstCapture(): Boolean {
        if (!captureState.compareAndSet(CaptureState.IDLE, CaptureState.CAPTURING)) {
            return false
        }

        val duration = _uiState.value.selectedDuration
        if (duration.isProOnly && !_uiState.value.settings.isProUnlocked) {
            captureState.set(CaptureState.IDLE)
            _uiState.value = _uiState.value.copy(
                hintMessage = "SpectreFlow Pro required for ${duration.seconds}s exposure"
            )
            return false
        }

        triggerHaptic()
        cleanupCapture()
        captureState.set(CaptureState.CAPTURING)

        // Resolve active mode (Auto picks detected scene mode)
        effectiveModeAtCapture = if (_uiState.value.selectedMode == LongExposureMode.AUTO) {
            _uiState.value.detectedMode
        } else {
            _uiState.value.selectedMode
        }

        if (_uiState.value.settings.saveCompanionVideo) {
            tempVideoFile = File(getApplication<Application>().cacheDir, "spectreflow_burst_${System.currentTimeMillis()}.mp4")
            videoRecorder = CompanionVideoRecorder(getApplication())
        } else {
            videoRecorder = null
            tempVideoFile = null
        }

        startFrameConsumer()

        _uiState.value = _uiState.value.copy(
            isCapturing = true,
            captureProgress = 0f,
            elapsedSeconds = 0f,
            hintMessage = null,
            statusMessage = "Capturing burst frames…"
        )
        return true
    }

    private fun startFrameConsumer() {
        val channel = Channel<Bitmap>(capacity = Channel.CONFLATED)
        frameChannel = channel

        frameConsumerJob = viewModelScope.launch(Dispatchers.Default) {
            for (bitmap in channel) {
                if (captureState.get() != CaptureState.CAPTURING) {
                    bitmap.recycle()
                    continue
                }

                try {
                    val width = bitmap.width
                    val height = bitmap.height

                    if (isFirstFrame) {
                        blenderEngine.start(effectiveModeAtCapture, width, height)
                        frameAligner = FrameAligner(width, height)

                        tempVideoFile?.let { file ->
                            videoRecorder?.start(file, width, height)
                        }
                        isFirstFrame = false
                    }

                    val isTripod = stabilityState.value == StabilityState.STABLE
                    val alignment = frameAligner?.alignFrame(bitmap, isTripod)
                    val alignedBitmap = alignment?.alignedBitmap ?: bitmap

                    blenderEngine.addFrame(alignedBitmap)
                    videoRecorder?.addFrame(alignedBitmap)

                    // Keyframe preview cache (up to 16 frames max, downsampled to 360px wide to save RAM)
                    if (burstKeyframes.size < 16) {
                        val thumbW = 360
                        val thumbH = (360f * height / width).toInt().coerceAtLeast(1)
                        val thumb = Bitmap.createScaledBitmap(alignedBitmap, thumbW, thumbH, false)
                        burstKeyframes.add(thumb)
                    }

                    if (alignedBitmap != bitmap) {
                        alignedBitmap.recycle()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    bitmap.recycle()
                }
            }
        }
    }

    fun onFrameCaptured(bitmap: Bitmap) {
        if (captureState.get() != CaptureState.CAPTURING) {
            bitmap.recycle()
            return
        }

        val channel = frameChannel
        if (channel == null || channel.isClosedForSend) {
            bitmap.recycle()
            return
        }

        val result = channel.trySend(bitmap)
        if (!result.isSuccess) {
            bitmap.recycle()
        }
    }

    fun onBurstProgressUpdate(progress: Float, elapsedSeconds: Float) {
        _uiState.value = _uiState.value.copy(
            captureProgress = progress,
            elapsedSeconds = elapsedSeconds
        )
    }

    fun onShutterReleased(elapsedMs: Long) {
        if (!captureState.compareAndSet(CaptureState.CAPTURING, CaptureState.PROCESSING)) {
            return
        }

        frameChannel?.close()

        if (elapsedMs < 1000L) {
            // Released earlier than minimum 1 second hold time -> discard
            cleanupCapture()
            captureState.set(CaptureState.IDLE)

            _uiState.value = _uiState.value.copy(
                isCapturing = false,
                captureProgress = 0f,
                elapsedSeconds = 0f,
                currentScreen = AppScreen.CAMERA,
                hintMessage = "Hold longer to capture (min 1s)"
            )
        } else {
            finalizeCapture()
        }
    }

    fun onBurstCompleted() {
        if (!captureState.compareAndSet(CaptureState.CAPTURING, CaptureState.PROCESSING)) {
            return
        }
        frameChannel?.close()
        finalizeCapture()
    }

    private fun finalizeCapture() {
        triggerHaptic()
        _uiState.value = _uiState.value.copy(
            isCapturing = false,
            currentScreen = AppScreen.PROCESSING,
            statusMessage = "Aligning & blending long exposure…",
            processingProgress = 0.5f,
            hintMessage = null
        )

        viewModelScope.launch(Dispatchers.Default) {
            try {
                // Wait for any in-flight frame to complete
                frameConsumerJob?.join()

                val cropRect = frameAligner?.cropBounds
                val finalBitmap = blenderEngine.finalizeResult(cropRect)

                val finalizedVideoFile = videoRecorder?.finish()
                videoRecorder = null

                var videoUri: Uri? = null
                if (finalizedVideoFile != null && finalizedVideoFile.exists()) {
                    val saveResult = mediaSaver.saveVideoToGallery(finalizedVideoFile)
                    videoUri = saveResult.getOrNull()
                }

                _uiState.value = _uiState.value.copy(
                    currentScreen = AppScreen.RESULT,
                    resultBitmap = finalBitmap,
                    keyframes = burstKeyframes.toList(),
                    companionVideoUri = videoUri,
                    companionVideoFile = finalizedVideoFile,
                    processingProgress = 1f
                )
                captureState.set(CaptureState.RESULT)
            } catch (e: Exception) {
                e.printStackTrace()
                cleanupCapture()
                captureState.set(CaptureState.ERROR)
                _uiState.value = _uiState.value.copy(
                    currentScreen = AppScreen.CAMERA,
                    isCapturing = false,
                    hintMessage = "Processing failed: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    private fun cleanupCapture() {
        frameChannel?.close()
        frameConsumerJob?.cancel()
        frameConsumerJob = null
        frameChannel = null
        blenderEngine.reset()
        frameAligner?.reset()
        frameAligner = null
        videoRecorder?.cancel()
        videoRecorder = null
        tempVideoFile?.delete()
        tempVideoFile = null
        burstKeyframes.clear()
        isFirstFrame = true
    }

    fun onCancelCapture() {
        if (captureState.get() == CaptureState.CAPTURING || captureState.get() == CaptureState.PROCESSING) {
            cleanupCapture()
            captureState.set(CaptureState.IDLE)
            _uiState.value = _uiState.value.copy(
                isCapturing = false,
                captureProgress = 0f,
                elapsedSeconds = 0f,
                currentScreen = AppScreen.CAMERA
            )
        }
    }

    fun onAppBackgrounded() {
        if (captureState.get() == CaptureState.CAPTURING) {
            onCancelCapture()
        }
    }

    fun clearHintMessage() {
        _uiState.value = _uiState.value.copy(hintMessage = null)
    }

    fun onSaveResult(bitmap: Bitmap, onSuccess: (Uri) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = mediaSaver.saveBitmapToGallery(bitmap)
            result.onSuccess { uri ->
                _uiState.value = _uiState.value.copy(savedUri = uri)
                onSuccess(uri)
            }.onFailure { error ->
                onError(error.localizedMessage ?: "Failed to save photo")
            }
        }
    }

    fun onExportLoopingWallpaper(onSuccess: (Uri) -> Unit, onError: (String) -> Unit) {
        val frames = burstKeyframes.toList()
        if (frames.isEmpty()) {
            onError("No frames available")
            return
        }
        triggerHaptic()
        val app = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val loopFile = File(app.cacheDir, "spectreflow_loop_${System.currentTimeMillis()}.mp4")
                val recorder = CompanionVideoRecorder(app)
                val generated = recorder.generateLoopingVideo(frames, loopFile)
                if (generated != null && generated.exists()) {
                    val saveResult = mediaSaver.saveVideoToGallery(generated, "SpectreFlow_Wallpaper")
                    saveResult.onSuccess { uri ->
                        withContext(Dispatchers.Main) {
                            onSuccess(uri)
                        }
                    }.onFailure { err ->
                        withContext(Dispatchers.Main) {
                            onError(err.localizedMessage ?: "Failed to save wallpaper")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onError("Failed to encode looping video")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.localizedMessage ?: "Export error")
                }
            }
        }
    }

    fun onDiscardResult() {
        cleanupCapture()
        captureState.set(CaptureState.IDLE)
        _uiState.value = _uiState.value.copy(
            resultBitmap = null,
            keyframes = emptyList(),
            companionVideoUri = null,
            companionVideoFile = null,
            savedUri = null,
            currentScreen = AppScreen.CAMERA
        )
    }

    fun onModeSelected(mode: LongExposureMode) {
        if (!_uiState.value.isCapturing) {
            triggerHaptic()
            _uiState.value = _uiState.value.copy(selectedMode = mode)
        }
    }

    fun onDurationSelected(duration: CaptureDuration) {
        if (!_uiState.value.isCapturing) {
            triggerHaptic()
            _uiState.value = _uiState.value.copy(selectedDuration = duration)
        }
    }

    fun onOpenSettings() {
        _uiState.value = _uiState.value.copy(currentScreen = AppScreen.SETTINGS)
    }

    fun onCloseSettings() {
        _uiState.value = _uiState.value.copy(currentScreen = AppScreen.CAMERA)
    }

    fun onSettingsChanged(newSettings: AppSettingsState) {
        settingsManager.saveSettings(newSettings)
        billingService.setProUnlocked(newSettings.isProUnlocked)
        _uiState.value = _uiState.value.copy(settings = newSettings)
    }

    private fun triggerHaptic() {
        if (!_uiState.value.settings.hapticEnabled) return
        val context = getApplication<Application>()
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(30)
            }
        } catch (e: Exception) {
            // Ignore if vibration unsupported
        }
    }

    override fun onCleared() {
        super.onCleared()
        stabilityDetector.stopListening()
        blenderEngine.reset()
        videoRecorder?.cancel()
    }
}
