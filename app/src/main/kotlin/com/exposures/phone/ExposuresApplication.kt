package com.exposures.phone

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.exposures.phone.sync.UploadScheduler

/**
 * Implements [Configuration.Provider] and initializes WorkManager itself (with the manifest's
 * default androidx.startup initializer disabled — see AndroidManifest.xml) rather than relying on
 * that initializer's automatic ContentProvider-based startup, which doesn't reliably run under
 * Robolectric — the getInstance()-then-initialize() fallback below makes this safe regardless of
 * whether the manifest disable is actually honored in a given environment.
 */
class ExposuresApplication : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration = Configuration.Builder().build()

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        try {
            WorkManager.getInstance(this)
        } catch (e: IllegalStateException) {
            WorkManager.initialize(this, workManagerConfiguration)
        }
        UploadScheduler.schedulePeriodic(this)
    }
}
