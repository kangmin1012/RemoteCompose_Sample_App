package kang.mingu.remotecomposesample

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun changedBytesAdvancePlayerRevisionEvenWhenClockIsUnchanged() = runTest(dispatcher) {
        var bytes = byteArrayOf(1)
        val model = MainViewModel(fetchDocument = { bytes }, currentTimeMillis = { 1000L })
        model.setConfigUrl("https://example.test/config.rc")
        runCurrent()
        val old = model.uiState.value
        bytes = byteArrayOf(2)
        model.refresh()
        runCurrent()
        val new = model.uiState.value
        assertArrayEquals(bytes, new.documentBytes)
        assertEquals(old.lastUpdated, new.lastUpdated)
        assertEquals(old.documentRevision + 1, new.documentRevision)
        assertFalse(new.isLoading)
    }

    @Test fun unchangedDocumentUpdatesCheckTimeWithoutRecreatingPlayer() = runTest(dispatcher) {
        var now = 1000L
        val model = MainViewModel(fetchDocument = { byteArrayOf(1) }, currentTimeMillis = { now })
        model.setConfigUrl("https://example.test/config.rc")
        runCurrent()
        val old = model.uiState.value
        now = 2000L
        model.refresh()
        runCurrent()
        val new = model.uiState.value
        assertEquals(2000L, new.lastUpdated)
        assertEquals(old.documentRevision, new.documentRevision)
        assertSame(old.documentBytes, new.documentBytes)
    }

    @Test fun refreshKeepsContentWhileLoadingAndFailureEndsLoading() = runTest(dispatcher) {
        val pending = CompletableDeferred<ByteArray>()
        var calls = 0
        val model = MainViewModel(fetchDocument = {
            if (calls++ == 0) byteArrayOf(1) else pending.await()
        })
        model.setConfigUrl("https://example.test/config.rc")
        runCurrent()
        model.refresh()
        runCurrent()
        assertTrue(model.uiState.value.isLoading)
        assertArrayEquals(byteArrayOf(1), model.uiState.value.documentBytes)
        pending.completeExceptionally(IOException("offline"))
        runCurrent()
        assertFalse(model.uiState.value.isLoading)
        assertEquals("offline", model.uiState.value.errorMessage)
    }

    @Test fun returningToSameScreenFetchesTheLatestDocument() = runTest(dispatcher) {
        var calls = 0
        val model = MainViewModel(fetchDocument = { byteArrayOf((++calls).toByte()) })
        model.setConfigUrl("https://example.test/config.manifest.json")
        runCurrent()
        model.setConfigUrl("https://example.test/config.manifest.json")
        runCurrent()
        assertEquals(2, calls)
        assertArrayEquals(byteArrayOf(2), model.uiState.value.documentBytes)
    }
}
