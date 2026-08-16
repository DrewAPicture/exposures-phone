package com.exposures.sync

import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class SyncDrainerTest {

    @Test
    fun `every item that uploads successfully is reported and marked synced`() = runTest {
        val synced = mutableListOf<String>()
        val drainer = SyncDrainer()

        val result = drainer.drain(
            items = listOf("a", "b", "c"),
            upload = { it -> "remote-$it" },
            onSuccess = { item, remoteId -> synced.add("$item:$remoteId") },
            onFailure = { _, _ -> error("should not fail") },
        )

        assertEquals(DrainResult(succeeded = 3, failed = 0), result)
        assertEquals(listOf("a:remote-a", "b:remote-b", "c:remote-c"), synced)
    }

    @Test
    fun `a network failure on one item is recorded as a failure without stopping the batch`() = runTest {
        val failed = mutableListOf<String>()
        val succeeded = mutableListOf<String>()
        val drainer = SyncDrainer()

        val result = drainer.drain(
            items = listOf("a", "b", "c"),
            upload = { item -> if (item == "b") throw IOException("offline") else "remote-$item" },
            onSuccess = { item, _ -> succeeded.add(item) },
            onFailure = { item, error -> failed.add("$item:$error") },
        )

        assertEquals(DrainResult(succeeded = 2, failed = 1), result)
        assertEquals(listOf("a", "c"), succeeded)
        assertEquals(listOf("b:offline"), failed)
    }

    @Test
    fun `an HTTP error failure is reported with the status code`() = runTest {
        val failed = mutableListOf<String>()
        val drainer = SyncDrainer()
        val httpException = HttpException(Response.error<Unit>(500, "".toResponseBody(null)))

        drainer.drain(
            items = listOf("a"),
            upload = { throw httpException },
            onSuccess = { _, _ -> error("should not succeed") },
            onFailure = { item, error -> failed.add("$item:$error") },
        )

        assertEquals(listOf("a:HTTP 500"), failed)
    }

    @Test
    fun `an empty batch drains to a zero result without calling either callback`() = runTest {
        val drainer = SyncDrainer()

        val result = drainer.drain<String>(
            items = emptyList(),
            upload = { error("should not be called") },
            onSuccess = { _, _ -> error("should not be called") },
            onFailure = { _, _ -> error("should not be called") },
        )

        assertEquals(DrainResult(0, 0), result)
    }
}
