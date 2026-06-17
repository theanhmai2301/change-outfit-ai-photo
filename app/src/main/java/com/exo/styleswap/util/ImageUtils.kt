package com.exo.styleswap.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

/** Helpers for preparing picked images for upload and saving results to the gallery. */
object ImageUtils {

    private const val MAX_DIM = 1280

    /**
     * Decodes [uri], fixes EXIF rotation, downsizes to <= [MAX_DIM] on the longest edge and
     * writes a JPEG into the cache dir. Returns the saved file (or null on failure).
     */
    fun prepareForUpload(context: Context, uri: Uri, outName: String): File? {
        return try {
            val resolver = context.contentResolver

            // 1. Bounds
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            // 2. Sample size
            val opts = BitmapFactory.Options().apply {
                inSampleSize = calcInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIM)
            }
            var bmp = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return null

            // 3. EXIF rotation
            val orientation = resolver.openInputStream(uri)?.use { stream ->
                try {
                    ExifInterface(stream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                    )
                } catch (e: Exception) {
                    ExifInterface.ORIENTATION_NORMAL
                }
            } ?: ExifInterface.ORIENTATION_NORMAL
            bmp = applyOrientation(bmp, orientation)

            // 4. Final clamp
            bmp = clampToMax(bmp, MAX_DIM)

            // 5. Write JPEG
            val out = File(context.cacheDir, outName)
            FileOutputStream(out).use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            bmp.recycle()
            out
        } catch (e: Exception) {
            null
        }
    }

    fun saveBitmapToFile(context: Context, bmp: Bitmap, outName: String): File {
        val out = File(context.cacheDir, outName)
        FileOutputStream(out).use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        return out
    }

    /** Persists raw image [bytes] into the device gallery under "Outfitly". */
    fun saveImageToGallery(context: Context, bytes: ByteArray, displayName: String, mime: String): Boolean {
        return try {
            val resolver = context.contentResolver
            val safeMime = if (mime.isBlank()) "image/png" else mime
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, safeMime)
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Outfitly")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return false
                resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return false
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                true
            } else {
                @Suppress("DEPRECATION")
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "Outfitly"
                )
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, displayName)
                FileOutputStream(file).use { it.write(bytes) }
                android.media.MediaScannerConnection.scanFile(
                    context, arrayOf(file.absolutePath), arrayOf(safeMime), null
                )
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun calcInSampleSize(w: Int, h: Int, maxDim: Int): Int {
        var sample = 1
        var longest = maxOf(w, h)
        while (longest / sample > maxDim * 2) sample *= 2
        return sample
    }

    private fun clampToMax(bmp: Bitmap, maxDim: Int): Bitmap {
        val longest = maxOf(bmp.width, bmp.height)
        if (longest <= maxDim) return bmp
        val scale = maxDim.toFloat() / longest
        val scaled = Bitmap.createScaledBitmap(
            bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true
        )
        if (scaled != bmp) bmp.recycle()
        return scaled
    }

    private fun applyOrientation(bmp: Bitmap, orientation: Int): Bitmap {
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
            else -> return bmp
        }
        return try {
            val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            if (rotated != bmp) bmp.recycle()
            rotated
        } catch (e: Exception) {
            bmp
        }
    }
}
