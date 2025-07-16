package com.example.finvuAuthDemo

import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.finvu.android.authenticationwrapper.FinvuAuthenticationWrapper


class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var loadWebViewButton: Button
    private lateinit var finvuAuthenticationWrapper: FinvuAuthenticationWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        loadWebViewButton = findViewById(R.id.loadWebViewButton)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            mixedContentMode = MIXED_CONTENT_COMPATIBILITY_MODE
            setSupportMultipleWindows(true)
            databaseEnabled = true
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?, request: WebResourceRequest?, error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
            }
        }

        finvuAuthenticationWrapper = FinvuAuthenticationWrapper.instance

        loadWebViewButton.setOnClickListener {
            try {

                finvuAuthenticationWrapper.setupWebView(
                    webView = webView,
                    activity = this,
                    scope = lifecycleScope
                )
                loadWebViewButton.visibility = View.GONE

                webView.visibility = View.VISIBLE
                val url = "your actual URL"
                webView.loadUrl(url)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
