package com.exposures.phone.ui.filmroll

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import com.exposures.model.FilmColorType
import com.exposures.model.FilmFormat
import com.exposures.phone.ExposuresViewModelFactory
import com.exposures.phone.ui.appContainer
import com.exposures.phone.ui.components.DropdownField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilmRollEditScreen(id: String?, onDone: () -> Unit) {
    val container = appContainer()
    val viewModel: FilmRollEditViewModel = viewModel(
        factory = ExposuresViewModelFactory(container.repository, container.syncPusher, container.dataLayerClient, entityId = id),
    )
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.done) { if (state.done) onDone() }

    Scaffold(topBar = { TopAppBar(title = { Text(if (state.isNew) "New Film Roll" else "Edit Film Roll") }) }) { padding ->
        Column(modifier = Modifier.fillMaxWidth().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text("Roll name") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.filmStock,
                onValueChange = viewModel::setFilmStock,
                label = { Text("Film stock") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = state.boxSpeedIso,
                onValueChange = viewModel::setBoxSpeedIso,
                label = { Text("Box speed ISO") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            DropdownField(
                label = "Format",
                value = state.format,
                options = FilmFormat.entries,
                optionLabel = { it.name },
                onValueChange = viewModel::setFormat,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            DropdownField(
                label = "Color type",
                value = state.colorType,
                options = FilmColorType.entries,
                optionLabel = { it.name },
                onValueChange = viewModel::setColorType,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            if (state.availableCameraBodies.isEmpty()) {
                Text(
                    "Add a camera body first — a roll needs one.",
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                DropdownField(
                    label = "Camera body",
                    // Falls back to the first available body if the roll's saved cameraBodyId no
                    // longer exists (e.g. that body was deleted) — picking it re-links the roll.
                    value = state.availableCameraBodies.firstOrNull { it.id == state.cameraBodyId }
                        ?: state.availableCameraBodies.first(),
                    options = state.availableCameraBodies,
                    optionLabel = { it.name },
                    onValueChange = { viewModel.setCameraBody(it.id) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            DropdownField(
                label = "Light meter (optional)",
                value = state.availableLightMeters.firstOrNull { it.id == state.lightMeterId },
                options = listOf(null) + state.availableLightMeters,
                optionLabel = { it?.name ?: "None" },
                onValueChange = { viewModel.setLightMeter(it?.id) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = state.targetFrameCount,
                onValueChange = viewModel::setTargetFrameCount,
                label = { Text("Target frame count") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
