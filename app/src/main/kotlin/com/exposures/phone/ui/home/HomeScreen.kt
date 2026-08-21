package com.exposures.phone.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            Text("Quick Stats", style = MaterialTheme.typography.titleMedium)
            HomeStatsSection(
                allTimeSynced = state.exposureCount,
                syncedToday = state.syncedTodayCount,
                pendingSync = state.pendingSyncCount,
                favoriteCameraName = state.favoriteCameraName,
                watchReachable = state.watchReachable,
                onRefreshWatch = viewModel::refreshPairingStatus,
            )
            Text("Equipment", style = MaterialTheme.typography.titleMedium)

            EquipmentTiles(
                cameraBodyCount = state.cameraBodyCount,
                lensCount = state.lensCount,
                lightMeterCount = state.lightMeterCount,
                filmBackCount = state.filmBackCount,
                filmRollCount = state.filmRollCount,
                onOpenSettings = onOpenSettings,
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
                        Text("Data", style = MaterialTheme.typography.titleSmall)
                        Text("Export exposures as CSV", style = MaterialTheme.typography.bodyMedium)
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
private fun HomeStatsSection(
    allTimeSynced: Int,
    syncedToday: Int,
    pendingSync: Int,
    favoriteCameraName: String,
    watchReachable: Boolean?,
    onRefreshWatch: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CircularStatTile(
                label = "All time",
                value = allTimeSynced.toString(),
                modifier = Modifier.weight(1f),
            )
            CircularStatTile(
                label = "Today",
                value = syncedToday.toString(),
                modifier = Modifier.weight(1f),
            )
            CircularStatTile(
                label = "Pending",
                value = pendingSync.toString(),
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            InlineStatTile(
                label = "Most Used",
                value = favoriteCameraName,
                icon = Icons.Filled.FavoriteBorder,
                modifier = Modifier.weight(1f),
            )
            val statusText = when (watchReachable) {
                true -> "Connected"
                false -> "Disconnected"
                null -> "Checking"
            }
            val statusColor = when (watchReachable) {
                true -> MaterialTheme.colorScheme.primary
                false -> MaterialTheme.colorScheme.onSurfaceVariant
                null -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            InlineStatTile(
                label = "Watch Status",
                value = statusText,
                icon = Icons.Filled.Watch,
                valueColor = statusColor,
                onRefresh = onRefreshWatch,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CircularStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = value,
                    fontSize = statValueFontSize(value),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun InlineStatTile(
    label: String,
    value: String,
    icon: ImageVector? = null,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onRefresh: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 88.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Box(
            modifier = Modifier.height(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = valueColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.padding(start = if (icon != null) 6.dp else 0.dp),
                )
            }
            if (onRefresh != null) {
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh watch status",
                    )
                }
            } else {
                Box(modifier = Modifier.size(48.dp))
            }
        }
    }
}

private fun statValueFontSize(value: String) = when {
    value.length <= 3 -> 30.sp
    value.length <= 5 -> 24.sp
    value.length <= 8 -> 20.sp
    else -> 16.sp
}

@Composable
private fun EquipmentTiles(
    cameraBodyCount: Int,
    lensCount: Int,
    lightMeterCount: Int,
    filmBackCount: Int,
    filmRollCount: Int,
    onOpenSettings: () -> Unit,
    onOpenCameraBodies: () -> Unit,
    onOpenLenses: () -> Unit,
    onOpenLightMeters: () -> Unit,
    onOpenFilmBacks: () -> Unit,
    onOpenFilmRolls: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EquipmentTile(
                title = "Film Backs",
                subtitle = "$filmBackCount configured",
                onClick = onOpenFilmBacks,
                modifier = Modifier.weight(1f),
            )
            EquipmentTile(
                title = "Film Rolls",
                subtitle = "$filmRollCount configured",
                onClick = onOpenFilmRolls,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EquipmentTile(
                title = "Light Meters",
                subtitle = "$lightMeterCount configured",
                onClick = onOpenLightMeters,
                modifier = Modifier.weight(1f),
            )
            EquipmentTile(
                title = "Settings",
                subtitle = "App, sync, permissions",
                onClick = onOpenSettings,
                icon = Icons.Filled.Settings,
                emphasized = true,
                modifier = Modifier.weight(1f),
            )
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
    icon: ImageVector? = null,
    emphasized: Boolean = false,
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
    val tileColors = if (emphasized) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    } else {
        CardDefaults.cardColors()
    }
    Card(modifier = tileModifier, colors = tileColors) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
