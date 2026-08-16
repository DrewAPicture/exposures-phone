package com.exposures.phone.ui.lightmeter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.exposures.model.LightMeter
import com.exposures.phone.ExposuresViewModelFactory
import com.exposures.phone.ui.appContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LightMeterListScreen(onAdd: () -> Unit, onEdit: (String) -> Unit) {
    val container = appContainer()
    val viewModel: LightMeterListViewModel = viewModel(
        factory = ExposuresViewModelFactory(container.repository, container.syncPusher, container.dataLayerClient),
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Light Meters") }) },
        floatingActionButton = { FloatingActionButton(onClick = onAdd) { Text("+", modifier = Modifier.padding(8.dp)) } },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(padding)) {
            items(state.lightMeters) { lightMeter: LightMeter ->
                ListItem(
                    headlineContent = { Text(lightMeter.name) },
                    supportingContent = { Text("${lightMeter.manufacturer} · ${lightMeter.type}") },
                    modifier = Modifier.fillMaxWidth().clickable { onEdit(lightMeter.id) },
                )
            }
        }
    }
}
