package com.xylotune.app.data

import com.xylotune.app.model.SyncFile
import com.xylotune.app.util.XyloJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.net.URLEncoder

const val DRIVE_SYNC_FILENAME = "xylotune-songs.json"
private const val DEFAULT_BASE_URL = "https://www.googleapis.com"
private val JSON_MEDIA_TYPE = "application/json".toMediaType()
private const val UPLOAD_BOUNDARY = "xylotune-boundary"

class DriveApiException(message: String, val statusCode: Int? = null) : Exception(message)

/**
 * Raw Drive v3 REST calls — no generated client SDK, endpoint-for-endpoint the same as the
 * web's driveFetch/findSyncFileId/downloadSyncFile/uploadSyncFile (index.html lines
 * ~450-487), so the two apps read/write the same file by construction, not convention.
 * Every call blocks its thread (plain OkHttp `execute()`), so every public method here
 * hops to Dispatchers.IO itself — callers never need to remember to.
 *
 * [baseUrl] defaults to the real Drive API host; tests override it to point at a
 * MockWebServer instance so the exact request shape (headers, method, multipart body) gets
 * real coverage without a live network call.
 */
class DriveApi(private val client: OkHttpClient = OkHttpClient(), private val baseUrl: String = DEFAULT_BASE_URL) {

    suspend fun findSyncFileId(accessToken: String): String? = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode("name='$DRIVE_SYNC_FILENAME' and trashed=false", "UTF-8")
        val url = "$baseUrl/drive/v3/files?q=$q&spaces=drive&fields=files(id)"
        executeChecked(authedRequest(url, accessToken).build()).use { resp ->
            val body = resp.body.string()
            val files = XyloJson.parseToJsonElement(body).jsonObject["files"]?.jsonArray
            files?.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.content
        }
    }

    suspend fun downloadSyncFile(fileId: String, accessToken: String): SyncFile = withContext(Dispatchers.IO) {
        val url = "$baseUrl/drive/v3/files/$fileId?alt=media"
        executeChecked(authedRequest(url, accessToken).build()).use { resp ->
            XyloJson.decodeFromString(resp.body.string())
        }
    }

    /** Returns the file's id (the existing one, or a freshly created one when [fileId] is null). */
    suspend fun uploadSyncFile(fileId: String?, body: SyncFile, accessToken: String): String =
        withContext(Dispatchers.IO) {
            val json = XyloJson.encodeToString(body)
            if (fileId != null) {
                val url = "$baseUrl/upload/drive/v3/files/$fileId?uploadType=media"
                val request = authedRequest(url, accessToken)
                    .patch(json.toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                executeChecked(request).close()
                return@withContext fileId
            }

            val metadata = """{"name":"$DRIVE_SYNC_FILENAME","mimeType":"application/json"}"""
            val multipartBody =
                "--$UPLOAD_BOUNDARY\r\nContent-Type: application/json\r\n\r\n$metadata\r\n" +
                    "--$UPLOAD_BOUNDARY\r\nContent-Type: application/json\r\n\r\n$json\r\n--$UPLOAD_BOUNDARY--"
            val url = "$baseUrl/upload/drive/v3/files?uploadType=multipart"
            val mediaType = "multipart/related; boundary=$UPLOAD_BOUNDARY".toMediaType()
            val request = authedRequest(url, accessToken)
                .post(multipartBody.toRequestBody(mediaType))
                .build()
            executeChecked(request).use { resp ->
                val id = XyloJson.parseToJsonElement(resp.body.string()).jsonObject["id"]?.jsonPrimitive?.content
                id ?: throw DriveApiException("No id in Drive's create-file response")
            }
        }

    private fun authedRequest(url: String, accessToken: String): Request.Builder =
        Request.Builder().url(url).header("Authorization", "Bearer $accessToken")

    private fun executeChecked(request: Request): Response {
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val code = response.code
            response.close()
            throw DriveApiException("Drive request failed ($code)", code)
        }
        return response
    }
}
