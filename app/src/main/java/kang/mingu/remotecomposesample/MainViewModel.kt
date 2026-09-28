package kang.mingu.remotecomposesample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kang.mingu.remotecomposesample.data.remote.RemoteConfigFetcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val documentBytes: ByteArray? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val lastUpdated: Long = 0L,
    val documentRevision: Long = 0L,
)

/** One ViewModel per navigation entry, with explicit loading, success and error states. */
class MainViewModel(
    private val fetchDocument: suspend (String) -> ByteArray = RemoteConfigFetcher::fetchDocument,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val state = MutableStateFlow(MainUiState())
    val uiState = state.asStateFlow()
    private var configUrl: String? = null
    private var loadJob: Job? = null

    fun setConfigUrl(url: String) {
        if (url == configUrl) return
        configUrl = url
        refresh()
    }

    fun refresh() {
        val url = configUrl ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val bytes = fetchDocument(url)
                state.update {
                    val changed = !bytes.contentEquals(it.documentBytes)
                    it.copy(
                        documentBytes = if (changed) bytes else it.documentBytes,
                        isLoading = false,
                        lastUpdated = currentTimeMillis(),
                        documentRevision = if (changed) it.documentRevision + 1 else it.documentRevision,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                state.update {
                    it.copy(isLoading = false, documentBytes = null,
                        errorMessage = error.message ?: "화면을 불러오지 못했습니다")
                }
            }
        }
    }
}
