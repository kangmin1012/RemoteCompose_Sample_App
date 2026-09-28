package kang.mingu.remotecomposesample.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.UUID
import java.security.MessageDigest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Downloads a public binary document. No GitHub token is stored in the APK. */
object RemoteConfigFetcher {
    suspend fun fetchPublishedDocument(manifestUrl: String): ByteArray {
        val base = manifestUrl.toHttpUrl()
        val manifest = try {
            Json.parseToJsonElement(fetchDocument(manifestUrl).decodeToString()).jsonObject
        } catch (error: kotlinx.serialization.SerializationException) {
            throw IOException("화면 배포 명세를 읽을 수 없습니다", error)
        }
        if (manifest["schemaVersion"]?.jsonPrimitive?.content != "1") {
            throw IOException("지원하지 않는 화면 배포 명세입니다")
        }
        val expectedHash = manifest["sha256"]?.jsonPrimitive?.content.orEmpty()
        val file = manifest["file"]?.jsonPrimitive?.content.orEmpty()
        if (!expectedHash.matches(Regex("[a-f0-9]{64}")) ||
            !file.matches(Regex("documents/config(?:_detail)?\\.$expectedHash\\.rc"))) {
            throw IOException("화면 배포 명세의 문서 정보가 올바르지 않습니다")
        }
        val documentUrl = base.resolve(file) ?: throw IOException("문서 주소가 올바르지 않습니다")
        val bytes = fetchDocument(documentUrl.toString())
        if (sha256(bytes) != expectedHash) {
            throw IOException("배포된 화면 버전과 받은 문서가 다릅니다. 다시 시도해 주세요")
        }
        return bytes
    }

    fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }

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
