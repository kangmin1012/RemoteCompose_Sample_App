package kang.mingu.remotecomposesample

import kang.mingu.remotecomposesample.data.remote.RemoteConfigFetcher
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException

class RemoteConfigFetcherTest {
    private val server = MockWebServer()

    @Before fun startServer() = server.start()
    @After fun stopServer() = server.shutdown()

    @Test fun downloadsBinaryWithoutTextConversion() = runBlocking {
        val document = byteArrayOf(0, 1, 127, -128, -1)
        server.enqueue(MockResponse().setBody(Buffer().write(document)))
        val actual = RemoteConfigFetcher.fetchDocument(server.url("/config.rc").toString())
        assertArrayEquals(document, actual)
    }

    @Test fun rejectsMissingDocument() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))
        assertDownloadFails("HTTP 404")
    }

    @Test fun rejectsEmptyDocument() = runBlocking {
        server.enqueue(MockResponse().setBody(""))
        assertDownloadFails("empty")
    }

    private suspend fun assertDownloadFails(message: String) {
        try {
            RemoteConfigFetcher.fetchDocument(server.url("/config.rc").toString())
            fail("Expected an IOException")
        } catch (error: IOException) {
            assertTrue(error.message.orEmpty().contains(message))
        }
    }
}
