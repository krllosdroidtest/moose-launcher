package com.example.data.system

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object WallpaperStorage {

    private const val FILE_NAME = "custom_wallpaper.jpg"
    private const val MAX_PREVIEW_DIM = 1920

    @Volatile
    private var cachedPath: String? = null

    @Volatile
    private var cachedImageBitmap: ImageBitmap? = null

    suspend fun decodePreviewBitmap(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

            var sampleSize = 1
            val maxDim = max(bounds.outWidth, bounds.outHeight)
            while (maxDim / sampleSize > MAX_PREVIEW_DIM) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val rawBitmap = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return@runCatching null

            val rotationDegrees = resolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f

            if (rotationDegrees == 0f) {
                rawBitmap
            } else {
                val matrix = Matrix().apply { postRotate(rotationDegrees) }
                val rotated = Bitmap.createBitmap(
                    rawBitmap,
                    0,
                    0,
                    rawBitmap.width,
                    rawBitmap.height,
                    matrix,
                    true
                )
                if (rotated != rawBitmap) rawBitmap.recycle()
                rotated
            }
        }.getOrNull()
    }

    suspend fun cropAndSaveWallpaper(
        context: Context,
        sourceBitmap: Bitmap,
        viewportWidth: Float,
        viewportHeight: Float,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        applyToSystem: Boolean
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val targetWidth = viewportWidth.roundToInt().coerceIn(360, 1440)
            val targetHeight = viewportHeight.roundToInt().coerceIn(640, 2560)
            val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)

            val viewW = targetWidth.toFloat()
            val viewH = targetHeight.toFloat()
            val imgW = sourceBitmap.width.toFloat()
            val imgH = sourceBitmap.height.toFloat()

            val baseScale = max(viewW / imgW, viewH / imgH)
            val totalScale = baseScale * scale.coerceIn(1f, 5f)

            val drawW = imgW * totalScale
            val drawH = imgH * totalScale

            val scaleRatioX = viewW / max(1f, viewportWidth)
            val scaleRatioY = viewH / max(1f, viewportHeight)

            val left = (viewW - drawW) / 2f + (offsetX * scaleRatioX)
            val top = (viewH - drawH) / 2f + (offsetY * scaleRatioY)

            val matrix = Matrix().apply {
                postScale(totalScale, totalScale)
                postTranslate(left, top)
            }
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(sourceBitmap, matrix, paint)

            val outFile = File(context.filesDir, FILE_NAME)
            FileOutputStream(outFile).use { fos ->
                output.compress(Bitmap.CompressFormat.JPEG, 92, fos)
                fos.flush()
            }

            cachedPath = outFile.absolutePath
            cachedImageBitmap = output.asImageBitmap()

            if (applyToSystem) {
                runCatching {
                    WallpaperManager.getInstance(context).setBitmap(output)
                }
            }

            outFile.absolutePath
        }.getOrNull()
    }

    suspend fun loadCachedWallpaper(path: String): ImageBitmap? = withContext(Dispatchers.IO) {
        if (path.isBlank()) return@withContext null
        if (cachedPath == path && cachedImageBitmap != null) {
            return@withContext cachedImageBitmap
        }
        val file = File(path)
        if (!file.exists()) return@withContext null
        val decoded = runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            val maxDim = max(bounds.outWidth, bounds.outHeight)
            var sample = 1
            while (maxDim / sample > MAX_PREVIEW_DIM) {
                sample *= 2
            }
            BitmapFactory.decodeFile(
                path,
                BitmapFactory.Options().apply {
                    inSampleSize = min(sample, 8)
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
            )?.asImageBitmap()
        }.getOrNull()
        if (decoded != null) {
            cachedPath = path
            cachedImageBitmap = decoded
        }
        decoded
    }
}
