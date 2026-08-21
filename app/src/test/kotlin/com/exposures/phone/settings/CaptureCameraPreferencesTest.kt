package com.exposures.phone.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CaptureCameraPreferencesTest {

    private fun clearPrefs() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun `defaults to rear camera`() {
        clearPrefs()
        val context = ApplicationProvider.getApplicationContext<Context>()

        val preferences = CaptureCameraPreferences(context)

        assertEquals(CaptureCameraPreference.REAR, preferences.preference.value)
    }

    @Test
    fun `setPreference persists across instances`() {
        clearPrefs()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = CaptureCameraPreferences(context)

        preferences.setPreference(CaptureCameraPreference.FRONT)

        assertEquals(CaptureCameraPreference.FRONT, preferences.preference.value)
        assertEquals(CaptureCameraPreference.FRONT, CaptureCameraPreferences(context).preference.value)
    }
}
