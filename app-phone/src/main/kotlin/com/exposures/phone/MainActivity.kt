package com.exposures.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.exposures.phone.ui.ExposuresNavHost
import com.exposures.phone.ui.theme.ExposuresTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExposuresTheme {
                ExposuresNavHost()
            }
        }
    }
}
