# FinvuAuthSDK

A simple, secure Android SDK for integrating Finvu Silent Network Authentication (SNA) into your app, with seamless support for WebView-based flows and JavaScript bridging.

---

## 📋 Requirements

**Minimum SDK version:** 25

**Minimum Kotlin
 version:** 1.9.0

---

## 📦 Installation

### 1. Add GitHub Packages Repository
Add the following to your **project-level** `build.gradle` or `settings.gradle.kts`:

```kotlin
maven {
    url = uri("https://maven.pkg.github.com/Cookiejar-technologies/finvu-auth-sdk-android")
    credentials {
        username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
        password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
    }
}
```

### 2. Add Your Credentials
Add to your `~/.gradle/gradle.properties` (do **not** commit this file):

```
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_PAT
```

### 3. Add the SDK Dependency
In your **app module** `build.gradle(.kts)`:

```kotlin
dependencies {
    implementation("com.finvu.android:finvuauthenticationsdk:1.0.7") // Use the latest version
}
```

### 4. Add Network Security Config
Add the following attribute to your `<application>` tag in your `AndroidManifest.xml`[(Why SNA Config is needed in the customer App for SNA:)](https://docs.google.com/document/d/1TQndJJ1IvKAEt5aZxJE-EL156-Zw3e2RfhS7K-NgXHk/edit?usp=sharing) :
```xml
<application
    ...
    android:networkSecurityConfig="@xml/finvu_silent_network_authentication_network_security_config"
    ... >
    <!-- Other attributes and activities -->
</application>
```

---

## 📋 Code Guidelines

### 1. 🚫 Avoid Third-Party Imports in Authentication Flow

Authentication screens (e.g., `AuthActivity`) should **only handle auth-related logic**. Do not use third-party analytics, logging, or unrelated services here.

```kotlin
// ❌ Avoid
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    ThirdPartyAnalytics.track("auth_started") // ❌ Not allowed
    finvuAuthenticationWrapper.setupWebView(
      webView, this, lifecycleScope, FinvuAuthEnvironment.DEVELOPMENT,
    )
}

// ✅ Recommended
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    finvuAuthenticationWrapper.setupWebView(
      webView, this, lifecycleScope, FinvuAuthEnvironment.DEVELOPMENT,
    ) // ✅ Only WebView or auth setup logic
}
```

### 2. 🔐 Do Not Store Sensitive Data in Local Storage

Never store auth tokens or personal info in SharedPreferences, databases, or files. Pass data using callbacks or result intents.

```Kotlin
// ❌ Avoid
val prefs = getSharedPreferences("auth", MODE_PRIVATE)
prefs.edit().putString("sna_token", token).apply()
```

### 3. 🧹 Clean Data and Instances at End of Authentication Journey

Always reset temporary variables and SDK resources once the auth process ends (on success, failure, or user exit).

```Kotlin 
override fun onDestroy() {
    super.onDestroy()
    finvuAuthenticationWrapper.onDestroy()
}
```

### 4. 🔁 Avoid Redundant Authentication Method Calls

Calling the same auth method multiple times (e.g., via double taps or spamming) leads to unwanted network traffic and unstable behavior.

``` Kotlin
// ❌ Avoid multiple calls
window.finvu_authentication_bridge.startAuth(snaUri, "callbackName");
window.finvu_authentication_bridge.startAuth(snaUri, "callbackName"); // Redundant

// ✅ Recommended
let isAuthInProgress = false;

function handleStartAuth() {
    if (isAuthInProgress) return;

    isAuthInProgress = true;
    window.finvu_authentication_bridge.startAuth(snaUri, "callbackName");
}

window.handleStartAuthResponse = function(response) {
    isAuthInProgress = false;
    // Process response
};

```

### 5. 📲 Cleanup When User Exits Authentication Journey

Clean up the authentication session when the user exits (via back press, auth complete, or app backgrounding).

```Kotlin
class AuthActivity : AppCompatActivity() {

    override fun onBackPressed() {
        cleanup()
        super.onBackPressed()
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    private fun cleanup() {
       finvuAuthenticationWrapper.onDestroy()
        // Reset any temporary state
    }
}
```

---
## 🚀 Android Integration

### Setup the WebView Bridge

The SDK provides a single method to set up the WebView bridge. No manual JS interface wiring is needed!

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private val finvuAuthenticationWrapper = FinvuAuthenticationWrapper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webView)

        // ... WebView settings ...

        // Setup the bridge
        finvuAuthenticationWrapper.setupWebView(
            webView,
            this,
            lifecycleScope,
            FinvuAuthEnvironment.DEVELOPMENT // or FinvuAuthEnvironment.PRODUCTION
        )

        // Load your web app
        webView.loadUrl("https://your-web-app-url")
    }

    override fun onDestroy() {
        super.onDestroy()
        finvuAuthenticationWrapper.onDestroy()
    }
}
```

### Environment Configuration

The SDK supports different environments for development and production:

- **Development Environment** (`FinvuAuthEnvironment.DEVELOPMENT`): Enables verbose logging and debug features
- **Production Environment** (`FinvuAuthEnvironment.PRODUCTION`): Minimal logging and optimized performance

```kotlin
// Development environment (with debug logging)
val finvuAuthenticationWrapper = FinvuAuthenticationWrapper()
finvuAuthenticationWrapper.setupWebView(
  webView,
  this,
  lifecycleScope,
  FinvuAuthEnvironment.DEVELOPMENT
)

