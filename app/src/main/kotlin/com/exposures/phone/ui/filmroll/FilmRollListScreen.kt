package com.exposures.phone.ui.filmroll

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
import com.exposures.model.FilmRoll
import com.exposures.phone.ExposuresViewModelFactory
import com.exposures.phone.ui.appContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilmRollListScreen(onAdd: () -> Unit, onEdit: (String) -> Unit) {
    val container = appContainer()
    val viewModel: FilmRollListViewModel = viewModel(
        factory = ExposuresViewModelFactory(container.repository, container.syncPusher, container.dataLayerClient),
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Film Rolls") }) },
        floatingActionButton = { FloatingActionButton(onClick = onAdd) { Text("+", modifier = Modifier.padding(8.dp)) } },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(padding)) {
            items(state.rolls) { roll: FilmRoll ->
                ListItem(
                    headlineContent = { Text(roll.name) },
                    supportingContent = { Text("${roll.filmStock} · ${roll.status}") },
                    modifier = Modifier.fillMaxWidth().clickable { onEdit(roll.id) },
                )
            }
        }
    }
}
