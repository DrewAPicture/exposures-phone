package com.exposures.sync.contract

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * CI drift check for the checked-in phone HTTP OpenAPI spec. Regenerates the spec from [com.exposures.sync.SyncApi]
 * and requires it to match `docs/openapi/sync-api.json` byte-for-byte.
 *
 * After an intentional SyncApi/DTO change, regenerate with:
 * `./gradlew :core-sync:test -PupdateOpenApiSpec`
 */
class OpenApiSpecDriftTest {

    @Test
    fun `checked-in OpenAPI spec matches the spec generated from SyncApi and DTO descriptors`() {
        val generated = OpenApiSpecGenerator.render()
        val specFile = locateOpenApiSpec()
        if (System.getProperty("updateOpenApiSpec") == "true") {
            specFile.parentFile?.mkdirs()
            specFile.writeText(generated)
            return
        }
        check(specFile.isFile) {
            "Missing ${specFile.absolutePath}. Generate it with " +
                "./gradlew :core-sync:test -PupdateOpenApiSpec"
        }
        assertEquals(
            "OpenAPI spec drifted from SyncApi/DTO source. If the change is intentional, " +
                "regenerate with ./gradlew :core-sync:test -PupdateOpenApiSpec and include " +
                "the spec diff in the same commit.",
            specFile.readText(),
            generated,
        )
    }

    companion object {
        private const val RELATIVE_SPEC_PATH = "docs/openapi/sync-api.json"

        fun locateOpenApiSpec(): File {
            System.getProperty("sync.openapi.spec")?.let { return File(it) }
            var dir = File(System.getProperty("user.dir") ?: ".").absoluteFile
            while (true) {
                val atRepoRoot = File(dir, "settings.gradle.kts").isFile &&
                    File(dir, "core-sync").isDirectory
                if (atRepoRoot) return File(dir, RELATIVE_SPEC_PATH)
                dir = dir.parentFile ?: error(
                    "Could not locate $RELATIVE_SPEC_PATH from ${System.getProperty("user.dir")}",
                )
            }
        }
    }
}
