package kang.mingu.remotecomposesample.ui.components

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.remote.player.view.RemoteComposePlayer

@Composable
// alpha05 exposes its View player as a restricted API; keep the opt-in at this boundary.
@SuppressLint("RestrictedApi")
fun RemoteDocumentView(
    documentBytes: ByteArray,
    modifier: Modifier = Modifier,
    contentKey: Any = documentBytes.contentHashCode(),
    onAction: (id: Int, metadata: String?) -> Unit = { _, _ -> },
) {
    key(contentKey) {
        AndroidView(
            factory = { ctx ->
                RemoteComposePlayer(ctx).apply {
                    setDocument(documentBytes)
                    addIdActionListener { id, metadata ->
                        onAction(id, metadata)
                    }
                }
            },
            modifier = modifier
        )
    }
}
