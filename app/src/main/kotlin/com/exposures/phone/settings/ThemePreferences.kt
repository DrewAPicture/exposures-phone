package com.exposures.phone.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
}

class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _preference = MutableStateFlow(readPreference())
    val preference: StateFlow<AppThemePreference> = _preference.asStateFlow()

    fun setPreference(value: AppThemePreference) {
        prefs.edit().putString(KEY_THEME_MODE, value.name).apply()
        _preference.value = value
    }

    private fun readPreference(): AppThemePreference {
        val raw = prefs.getString(KEY_THEME_MODE, AppThemePreference.SYSTEM.name)
        return AppThemePreference.entries.firstOrNull { it.name == raw } ?: AppThemePreference.SYSTEM
    }

    private companion object {
        const val PREFS_NAME = "app_settings"
        const val KEY_THEME_MODE = "theme_mode"
    }
}
