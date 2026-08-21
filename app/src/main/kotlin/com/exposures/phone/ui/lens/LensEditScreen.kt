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
import com.exposures.model.LensType
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
            DropdownField(
                label = "Camera body (optional)",
                value = state.availableCameraBodies.firstOrNull { it.id == state.cameraBodyId },
                options = listOf(null) + state.availableCameraBodies,
                optionLabel = { it?.name ?: "None" },
                onValueChange = { viewModel.setCameraBody(it?.id) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = state.minAperture,
                onValueChange = viewModel::setMinAperture,
                label = { Text("Min aperture (widest, e.g. ƒ/2.8)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = state.maxAperture,
                onValueChange = viewModel::setMaxAperture,
                label = { Text("Max aperture (smallest, e.g. ƒ/32)") },
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
            DropdownField(
                label = "Lens Type",
                value = state.lensType,
                options = LensType.entries,
                optionLabel = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
                onValueChange = viewModel::setLensType,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            if (state.lensType == LensType.PRIME) {
                OutlinedTextField(
                    value = state.focalLengthMm,
                    onValueChange = viewModel::setFocalLengthMm,
                    label = { Text("Focal Length") },
                    suffix = { Text("mm") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            } else {
                OutlinedTextField(
                    value = state.focalLengthMinMm,
                    onValueChange = viewModel::setFocalLengthMinMm,
                    label = { Text("Focal Length (min)") },
                    suffix = { Text("mm") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = state.focalLengthMaxMm,
                    onValueChange = viewModel::setFocalLengthMaxMm,
                    label = { Text("Focal Length (max)") },
                    suffix = { Text("mm") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            OutlinedTextField(
                value = state.referencePhotoZoomRatio,
                onValueChange = viewModel::setReferencePhotoZoomRatio,
                label = { Text("Reference photo zoom") },
                supportingText = {
                    Text(
                        "How far to zoom the phone's camera for this lens's reference photo — " +
                            "1.0 = no zoom, higher for longer lenses, lower for wider ones.",
                    )
                },
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
