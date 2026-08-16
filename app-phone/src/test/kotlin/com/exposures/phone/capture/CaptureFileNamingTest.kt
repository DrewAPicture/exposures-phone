package com.exposures.phone.capture

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class CaptureFileNamingTest {

    @Test
    fun `output file nests under the film roll id and is named for the exposure id`() {
        val baseDir = File("/base")

        val file = CaptureFileNaming.outputFile(baseDir, filmRollId = "roll-1", exposureId = "exp-1")

        assertEquals(File("/base/roll-1/exp-1.jpg"), file)
    }

    @Test
    fun `different exposures on the same roll get different files in the same directory`() {
        val baseDir = File("/base")

        val first = CaptureFileNaming.outputFile(baseDir, "roll-1", "exp-1")
        val second = CaptureFileNaming.outputFile(baseDir, "roll-1", "exp-2")

        assertEquals(first.parentFile, second.parentFile)
        assertEquals("exp-1.jpg", first.name)
        assertEquals("exp-2.jpg", second.name)
    }
}
