package com.spectreflow.app.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
import android.view.Surface
import java.io.File

/**
 * Encodes burst frames into an MP4 companion video using Android's hardware MediaCodec & MediaMuxer.
 * Uses Surface-input so the hardware encoder handles colorspace conversion efficiently.
 */
class CompanionVideoRecorder(private val context: Context) {
    private var encoder: MediaCodec? = null
    private var muxer: MediaMuxer? = null
    private var inputSurface: Surface? = null
    private var trackIndex = -1
    private var isMuxerStarted = false
    private var isFailed = false
    private var targetWidth = 1080
    private var targetHeight = 1920
    private val frameRate = 15
    private var outputFile: File? = null

    fun start(outputFile: File, width: Int, height: Int) {
        this.outputFile = outputFile
        this.isFailed = false
        // Ensure dimensions are even numbers required by H.264
        this.targetWidth = if (width % 2 == 0) width else width - 1
        this.targetHeight = if (height % 2 == 0) height else height - 1

        try {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, targetWidth, targetHeight).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, 6_000_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            val enc = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
                configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                inputSurface = createInputSurface()
                start()
            }
            encoder = enc

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            trackIndex = -1
            isMuxerStarted = false
        } catch (t: Throwable) {
            Log.e("CompanionVideoRecorder", "Failed to initialize video encoder safely: ${t.localizedMessage}", t)
            cleanup()
            isFailed = true
        }
    }

    fun addFrame(bitmap: Bitmap) {
        if (isFailed) return
        val surface = inputSurface ?: return
        val enc = encoder ?: return

        var canvas: Canvas? = null
        try {
            canvas = try {
                @Suppress("DEPRECATION")
                surface.lockCanvas(null)
            } catch (t: Throwable) {
                Log.w("CompanionVideoRecorder", "surface.lockCanvas failed: ${t.localizedMessage}")
                null
            }

            if (canvas != null) {
                val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
                val dstRect = Rect(0, 0, targetWidth, targetHeight)
                val paint = Paint(Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
                surface.unlockCanvasAndPost(canvas)
                drainEncoder(endOfStream = false)
            }
        } catch (t: Throwable) {
            Log.e("CompanionVideoRecorder", "addFrame failed safely: ${t.localizedMessage}", t)
            // If surface unlock fails, avoid cascading failures
            try {
                if (canvas != null) {
                    surface.unlockCanvasAndPost(canvas)
                }
            } catch (_: Throwable) {}
        }
    }

    private fun drainEncoder(endOfStream: Boolean) {
        if (isFailed) return
        val enc = encoder ?: return
        val mux = muxer ?: return
        val bufferInfo = MediaCodec.BufferInfo()

        try {
            if (endOfStream) {
                try {
                    enc.signalEndOfInputStream()
                } catch (t: Throwable) {
                    Log.w("CompanionVideoRecorder", "signalEndOfInputStream failed: ${t.localizedMessage}")
                }
            }

            var tries = 0
            while (tries < 10) {
                val outputBufferIndex = enc.dequeueOutputBuffer(bufferInfo, 10_000)
                if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (!endOfStream) break
                    tries++
                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (!isMuxerStarted) {
                        val newFormat = enc.outputFormat
                        trackIndex = mux.addTrack(newFormat)
                        mux.start()
                        isMuxerStarted = true
                    }
                } else if (outputBufferIndex >= 0) {
                    val encodedData = enc.getOutputBuffer(outputBufferIndex)
                    if (encodedData != null) {
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            bufferInfo.size = 0
                        }

                        if (bufferInfo.size != 0 && isMuxerStarted && trackIndex >= 0) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            mux.writeSampleData(trackIndex, encodedData, bufferInfo)
                        }

                        enc.releaseOutputBuffer(outputBufferIndex, false)
                    }

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e("CompanionVideoRecorder", "drainEncoder failed safely: ${t.localizedMessage}", t)
        }
    }

    fun finish(): File? {
        try {
            drainEncoder(endOfStream = true)
        } catch (t: Throwable) {
            Log.w("CompanionVideoRecorder", "drainEncoder on finish failed: ${t.localizedMessage}")
        }
        cleanup()
        return if (isMuxerStarted && outputFile?.exists() == true && (outputFile?.length() ?: 0L) > 0L) {
            outputFile
        } else {
            outputFile?.delete()
            null
        }
    }

    fun cancel() {
        cleanup()
        try {
            outputFile?.delete()
            outputFile = null
        } catch (_: Throwable) {}
    }

    private fun cleanup() {
        try {
            encoder?.stop()
        } catch (_: Throwable) {}
        try {
            encoder?.release()
        } catch (_: Throwable) {}
        encoder = null

        try {
            inputSurface?.release()
        } catch (_: Throwable) {}
        inputSurface = null

        try {
            if (isMuxerStarted) {
                muxer?.stop()
            }
        } catch (_: Throwable) {}
        try {
            muxer?.release()
        } catch (_: Throwable) {}
        muxer = null
        isMuxerStarted = false
    }

    /**
     * Synthesizes a seamless ping-pong (boomerang) looping MP4 from burst keyframes
     * suitable for Live Wallpapers and looping video sharing.
     */
    fun generateLoopingVideo(keyframes: List<Bitmap>, outputFile: File): File? {
        if (keyframes.isEmpty()) return null

        val first = keyframes.first()
        val width = (first.width / 2) * 2
        val height = (first.height / 2) * 2

        start(outputFile, width, height)

        // Ping-pong sequence: forward 0..N-1, backward N-2..1
        val sequence = ArrayList<Bitmap>(keyframes.size * 2)
        sequence.addAll(keyframes)
        if (keyframes.size > 2) {
            for (i in (keyframes.size - 2) downTo 1) {
                sequence.add(keyframes[i])
            }
        }

        // Repeat loop twice for smooth sustained loop
        val fullLoop = sequence + sequence
        for (frame in fullLoop) {
            addFrame(frame)
        }

        return finish()
    }
}
