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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.exposures.model.FilmRoll
import com.exposures.phone.ExposuresViewModelFactory
import com.exposures.phone.export.CsvFileSharer
import com.exposures.phone.ui.appContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilmRollListScreen(onAdd: () -> Unit, onEdit: (String) -> Unit) {
    val container = appContainer()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val viewModel: FilmRollListViewModel = viewModel(
        factory = ExposuresViewModelFactory(
            container.repository,
            container.syncPusher,
            container.dataLayerClient,
            container.csvExportCoordinator,
        ),
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
                    trailingContent = {
                        TextButton(onClick = {
                            coroutineScope.launch {
                                viewModel.exportCsv(roll.id)?.let { csv ->
                                    CsvFileSharer.share(context, csv, "${roll.name.sanitizedForFileName()}.csv")
                                }
                            }
                        }) { Text("Export") }
                    },
                    modifier = Modifier.fillMaxWidth().clickable { onEdit(roll.id) },
                )
            }
        }
    }
}

private fun String.sanitizedForFileName(): String = replace(Regex("[^A-Za-z0-9]+"), "_")
