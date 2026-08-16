package com.exposures.phone.ui.lens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.exposures.model.StopIncrement
import com.exposures.phone.ExposuresViewModelFactory
import com.exposures.phone.ui.appContainer
import com.exposures.phone.ui.components.DropdownField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LensEditScreen(id: String?, onDone: () -> Unit) {
    val container = appContainer()
    val viewModel: LensEditViewModel = viewModel(
        factory = ExposuresViewModelFactory(container.repository, container.syncPusher, container.dataLayerClient, entityId = id),
    )
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.done) { if (state.done) onDone() }

    Scaffold(topBar = { TopAppBar(title = { Text(if (state.isNew) "New Lens" else "Edit Lens") }) }) { padding ->
        Column(modifier = Modifier.fillMaxWidth().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.minAperture,
                onValueChange = viewModel::setMinAperture,
                label = { Text("Min aperture (widest, e.g. 2.8)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = state.maxAperture,
                onValueChange = viewModel::setMaxAperture,
                label = { Text("Max aperture (smallest, e.g. 32)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            DropdownField(
                label = "Stop increment",
                value = state.stopIncrement,
                options = StopIncrement.entries,
                optionLabel = { it.name },
                onValueChange = viewModel::setStopIncrement,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = state.referencePhotoZoomRatio,
                onValueChange = viewModel::setReferencePhotoZoomRatio,
                label = { Text("Reference photo zoom (e.g. 1.0, 3.0)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Button(
                onClick = viewModel::save,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) { Text("Save") }
            if (!state.isNew) {
                OutlinedButton(
                    onClick = viewModel::delete,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) { Text("Delete") }
            }
        }
    }
}
