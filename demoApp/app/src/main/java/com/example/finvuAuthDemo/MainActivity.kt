package com.example.finvuAuthDemo

import android.content.Intent
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
import com.finvu.android.authenticationwrapper.utils.FinvuAuthEnvironment


class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var loadWebViewButton: Button
    private lateinit var loadNativeViewButton: Button
    private lateinit var finvuAuthenticationWrapper: FinvuAuthenticationWrapper

    companion object {
        private const val TAG = "FinvuExample"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        webView = findViewById(R.id.webView)
        loadWebViewButton = findViewById(R.id.loadWebViewButton)
        loadNativeViewButton = findViewById(R.id.loadNativeViewButton)

        try {
            finvuAuthenticationWrapper = FinvuAuthenticationWrapper()
            Log.d(TAG, "SDK instance created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "SDK init failed", e)
            Toast.makeText(this, "SDK init failed", Toast.LENGTH_LONG).show()
            return
        }

        setupWebView()
        bindListeners()
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            setSupportMultipleWindows(true)
            databaseEnabled = true
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?, request: WebResourceRequest?, error: WebResourceError?
            ) {
                Log.d(TAG, "WebView error: ${error?.description}")
            }
        }
    }

    private fun bindListeners() {
        loadWebViewButton.setOnClickListener {
            try {
                finvuAuthenticationWrapper.setupWebView(
                    webView, this, lifecycleScope, FinvuAuthEnvironment.DEVELOPMENT
                )
                Log.d(TAG, "WebView setup successful")
                loadWebViewButton.visibility = View.GONE
                loadNativeViewButton.visibility = View.GONE
                webView.visibility = View.VISIBLE
                webView.loadUrl("https://test-web-app-8a50c.web.app")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up WebView", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        loadNativeViewButton.setOnClickListener {

            startActivity(Intent(this, NativeViewActivity::class.java))
        }
    }

    override fun onBackPressed() {

        if (webView.visibility == View.VISIBLE) {
            // Reset UI to initial state
            webView.visibility = View.GONE
            loadWebViewButton.visibility = View.VISIBLE
            loadNativeViewButton.visibility = View.VISIBLE

            try {
                finvuAuthenticationWrapper.onDestroy()
                Log.d(TAG, "SDK cleanup completed on back press")
            } catch (e: Exception) {
                Log.e(TAG, "Error during SDK cleanup on back press", e)
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        try {
            finvuAuthenticationWrapper.onDestroy()
            Log.d(TAG, "SDK cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during SDK cleanup", e)
        }
    }
}