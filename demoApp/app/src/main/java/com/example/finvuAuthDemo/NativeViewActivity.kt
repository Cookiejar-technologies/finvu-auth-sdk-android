package com.example.finvuAuthDemo


import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.finvu.android.authenticationwrapper.FinvuAuthenticationNativeWrapper
import com.finvu.android.authenticationwrapper.utils.FinvuAuthEnvironment

class NativeViewActivity : AppCompatActivity() {

    private lateinit var phoneEditText: EditText
    private lateinit var initAuthButton: Button
    private lateinit var startAuthButton: Button
    private lateinit var responseTextView: TextView

    private lateinit var finvuAuthenticationWrapper: FinvuAuthenticationNativeWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.native_view)

        phoneEditText = findViewById(R.id.phoneEditText)
        initAuthButton = findViewById(R.id.initAuthButton)
        startAuthButton = findViewById(R.id.startAuthButton)
        responseTextView = findViewById(R.id.responseTextView)

        finvuAuthenticationWrapper = FinvuAuthenticationNativeWrapper()

        finvuAuthenticationWrapper.setup(FinvuAuthEnvironment.DEVELOPMENT, this, lifecycleScope)

        initAuthButton.setOnClickListener {
            val initConfig = mutableMapOf<String, Any>(
                "appId" to "58FI169DV4A0Q4PGMDCR",
                "requestId" to "7260c3c0-bc6e-4003-9295-c9913b9b521f"
            )
            responseTextView.text = "processing"

            finvuAuthenticationWrapper.initAuth(initConfig) { result ->
                if (result.isSuccess) {
                    runOnUiThread {
                        val response = result.getOrNull()
                        responseTextView.text = "InitAuth Success:\n$response"
                        Log.d("Finvu", "InitAuth Success: $response")
                    }
                } else {
                    runOnUiThread {
                        val error = result.exceptionOrNull()
                        responseTextView.text = "InitAuth Error:\n${error}"
                        Log.e("Finvu", "InitAuth Error", error)
                    }
                }
            }
        }

        startAuthButton.setOnClickListener {

            val phone = phoneEditText.text.toString().trim()
            if (phone.length != 10) {
                Toast.makeText(this, "Enter valid 10-digit mobile number", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            responseTextView.text = "processing"
            finvuAuthenticationWrapper.startAuth(phone) { result ->
                if (result.isSuccess) {
                    runOnUiThread {
                        val response = result.getOrNull()
                        responseTextView.text = "InitAuth Success:\n$response"
                        Log.d("Finvu", "InitAuth Success: $response")
                    }
                } else {
                    runOnUiThread {
                        val error = result.exceptionOrNull()
                        responseTextView.text = "InitAuth Error:\n${error}"
                        Log.e("Finvu", "InitAuth Error", error)
                    }
                }
            }
        }
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
