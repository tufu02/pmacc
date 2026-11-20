package com.pg.pmacc

import android.app.Activity
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * 预留的扩展入口：可以用
 *   am start -n com.pg.pmacc/.PanelActivity
 * 直接打开同一个面板，方便后续做快捷方式 / 自动化调用。
 */
class PanelActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val wv = WebView(this)
        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        wv.settings.allowFileAccess = true
        wv.webViewClient = WebViewClient()
        wv.addJavascriptInterface(Bridge(this), "N")
        wv.loadUrl("file:///android_asset/home.html")
        setContentView(wv)
    }
}
