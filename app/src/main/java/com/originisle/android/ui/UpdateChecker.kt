package com.originisle.android.ui

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Checks GitHub Releases API for LTMori updates, downloads the APK directly
 * with progress callbacks, and launches Android's PackageInstaller.
 */
object UpdateChecker {
    private const val TAG = "UpdateChecker"
    private const val REPO_PUBLIC = "tranlocbtr99/DynAmic_Origin"
    private const val REPO_PRIVATE = "tranlocbtr99/DynAmic_Origin-private"
    private const val API_URL = "https://api.github.com/repos/$REPO_PUBLIC/releases/latest"
    private const val API_PRIVATE_URL = "https://api.github.com/repos/$REPO_PRIVATE/releases/latest"
    private const val APK_MIME = "application/vnd.android.package-archive"
    private const val MAX_APK_SIZE = 150L * 1024 * 1024

    sealed class Result {
        data class UpToDate(val current: String) : Result()
        data class UpdateAvailable(
            val current: String,
            val latest: String,
            val releaseNotes: String,
            val downloadUrl: String,
            val apkSize: Long,
        ) : Result()
        data class Error(val message: String) : Result()
    }

    fun currentVersion(context: Context): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "3.0.1"

    suspend fun check(context: Context): Result = withContext(Dispatchers.IO) {
        val current = currentVersion(context)
        try {
            val json = fetchReleaseJson(API_URL) ?: fetchReleaseJson(API_PRIVATE_URL)
                ?: return@withContext Result.Error("Không tìm thấy bản phát hành trên GitHub.")

            val tagName = json.optString("tag_name", "").trim()
            val latestVersion = tagName.removePrefix("v").trim()
            if (latestVersion.isBlank()) {
                return@withContext Result.Error("Dữ liệu phiên bản không hợp lệ.")
            }

            if (!isNewer(latestVersion, current)) {
                return@withContext Result.UpToDate(current)
            }

            val body = json.optString("body", "").trim()
                .ifBlank { "Bản cập nhật tối ưu hóa hệ thống và bổ sung tính năng mới." }

            // Find APK asset
            val assets = json.optJSONArray("assets")
            var downloadUrl = ""
            var apkSize = 0L

            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url", "")
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            if (downloadUrl.isBlank()) {
                downloadUrl = "https://github.com/$REPO_PUBLIC/releases/tag/$tagName"
            }

            Result.UpdateAvailable(
                current = current,
                latest = latestVersion,
                releaseNotes = body,
                downloadUrl = downloadUrl,
                apkSize = apkSize,
            )
        } catch (t: Throwable) {
            Log.e(TAG, "Update check failed", t)
            Result.Error(t.localizedMessage ?: "Lỗi kết nối GitHub")
        }
    }

    private fun fetchReleaseJson(urlString: String): JSONObject? {
        return runCatching {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpsURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.setRequestProperty("User-Agent", "LTMori-App")
            conn.instanceFollowRedirects = true
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                conn.disconnect()
                return null
            }
            conn.inputStream.use { input ->
                val bytes = input.readBytes()
                JSONObject(String(bytes, Charsets.UTF_8))
            }
        }.getOrNull()
    }

    /** "3.0.2" > "3.0.1" comparison */
    private fun isNewer(latest: String, current: String): Boolean {
        val l = latest.split(".").map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
        val c = current.split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(l.size, c.size)) {
            val lv = l.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (lv != cv) return lv > cv
        }
        return false
    }

    suspend fun downloadAndInstall(
        context: Context,
        downloadUrl: String,
        expectedSize: Long,
        onProgress: (Int) -> Unit,
    ): kotlin.Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cacheFile = File(context.cacheDir, "ltmori-update.apk")
            if (cacheFile.exists()) cacheFile.delete()

            val tempFile = File.createTempFile("ltmori-dl-", ".apk", context.cacheDir)
            val url = URL(downloadUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 20000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "LTMori-App")
            }

            var actualConn = conn
            var code = actualConn.responseCode
            if (code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_MOVED_PERM || code == 307 || code == 308) {
                val newUrl = actualConn.getHeaderField("Location")
                actualConn = (URL(newUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 20000
                    setRequestProperty("User-Agent", "LTMori-App")
                }
            }

            val totalSize = if (expectedSize > 0) expectedSize else actualConn.contentLengthLong
            actualConn.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = bytearray(32768)
                    var downloaded = 0L
                    var lastPercent = -1
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (totalSize > 0) {
                            val percent = (downloaded * 100 / totalSize).toInt().coerceIn(0, 100)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                withContext(Dispatchers.Main) { onProgress(percent) }
                            }
                        }
                    }
                }
            }
            actualConn.disconnect()

            tempFile.renameTo(cacheFile)

            // Prompt package installation
            withContext(Dispatchers.Main) {
                launchInstaller(context, cacheFile)
            }
        }
    }

    private fun bytearray(size: Int) = ByteArray(size)

    fun launchInstaller(context: Context, file: File) {
        val pm = context.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!pm.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }
        }

        val uri = Uri.parse("content://${context.packageName}.ltmori.updates/release.apk")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            clipData = ClipData.newRawUri("LTMori APK", uri)
        }
        context.startActivity(intent)
    }
}
