package com.originisle.android.ui

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException

/**
 * Exposes the downloaded update APK to Android's PackageInstaller
 * via content://${applicationId}.ltmori.updates/release.apk
 */
class LtmApkProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String = "application/vnd.android.package-archive"

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if ("/release.apk" != uri.path || "r" != mode) {
            throw FileNotFoundException("Invalid URI or mode: $uri, mode: $mode")
        }
        val file = File(context!!.cacheDir, "ltmori-update.apk")
        if (!file.exists()) {
            throw FileNotFoundException("Update APK not found at ${file.absolutePath}")
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        args: Array<out String>?,
        sort: String?,
    ): Cursor {
        val columns = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        val result = MatrixCursor(columns)
        val file = File(context!!.cacheDir, "ltmori-update.apk")
        if ("/release.apk" != uri.path || !file.isFile) return result

        val row = arrayOfNulls<Any>(columns.size)
        for (i in columns.indices) {
            if (OpenableColumns.DISPLAY_NAME == columns[i]) row[i] = "LTMori-update.apk"
            if (OpenableColumns.SIZE == columns[i]) row[i] = file.length()
        }
        result.addRow(row)
        return result
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        throw UnsupportedOperationException()
    }

    override fun delete(uri: Uri, selection: String?, args: Array<out String>?): Int {
        throw UnsupportedOperationException()
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?): Int {
        throw UnsupportedOperationException()
    }
}
