package com.exposures.phone.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Writes a CSV to the cache dir and launches a share sheet for it — see the "csv" cache-path in res/xml/file_paths.xml. */
object CsvFileSharer {
    fun share(context: Context, csv: String, fileName: String) {
        val dir = File(context.cacheDir, "csv").apply { mkdirs() }
        val file = File(dir, fileName).apply { writeText(csv) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export CSV"))
    }
}
