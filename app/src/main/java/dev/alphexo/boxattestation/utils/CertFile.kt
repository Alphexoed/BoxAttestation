package dev.alphexo.boxattestation.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.util.regex.Pattern

class CertFile {
    fun cleanCertificate(input: String): String {
        val certificatePattern = Pattern.compile(
            "-----BEGIN CERTIFICATE-----(.*?)-----END CERTIFICATE-----".trimIndent(),
            Pattern.DOTALL
        )

        val matcher = certificatePattern.matcher(input)

        return if (matcher.find()) {
            val guh = matcher.group(1)!!.trimIndent().replace("\n", "").replace(" ", "")
            Log.v("CertFile", "cleanCertificate: $guh")
            guh
        } else {
            ""
        }
    }

    @SuppressLint("Range")
    fun getFileName(context: Context, uri: Uri): String {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val fileName =
                        cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    return fileName
                }
            }
        }

        return uri.path?.let { path ->
            File(path).name
        } ?: "unknown"
    }
}