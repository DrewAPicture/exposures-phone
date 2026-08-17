package com.exposures.phone.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.exposures.phone.ui.appContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenCameraBodies: () -> Unit,
    onOpenLenses: () -> Unit,
    onOpenLightMeters: () -> Unit,
    onOpenFilmBacks: () -> Unit,
    onOpenFilmRolls: () -> Unit,
) {
    val container = appContainer()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val viewModel: HomeViewModel = viewModel(
        factory = ExposuresViewModelFactory(
            container.repository,
            container.syncPusher,
            container.dataLayerClient,
        ),
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exposures") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Equipment", style = MaterialTheme.typography.titleMedium)

            EquipmentTiles(
                cameraBodyCount = state.cameraBodyCount,
                lensCount = state.lensCount,
                lightMeterCount = state.lightMeterCount,
                filmBackCount = state.filmBackCount,
                filmRollCount = state.filmRollCount,
                onOpenCameraBodies = onOpenCameraBodies,
                onOpenLenses = onOpenLenses,
                onOpenLightMeters = onOpenLightMeters,
                onOpenFilmBacks = onOpenFilmBacks,
                onOpenFilmRolls = onOpenFilmRolls,
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Watch data", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Exposures synced from watch: ${state.exposureCount}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    TextButton(onClick = {
                        coroutineScope.launch {
                            val csv = container.csvExportCoordinator.exportAll()
                            CsvFileSharer.share(context, csv, "exposures.csv")
                        }
                    }) { Text("Export All") }
                }
            }
        }
    }
}

@Composable
private fun EquipmentTiles(
    cameraBodyCount: Int,
    lensCount: Int,
    lightMeterCount: Int,
    filmBackCount: Int,
    filmRollCount: Int,
    onOpenCameraBodies: () -> Unit,
    onOpenLenses: () -> Unit,
    onOpenLightMeters: () -> Unit,
    onOpenFilmBacks: () -> Unit,
    onOpenFilmRolls: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val useTwoColumns = maxWidth >= 560.dp
        if (useTwoColumns) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EquipmentTile(
                        title = "Camera Bodies",
                        subtitle = "$cameraBodyCount configured",
                        onClick = onOpenCameraBodies,
                        modifier = Modifier.weight(1f),
                    )
                    EquipmentTile(
                        title = "Lenses",
                        subtitle = "$lensCount configured",
                        onClick = onOpenLenses,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EquipmentTile(
                        title = "Light Meters",
                        subtitle = "$lightMeterCount configured",
                        onClick = onOpenLightMeters,
                        modifier = Modifier.weight(1f),
                    )
                    EquipmentTile(
                        title = "Film Rolls",
                        subtitle = "$filmRollCount configured",
                        onClick = onOpenFilmRolls,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EquipmentTile(
                        title = "Film Backs",
                        subtitle = "$filmBackCount configured",
                        onClick = onOpenFilmBacks,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EquipmentTile(
                    title = "Camera Bodies",
                    subtitle = "$cameraBodyCount configured",
                    onClick = onOpenCameraBodies,
                    modifier = Modifier.fillMaxWidth(),
                )
                EquipmentTile(
                    title = "Lenses",
                    subtitle = "$lensCount configured",
                    onClick = onOpenLenses,
                    modifier = Modifier.fillMaxWidth(),
                )
                EquipmentTile(
                    title = "Light Meters",
                    subtitle = "$lightMeterCount configured",
                    onClick = onOpenLightMeters,
                    modifier = Modifier.fillMaxWidth(),
                )
                EquipmentTile(
                    title = "Film Rolls",
                    subtitle = "$filmRollCount configured",
                    onClick = onOpenFilmRolls,
                    modifier = Modifier.fillMaxWidth(),
                )
                EquipmentTile(
                    title = "Film Backs",
                    subtitle = "$filmBackCount configured",
                    onClick = onOpenFilmBacks,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
@Composable
private fun EquipmentTile(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val tileModifier = modifier
        .defaultMinSize(minHeight = 92.dp)
        .then(
            if (enabled) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            },
        )
    Card(modifier = tileModifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
