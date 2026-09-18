package kang.mingu.remotecomposesample.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Downloads a public binary document. No GitHub token is stored in the APK. */
object RemoteConfigFetcher {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun fetchDocument(url: String): ByteArray = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url)
            .header("Cache-Control", "no-cache")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}: ${response.message}")
            val bytes = response.body?.bytes()
            if (bytes == null || bytes.isEmpty()) throw IOException("The remote document is empty")
            bytes
        }
    }
}