// Production environment (minimal logging)
val finvuAuthenticationWrapper = FinvuAuthenticationWrapper()
finvuAuthenticationWrapper.setupWebView(
  webView,
  this,
  lifecycleScope,
  FinvuAuthEnvironment.PRODUCTION
)
```

---

## 🌐 WebView/JavaScript Usage

Once the bridge is set up, your web app can call the following methods from JavaScript:

### Available Methods

```javascript
// Initialize the SDK
window.finvu_authentication_bridge.initAuth(initConfig, callbackName);

// Start Silent Network Authentication with an SNA URI
window.finvu_authentication_bridge.startAuth(snaUri, callbackName);
```

### Method Details

#### 1. initAuth(initConfig, callbackName)
Initializes the Finvu authentication SDK and detects the cellular network (MCC/MNC).

**Parameters:**
- `initConfig` (string): JSON configuration. All fields are optional.
- `callbackName` (string): JavaScript callback function name

**Supported config fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `requestId` | string | No | Optional request identifier for tracking |
| `entityId` | string | No | Optional entity identifier |

**Example:**
```javascript
const config = JSON.stringify({ requestId: "req_001" });
window.finvu_authentication_bridge.initAuth(config, "handleInitAuthResponse");
```

**Success Response:**
```json
{
  "status": "SUCCESS",
  "mcc": "404",
  "mnc": "45"
}
```

**Failure Responses:**
```json
// Invalid JSON config
{
  "status": "FAILURE",
  "errorCode": "INVALID_JSON_CONFIGURATION",
  "errorMessage": "Invalid JSON configuration for Finvu Authentication SDK."
}

// No cellular network
{
  "status": "FAILURE",
  "errorCode": "SNA_CELLULAR_UNAVAILABLE",
  "errorMessage": "No cellular network available. Please ensure cellular data is enabled."
}

// WiFi active, not on mobile data
{
  "status": "FAILURE",
  "errorCode": "SNA_DEVICE_NOT_ON_MOBILE_DATA",
  "errorMessage": "Not connected to mobile data. Please disable WiFi and use cellular network."
}
```

#### 2. startAuth(snaUri, callbackName)
Performs Silent Network Authentication by following the SNA URI over the cellular network.

**Parameters:**
- `snaUri` (string): The SNA URI provided by your backend
- `callbackName` (string): JavaScript callback function name

**Example:**
```javascript
window.finvu_authentication_bridge.startAuth(
  "https://your-sna-endpoint.example.com/auth?...",
  "handleStartAuthResponse"
);
```

**Success Response:**
```json
{
  "status": "SUCCESS",
  "httpStatusCode": 200,
  "snaToken": "your_sna_token_here"
}
```

**Failure Responses:**
```json
// Empty or missing SNA URI
{
  "status": "FAILURE",
  "errorCode": "INVALID_SNA_URI",
  "errorMessage": "Invalid SNA URI provided"
}

// initAuth was not called first
{
  "status": "FAILURE",
  "errorCode": "SESSION_NOT_INITIALIZED",
  "errorMessage": "Session not initialized. Please call initAuth first."
}

// Network request failed
{
  "status": "FAILURE",
  "errorCode": "SNA_CONNECTION_FAILED",
  "errorMessage": "Connection failed"
}

