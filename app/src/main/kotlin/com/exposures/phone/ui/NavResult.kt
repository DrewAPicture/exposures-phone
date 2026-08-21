package com.exposures.phone.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.lifecycle.SavedStateHandle

/**
 * Reactive one-shot read of a result a child screen set on this [SavedStateHandle] (via
 * `savedStateHandle.set(key, ...)`) just before popping back — the "create inline, return with
 * selection" pattern used by the equipment edit screens (e.g. creating a film back from New Film
 * Roll and returning with it selected). The value is cleared right after being observed so it
 * can't be redelivered, e.g. on process-death recreation or a later visit to the same destination
 * that doesn't involve a new result.
 */
@Composable
fun SavedStateHandle.consumeResult(key: String): State<String?> {
    val flow = remember(this, key) { getStateFlow<String?>(key, null) }
    val state = flow.collectAsState()
    LaunchedEffect(state.value) { if (state.value != null) remove<String>(key) }
    return state
}
