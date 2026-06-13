package com.lumiroom.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Lumiroom Application class.
 *
 * - Entry point for the Hilt dependency injection graph.
 * - Provides custom WorkManager configuration with HiltWorkerFactory so that
 *   Workers can have their dependencies injected.
 */
@HiltAndroidApp
class LumiroomApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
            android.util.Log.d("LumiroomAppCheck", "Firebase initialized")
            val firebaseAppCheck = com.google.firebase.appcheck.FirebaseAppCheck.getInstance()
            if (com.lumiroom.app.BuildConfig.DEBUG) {
                android.util.Log.d("LumiroomAppCheck", "Installing DebugAppCheckProviderFactory")
                firebaseAppCheck.installAppCheckProviderFactory(
                    com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory.getInstance()
                )
            } else {
                android.util.Log.d("LumiroomAppCheck", "Installing PlayIntegrityAppCheckProviderFactory")
                firebaseAppCheck.installAppCheckProviderFactory(
                    com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory.getInstance()
                )
            }
            firebaseAppCheck.setTokenAutoRefreshEnabled(true)
        } catch (e: Exception) {
            android.util.Log.e("LumiroomAppCheck", "Failed to initialize App Check", e)
        }
    }
}
