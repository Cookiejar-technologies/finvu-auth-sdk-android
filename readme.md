# FinvuAuthSDK

A simple, secure Android SDK for integrating Finvu authentication into your app, with seamless support for WebView-based flows and JavaScript bridging.

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
    implementation("com.finvu.android:finvuAuthenticationSDK:latest_sdk_version) // Use the latest version
}
```

> **Note:** Replace `latest_ios_sdk_version` in your Podfile with the actual version number. Latest version is `1.0.3`.

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

Never store auth tokens or personal info like phone numbers in SharedPreferences, databases, or files. Pass data using callbacks or result intents.

```Kotlin
// ❌ Avoid
val prefs = getSharedPreferences("auth", MODE_PRIVATE)
prefs.edit().putString("auth_token", token).apply()
```

### 3. 🧹 Clean Data and Instances at End of Authentication Journey

Always reset temporary variables and SDK resources once the auth process ends (on success, failure, or user exit).

```Kotlin 
override fun onDestroy() {
    super.onDestroy()
      phoneNumber = null
}
```

### 4. 🔁 Avoid Redundant Authentication Method Calls

Calling the same auth method multiple times (e.g., via double taps or spamming) leads to unwanted network traffic and unstable behavior.

``` Kotlin
// ❌ Avoid multiple calls
window.finvu_authentication_bridge.startAuth(phoneNumber, "callbackName");
window.finvu_authentication_bridge.startAuth(phoneNumber, "callbackName"); // Redundant

// ✅ Recommended
let isAuthInProgress = false;

function handleStartAuth() {
    if (isAuthInProgress) return;

    isAuthInProgress = true;
    window.finvu_authentication_bridge.startAuth(phoneNumber, "callbackName");
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webView)

        // ... WebView settings ...

        // Setup the bridge
        val finvuAuthenticationWrapper = FinvuAuthenticationWrapper()
        finvuAuthenticationWrapper.setupWebView(
            webView,
            this,
            lifecycleScope,
            FinvuAuthEnvironment.DEVELOPMENT or FinvuAuthEnvironment.PRODUCTION
        )

        // Load your web app
        webView.loadUrl("https://your-web-app-url")
    }

    override fun onDestroy() {
        super.onDestroy()
        FinvuAuthenticationWrapper.onDestroy()
    }
}
```

### Environment Configuration

The SDK supports different environments for development and production:

- **Development Environment** (`FinvuAuthEnvironment.DEVELOPMENT `): Enables verbose logging and debug features
- **Production Environment** (`FinvuAuthEnvironment.PRODUCTION `): Minimal logging and optimized performance

```swift
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
// Initialize the SDK with your app configuration
window.finvu_authentication_bridge.initAuth(initConfig, callbackName);

// Start authentication with phone number
window.finvu_authentication_bridge.startAuth(phoneNumber, callbackName);

// Verify OTP
window.finvu_authentication_bridge.verifyOtp(phoneNumber, otp, callbackName);
```

### Method Details

#### 1. initAuth(initConfig, callbackName)
Initializes the Finvu authentication SDK.

**Parameters:**
- `initConfig` (string): JSON configuration containing your app ID
- `callbackName` (string): JavaScript callback function name

**Example:**
```javascript
const config = JSON.stringify({ appId: "YOUR_APP_ID" });
window.finvu_authentication_bridge.initAuth(config, "handleInitAuthResponse");
```

**Success Response:**
```json
{
  "status": "SUCCESS",
  "statusCode": "200"
}
```

**Failure Responses:**
```json
// Missing or empty app ID
{
  "status": "FAILURE",
  "errorCode": "1001",
  "errorMessage": "appId is required"
}

// SDK initialization failed
{
  "status": "FAILURE",
  "errorCode": "1002",
  "errorMessage": "Authentication failed, SDK initialization failed. Please try initializing the SDK again."
}
```

#### 2. startAuth(phoneNumber, callbackName)
Starts the authentication process for a phone number.

**Parameters:**
- `phoneNumber` (string): User's mobile number (without country code)
- `callbackName` (string): JavaScript callback function name

**Example:**
```javascript
window.finvu_authentication_bridge.startAuth("9876543210", "handleStartAuthResponse");
```

**Success Responses:**
```json


// Authentication completed with token
{
  "status": "SUCCESS",
  "statusCode": "200",
  "authType": "SILENT_AUTH",
  "token": "your_auth_token_here"
}
```

**Failure Responses:**
```json
// Invalid phone number format
{
  "status": "FAILURE",
  "errorCode": "1001",
  "errorMessage": "Invalid phone number format"
}

// Silent Network Authentication failed
{
  "status": "FAILURE",
  "errorCode": "1002",
  "errorMessage": "Authentication failed, SNA failed."
}

// Generic failure
{
  "status": "FAILURE",
  "errorCode": "1002",
  "errorMessage": "Authentication failed, something went wrong."
}
```

#### 3. verifyOtp(phoneNumber, otp, callbackName)
Verifies the OTP entered by the user.

**Parameters:**
- `phoneNumber` (string): User's mobile number (same as used in startAuth)
- `otp` (string): OTP entered by user
- `callbackName` (string): JavaScript callback function name

**Example:**
```javascript
window.finvu_authentication_bridge.verifyOtp("9876543210", "123456", "handleVerifyOtpResponse");
```

**Success Response:**
```json
{
  "status": "SUCCESS",
  "statusCode": "200",
  "authType": "OTP",
  "token": "your_auth_token_here"
}
```

**Failure Responses:**
```json
// Invalid OTP format
{
  "status": "FAILURE",
  "errorCode": "1001",
  "errorMessage": "Invalid OTP format"
}

