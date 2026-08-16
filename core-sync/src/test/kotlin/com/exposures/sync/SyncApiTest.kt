package com.exposures.sync

import com.exposures.sync.dto.ExposureSyncDto
import com.exposures.sync.dto.ShutterSpeedSyncDto
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncApiTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun api(authProvider: AuthProvider = NoOpAuthProvider) =
        SyncApiFactory.create(server.url("/").toString(), authProvider)

    private fun exposureDto() = ExposureSyncDto(
        id = "exp-1",
        filmRollId = "roll-1",
        frameNumber = 3,
        lensId = "lens-1",
        shutterSpeed = ShutterSpeedSyncDto("FRACTION", 1, 125),
        aperture = 8.0,
        isoUsed = 400,
        zone = 6,
        notes = "backlit",
        capturedAt = 1000L,
    )

    @Test
    fun `uploadExposure posts to the exposures path with a JSON body and returns the remote id`() = runTest {
        server.enqueue(MockResponse().setBody("""{"remoteId":"server-exp-1"}"""))

        val ack = api().uploadExposure(exposureDto())

        assertEquals("server-exp-1", ack.remoteId)
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/exposures", recorded.path)
        assertTrue(recorded.body.readUtf8().contains("\"id\":\"exp-1\""))
    }

    @Test
    fun `uploadReferencePhoto posts multipart to the exposure's reference-photo path`() = runTest {
        server.enqueue(MockResponse().setBody("""{"remoteUrl":"https://cdn.example/exp-1.jpg"}"""))
        val photo = MultipartBody.Part.createFormData(
            "photo",
            "exp-1.jpg",
            "fake-jpeg-bytes".toRequestBody("image/jpeg".toMediaType()),
        )

        val ack = api().uploadReferencePhoto("exp-1", photo)

        assertEquals("https://cdn.example/exp-1.jpg", ack.remoteUrl)
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/exposures/exp-1/reference-photo", recorded.path)
        assertTrue(recorded.headers["Content-Type"]?.contains("multipart/form-data") == true)
    }

    @Test
    fun `requests carry the auth provider's header when one is supplied`() = runTest {
        server.enqueue(MockResponse().setBody("""{"remoteId":"server-exp-1"}"""))
        val authProvider = object : AuthProvider {
            override fun authHeader() = "Bearer test-token"
        }

        api(authProvider).uploadExposure(exposureDto())

        assertEquals("Bearer test-token", server.takeRequest().headers["Authorization"])
    }

    @Test
    fun `requests carry no auth header when the provider is a no-op`() = runTest {
        server.enqueue(MockResponse().setBody("""{"remoteId":"server-exp-1"}"""))

        api(NoOpAuthProvider).uploadExposure(exposureDto())

        assertNull(server.takeRequest().headers["Authorization"])
    }
}
