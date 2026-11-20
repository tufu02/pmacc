package com.pg.pmacc

import android.app.Activity
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient

class HomeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildWebView("file:///android_asset/home.html"))
    }

    private fun buildWebView(url: String): WebView {
        val wv = WebView(this)
        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        wv.settings.allowFileAccess = true
        wv.webViewClient = WebViewClient()
        wv.addJavascriptInterface(Bridge(this), "N")
        wv.loadUrl(url)
        return wv
    }
}
