package com.lumiroom.feature.ar.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.Window
import android.view.PixelCopy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import android.os.Handler
import android.os.Looper

object ArCaptureUtils {
    
    suspend fun captureAndSaveScreenshot(context: Context, window: Window): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bitmap = Bitmap.createBitmap(window.decorView.width, window.decorView.height, Bitmap.Config.ARGB_8888)
            val latch = CountDownLatch(1)
            var copySuccess = false

            PixelCopy.request(window, bitmap, { copyResult ->
                if (copyResult == PixelCopy.SUCCESS) {
                    copySuccess = true
                }
                latch.countDown()
            }, Handler(Looper.getMainLooper()))
            
            latch.await()
            
            if (!copySuccess) {
                return@withContext Result.failure(Exception("Failed to copy pixels from SurfaceView"))
            }

            val filename = "Lumiroom_AR_${System.currentTimeMillis()}.png"
            var uriString = ""

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Lumiroom")
                }
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext Result.failure(Exception("Failed to create MediaStore entry"))
                
                resolver.openOutputStream(imageUri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                uriString = imageUri.toString()
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val lumiroomDir = File(imagesDir, "Lumiroom")
                if (!lumiroomDir.exists()) {
                    lumiroomDir.mkdirs()
                }
                val imageFile = File(lumiroomDir, filename)
                FileOutputStream(imageFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                uriString = imageFile.absolutePath
            }

            Result.success(uriString)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
