package com.example.goodmail.ui.screens.inbox

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.goodmail.ui.screens.inbox.components.FilterChips
import com.example.goodmail.ui.screens.inbox.components.ShimmerInbox
import com.example.goodmail.ui.screens.inbox.components.SwipeableEmailRow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    onEmailClick: (String) -> Unit,
    onSettings: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: InboxViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val signedOut by viewModel.isSignedOut.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(signedOut) {
        if (signedOut) onSignedOut()
    }

    LaunchedEffect(state.error) {
        state.error?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.dismissError()
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { /* result ignored; notifications simply stay off if denied */ }
        LaunchedEffect(Unit) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Goodmail") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            FilterChips(selected = filter, onSelect = viewModel::setFilter)
            if (state.isClassifying) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    state.isInitialLoading -> ShimmerInbox()
                    state.emails.isEmpty() -> EmptyInbox(filter)
                    else -> {
                        val nowMillis = remember(state.emails) { System.currentTimeMillis() }
                        val listState = rememberLazyListState()
                        // Ask for the next (older) page when the user scrolls near the bottom.
                        LaunchedEffect(listState) {
                            snapshotFlow {
                                val info = listState.layoutInfo
                                val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
                                info.totalItemsCount > 0 &&
                                    lastVisible >= info.totalItemsCount - LOAD_MORE_THRESHOLD
                            }
                                .distinctUntilChanged()
                                .collect { nearEnd -> if (nearEnd) viewModel.loadMore() }
                        }
                        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                            items(state.emails, key = { it.id }) { email ->
                                SwipeableEmailRow(
                                    email = email,
                                    nowMillis = nowMillis,
                                    onClick = {
                                        viewModel.markRead(email.id)
                                        onEmailClick(email.id)
                                    },
                                    onDelete = {
                                        viewModel.delete(email.id)
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Email moved to Trash",
                                                actionLabel = "Undo",
                                                duration = SnackbarDuration.Short,
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.undoDelete()
                                            }
                                        }
                                    },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                            if (state.isLoadingMore) {
                                item(key = "loading_more") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Start loading the next page once the user is within this many rows of the end. */
private const val LOAD_MORE_THRESHOLD = 5

@Composable
private fun EmptyInbox(filter: InboxFilter) {
    val message = when (filter) {
        InboxFilter.ALL -> "Your inbox is empty"
        InboxFilter.IMPORTANT -> "Nothing urgent right now"
        InboxFilter.OTHERS -> "Nothing here"
    }
    // LazyColumn (not a static Box) so the pull-to-refresh gesture still works when empty.
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
