package com.exposures.phone.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.exposures.phone.ExposuresViewModelFactory
import com.exposures.phone.export.CsvFileSharer
import com.exposures.phone.settings.AppThemePreference
import com.exposures.phone.ui.appContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val container = appContainer()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val viewModel: SettingsViewModel = viewModel(
        factory = ExposuresViewModelFactory(
            container.repository,
            container.syncPusher,
            container.dataLayerClient,
            container.csvExportCoordinator,
            container.themePreferences,
            triggerUpload = container::triggerUpload,
        ),
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (state.pendingSyncCount == 0) {
                            "Backend sync: up to date"
                        } else {
                            "Backend sync: ${state.pendingSyncCount} pending"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    TextButton(onClick = viewModel::syncNow) { Text("Sync Now") }
                }
            }

            PermissionsCard()

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Appearance", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ThemeOptionButton(
                            label = "System",
                            selected = state.themePreference == AppThemePreference.SYSTEM,
                            onClick = { viewModel.setThemePreference(AppThemePreference.SYSTEM) },
                            modifier = Modifier.weight(1f),
                        )
                        ThemeOptionButton(
                            label = "Light",
                            selected = state.themePreference == AppThemePreference.LIGHT,
                            onClick = { viewModel.setThemePreference(AppThemePreference.LIGHT) },
                            modifier = Modifier.weight(1f),
                        )
                        ThemeOptionButton(
                            label = "Dark",
                            selected = state.themePreference == AppThemePreference.DARK,
                            onClick = { viewModel.setThemePreference(AppThemePreference.DARK) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Data", style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = {
                        coroutineScope.launch {
                            val csv = viewModel.exportAllCsv()
                            CsvFileSharer.share(context, csv, "exposures.csv")
                        }
                    }) { Text("Export All") }
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(label) }
    }
}
