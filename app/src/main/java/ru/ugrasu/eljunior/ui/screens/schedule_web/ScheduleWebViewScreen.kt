package ru.ugrasu.eljunior.ui.screens.schedule_web

import android.annotation.SuppressLint
import android.webkit.CookieManager
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

private const val ITPORT_STUDENT_TIMETABLE_URL = "https://itport.ugrasu.ru/timetable/student"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleWebViewScreen(
    url: String = ITPORT_STUDENT_TIMETABLE_URL,
    authRepository: AuthRepository? = null
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
            authRepository = authRepository
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ItportWebView(
    modifier: Modifier,
    url: String,
    authRepository: AuthRepository?
) {
    val context = LocalContext.current
    val webView = remember(url) {
        CookieManager.getInstance().setAcceptCookie(true)
        authRepository?.getItportCookies()?.forEach { cookie ->
            CookieManager.getInstance().setCookie("https://itport.ugrasu.ru", cookie.toString())
        }
        CookieManager.getInstance().flush()

        WebView(context).apply {
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            settings.mediaPlaybackRequiresUserGesture = true

            webViewClient = object : WebViewClient() {
                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(view: WebView?, requestUrl: String?): Boolean {
                    if (requestUrl != null && requestUrl.contains("itport.ugrasu.ru")) {
                        view?.loadUrl(requestUrl)
                        return true
                    }
                    return false
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
