package com.mydailylife.schedule.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.mydailylife.schedule.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

sealed class UpdateCheckResult {
    data object UpToDate : UpdateCheckResult()
    data class Available(val manifest: UpdateManifest) : UpdateCheckResult()
}

class AppUpdater(context: Context) {
    private val appContext = context.applicationContext
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val updateDir: File
        get() = File(appContext.cacheDir, "updates").also { it.mkdirs() }

    suspend fun checkForUpdate(
        manifestUrl: String = BuildConfig.UPDATE_MANIFEST_URL,
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        val text = httpGetText(manifestUrl)
        val manifest = json.decodeFromString(UpdateManifest.serializer(), text)
        if (manifest.versionCode > BuildConfig.VERSION_CODE) {
            UpdateCheckResult.Available(manifest)
        } else {
            UpdateCheckResult.UpToDate
        }
    }

    /**
     * Downloads APK to cache. [onProgress] receives 0f..1f, or -1f when length unknown.
     */
    suspend fun downloadApk(
        manifest: UpdateManifest,
        onProgress: (Float) -> Unit = {},
    ): File = withContext(Dispatchers.IO) {
        val dest = File(updateDir, "MyDailyLife-update.apk")
        if (dest.exists()) dest.delete()

        val connection = openConnection(manifest.apkUrl)
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                error("下载失败（HTTP $code）")
            }
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                dest.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER)
                    var readTotal = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n <= 0) break
                        output.write(buffer, 0, n)
                        readTotal += n
                        if (total > 0L) {
                            onProgress((readTotal.toFloat() / total.toFloat()).coerceIn(0f, 1f))
                        } else {
                            onProgress(-1f)
                        }
                    }
                    output.flush()
                }
            }
        } finally {
            connection.disconnect()
        }

        if (!dest.exists() || dest.length() == 0L) {
            error("下载文件为空")
        }
        val expected = manifest.sha256?.trim()?.lowercase()
        if (!expected.isNullOrEmpty()) {
            val actual = sha256Hex(dest)
            if (actual != expected) {
                dest.delete()
                error("安装包校验失败")
            }
        }
        onProgress(1f)
        dest
    }

    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun installPermissionSettingsIntent(): Intent {
        return Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${appContext.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun installApk(apkFile: File) {
        val uri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            apkFile,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        appContext.startActivity(intent)
    }

    private fun httpGetText(url: String): String {
        val connection = openConnection(url)
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                error("检查更新失败（HTTP $code）")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(urlString: String): HttpURLConnection {
        var current = urlString
        repeat(MAX_REDIRECTS) {
            val connection = (URL(current).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 20_000
                readTimeout = 120_000
                requestMethod = "GET"
                setRequestProperty("Accept", "*/*")
                setRequestProperty(
                    "User-Agent",
                    "MyDailyLife/${BuildConfig.VERSION_NAME} (${appContext.packageName})",
                )
            }
            connection.connect()
            val code = connection.responseCode
            if (code in 300..399) {
                val location = connection.getHeaderField("Location")
                connection.disconnect()
                if (location.isNullOrBlank()) {
                    error("下载重定向失败（HTTP $code）")
                }
                current = resolveRedirect(current, location)
            } else {
                return connection
            }
        }
        error("下载重定向次数过多")
    }

    private fun resolveRedirect(currentUrl: String, location: String): String {
        return if (location.startsWith("http://", ignoreCase = true) ||
            location.startsWith("https://", ignoreCase = true)
        ) {
            location
        } else {
            URL(URL(currentUrl), location).toString()
        }
    }

    private fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER)
            while (true) {
                val n = input.read(buffer)
                if (n <= 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { b -> "%02x".format(b) }
    }

    companion object {
        private const val DEFAULT_BUFFER = 64 * 1024
        private const val MAX_REDIRECTS = 8
    }
}
