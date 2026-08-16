package com.exposures.phone.capture

import java.io.File

/** Where a reference photo gets written: `<baseDir>/<filmRollId>/<exposureId>.jpg`. */
object CaptureFileNaming {
    fun outputFile(baseDir: File, filmRollId: String, exposureId: String): File =
        File(File(baseDir, filmRollId), "$exposureId.jpg")
}