// Request timed out
{
  "status": "FAILURE",
  "errorCode": "SNA_CELLULAR_TIMEOUT",
  "errorMessage": "Connection timeout - SNA request took too long"
}
```

### Example Integration

```javascript
// Global callback functions
window.handleInitAuthResponse = (responseStr) => {
  const response = JSON.parse(responseStr);
  if (response.status === "SUCCESS") {
    console.log("SDK initialized. MCC:", response.mcc, "MNC:", response.mnc);
    // Proceed: fetch SNA URI from your backend and call startAuth
  } else {
    console.error("SDK initialization failed:", response.errorCode, response.errorMessage);
  }
};

window.handleStartAuthResponse = (responseStr) => {
  const response = JSON.parse(responseStr);

  if (response.status === "SUCCESS" && response.snaToken) {
    // Send snaToken to your backend to complete verification
    handleAuthSuccess(response.snaToken);
  } else {
    console.error("SNA failed:", response.errorCode, response.errorMessage);
    handleAuthFailure(response.errorMessage);
  }
};

// Usage
function initializeAuth() {
  const config = JSON.stringify({ requestId: "req_001" });
  window.finvu_authentication_bridge.initAuth(config, "handleInitAuthResponse");
}

function startAuthentication(snaUri) {
  window.finvu_authentication_bridge.startAuth(snaUri, "handleStartAuthResponse");
}
```

---

## 📤 Response Format & Error Code Reference

### Response Structure

**Success Responses** contain:
- `status`: `"SUCCESS"`
- Additional fields depending on the method (`mcc`, `mnc`, `httpStatusCode`, `snaToken`)

**Failure Responses** contain:
- `status`: `"FAILURE"`
- `errorCode`: String code identifying the failure type
- `errorMessage`: Human-readable error description

### Error Code Reference

#### initAuth Error Codes

| Error Code | Error Message | Cause |
|------------|---------------|-------|
| `INVALID_JSON_CONFIGURATION` | Invalid JSON configuration for Finvu Authentication SDK. | Malformed JSON passed to initAuth |
| `SNA_CELLULAR_UNAVAILABLE` | No cellular network available. Please ensure cellular data is enabled. | No SIM / no mobile signal |
| `SNA_DEVICE_NOT_ON_MOBILE_DATA` | Not connected to mobile data. Please disable WiFi and use cellular network. | WiFi is active, mobile data is off |

#### startAuth Error Codes

| Error Code | Error Message | Cause |
|------------|---------------|-------|
| `INVALID_SNA_URI` | Invalid SNA URI provided | Empty or blank snaUri argument |
| `SESSION_NOT_INITIALIZED` | Session not initialized. Please call initAuth first. | startAuth called before initAuth |
| `SNA_CONNECTION_FAILED` | Connection failed | General network connectivity failure |
| `SNA_CELLULAR_REQUEST_FAILED` | SNA request failed | HTTP request over cellular failed |
| `SNA_INVALID_REDIRECT` | Invalid redirect URL | Redirect chain returned a bad URL |
| `SNA_TOO_MANY_REDIRECTS` | Too many redirects | Redirect loop detected |
| `SNA_INVALID_RESPONSE` | Response has no data or is corrupt | Server returned an unexpected response |
| `SNA_CELLULAR_TIMEOUT` | Connection timeout - SNA request took too long | Request exceeded the timeout threshold |

### Input Validation Rules

- **SNA URI**: Must be a non-empty string
- **initConfig**: Must be valid JSON (all fields optional)

---

## ❓ FAQ

### Q: What conditions are required for Silent Network Authentication (SNA)?
**A:** For SNA to work properly:
- **SIM internet must be ON** (mobile data enabled)
- **WiFi must be OFF** (disconnect from WiFi networks)
- Device must have active mobile network connectivity
- SIM card must support the required network protocols

### Q: What is the SNA URI and where do I get it?
**A:** The SNA URI is a one-time URL generated by your backend (via the Finvu API) for the specific authentication session. Your web app should request it from your server after calling `initAuth`, then pass it to `startAuth`.

### Q: Why am I getting `SNA_CELLULAR_UNAVAILABLE` or `SNA_DEVICE_NOT_ON_MOBILE_DATA`?
**A:** These errors indicate the device is not routed over cellular:
- `SNA_CELLULAR_UNAVAILABLE`: Enable mobile data / ensure SIM is active
- `SNA_DEVICE_NOT_ON_MOBILE_DATA`: Disable WiFi and retry

### Q: Why am I getting `SESSION_NOT_INITIALIZED`?
**A:** `startAuth` was called before `initAuth` completed successfully. Always wait for a `SUCCESS` response from `initAuth` before calling `startAuth`.

---

## 🛡️ ProGuard

The SDK provides consumer rules for required dependencies. No extra configuration is needed unless you have custom requirements.
