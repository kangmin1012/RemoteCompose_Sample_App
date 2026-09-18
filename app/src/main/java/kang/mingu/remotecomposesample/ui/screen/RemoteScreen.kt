package kang.mingu.remotecomposesample.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kang.mingu.remotecomposesample.MainViewModel
import kang.mingu.remotecomposesample.ui.components.ErrorContent
import kang.mingu.remotecomposesample.ui.components.RemoteDocumentView
import kang.mingu.remotecomposesample.ui.components.RemoteScreenTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen(
    configUrl: String,
    title: String,
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
    viewModel: MainViewModel = viewModel(key = configUrl),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(configUrl) {
        viewModel.setConfigUrl(configUrl)
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val subtitle = if (state.lastUpdated > 0) {
        "Last updated: ${timeFormat.format(Date(state.lastUpdated))}"
    } else null

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            RemoteScreenTopBar(
                title = title,
                subtitle = subtitle,
                showBack = showBack,
                isLoading = state.isLoading,
                onBack = onBack,
                onRefresh = { viewModel.refresh() }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            val errorMessage = state.errorMessage
            val documentBytes = state.documentBytes

            when {
                state.isLoading && documentBytes == null -> {
                    CircularProgressIndicator()
                }
                errorMessage != null && documentBytes == null -> {
                    ErrorContent(
                        errorMessage = errorMessage,
                        onRetry = { viewModel.refresh() }
                    )
                }
                documentBytes != null -> {
                    RemoteDocumentView(
                        documentBytes = documentBytes,
                        modifier = Modifier.fillMaxSize(),
                        contentKey = state.lastUpdated,
                        onAction = { id, metadata ->
                            if (metadata?.startsWith("navigate:") == true) {
                                onNavigate(metadata.removePrefix("navigate:"))
                            } else {
                                val msg = "Action (ID: $id, Data: $metadata)"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}
