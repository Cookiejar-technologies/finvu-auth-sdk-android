package com.example.finvuAuthDemo


import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.finvu.android.authenticationwrapper.FinvuAuthenticationNativeWrapper
import com.finvu.android.authenticationwrapper.utils.FinvuAuthEnvironment

class NativeViewActivity : AppCompatActivity() {

    private lateinit var requestIdEditText: EditText
    private lateinit var snaLinkEditText: EditText
    private lateinit var initAuthButton: Button
    private lateinit var startAuthButton: Button
    private lateinit var responseTextView: TextView

    private lateinit var finvuAuthenticationWrapper: FinvuAuthenticationNativeWrapper

    private var initDone = false
    private var busy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.native_view)

        requestIdEditText = findViewById(R.id.requestIdEditText)
        snaLinkEditText = findViewById(R.id.snaLinkEditText)
        initAuthButton = findViewById(R.id.initAuthButton)
        startAuthButton = findViewById(R.id.startAuthButton)
        responseTextView = findViewById(R.id.responseTextView)

        finvuAuthenticationWrapper = FinvuAuthenticationNativeWrapper()
        finvuAuthenticationWrapper.setup(FinvuAuthEnvironment.DEVELOPMENT, this, lifecycleScope)

        snaLinkEditText.doAfterTextChanged { updateStartButton() }

        initAuthButton.setOnClickListener {
            if (busy) return@setOnClickListener
            busy = true
            initAuthButton.isEnabled = false
            responseTextView.text = "processing"

            val requestId = requestIdEditText.text.toString().trim()
            val initConfig = mutableMapOf<String, Any>("requestId" to requestId)

            finvuAuthenticationWrapper.initAuth(initConfig) { result ->
                runOnUiThread {
                    busy = false
                    if (result.isSuccess) {
                        initDone = true
                        requestIdEditText.isEnabled = false
                        snaLinkEditText.isEnabled = true
                        updateStartButton()
                        val response = result.getOrNull()
                        responseTextView.text = "InitAuth Success:\n$response"
                        Log.d("Finvu", "InitAuth Success: $response")
                    } else {
                        initAuthButton.isEnabled = true
                        val error = result.exceptionOrNull()
                        responseTextView.text = "InitAuth Error:\n$error"
                        Log.e("Finvu", "InitAuth Error", error)
                    }
                }
            }
        }

        startAuthButton.setOnClickListener {
            if (busy) return@setOnClickListener
            busy = true
            updateStartButton()
            responseTextView.text = "processing"

            val snaUrl = snaLinkEditText.text.toString().trim()
            finvuAuthenticationWrapper.startAuth(snaUrl) { result ->
                runOnUiThread {
                    busy = false
                    updateStartButton()
                    if (result.isSuccess) {
                        val response = result.getOrNull()
                        responseTextView.text = "StartAuth Success:\n$response"
                        Log.d("Finvu", "StartAuth Success: $response")
                    } else {
                        val error = result.exceptionOrNull()
                        responseTextView.text = "StartAuth Error:\n$error"
                        Log.e("Finvu", "StartAuth Error", error)
                    }
                }
            }
        }
    }

    private fun updateStartButton() {
        startAuthButton.isEnabled = initDone && !busy && snaLinkEditText.text.toString().trim().isNotEmpty()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            finvuAuthenticationWrapper.onDestroy()
        } catch (e: Exception) {
            Log.e("Finvu", "Cleanup error", e)
        }
    }
}
