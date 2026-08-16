package com.exposures.phone.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.exposures.phone.ExposuresViewModelFactory
import com.exposures.phone.ui.appContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenCameraBodies: () -> Unit,
    onOpenLenses: () -> Unit,
    onOpenLightMeters: () -> Unit,
    onOpenFilmRolls: () -> Unit,
) {
    val container = appContainer()
    val viewModel: HomeViewModel = viewModel(
        factory = ExposuresViewModelFactory(container.repository, container.syncPusher, container.dataLayerClient),
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Exposures") }) }) { padding ->
        Column(modifier = Modifier.fillMaxWidth().padding(padding).padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = when (state.watchReachable) {
                            true -> "Watch: connected"
                            false -> "Watch: not reachable"
                            null -> "Watch: checking…"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    TextButton(onClick = viewModel::refreshPairingStatus) { Text("Refresh") }
                }
            }

            PermissionsCard()

            ListItem(
                headlineContent = { Text("Camera Bodies") },
                supportingContent = { Text("${state.cameraBodyCount} configured") },
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenCameraBodies),
            )
            ListItem(
                headlineContent = { Text("Lenses") },
                supportingContent = { Text("${state.lensCount} configured") },
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenLenses),
            )
            ListItem(
                headlineContent = { Text("Light Meters") },
                supportingContent = { Text("${state.lightMeterCount} configured") },
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenLightMeters),
            )
            ListItem(
                headlineContent = { Text("Film Rolls") },
                supportingContent = { Text("${state.filmRollCount} configured") },
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenFilmRolls),
            )
            ListItem(
                headlineContent = { Text("Exposures synced from watch") },
                supportingContent = { Text("${state.exposureCount}") },
            )
        }
    }
}
