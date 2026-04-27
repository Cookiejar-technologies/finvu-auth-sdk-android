package com.example.finvuAuthDemo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.finvu.android.authenticationwrapper.FinvuAuthenticationNativeWrapper
import com.finvu.android.authenticationwrapper.utils.FinvuAuthEnvironment
import com.finvu.android.authenticationwrapper.utils.FinvuAuthJsonKeys

class NativeViewActivity : AppCompatActivity() {

    private lateinit var snaUriEditText: EditText
    private lateinit var initAuthButton: Button
    private lateinit var startAuthButton: Button
    private lateinit var mccMncTextView: TextView
    private lateinit var responseTextView: TextView

    private lateinit var finvuAuthenticationWrapper: FinvuAuthenticationNativeWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.native_view)

        snaUriEditText = findViewById(R.id.snaUriEditText)
        initAuthButton = findViewById(R.id.initAuthButton)
        startAuthButton = findViewById(R.id.startAuthButton)
        mccMncTextView = findViewById(R.id.mccMncTextView)
        responseTextView = findViewById(R.id.responseTextView)

        finvuAuthenticationWrapper = FinvuAuthenticationNativeWrapper.instance
        finvuAuthenticationWrapper.setup(FinvuAuthEnvironment.DEVELOPMENT, this, lifecycleScope)

        ensurePhonePermission()

        initAuthButton.setOnClickListener {
            val initConfig = mutableMapOf<String, Any>(
                FinvuAuthJsonKeys.REQUEST_ID to "native-demo-${System.currentTimeMillis()}"
            )
            mccMncTextView.text = "MCC: —\nMNC: —"
            responseTextView.text = "processing…"

            finvuAuthenticationWrapper.initAuth(initConfig) { result ->
                if (result.isSuccess) {
                    runOnUiThread {
                        val json = result.getOrNull()
                        val mcc = json?.optString(FinvuAuthJsonKeys.MCC).orEmpty().ifBlank { "—" }
                        val mnc = json?.optString(FinvuAuthJsonKeys.MNC).orEmpty().ifBlank { "—" }
                        mccMncTextView.text = "MCC: $mcc\nMNC: $mnc"
                        responseTextView.text = "InitAuth Success:\n$json"
                        Log.d(TAG, "InitAuth Success mcc=$mcc mnc=$mnc full=$json")
                    }
                } else {
                    runOnUiThread {
                        val error = result.exceptionOrNull()
                        mccMncTextView.text = "MCC: —\nMNC: —"
                        responseTextView.text = "InitAuth Error:\n$error"
                        Log.e(TAG, "InitAuth Error", error)
                    }
                }
            }
        }

        startAuthButton.setOnClickListener {
            val uri = snaUriEditText.text.toString().trim()
            if (uri.isEmpty()) {
                Toast.makeText(this, "Enter SNA URI in the field above", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            responseTextView.text = "processing (cellular)…"

            finvuAuthenticationWrapper.startAuth(uri) { result ->
                if (result.isSuccess) {
                    runOnUiThread {
                        val response = result.getOrNull()
                        responseTextView.text = "StartAuth Success:\n$response"
                        Log.d(TAG, "StartAuth Success: $response")
                    }
                } else {
                    runOnUiThread {
                        val error = result.exceptionOrNull()
                        responseTextView.text = "StartAuth Error:\n$error"
                        Log.e(TAG, "StartAuth Error", error)
                    }
                }
            }
        }
    }

    private fun ensurePhonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                REQ_PHONE,
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_PHONE && grantResults.isNotEmpty()
            && grantResults[0] != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                this,
                "READ_PHONE_STATE denied — MCC/MNC may be empty on some devices",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            finvuAuthenticationWrapper.onDestroy()
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup error", e)
        }
    }

    companion object {
        private const val TAG = "FinvuDemo"
        private const val REQ_PHONE = 4401
    }
}
