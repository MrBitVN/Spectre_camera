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
import android.view.Surface
import java.io.File

/**
 * Encodes burst frames into an MP4 companion video using Android's hardware MediaCodec & MediaMuxer.
 * Uses Surface-input so the hardware encoder handles colorspace conversion efficiently on GPU.
 */
class CompanionVideoRecorder(private val context: Context) {
    private var encoder: MediaCodec? = null
    private var muxer: MediaMuxer? = null
    private var inputSurface: Surface? = null
    private var trackIndex = -1
    private var isMuxerStarted = false
    private var targetWidth = 1080
    private var targetHeight = 1920
    private val frameRate = 15
    private var outputFile: File? = null

    fun start(outputFile: File, width: Int, height: Int) {
        this.outputFile = outputFile
        // Ensure dimensions are even numbers required by H.264
        this.targetWidth = if (width % 2 == 0) width else width - 1
        this.targetHeight = if (height % 2 == 0) height else height - 1

        try {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, targetWidth, targetHeight).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, 8_000_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
                configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                inputSurface = createInputSurface()
                start()
            }

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            trackIndex = -1
            isMuxerStarted = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addFrame(bitmap: Bitmap) {
        val surface = inputSurface ?: return
        val enc = encoder ?: return

        try {
            val canvas: Canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                surface.lockHardwareCanvas()
            } else {
                @Suppress("DEPRECATION")
                surface.lockCanvas(null)
            }

            val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
            val dstRect = Rect(0, 0, targetWidth, targetHeight)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
            surface.unlockCanvasAndPost(canvas)

            drainEncoder(endOfStream = false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun drainEncoder(endOfStream: Boolean) {
        val enc = encoder ?: return
        val mux = muxer ?: return
        val bufferInfo = MediaCodec.BufferInfo()

        if (endOfStream) {
            try {
                enc.signalEndOfInputStream()
            } catch (e: Exception) {
                e.printStackTrace()
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

                    if (bufferInfo.size != 0 && isMuxerStarted) {
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
    }

    fun finish(): File? {
        try {
            drainEncoder(endOfStream = true)
            encoder?.stop()
            encoder?.release()
            encoder = null
            inputSurface?.release()
            inputSurface = null

            if (isMuxerStarted) {
                muxer?.stop()
                muxer?.release()
                muxer = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return outputFile
    }

    fun cancel() {
        try {
            encoder?.stop()
            encoder?.release()
            encoder = null
            inputSurface?.release()
            inputSurface = null
            muxer?.release()
            muxer = null
            outputFile?.delete()
            outputFile = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
