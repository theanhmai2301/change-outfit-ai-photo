package com.exo.styleswap.ui.settings

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.exo.styleswap.databinding.ActivityPrivacyBinding
import com.exo.styleswap.firstopen.LocaleHelper

/**
 * Renders the bundled privacy policy page (assets/privacy-policy.html) in a WebView.
 * The page ships its own light/dark styling via `prefers-color-scheme`; we inject a
 * `:root` variable override so it always matches the app's current theme (not the
 * system theme). Tapped links / mailto open in the proper external app, not in-WebView.
 */
class PrivacyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPrivacyBinding

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }

        val dark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

        binding.webPrivacy.apply {
            setBackgroundColor(if (dark) 0xFF0D0E15.toInt() else 0xFFFFFFFF.toInt())
            settings.javaScriptEnabled = false
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView, request: WebResourceRequest
                ): Boolean {
                    val uri = request.url ?: return false
                    return when (uri.scheme) {
                        "http", "https", "mailto" -> {
                            try { startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: Exception) {}
                            true
                        }
                        else -> false
                    }
                }
            }
            loadDataWithBaseURL(
                "file:///android_asset/",
                buildHtml(dark),
                "text/html",
                "UTF-8",
                null
            )
        }
    }

    /** Reads the bundled page and forces its theme to match the app's light/dark mode. */
    private fun buildHtml(dark: Boolean): String {
        val raw = assets.open("privacy-policy.html").bufferedReader().use { it.readText() }
        val vars = if (dark) {
            ":root{--bg:#0D0E15;--fg:#f0f0f0;--muted:#9E9EAF;--accent:#B266FF;--border:#252736;}" +
                "th{background:rgba(255,255,255,0.05);}code{background:rgba(255,255,255,0.1);}" +
                ".toc,.crosslink{background:rgba(255,255,255,0.04);}"
        } else {
            ":root{--bg:#fff;--fg:#1a1a1a;--muted:#666;--accent:#8A2BE2;--border:#e5e5e5;}"
        }
        return raw.replace("</head>", "<style>$vars</style></head>")
    }
}
