package ru.ugrasu.eljunior.ui.screens.schedule_web

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import ru.ugrasu.eljunior.ui.theme.TextPrimary

private const val IT_PORT_LOGIN_URL = "https://itport.ugrasu.ru/login"
private const val IT_PORT_SCHEDULE_URL = "https://itport.ugrasu.ru/timetable/group/8913/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleWebViewScreen() {
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
            url = IT_PORT_LOGIN_URL
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ItportWebView(
    modifier: Modifier,
    url: String
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            CookieManager.getInstance().setAcceptCookie(true)
            WebView(ctx).apply {
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                
                // Обработчик для навигации внутри WebView
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        // Разрешаем навигацию по всему itport.ugrasu.ru
                        if (url != null && url.contains("itport.ugrasu.ru")) {
                            view?.loadUrl(url)
                            return true
                        }
                        return false
                    }
                }
                
                // Обработчик для JavaScript
                webChromeClient = WebChromeClient()
                
                loadUrl(url)
            }
        }
    )
}

