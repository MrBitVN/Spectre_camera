package com.spectreflow.app

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.spectreflow.app.camera.CameraXManager
import com.spectreflow.app.settings.AppLanguage
import com.spectreflow.app.ui.screens.CameraScreen
import com.spectreflow.app.ui.screens.OnboardingScreen
import com.spectreflow.app.ui.screens.PermissionRationaleDialog
import com.spectreflow.app.ui.screens.ProcessingScreen
import com.spectreflow.app.ui.screens.ResultScreen
import com.spectreflow.app.ui.screens.SettingsScreen
import com.spectreflow.app.ui.theme.BgObsidian
import com.spectreflow.app.ui.theme.SpectreFlowTheme
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var cameraManager: CameraXManager? = null

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                viewModel.onPermissionGranted(isLimited = false)
            } else {
                viewModel.onPermissionDenied()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable Wide Color Gamut (Display P3) where supported
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.colorMode = ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT
        }

        cameraManager = CameraXManager(this)

        checkCameraPermission()

        setContent {
            SpectreFlowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgObsidian
                ) {
                    val uiState by viewModel.uiState.collectAsState()
                    val stability by viewModel.stabilityState.collectAsState()
                    var previewViewInstance by remember { mutableStateOf<PreviewView?>(null) }

                    // Apply localized language configuration if changed
                    LaunchedEffect(uiState.settings.language) {
                        applyLocale(uiState.settings.language)
                    }

                    // Observe lifecycle to cancel burst if app is paused/backgrounded
                    DisposableEffect(this) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                                viewModel.onAppBackgrounded()
                                cameraManager?.cancelBurst()
                            }
                        }
                        lifecycle.addObserver(observer)
                        onDispose {
                            lifecycle.removeObserver(observer)
                        }
                    }

                    when (uiState.currentScreen) {
                        AppScreen.ONBOARDING -> {
                            OnboardingScreen(
                                onDismiss = { viewModel.onDismissOnboarding() }
                            )
                        }

                        AppScreen.CAMERA -> {
                            CameraScreen(
                                stability = stability,
                                isCapturing = uiState.isCapturing,
                                captureProgress = uiState.captureProgress,
                                elapsedSeconds = uiState.elapsedSeconds,
                                hintMessage = uiState.hintMessage,
                                selectedMode = uiState.selectedMode,
                                detectedMode = uiState.detectedMode,
                                selectedDuration = uiState.selectedDuration,
                                supportedLenses = uiState.supportedLenses,
                                currentZoom = uiState.currentZoom,
                                isFrontCamera = uiState.isFrontCamera,
                                isLimitedAccess = uiState.isLimitedAccess,
                                showGrid = uiState.settings.showGrid,
                                isProUnlocked = uiState.settings.isProUnlocked,
                                onPreviewViewCreated = { previewView ->
                                    previewViewInstance = previewView
                                    setupCamera(previewView)
                                },
                                onLensSelected = { ratio ->
                                    cameraManager?.setZoomRatio(ratio)
                                    viewModel.onZoomChanged(ratio)
                                },
                                onFlipCamera = {
                                    previewViewInstance?.let { pv ->
                                        cameraManager?.flipCamera(this, pv.surfaceProvider)
                                        viewModel.onFrontCameraChanged(cameraManager?.isFrontCamera ?: false)
                                    }
                                },
                                onShutterPressStart = {
                                    val started = viewModel.onStartBurstCapture()
                                    if (started) {
                                        cameraManager?.startBurst(uiState.selectedDuration.seconds)
                                    }
                                },
                                onShutterPressRelease = {
                                    val elapsedMs = cameraManager?.stopBurst() ?: 0L
                                    viewModel.onShutterReleased(elapsedMs)
                                },
                                onShutterPressCancel = {
                                    cameraManager?.cancelBurst()
                                    viewModel.onCancelCapture()
                                },
                                onModeSelected = { viewModel.onModeSelected(it) },
                                onDurationSelected = { viewModel.onDurationSelected(it) },
                                onOpenSettings = { viewModel.onOpenSettings() }
                            )
                        }

                        AppScreen.PROCESSING -> {
                            ProcessingScreen(
                                statusMessage = uiState.statusMessage,
                                progress = uiState.processingProgress,
                                onCancel = {
                                    viewModel.onCancelCapture()
                                    cameraManager?.cancelBurst()
                                }
                            )
                        }

                        AppScreen.RESULT -> {
                            uiState.resultBitmap?.let { bitmap ->
                                ResultScreen(
                                    resultBitmap = bitmap,
                                    keyframes = uiState.keyframes,
                                    hasCompanionVideo = uiState.companionVideoUri != null,
                                    onSavePhoto = { pickedBitmap ->
                                        viewModel.onSaveResult(
                                            bitmap = pickedBitmap,
                                            onSuccess = {
                                                Toast.makeText(this, getString(R.string.save_success), Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { msg ->
                                                Toast.makeText(this, "${getString(R.string.save_failed)}: $msg", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    },
                                    onSharePhoto = { pickedBitmap -> shareBitmap(pickedBitmap) },
                                    onShareVideo = {
                                        uiState.companionVideoUri?.let { videoUri ->
                                            shareVideo(videoUri)
                                        }
                                    },
                                    onExportWallpaper = {
                                        viewModel.onExportLoopingWallpaper(
                                            onSuccess = {
                                                Toast.makeText(this, getString(R.string.wallpaper_saved), Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { msg ->
                                                Toast.makeText(this, "${getString(R.string.wallpaper_failed)}: $msg", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    },
                                    onDiscard = { viewModel.onDiscardResult() }
                                )
                            }
                        }

                        AppScreen.SETTINGS -> {
                            SettingsScreen(
                                settings = uiState.settings,
                                onSettingsChanged = { viewModel.onSettingsChanged(it) },
                                onOpenOnboarding = { viewModel.onOpenOnboarding() },
                                onBack = { viewModel.onCloseSettings() }
                            )
                        }
                    }

                    if (uiState.showPermissionDialog) {
                        PermissionRationaleDialog(
                            onGrantClick = {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.onPermissionGranted(isLimited = false)
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun setupCamera(previewView: PreviewView) {
        val manager = cameraManager ?: return
        manager.initialize(this, previewView.surfaceProvider) {
            manager.onFrameAvailable = { frame ->
                viewModel.onFrameCaptured(frame)
            }
            manager.onPreviewFrameAvailable = { previewFrame ->
                viewModel.onPreviewFrameCaptured(previewFrame)
            }
            manager.onSupportedLensesChanged = { lenses ->
                viewModel.onSupportedLensesChanged(lenses)
            }
            manager.onBurstProgress = { progress, elapsedSec ->
                viewModel.onBurstProgressUpdate(progress, elapsedSec)
            }
            manager.onBurstComplete = {
                viewModel.onBurstCompleted()
            }
        }
    }

    private fun shareBitmap(bitmap: Bitmap) {
        try {
            val cachePath = File(cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "spectreflow_share_${System.currentTimeMillis()}.jpg")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            stream.close()

            val contentUri: Uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share Long Exposure Photo"))
        } catch (e: Exception) {
            Toast.makeText(this, "Could not prepare share file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareVideo(videoUri: Uri) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, videoUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share Companion Video"))
        } catch (e: Exception) {
            Toast.makeText(this, "Could not share video", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyLocale(language: AppLanguage) {
        val locale = Locale(language.code)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager?.shutdown()
    }
}
