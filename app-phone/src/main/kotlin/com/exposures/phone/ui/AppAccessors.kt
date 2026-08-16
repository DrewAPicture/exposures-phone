package com.exposures.phone.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.exposures.phone.AppContainer
import com.exposures.phone.ExposuresApplication

@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as ExposuresApplication).container
