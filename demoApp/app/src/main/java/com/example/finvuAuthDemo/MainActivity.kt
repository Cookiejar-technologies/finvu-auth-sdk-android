package com.example.finvuAuthDemo

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
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
    private lateinit var urlLabel: TextView
    private lateinit var urlEditText: EditText
    private lateinit var logsButton: Button

    private val consoleLogs = mutableListOf<String>()

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
        urlLabel = findViewById(R.id.urlLabel)
        urlEditText = findViewById(R.id.urlEditText)
        logsButton = findViewById(R.id.logsButton)

        try {
            finvuAuthenticationWrapper = FinvuAuthenticationWrapper()
            Log.d(TAG, "SDK instance created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "SDK init failed", e)
            Toast.makeText(this, "SDK init failed", Toast.LENGTH_LONG).show()
            return
        }

        setupWebView()
        setupInputListeners()
        bindListeners()
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?, request: WebResourceRequest?, error: WebResourceError?
            ) {
                Log.d(TAG, "WebView error: ${error?.description}")
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                val level = message.messageLevel().name
                val log = "[$level] ${message.message()} (${message.sourceId()}:${message.lineNumber()})"
                consoleLogs.add(log)
                Log.d(TAG, "JS Console: $log")
                return true
            }
        }
    }

    private fun setupInputListeners() {
        loadWebViewButton.isEnabled = false
        urlEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                loadWebViewButton.isEnabled = !s.isNullOrBlank()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        loadWebViewButton.isEnabled = urlEditText.text.isNotBlank()
    }

    private fun bindListeners() {
        loadWebViewButton.setOnClickListener {
            try {
                consoleLogs.clear()
                finvuAuthenticationWrapper.setupWebView(
                    webView, this, lifecycleScope, FinvuAuthEnvironment.DEVELOPMENT
                )
                Log.d(TAG, "WebView setup successful")
                showWebViewMode()
                val url = urlEditText.text.toString().trim()
                webView.loadUrl(if (url.isNotEmpty()) url else "https://test-web-app-8a50c.web.app")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up WebView", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        loadNativeViewButton.setOnClickListener {
            startActivity(Intent(this, NativeViewActivity::class.java))
        }

        logsButton.setOnClickListener {
            showLogsDialog()
        }
    }

    private fun showWebViewMode() {
        urlLabel.visibility = View.GONE
        urlEditText.visibility = View.GONE
        loadWebViewButton.visibility = View.GONE
        loadNativeViewButton.visibility = View.GONE
        webView.visibility = View.VISIBLE
        logsButton.visibility = View.VISIBLE
    }

    private fun showHomeMode() {
        webView.visibility = View.GONE
        logsButton.visibility = View.GONE
        urlLabel.visibility = View.VISIBLE
        urlEditText.visibility = View.VISIBLE
        loadWebViewButton.visibility = View.VISIBLE
        loadNativeViewButton.visibility = View.VISIBLE
    }

    private fun showLogsDialog() {
        val textView = TextView(this).apply {
            text = if (consoleLogs.isEmpty()) "No logs yet." else consoleLogs.joinToString("\n\n")
            setPadding(32, 32, 32, 32)
            setTextColor(0xFF00FF66.toInt())
            setBackgroundColor(0xFF1E1E1E.toInt())
            typeface = Typeface.MONOSPACE
            textSize = 11f
            setTextIsSelectable(true)
        }

        val scrollView = ScrollView(this).apply {
            addView(textView)
            setBackgroundColor(0xFF1E1E1E.toInt())
        }

        AlertDialog.Builder(this)
            .setTitle("JS Console (${consoleLogs.size})")
            .setView(scrollView)
            .setPositiveButton("Close", null)
            .setNeutralButton("Clear") { _, _ -> consoleLogs.clear() }
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.visibility == View.VISIBLE) {
            showHomeMode()
            try {
                finvuAuthenticationWrapper.onDestroy()
                Log.d(TAG, "SDK cleanup completed on back press")
            } catch (e: Exception) {
                Log.e(TAG, "Error during SDK cleanup on back press", e)
            }
        } else {
            super.onBackPressed()
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