// OTP verification failed
{
  "status": "FAILURE",
  "errorCode": "1002",
  "errorMessage": "Authentication failed, something went wrong."
}
```

### Callback Flow & Status Handling

> **Important:** After calling `startAuth(phoneNumber, callbackName)`, the same callback function will be invoked for all subsequent statuses in the authentication flow, including `INITIATE`, `OTP_AUTO_READ`, `VERIFY`, and `SUCCESS`.
>
> - **Silent Authentication**: If `authType` is `SILENT_AUTH`, wait for a `SUCCESS` status with a `token` in the same callback before proceeding.
> - **OTP Flow**: If `authType` is `OTP`, prompt the user to enter the OTP, then call `verifyOtp`. The response will be delivered to its own callback.
> - **Auto-read**: If OTP auto-read is successful, you may receive `OTP_AUTO_READ` and then `SUCCESS` automatically.

### Example Integration

```javascript
// Global callback functions
window.handleInitAuthResponse = (responseStr) => {
  const response = JSON.parse(responseStr);
  if (response.status === "SUCCESS") {
    console.log("SDK initialized successfully");
    // Proceed with authentication
  } else {
    console.error("SDK initialization failed:", response.errorMessage);
  }
};

window.handleStartAuthResponse = (responseStr) => {
  const response = JSON.parse(responseStr);
  console.log("Auth response:", response);
  
  switch (response.status) {
    case "INITIATE":
      // Show OTP input field
      showOtpInput();
      break;

    case "SUCCESS":
      if (response.token) {
        // Use token for API calls
        handleAuthSuccess(response.token);
      }
      break;
    case "FAILURE":
      // Handle error
      showError(response.errorMessage);
      break;
  }
};

window.handleVerifyOtpResponse = (responseStr) => {
  const response = JSON.parse(responseStr);
  if (response.status === "SUCCESS" && response.token) {
    handleAuthSuccess(response.token);
  } else {
    showError(response.errorMessage || "OTP verification failed");
  }
};

// Usage
function initializeAuth() {
  const config = JSON.stringify({ appId: "YOUR_APP_ID" });
  window.finvu_authentication_bridge.initAuth(config, "handleInitAuthResponse");
}

function startAuthentication(phoneNumber) {
  window.finvu_authentication_bridge.startAuth(phoneNumber, "handleStartAuthResponse");
}

function verifyOTP(phoneNumber, otp) {
  window.finvu_authentication_bridge.verifyOtp(phoneNumber, otp, "handleVerifyOtpResponse");
}
```

---

## 📤 Response Format & Error Code Reference

### Response Structure

**Success Responses** contain:
- `status`: Operation status (SUCCESS, INITIATE, etc.)
- `statusCode`: HTTP-style status code (e.g., "200")
- Additional fields like `token`, `authType`, `otp`, etc.

**Failure Responses** contain:
- `status`: Always "FAILURE"
- `errorCode`: Specific error code for troubleshooting
- `errorMessage`: Human-readable error description

### Status Types

| Status              | Description                          | Next Action                        |
|---------------------|--------------------------------------|------------------------------------|
| SUCCESS             | Operation completed successfully     | Use token or proceed               |
| FAILURE             | Operation failed                     | Handle error, retry if appropriate |

### Error Codes (Only in Failure Responses)

| Error Code | Description                    | Common Causes                                    |
|------------|--------------------------------|--------------------------------------------------|
| 1001       | Invalid parameter              | Missing appId, invalid phone number/OTP format  |
| 1002       | Generic failure                | Network issues, service unavailable             |

### Input Validation Rules

- **Phone Number**: 10 digits, cannot start with 0
- **OTP**: 4-8 digits only
- **App ID**: Required, cannot be empty

---

## ❓ FAQ

### Q: What conditions are required for Silent Network Authentication (SNA)?
**A:** For SNA to work properly:
- **SIM internet must be ON** (mobile data enabled)
- **WiFi must be OFF** (disconnect from WiFi networks)
- Device must have active mobile network connectivity
- SIM card must support the required network protocols

If these conditions are not met, the SDK will automatically fall back to OTP-based authentication.

### Q: Why am I getting error code 1001?
**A:** Error 1001 indicates invalid parameters. Check:
- App ID is provided and not empty in initAuth
- Phone number format is correct (7-15 digits, no leading zero)
- OTP format is correct (4-8 digits) when calling verifyOtp

### Q: How do I handle multiple authentication statuses?
**A:** Use the same callback function for startAuth to receive all status updates (INITIATE, OTP_AUTO_READ, VERIFY, SUCCESS). Each status provides context for the next step in the authentication flow.

---

## 🛡️ ProGuard

The SDK provides consumer rules for required dependencies. No extra configuration is needed unless you have custom requirements.
