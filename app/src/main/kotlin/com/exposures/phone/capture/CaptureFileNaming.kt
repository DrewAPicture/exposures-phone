package com.exposures.phone.capture

import java.io.File

/** Where a reference photo gets written: `<baseDir>/Exposures/<filmRollId>/<exposureId>.jpg`. */
object CaptureFileNaming {
    /** Shared with the Q+ MediaStore path's `RELATIVE_PATH` construction in
     * `CaptureForegroundService.createCaptureDestination` so the two write paths can't drift apart
     * again — this fallback path used to omit the root folder entirely. */
    const val ROOT_DIR_NAME = "Exposures"

    fun outputFile(baseDir: File, filmRollId: String, exposureId: String): File =
        File(File(File(baseDir, ROOT_DIR_NAME), filmRollId), "$exposureId.jpg")
}
