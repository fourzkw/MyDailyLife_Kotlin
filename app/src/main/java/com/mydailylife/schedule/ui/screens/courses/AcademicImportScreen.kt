package com.mydailylife.schedule.ui.screens.courses

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mydailylife.schedule.data.CourseGridDefaults
import com.mydailylife.schedule.data.CourseItem
import com.mydailylife.schedule.data.CourseRepository
import com.mydailylife.schedule.data.academic.AcademicCaptureJs
import com.mydailylife.schedule.data.academic.AcademicCaptureResult
import com.mydailylife.schedule.data.academic.AcademicHttpFetch
import com.mydailylife.schedule.data.academic.AcademicPortals
import com.mydailylife.schedule.data.academic.AcademicSchools
import com.mydailylife.schedule.data.academic.AcademicTimetableParser
import com.mydailylife.schedule.data.academic.BnuTimetableHtmlParser
import com.mydailylife.schedule.ui.components.MdlTopAppBar
import com.mydailylife.schedule.ui.components.PrimaryPillButton
import com.mydailylife.schedule.ui.theme.Canvas
import com.mydailylife.schedule.ui.theme.Ink
import com.mydailylife.schedule.ui.theme.Muted
import com.mydailylife.schedule.ui.theme.SurfaceSoft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference
import android.os.Message
import android.webkit.WebView.WebViewTransport
import kotlin.coroutines.resume

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicImportScreen(
    schoolId: String,
    courseRepository: CourseRepository,
    onBack: () -> Unit,
    onImported: () -> Unit,
) {
    val school = remember(schoolId) { AcademicSchools.findById(schoolId) }
    val portal = remember(schoolId) { AcademicPortals.forSchoolId(schoolId) }
    if (school == null || portal == null) {
        UnsupportedSchoolScreen(
            schoolName = school?.name,
            onBack = onBack,
        )
        return
    }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var progress by remember { mutableFloatStateOf(0f) }
    var capturing by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(true) }
    var timetableBuffered by remember { mutableStateOf(false) }
    var pendingCourses by remember { mutableStateOf<List<CourseItem>?>(null) }
    var sourceHint by remember { mutableStateOf("") }
    val webViewRef = remember { AtomicReference<WebView?>(null) }
    val latestNetworkBody = remember { AtomicReference<String?>(null) }
    val latestTimetableHtml = remember { AtomicReference<String?>(null) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    fun rememberPayload(url: String, body: String) {
        if (body.isBlank()) return
        latestNetworkBody.set(body)
        if (BnuTimetableHtmlParser.looksLikeBnuTimetable(body)) {
            latestTimetableHtml.set(body)
            mainHandler.post { timetableBuffered = true }
        }
    }

    /** Install hooks → push mytable → capture JSON (bridge fills buffers asynchronously). */
    suspend fun suspendCaptureRaw(webView: WebView): String =
        suspendCancellableCoroutine { cont ->
            webView.evaluateJavascript(AcademicCaptureJs.INSTALL_HOOKS) {
                webView.evaluateJavascript(AcademicCaptureJs.PUSH_TIMETABLE_HTML) {
                    mainHandler.postDelayed({
                        webView.evaluateJavascript(AcademicCaptureJs.CAPTURE_NOW) { raw ->
                            if (cont.isActive) cont.resume(raw ?: "")
                        }
                    }, 150)
                }
            }
        }

    fun runCapture(webView: WebView) {
        capturing = true
        scope.launch {
            try {
                val pageUrl = webView.url.orEmpty()

                // 1) If already on xskcb document, re-GET with cookies (GBK).
                if (pageUrl.isNotBlank() && AcademicHttpFetch.isBnuTimetableUrl(pageUrl)) {
                    val html = withContext(Dispatchers.IO) {
                        AcademicHttpFetch.fetchHtml(pageUrl)
                    }
                    if (!html.isNullOrBlank()) {
                        rememberPayload(pageUrl, html)
                        val courses = withContext(Dispatchers.Default) {
                            AcademicTimetableParser.parseBody(html)
                        }
                        if (courses.isNotEmpty()) {
                            pendingCourses = courses
                            sourceHint = "bnu-http"
                            return@launch
                        }
                    }
                }

                // 2) Push DOM #mytable via bridge (incl. frameset children), then parse.
                val raw = withContext(Dispatchers.Main) {
                    suspendCaptureRaw(webView)
                }
                val result = withContext(Dispatchers.Default) {
                    val bufferedHtml = latestTimetableHtml.get()
                    if (!bufferedHtml.isNullOrBlank()) {
                        val courses = AcademicTimetableParser.parseBody(bufferedHtml)
                        if (courses.isNotEmpty()) {
                            return@withContext AcademicCaptureResult(
                                courses = courses,
                                sourceHint = "bnu-buffer",
                            )
                        }
                    }
                    runCatching {
                        AcademicTimetableParser.parseCapturePayload(raw)
                    }.recoverCatching { firstError ->
                        val buffered = latestNetworkBody.get()
                        if (buffered.isNullOrBlank()) throw firstError
                        val courses = AcademicTimetableParser.parseBody(buffered)
                        if (courses.isEmpty()) throw firstError
                        AcademicCaptureResult(
                            courses = courses,
                            sourceHint = "network-buffer",
                        )
                    }.getOrThrow()
                }
                pendingCourses = result.courses
                sourceHint = result.sourceHint
            } catch (e: Exception) {
                val onHomes = webView.url.orEmpty().contains("homes.html")
                val tip = if (onHomes && latestTimetableHtml.get().isNullOrBlank()) {
                    "请先打开「我的课表」并检索出课表网格，再点捕获"
                } else {
                    e.message ?: "捕获失败"
                }
                snackbar.showSnackbar(tip)
            } finally {
                capturing = false
            }
        }
    }

    if (showGuide) {
        AlertDialog(
            onDismissRequest = { showGuide = false },
            title = { Text("教务导入 · ${portal.schoolLabel}") },
            text = {
                Column {
                    portal.guidanceSteps.forEachIndexed { index, step ->
                        Text(
                            text = "${index + 1}. $step",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ink,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "账号密码仅在学校页面输入，应用不会上传或保存教务密码。",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showGuide = false }) { Text("开始") }
            },
        )
    }

    pendingCourses?.let { courses ->
        val preview = courses.take(6).joinToString("\n") {
            "· ${it.title} 周${it.weekday} ${it.startSlot}-${it.endSlot}节"
        } + if (courses.size > 6) "\n…" else ""
        TermStartConfirmDialog(
            title = "确认导入",
            summary = "识别到 ${courses.size} 门课（来源：$sourceHint）。导入将替换当前课表。\n\n$preview",
            initialTermStart = courseRepository.store.value.termStartDate
                ?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
                ?: CourseGridDefaults.defaultTermStart(),
            onDismiss = { pendingCourses = null },
            onConfirm = { termStart ->
                scope.launch {
                    val maxWeek = courses.flatMap { it.teachingWeeks }.maxOrNull() ?: 25
                    courseRepository.replaceAll(
                        items = courses,
                        termStartDate = termStart,
                        maxTeachingWeek = maxWeek,
                    )
                    pendingCourses = null
                    snackbar.showSnackbar("已导入 ${courses.size} 条排课")
                    onImported()
                }
            },
        )
    }

    Scaffold(
        topBar = {
            MdlTopAppBar(
                title = "教务导入 · ${school.name}",
                onBack = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (progress in 0f..<1f) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                text = when {
                    timetableBuffered -> "已缓存课表页面，可点下方「捕获课表」导入"
                    else -> "登录后选「按课表显示」并检索；表格出现后点捕获"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            AcademicWebView(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(SurfaceSoft),
                entryUrl = portal.entryUrl,
                onProgress = { progress = it },
                onWebViewReady = { webViewRef.set(it) },
                onNetworkPayload = { url, body -> rememberPayload(url, body) },
                onDocumentUrl = { url ->
                    if (!AcademicHttpFetch.isBnuTimetableUrl(url)) return@AcademicWebView
                    val pageUrl = webViewRef.get()?.url
                    scope.launch(Dispatchers.IO) {
                        val html = AcademicHttpFetch.fetchHtml(url, pageUrl)
                        if (!html.isNullOrBlank()) {
                            rememberPayload(url, html)
                        }
                    }
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
            PrimaryPillButton(
                text = if (capturing) "捕获中…" else "捕获课表",
                onClick = {
                    val wv = webViewRef.get()
                    if (wv == null) {
                        scope.launch { snackbar.showSnackbar("页面尚未就绪") }
                    } else {
                        runCapture(wv)
                    }
                },
                enabled = !capturing,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun AcademicWebView(
    entryUrl: String,
    modifier: Modifier = Modifier,
    onProgress: (Float) -> Unit,
    onWebViewReady: (WebView) -> Unit,
    onNetworkPayload: (url: String, body: String) -> Unit,
    onDocumentUrl: (url: String) -> Unit = {},
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.setSupportMultipleWindows(true)
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.userAgentString = settings.userAgentString.replace("; wv", "")

                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onNetworkPayload(url: String, body: String) {
                            onNetworkPayload(url, body)
                        }
                    },
                    AcademicCaptureJs.BRIDGE_NAME,
                )

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgress(newProgress / 100f)
                    }

                    // 「检索 / 电脑端」常会 window.open；把新窗口导航拉回当前 WebView。
                    override fun onCreateWindow(
                        view: WebView?,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: Message?,
                    ): Boolean {
                        val host = view ?: return false
                        val trampoline = WebView(host.context)
                        trampoline.webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                v: WebView?,
                                request: WebResourceRequest?,
                            ): Boolean {
                                val next = request?.url?.toString().orEmpty()
                                if (next.isNotBlank()) host.loadUrl(next)
                                return true
                            }
                        }
                        (resultMsg?.obj as? WebViewTransport)?.webView = trampoline
                        resultMsg?.sendToTarget()
                        return true
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): Boolean = false

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): android.webkit.WebResourceResponse? {
                        val url = request?.url?.toString().orEmpty()
                        if (AcademicHttpFetch.isBnuTimetableUrl(url)) {
                            // Subframe 导航不会走 onPageFinished；在此触发 Cookie 回拉。
                            Handler(Looper.getMainLooper()).post {
                                onDocumentUrl(url)
                            }
                        }
                        return null
                    }

                    override fun doUpdateVisitedHistory(
                        view: WebView?,
                        url: String?,
                        isReload: Boolean,
                    ) {
                        super.doUpdateVisitedHistory(view, url, isReload)
                        if (!url.isNullOrBlank()) onDocumentUrl(url)
                        view?.postDelayed({
                            view.evaluateJavascript(AcademicCaptureJs.PUSH_TIMETABLE_HTML, null)
                        }, 500)
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        view?.evaluateJavascript(AcademicCaptureJs.INSTALL_HOOKS, null)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        view?.evaluateJavascript(AcademicCaptureJs.INSTALL_HOOKS, null)
                        if (!url.isNullOrBlank()) onDocumentUrl(url)
                        // frameset 内课表晚于顶层加载；多次扫全部 frame。
                        listOf(400L, 1200L, 2500L, 4000L).forEach { delay ->
                            view?.postDelayed({
                                view.evaluateJavascript(AcademicCaptureJs.PUSH_TIMETABLE_HTML, null)
                            }, delay)
                        }
                    }
                }
                onWebViewReady(this)
                loadUrl(entryUrl)
            }
        },
        update = { webView ->
            onWebViewReady(webView)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnsupportedSchoolScreen(
    schoolName: String?,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            MdlTopAppBar(title = "教务导入", onBack = onBack)
        },
        containerColor = Canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Text(
            text = if (schoolName.isNullOrBlank()) {
                "未找到该学校，请返回重新选择"
            } else {
                "「$schoolName」暂未接入教务导入"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
        )
    }
}
