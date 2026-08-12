package my.novelreader.webview

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import my.novelreader.coreui.theme.Theme
import my.novelreader.coreui.theme.ThemeProvider
import my.novelreader.core.Toasty
import my.novelreader.core.utils.Extra_Boolean
import my.novelreader.core.utils.Extra_String
import my.novelreader.network.toUrl
import javax.inject.Inject

@AndroidEntryPoint
class WebViewActivity : ComponentActivity() {

    @Inject
    lateinit var toasty: Toasty

    @Inject
    lateinit var themeProvider: ThemeProvider

    class IntentData : Intent {
        var url by Extra_String()
        var cloudflareChallenge by Extra_Boolean()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, url: String) : super(ctx, WebViewActivity::class.java) {
            this.url = url
        }
    }

    private val extras by lazy { IntentData(intent) }
    private val cookieCheckHandler = Handler(Looper.getMainLooper())
    private var isClosing = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = WebView(this).also {
            it.loadUrl(extras.url)
        }
        setContent {
            Theme(themeProvider = themeProvider) {
                WebViewScreen(
                    toolbarTitle = extras.url,
                    webViewFactory = { webView },
                    onBackClicked = { this@WebViewActivity.onBackPressed() },
                    onReloadClicked = { webView.reload() }
                )
            }
        }

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_WEBVIEW)) {
            toasty.show(R.string.web_view_not_available)
            finish()
            return
        }

        extras.url.toUrl()?.authority ?: run {
            toasty.show(R.string.invalid_URL)
            finish()
            return
        }

        val isCloudflare = extras.cloudflareChallenge

        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (view != null && url != null) {
                    if (isCloudflare) {
                        checkCfClearanceAndClose(url)
                    } else {
                        toasty.show(R.string.cookies_saved)
                    }
                }
            }
        }

        // For Cloudflare challenges, also poll cookies periodically
        // in case the challenge is solved via JS without a page navigation
        if (isCloudflare) {
            startCookieMonitor(extras.url)
        }
    }

    private fun checkCfClearanceAndClose(url: String) {
        if (isClosing) return
        try {
            val cookies = CookieManager.getInstance()?.getCookie(url) ?: return
            if (cookies.contains("cf_clearance")) {
                isClosing = true
                CookieManager.getInstance().flush()
                // Small delay to ensure cookie is fully synced
                cookieCheckHandler.postDelayed({ finish() }, 800)
            }
        } catch (_: Exception) {
            // CookieManager may not be initialized
        }
    }

    private fun startCookieMonitor(url: String) {
        val checkRunnable = object : Runnable {
            override fun run() {
                if (!isFinishing && !isClosing) {
                    checkCfClearanceAndClose(url)
                    cookieCheckHandler.postDelayed(this, 1000)
                }
            }
        }
        cookieCheckHandler.postDelayed(checkRunnable, 2000)
    }

    override fun onDestroy() {
        cookieCheckHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}