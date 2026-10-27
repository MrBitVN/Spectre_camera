package com.spectreflow.app

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.spectreflow.app.ui.theme.AccentEmerald
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

        // Global crash logging and error dialog presentation
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("SpectreFlow_Crash", "CRASH in thread ${thread.name}: ${throwable.localizedMessage}", throwable)
            val stackTrace = throwable.stackTraceToString()
            runOnUiThread {
                viewModel.showCrashDialog(
                    title = getString(R.string.error_dialog_title),
                    message = "${throwable.javaClass.simpleName}: ${throwable.localizedMessage ?: "System exception"}",
                    stackTrace = stackTrace
                )
            }
        }

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
                    val crashInfo by viewModel.crashDialog.collectAsState()
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
                                    try {
                                        val started = viewModel.onStartBurstCapture()
                                        if (started) {
                                            cameraManager?.startBurst(uiState.selectedDuration.seconds)
                                        }
                                    } catch (t: Throwable) {
                                        Log.e("MainActivity", "onShutterPressStart error", t)
                                        viewModel.showCrashDialog(
                                            title = getString(R.string.error_start_title),
                                            message = "${t.javaClass.simpleName}: ${t.localizedMessage ?: "Failed to start camera capture"}",
                                            stackTrace = t.stackTraceToString()
                                        )
                                    }
                                },
                                onShutterPressRelease = {
                                    try {
                                        val elapsedMs = cameraManager?.stopBurst() ?: 0L
                                        viewModel.onShutterReleased(elapsedMs)
                                    } catch (t: Throwable) {
                                        Log.e("MainActivity", "onShutterPressRelease error", t)
                                        viewModel.showCrashDialog(
                                            title = getString(R.string.error_stop_title),
                                            message = "${t.javaClass.simpleName}: ${t.localizedMessage ?: "Failed to stop capture session"}",
                                            stackTrace = t.stackTraceToString()
                                        )
                                    }
                                },
                                onShutterPressCancel = {
                                    try {
                                        cameraManager?.cancelBurst()
                                        viewModel.onCancelCapture()
                                    } catch (t: Throwable) {
                                        Log.e("MainActivity", "onShutterPressCancel error", t)
                                    }
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

                    // Visible Error Dialog in Vietnamese with stack trace and copy button
                    crashInfo?.let { info ->
                        AlertDialog(
                            onDismissRequest = { viewModel.dismissCrashDialog() },
                            title = {
                                Text(
                                    text = info.title,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF5252),
                                    fontSize = 18.sp
                                )
                            },
                            text = {
                                Column(
                                    modifier = Modifier
                                        .verticalScroll(rememberScrollState())
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = info.message,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = stringResource(R.string.error_stack_trace_label),
                                        color = Color(0xFF90A4AE),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    SelectionContainer {
                                        Text(
                                            text = info.stackTrace.take(1200),
                                            color = Color(0xFFCFD8DC),
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText(
                                            "SpectreFlow Error Log",
                                            "${info.title}\n${info.message}\n\n${info.stackTrace}"
                                        )
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(this@MainActivity, getString(R.string.toast_error_copied), Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald)
                                ) {
                                    Text(stringResource(R.string.action_copy_error), color = BgObsidian, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                OutlinedButton(
                                    onClick = { viewModel.dismissCrashDialog() }
                                ) {
                                    Text(stringResource(R.string.action_close), color = Color.White)
                                }
                            },
                            containerColor = Color(0xFF1E2228),
                            shape = RoundedCornerShape(16.dp)
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
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_photo_title)))
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.share_prep_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareVideo(videoUri: Uri) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, videoUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_video_title)))
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.share_video_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyLocale(language: AppLanguage) {
        val locale = Locale("en", "US")
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
