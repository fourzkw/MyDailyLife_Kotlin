package com.mydailylife.schedule.data.academic

import android.webkit.CookieManager
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

/** Re-fetch academic document HTML using the WebView cookie jar (for document navigations). */
object AcademicHttpFetch {
    fun isBnuTimetableUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val u = url.lowercase()
        return "xskcb" in u || u.contains("wsxk.xskcb")
    }

    /** Turn VPN-relative paths into absolute https://onevpn… URLs. */
    fun normalizeUrl(url: String, pageUrl: String? = null): String {
        val trimmed = url.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        val base = pageUrl?.takeIf { it.startsWith("http") } ?: "https://onevpn.bnu.edu.cn/"
        return runCatching { URL(URL(base), trimmed).toString() }.getOrElse {
            if (trimmed.startsWith("/")) "https://onevpn.bnu.edu.cn$trimmed"
            else "https://onevpn.bnu.edu.cn/$trimmed"
        }
    }

    /**
     * GET [url] with WebView cookies. BNU pages are often `charset=GBK`.
     * Returns null on failure.
     */
    fun fetchHtml(url: String, pageUrl: String? = null): String? = runCatching {
        val absolute = normalizeUrl(url, pageUrl)
        val conn = (URL(absolute).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 20_000
            readTimeout = 20_000
            requestMethod = "GET"
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
            )
            setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9")
            val cookie = CookieManager.getInstance().getCookie(absolute)
                ?: CookieManager.getInstance().getCookie("https://onevpn.bnu.edu.cn/")
            if (!cookie.isNullOrBlank()) {
                setRequestProperty("Cookie", cookie)
            }
        }
        conn.connect()
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val bytes = stream?.readBytes() ?: return@runCatching null
        val contentType = conn.contentType.orEmpty()
        val charsetName = Regex("""charset\s*=\s*([^\s;]+)""", RegexOption.IGNORE_CASE)
            .find(contentType)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.trim('"', '\'')
            ?: "GBK"
        val charset = runCatching { Charset.forName(charsetName) }.getOrElse { Charset.forName("GBK") }
        String(bytes, charset)
    }.getOrNull()
}
