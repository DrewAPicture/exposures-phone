package com.exposures.phone.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.exposures.phone.settings.AppThemePreference

@Composable
fun ExposuresTheme(
    themePreference: AppThemePreference = AppThemePreference.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themePreference) {
        AppThemePreference.SYSTEM -> isSystemInDarkTheme()
        AppThemePreference.LIGHT -> false
        AppThemePreference.DARK -> true
    }
    val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = colorScheme, content = content)
}
