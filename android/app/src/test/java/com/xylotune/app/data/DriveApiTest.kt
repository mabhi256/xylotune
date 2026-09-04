package com.xylotune.app.data

import com.xylotune.app.model.SyncFile
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

// DriveApi's baseUrl is overridable specifically so these can exercise the real request-
// building code (headers, method choice, multipart body layout, JSON field extraction)
// against a fake server instead of a live network call — see DriveApi's docstring.
class DriveApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: DriveApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val baseUrl = server.url("/").toString().trimEnd('/')
        api = DriveApi(client = OkHttpClient(), baseUrl = baseUrl)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `findSyncFileId sends the bearer token and the exact query string`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"files":[{"id":"abc123"}]}"""))
        val id = api.findSyncFileId("token-xyz")
        assertEquals("abc123", id)

        val request = server.takeRequest()
        assertEquals("Bearer token-xyz", request.getHeader("Authorization"))
        assertTrue(request.path!!.contains("q=name%3D%27$DRIVE_SYNC_FILENAME%27+and+trashed%3Dfalse"))
    }

    @Test
    fun `findSyncFileId returns null when there are no matching files`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"files":[]}"""))
        assertNull(api.findSyncFileId("token"))
    }

    @Test
    fun `downloadSyncFile requests alt=media and decodes the body`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"songs":{},"tombstones":{}}"""))
        val sync = api.downloadSyncFile("file1", "token")
        assertEquals(SyncFile(), sync)
        val request = server.takeRequest()
        assertTrue(request.path!!.contains("alt=media"))
    }

    @Test
    fun `uploadSyncFile with an existing id PATCHes the media endpoint`() = runBlocking {
        server.enqueue(MockResponse().setBody("{}"))
        val id = api.uploadSyncFile("file1", SyncFile(), "token")
        assertEquals("file1", id)
        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertTrue(request.path!!.contains("uploadType=media"))
        assertTrue(request.getHeader("Content-Type")!!.startsWith("application/json"))
    }

    @Test
    fun `uploadSyncFile with no id POSTs a multipart body and returns the new id`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"id":"new-id"}"""))
        val id = api.uploadSyncFile(null, SyncFile(), "token")
        assertEquals("new-id", id)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertTrue(request.path!!.contains("uploadType=multipart"))
        val contentType = request.getHeader("Content-Type")!!
        assertTrue(contentType.startsWith("multipart/related; boundary="))
        val body = request.body.readUtf8()
        assertTrue(body.contains(""""name":"$DRIVE_SYNC_FILENAME""""))
        assertTrue(body.contains(""""mimeType":"application/json""""))
    }

    @Test
    fun `a non-2xx response throws DriveApiException with the status code`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401))
        try {
            api.findSyncFileId("token")
            fail("expected DriveApiException")
        } catch (e: DriveApiException) {
            assertEquals(401, e.statusCode)
        }
    }
}
