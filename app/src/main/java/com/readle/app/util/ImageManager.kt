package com.readle.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.UUID

object ImageManager {

    suspend fun downloadAndSaveImage(context: Context, imageUrl: String): String? {
        return withContext(Dispatchers.IO) {
            try {

                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()

                val result = context.imageLoader.execute(request)
                val drawable = result.drawable

                if (drawable == null) {
                    android.util.Log.e("ImageManager", "Drawable is null for URL: $imageUrl")
                    return@withContext null
                }

                val bitmap = when (drawable) {
                    is android.graphics.drawable.BitmapDrawable -> drawable.bitmap
                    else -> {
                        val bmp = Bitmap.createBitmap(
                            drawable.intrinsicWidth.takeIf { it > 0 } ?: 100,
                            drawable.intrinsicHeight.takeIf { it > 0 } ?: 100,
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = android.graphics.Canvas(bmp)
                        drawable.setBounds(0, 0, canvas.width, canvas.height)
                        drawable.draw(canvas)
                        bmp
                    }
                }

                val path = saveImageToInternalStorage(context, bitmap)
                path
            } catch (e: Exception) {
                android.util.Log.e("ImageManager", "Error downloading image", e)
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun saveImageToInternalStorage(context: Context, bitmap: Bitmap): String? {
        return withContext(Dispatchers.IO) {
            try {
                val filename = "cover_${UUID.randomUUID()}.jpg"
                val directory = File(context.filesDir, "covers")
                if (!directory.exists()) {
                    directory.mkdirs()
                }

                val file = File(directory, filename)
                FileOutputStream(file).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                }

                file.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun loadImageFromPath(path: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                BitmapFactory.decodeFile(path)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun deleteImage(path: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                } else {
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    fun getImagesDirectory(context: Context): File {
        val directory = File(context.filesDir, "covers")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }
}

