package kang.mingu.remotecomposesample

import kang.mingu.remotecomposesample.data.remote.RemoteConfigFetcher
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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

    @Test fun resolvesAndVerifiesEachPublishedRevision() = runBlocking {
        val url = server.url("/site/config.manifest.json").toString()
        for (bytes in listOf(byteArrayOf(1, 2), byteArrayOf(3, 4))) {
            val hash = RemoteConfigFetcher.sha256(bytes)
            server.enqueue(MockResponse().setBody("""{"schemaVersion":1,"sha256":"$hash","file":"documents/config.$hash.rc"}"""))
            server.enqueue(MockResponse().setBody(Buffer().write(bytes)))
            assertArrayEquals(bytes, RemoteConfigFetcher.fetchPublishedDocument(url))
            assertEquals("/site/config.manifest.json", server.takeRequest().requestUrl!!.encodedPath)
            assertEquals("/site/documents/config.$hash.rc", server.takeRequest().requestUrl!!.encodedPath)
        }
    }

    @Test fun rejectsStaleBinaryRatherThanReportingSuccessfulRefresh() = runBlocking {
        val hash = RemoteConfigFetcher.sha256(byteArrayOf(2))
        server.enqueue(MockResponse().setBody("""{"schemaVersion":1,"sha256":"$hash","file":"documents/config.$hash.rc"}"""))
        server.enqueue(MockResponse().setBody(Buffer().write(byteArrayOf(1))))
        try {
            RemoteConfigFetcher.fetchPublishedDocument(server.url("/config.manifest.json").toString())
            fail("A stale document must never be applied")
        } catch (error: IOException) {
            assertTrue(error.message.orEmpty().contains("다릅니다"))
        }
    }

    @Test fun refreshBypassesUrlKeyedCdnCacheAndPreservesQueryParameters() = runBlocking {
        val cache = mutableMapOf<String, String>()
        var latest = "old document"
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val content = cache.getOrPut(request.path!!) { latest }
                return MockResponse().setHeader("Cache-Control", "max-age=300").setBody(content)
            }
        }
        val url = server.url("/config.rc?version=sample&_refresh=old").toString()
        assertEquals("old document", RemoteConfigFetcher.fetchDocument(url).decodeToString())
        latest = "new document"
        assertEquals("new document", RemoteConfigFetcher.fetchDocument(url).decodeToString())
        val first = server.takeRequest()
        val second = server.takeRequest()
        assertNotEquals(first.requestUrl, second.requestUrl)
        assertEquals("sample", second.requestUrl!!.queryParameter("version"))
        assertEquals(1, second.requestUrl!!.queryParameterValues("_refresh").size)
        assertEquals("no-cache, no-store", second.getHeader("Cache-Control"))
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
