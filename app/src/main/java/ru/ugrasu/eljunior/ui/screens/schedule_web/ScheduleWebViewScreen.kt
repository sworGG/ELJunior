package ru.ugrasu.eljunior.ui.screens.schedule_web

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import ru.ugrasu.eljunior.data.repository.AuthRepository
import ru.ugrasu.eljunior.ui.theme.TextPrimary

private const val ITPORT_ORIGIN = "https://itport.ugrasu.ru"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleWebViewScreen(
    url: String,
    authRepository: AuthRepository? = null,
    targetGroupUrl: String? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Расписание") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        ItportWebView(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            url = url,
            targetGroupUrl = targetGroupUrl ?: url.takeIf { it.contains("/timetable/group/") },
            authRepository = authRepository
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ItportWebView(
    modifier: Modifier,
    url: String,
    targetGroupUrl: String?,
    authRepository: AuthRepository?
) {
    val context = LocalContext.current
    val webView = remember(url, targetGroupUrl) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        authRepository?.getItportCookies()?.forEach { cookie ->
            cookieManager.setCookie(ITPORT_ORIGIN, authRepository.buildWebViewCookie(cookie))
        }
        cookieManager.flush()

        WebView(context).apply {
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            settings.javaScriptEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            settings.mediaPlaybackRequiresUserGesture = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            webChromeClient = WebChromeClient()

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val requestUrl = request?.url?.toString().orEmpty()
                    return if (requestUrl.contains("itport.ugrasu.ru")) {
                        false
                    } else {
                        super.shouldOverrideUrlLoading(view, request)
                    }
                }

                override fun shouldOverrideUrlLoading(view: WebView?, requestUrl: String?): Boolean {
                    return if (!requestUrl.isNullOrBlank() && requestUrl.contains("itport.ugrasu.ru")) {
                        false
                    } else {
                        super.shouldOverrideUrlLoading(view, requestUrl)
                    }
                }

                override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                    val current = finishedUrl ?: view?.url
                    if (current != null &&
                        targetGroupUrl != null &&
                        isTimetablePickerUrl(current) &&
                        current != targetGroupUrl
                    ) {
                        view?.loadUrl(targetGroupUrl)
                    }
                }
            }

            loadUrl(url)
        }
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { webView }
    )
}

private fun isTimetablePickerUrl(url: String): Boolean {
    if (!url.contains("itport.ugrasu.ru")) return false
    if (url.contains("/login")) return false
    if (url.contains("/timetable/group/")) return false

    val path = Uri.parse(url).path.orEmpty().trimEnd('/')
    return path == "/timetable" ||
        path == "/timetable/student" ||
        path == "/timetable/search" ||
        path.startsWith("/timetable/index")
}
