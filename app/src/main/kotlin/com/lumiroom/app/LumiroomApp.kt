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
}
