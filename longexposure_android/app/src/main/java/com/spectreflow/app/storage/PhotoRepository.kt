package com.spectreflow.app.storage

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Model representing a synthesized long exposure photograph stored in MediaStore.
 */
data class AppPhoto(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateAddedSeconds: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long
)

/**
 * Single source of truth repository for all photos captured and saved by SpectreFlow.
 * Queries MediaStore scoped by the app's dedicated album directory ("Pictures/SpectreFlow")
 * and filename prefix ("SpectreFlow_"), ordered by DATE_ADDED descending.
 *
 * Keeps a ContentObserver on MediaStore to automatically refresh whenever photos are added or deleted.
 */
class PhotoRepository(private val context: Context) {

    private val _photos = MutableStateFlow<List<AppPhoto>>(emptyList())
    val photos: StateFlow<List<AppPhoto>> = _photos.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var contentObserver: ContentObserver? = null

    init {
        startObserving()
        refresh()
    }

    fun startObserving() {
        if (contentObserver != null) return
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                Log.d("PhotoRepository", "MediaStore change detected, refreshing photos: $uri")
                refresh()
            }
        }
        contentObserver = observer
        try {
            context.contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
        } catch (e: Exception) {
            Log.e("PhotoRepository", "Failed to register ContentObserver", e)
        }
    }

    fun stopObserving() {
        contentObserver?.let {
            try {
                context.contentResolver.unregisterContentObserver(it)
            } catch (e: Exception) {
                Log.e("PhotoRepository", "Failed to unregister ContentObserver", e)
            }
            contentObserver = null
        }
    }

    fun refresh() {
        scope.launch {
            val list = queryAppPhotos()
            _photos.value = list
            Log.d("PhotoRepository", "Loaded ${list.size} photos for SpectreFlow")
        }
    }

    suspend fun queryAppPhotos(): List<AppPhoto> = withContext(Dispatchers.IO) {
        val result = mutableListOf<AppPhoto>()
        val projection = mutableListOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection.add(MediaStore.Images.Media.RELATIVE_PATH)
            projection.add(MediaStore.Images.Media.IS_PENDING)
        }

        val selection: String
        val selectionArgs: Array<String>

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selection = "(${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? OR ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?) AND ${MediaStore.Images.Media.IS_PENDING} = 0"
            selectionArgs = arrayOf("Pictures/SpectreFlow%", "SpectreFlow_%")
        } else {
            selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
            selectionArgs = arrayOf("SpectreFlow_%")
        }

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC, ${MediaStore.Images.Media._ID} DESC"

        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection.toTypedArray(),
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "SpectreFlow_photo.jpg"
                    val dateAdded = cursor.getLong(dateCol)
                    val width = cursor.getInt(widthCol)
                    val height = cursor.getInt(heightCol)
                    val sizeBytes = cursor.getLong(sizeCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                    result.add(
                        AppPhoto(
                            id = id,
                            uri = contentUri,
                            displayName = name,
                            dateAddedSeconds = dateAdded,
                            width = width,
                            height = height,
                            sizeBytes = sizeBytes
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("PhotoRepository", "Failed to query MediaStore photos", e)
        }

        result
    }

    suspend fun loadThumbnail(uri: Uri, targetSize: Int = 200): Bitmap? = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(uri, Size(targetSize, targetSize), null)
            } else {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 2
                    }
                    BitmapFactory.decodeStream(stream, null, options)
                }
            }
        } catch (e: Exception) {
            Log.w("PhotoRepository", "Could not load thumbnail for $uri", e)
            null
        }
    }

    suspend fun loadFullBitmap(uri: Uri, maxDimension: Int = 2048): Bitmap? = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val maxEdge = maxOf(info.size.width, info.size.height)
                    if (maxEdge > maxDimension) {
                        val scale = maxDimension.toFloat() / maxEdge
                        decoder.setTargetSize(
                            (info.size.width * scale).toInt().coerceAtLeast(1),
                            (info.size.height * scale).toInt().coerceAtLeast(1)
                        )
                    }
                }
            } else {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
        } catch (e: Exception) {
            Log.e("PhotoRepository", "Failed to decode full bitmap for $uri", e)
            null
        }
    }

    suspend fun deletePhoto(photo: AppPhoto): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val rows = context.contentResolver.delete(photo.uri, null, null)
            if (rows > 0) {
                refresh()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Photo could not be deleted from MediaStore"))
            }
        } catch (e: Exception) {
            Log.e("PhotoRepository", "Error deleting photo ${photo.uri}", e)
            Result.failure(e)
        }
    }
}
