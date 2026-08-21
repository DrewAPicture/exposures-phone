package com.exposures.phone.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CaptureCameraPreference {
    REAR,
    FRONT,
}

class CaptureCameraPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _preference = MutableStateFlow(readPreference())
    val preference: StateFlow<CaptureCameraPreference> = _preference.asStateFlow()

    fun setPreference(value: CaptureCameraPreference) {
        prefs.edit().putString(KEY_CAPTURE_CAMERA, value.name).apply()
        _preference.value = value
    }

    private fun readPreference(): CaptureCameraPreference {
        val raw = prefs.getString(KEY_CAPTURE_CAMERA, CaptureCameraPreference.REAR.name)
        return CaptureCameraPreference.entries.firstOrNull { it.name == raw } ?: CaptureCameraPreference.REAR
    }

    private companion object {
        const val PREFS_NAME = "app_settings"
        const val KEY_CAPTURE_CAMERA = "capture_camera"
    }
}
