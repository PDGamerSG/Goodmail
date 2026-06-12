package com.example.goodmail.ui.screens.detail

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.goodmail.domain.model.EmailImportance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailDetailScreen(
    onBack: () -> Unit,
    viewModel: EmailDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onBack()
    }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.email?.subject ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::delete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Always important from sender") },
                            onClick = {
                                menuExpanded = false
                                viewModel.addSenderRule(EmailImportance.IMPORTANT)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Never important from sender") },
                            onClick = {
                                menuExpanded = false
                                viewModel.addSenderRule(EmailImportance.NOT_IMPORTANT)
                            },
                        )
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
            state.email?.let { email ->
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = email.subject,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = email.from,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider()
            }

            when {
                state.isLoadingBody -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                state.error != null -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = state.error ?: "", color = MaterialTheme.colorScheme.error)
                }

                else -> EmailBodyWebView(
                    html = state.body?.takeIf { it.isNotBlank() } ?: state.email?.snippet.orEmpty(),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun EmailBodyWebView(
    html: String,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                setBackgroundColor(AndroidColor.WHITE)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, wrapHtml(html), "text/html", "UTF-8", null)
        },
    )
}

/**
 * Email HTML is authored against a white background; senders set explicit dark text colors
 * without declaring a background. Forcing the app's dark background underneath made that text
 * invisible, so the body always renders on a fixed light surface (as Gmail does).
 */
private fun wrapHtml(body: String): String = """
    <!DOCTYPE html>
    <html>
    <head>
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <style>
        html, body { margin: 0; padding: 12px; background: #FFFFFF; color: #1F1F1F;
          font-family: sans-serif; font-size: 15px; line-height: 1.5; word-wrap: break-word; }
        img { max-width: 100%; height: auto; }
        a { color: #1A73E8; }
        table { max-width: 100%; }
        * { max-width: 100%; box-sizing: border-box; }
      </style>
    </head>
    <body>$body</body>
    </html>
""".trimIndent()
