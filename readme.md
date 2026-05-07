# Finvu Auth SDK — Android

**Version:** `1.0.0` · **Min SDK:** API 25 · **Kotlin:** 1.9.0+

Silent Network Authentication (SNA) SDK for Android, with WebView bridge support for web-based authentication flows.

---

## Installation

Maven Central is included by default in modern Android projects. Verify your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

Add the dependency to your **app-level** `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.cookiejar-technologies:finvuauthenticationsdk:1.0.0")
}
```

---

## Android Setup

### 1. Internet Permission

Add to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
```

### 2. Network Security Config

Add to your `<application>` tag in `AndroidManifest.xml`:

```xml
<application
    android:networkSecurityConfig="@xml/finvu_silent_network_authentication_network_security_config"
    ...>
</application>
```

> Required for Silent Network Authentication (SNA) to make carrier-specific HTTP calls. See [why SNA config is needed](https://docs.google.com/document/d/1TQndJJ1IvKAEt5aZxJE-EL156-Zw3e2RfhS7K-NgXHk/edit?usp=sharing).

---

## Integration

### Option A — WebView App

Use this if your app loads a web page inside a `WebView` and the web app drives the authentication flow.

```kotlin
import com.finvu.android.authenticationwrapper.FinvuAuthenticationWrapper
import com.finvu.android.authenticationwrapper.utils.FinvuAuthEnvironment

class AuthActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private val finvuWrapper = FinvuAuthenticationWrapper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        webView = findViewById(R.id.webView)

        finvuWrapper.setupWebView(
            webView,
            this,
            lifecycleScope,
            FinvuAuthEnvironment.PRODUCTION  // or DEVELOPMENT
        )

        webView.loadUrl("https://your-web-app-url")
    }

    override fun onDestroy() {
        super.onDestroy()
        finvuWrapper.onDestroy()
    }
}
```

Your web app communicates with the SDK through the `finvu_authentication_bridge` JavaScript bridge.

---

### Option B — Native App

Use this if your app handles the authentication flow entirely in Kotlin, without a WebView.

```kotlin
import com.finvu.android.authenticationwrapper.FinvuAuthenticationNativeWrapper
import com.finvu.android.authenticationwrapper.utils.FinvuAuthEnvironment

val nativeWrapper = FinvuAuthenticationNativeWrapper()

// 1. Setup — call once
nativeWrapper.setup(FinvuAuthEnvironment.PRODUCTION, activity, coroutineScope)

// 2. Init — call with requestId from your backend
nativeWrapper.initAuth(mapOf("requestId" to "REQUEST_ID")) { result ->
    if (result.isSuccess) {
        // proceed to startAuth
    }
}

// 3. Start SNA — call with the SNA URL returned by your backend
nativeWrapper.startAuth("SNA_URL") { result ->
    if (result.isSuccess) {
        val token = result.getOrNull() // use token
    }
}

// 4. Cleanup — call when done or user exits
nativeWrapper.onDestroy()
```


## Support

support@cookiejar.co.in · [finvu.in](https://finvu.in)
