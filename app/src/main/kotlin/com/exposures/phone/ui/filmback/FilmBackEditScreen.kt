package com.exposures.phone.ui.filmback

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
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.exposures.model.FilmBackType
import com.exposures.phone.ExposuresViewModelFactory
import com.exposures.phone.ui.appContainer
import com.exposures.phone.ui.components.DropdownField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilmBackEditScreen(
    id: String?,
    initialCameraBodyId: String?,
    onDone: () -> Unit,
    onSaved: (savedId: String) -> Unit,
    onAddCameraBody: () -> Unit,
    createdCameraBodyId: State<String?>,
) {
    val container = appContainer()
    val viewModel: FilmBackEditViewModel = viewModel(
        factory = ExposuresViewModelFactory(
            container.repository, container.syncPusher, container.dataLayerClient,
            entityId = id, initialCameraBodyId = initialCameraBodyId,
        ),
    )
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.done) {
        if (state.done) {
            state.savedId?.let(onSaved)
            onDone()
        }
    }
    val newCameraBodyId by createdCameraBodyId
    LaunchedEffect(newCameraBodyId) { newCameraBodyId?.let { viewModel.setCameraBody(it) } }

    Scaffold(topBar = { TopAppBar(title = { Text(if (state.isNew) "New Film Back" else "Edit Film Back") }) }) { padding ->
        Column(modifier = Modifier.fillMaxWidth().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.availableCameraBodies.isEmpty()) {
                Text(
                    "Add a camera body first — a back needs one.",
                    modifier = Modifier.padding(top = 8.dp),
                )
                Button(
                    onClick = onAddCameraBody,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) { Text("Add camera body") }
            } else {
                DropdownField(
                    label = "Camera body",
                    value = state.availableCameraBodies.firstOrNull { it.id == state.cameraBodyId }
                        ?: state.availableCameraBodies.first(),
                    options = state.availableCameraBodies,
                    optionLabel = { it.name },
                    onValueChange = { viewModel.setCameraBody(it.id) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            DropdownField(
                label = "Type",
                value = state.type,
                options = FilmBackType.entries,
                optionLabel = { it.name },
                onValueChange = viewModel::setType,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = state.primaryFrameCount,
                onValueChange = viewModel::setPrimaryFrameCount,
                label = { Text("Frame count") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = state.alternateFrameCount,
                onValueChange = viewModel::setAlternateFrameCount,
                label = { Text("Alternate frame count (optional)") },
                supportingText = {
                    Text("If this back sometimes yields a different count — e.g. tight loading squeezing out one extra frame.")
                },
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
