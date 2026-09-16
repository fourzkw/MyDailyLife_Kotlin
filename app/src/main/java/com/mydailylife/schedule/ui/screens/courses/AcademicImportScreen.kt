package com.mydailylife.schedule.ui.screens.courses

import android.annotation.SuppressLint
import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.mydailylife.schedule.data.academic.AcademicTimetableParser
import com.mydailylife.schedule.data.academic.CquPortal
import com.mydailylife.schedule.ui.components.PrimaryPillButton
import com.mydailylife.schedule.ui.theme.Canvas
import com.mydailylife.schedule.ui.theme.Ink
import com.mydailylife.schedule.ui.theme.Muted
import com.mydailylife.schedule.ui.theme.SurfaceSoft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicImportScreen(
    courseRepository: CourseRepository,
    onBack: () -> Unit,
    onImported: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var progress by remember { mutableFloatStateOf(0f) }
    var capturing by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(true) }
    var pendingCourses by remember { mutableStateOf<List<CourseItem>?>(null) }
    var sourceHint by remember { mutableStateOf("") }
    val webViewRef = remember { AtomicReference<WebView?>(null) }
    val latestNetworkBody = remember { AtomicReference<String?>(null) }

    fun runCapture(webView: WebView) {
        capturing = true
        // Re-install hooks in case SPA navigated without a full reload.
        webView.evaluateJavascript(AcademicCaptureJs.INSTALL_HOOKS) {
            webView.evaluateJavascript(AcademicCaptureJs.CAPTURE_NOW) { raw ->
                scope.launch {
                    try {
                        val result = withContext(Dispatchers.Default) {
                            runCatching {
                                AcademicTimetableParser.parseCapturePayload(raw ?: "")
                            }.recoverCatching { firstError ->
                                val buffered = latestNetworkBody.get()
                                if (buffered.isNullOrBlank()) throw firstError
                                val courses = AcademicTimetableParser.parseJsonBlob(buffered)
                                if (courses.isEmpty()) throw firstError
                                AcademicCaptureResult(courses = courses, sourceHint = "network-buffer")
                            }.getOrThrow()
                        }
                        pendingCourses = result.courses
                        sourceHint = result.sourceHint
                    } catch (e: Exception) {
                        snackbar.showSnackbar(e.message ?: "捕获失败")
                    } finally {
                        capturing = false
                    }
                }
            }
        }
    }

    if (showGuide) {
        AlertDialog(
            onDismissRequest = { showGuide = false },
            title = { Text("教务导入 · ${CquPortal.SCHOOL_LABEL}") },
            text = {
                Column {
                    CquPortal.guidanceSteps.forEachIndexed { index, step ->
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
            TopAppBar(
                title = { Text("教务导入") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Canvas,
                    titleContentColor = Ink,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Canvas,
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
                text = "登录并打开课表后，点下方按钮捕获",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            AcademicWebView(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(SurfaceSoft),
                entryUrl = CquPortal.ENTRY_URL,
                onProgress = { progress = it },
                onWebViewReady = { webViewRef.set(it) },
                onNetworkPayload = { _, body -> latestNetworkBody.set(body) },
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
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): Boolean = false

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        view?.evaluateJavascript(AcademicCaptureJs.INSTALL_HOOKS, null)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        view?.evaluateJavascript(AcademicCaptureJs.INSTALL_HOOKS, null)
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
