package com.exo.styleswap.data

import com.exo.styleswap.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/** Result of a successful /v1/tryon call. */
data class TryOnResult(
    val url: String?,
    val imageBase64: String?,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val requestId: String?
)

/** Thrown for non-2xx responses, carrying the HTTP status for tailored messages. */
class TryOnException(val code: Int, message: String) : Exception(message)

object TryOnApi {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .callTimeout(240, TimeUnit.SECONDS)
            .build()
    }

    private val jpeg = "image/jpeg".toMediaType()

    /**
     * Performs the virtual try-on. [person] and [garment] are local JPEG files.
     */
    suspend fun tryOn(
        person: File,
        garment: File,
        category: String,
        poses: Int,
        quality: String,
        responseFormat: String = "url"
    ): TryOnResult = withContext(Dispatchers.IO) {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "person.jpg", person.asRequestBody(jpeg))
            .addFormDataPart("garment", "garment.jpg", garment.asRequestBody(jpeg))
            .addFormDataPart("category", category)
            .addFormDataPart("poses", poses.toString())
            .addFormDataPart("quality", quality)
            .addFormDataPart("response_format", responseFormat)
            .build()

        val request = Request.Builder()
            .url(BuildConfig.TRYON_BASE_URL.trimEnd('/') + "/v1/tryon")
            .header("X-API-Key", BuildConfig.TRYON_API_KEY)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw TryOnException(response.code, extractError(text, response.code))
            }
            val json = JSONObject(text)
            TryOnResult(
                url = json.optString("url").takeIf { it.isNotBlank() && it != "null" },
                imageBase64 = json.optString("image_base64").takeIf { it.isNotBlank() && it != "null" },
                mimeType = json.optString("mime_type", "image/png"),
                width = json.optInt("width", 0),
                height = json.optInt("height", 0),
                requestId = json.optString("request_id").takeIf { it.isNotBlank() && it != "null" }
            )
        }
    }

    /** Downloads raw bytes from a result URL (absolute or relative to the base host). */
    suspend fun downloadBytes(url: String): ByteArray = withContext(Dispatchers.IO) {
        val full = if (url.startsWith("http", ignoreCase = true)) {
            url
        } else {
            BuildConfig.TRYON_BASE_URL.trimEnd('/') + "/" + url.trimStart('/')
        }
        val request = Request.Builder()
            .url(full)
            .header("X-API-Key", BuildConfig.TRYON_API_KEY)
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw TryOnException(response.code, "Tải ảnh kết quả thất bại (${response.code})")
            }
            response.body?.bytes() ?: throw TryOnException(response.code, "Phản hồi rỗng")
        }
    }

    /** The result URL resolved to an absolute URL for image loaders. */
    fun absoluteUrl(url: String): String =
        if (url.startsWith("http", ignoreCase = true)) url
        else BuildConfig.TRYON_BASE_URL.trimEnd('/') + "/" + url.trimStart('/')

    private fun extractError(text: String, code: Int): String {
        return try {
            val json = JSONObject(text)
            val detail = json.opt("detail")
            when (detail) {
                is String -> detail
                else -> json.optString("message").ifBlank { "HTTP $code" }
            }
        } catch (e: Exception) {
            if (text.isNotBlank()) text.take(160) else "HTTP $code"
        }
    }
}
