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

import android.graphics.Canvas
import android.view.SurfaceView
import android.view.ViewGroup
import android.view.View

object ArCaptureUtils {
    
    private fun findSurfaceView(view: View): SurfaceView? {
        if (view is SurfaceView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findSurfaceView(view.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }

    suspend fun captureAndSaveScreenshot(context: Context, window: Window): Result<String> = withContext(Dispatchers.IO) {
        try {
            val width = window.decorView.width
            val height = window.decorView.height
            
            // 1. Capture the AR SurfaceView
            val surfaceView = findSurfaceView(window.decorView)
            val arBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            
            if (surfaceView != null) {
                val surfaceLatch = CountDownLatch(1)
                PixelCopy.request(surfaceView, arBitmap, {
                    surfaceLatch.countDown()
                }, Handler(Looper.getMainLooper()))
                surfaceLatch.await()
            }

            // 2. Capture the UI Window Overlay
            val uiBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val windowLatch = CountDownLatch(1)
            PixelCopy.request(window, uiBitmap, {
                windowLatch.countDown()
            }, Handler(Looper.getMainLooper()))
            windowLatch.await()

            // 3. Composite them together
            val finalBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(finalBitmap)
            canvas.drawBitmap(arBitmap, 0f, 0f, null)
            canvas.drawBitmap(uiBitmap, 0f, 0f, null)

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
                    finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
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
                    finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                uriString = imageFile.absolutePath
            }

            Result.success(uriString)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
