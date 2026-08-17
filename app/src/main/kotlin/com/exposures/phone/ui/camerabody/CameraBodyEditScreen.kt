package com.exposures.phone.ui.camerabody

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import com.exposures.model.ShutterSpeed
import com.exposures.phone.ExposuresViewModelFactory
import com.exposures.phone.ui.appContainer
import com.exposures.phone.ui.components.DropdownField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraBodyEditScreen(id: String?, onDone: () -> Unit) {
    val container = appContainer()
    val viewModel: CameraBodyEditViewModel = viewModel(
        factory = ExposuresViewModelFactory(container.repository, container.syncPusher, container.dataLayerClient, entityId = id),
    )
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.done) { if (state.done) onDone() }

    Scaffold(topBar = { TopAppBar(title = { Text(if (state.isNew) "New Camera Body" else "Edit Camera Body") }) }) { padding ->
        Column(modifier = Modifier.fillMaxWidth().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.manufacturer,
                onValueChange = viewModel::setManufacturer,
                label = { Text("Manufacturer") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            DropdownField(
                label = "Fastest shutter speed",
                value = state.fastestShutterSpeed,
                options = ShutterSpeed.STANDARD_FULL_STOPS,
                optionLabel = ShutterSpeed::label,
                onValueChange = viewModel::setFastestShutterSpeed,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            DropdownField(
                label = "Slowest shutter speed",
                value = state.slowestShutterSpeed,
                options = ShutterSpeed.STANDARD_FULL_STOPS,
                optionLabel = ShutterSpeed::label,
                onValueChange = viewModel::setSlowestShutterSpeed,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = state.otherFastShutterSpeedDenominator,
                onValueChange = viewModel::setOtherFastShutterSpeedDenominator,
                label = { Text("Other fast shutter speed (optional)") },
                supportingText = {
                    Text(
                        "For a body whose fastest speed doesn't land on a standard stop — e.g. a " +
                            "leaf shutter topping out at 1/400. Enter just the denominator (400).",
                    )
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Has bulb mode")
                Switch(checked = state.hasBulbMode, onCheckedChange = viewModel::setHasBulbMode)
            }
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
