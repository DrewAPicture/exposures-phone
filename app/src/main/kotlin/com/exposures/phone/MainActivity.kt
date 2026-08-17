package com.exposures.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.exposures.phone.ui.ExposuresNavHost
import com.exposures.phone.ui.theme.ExposuresTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as ExposuresApplication).container
        setContent {
            val themePreference by container.themePreferences.preference.collectAsState()
            ExposuresTheme(themePreference = themePreference) {
                ExposuresNavHost()
            }
        }
    }
}
