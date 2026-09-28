package kang.mingu.remotecomposesample.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.UUID

/** Downloads a public binary document. No GitHub token is stored in the APK. */
object RemoteConfigFetcher {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun fetchDocument(url: String): ByteArray = withContext(Dispatchers.IO) {
        // Raw GitHub is CDN-cached. A new cache key is needed for each refresh,
        // including repeated requests within the same millisecond.
        val freshUrl = url.toHttpUrl().newBuilder()
            .setQueryParameter("_refresh", UUID.randomUUID().toString())
            .build()
        val request = Request.Builder().url(freshUrl)
            .header("Cache-Control", "no-cache, no-store")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}: ${response.message}")
            val bytes = response.body?.bytes()
            if (bytes == null || bytes.isEmpty()) throw IOException("The remote document is empty")
            bytes
        }
    }
}
