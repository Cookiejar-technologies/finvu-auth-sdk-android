# FinvuAuthSDK

A simple, secure Android SDK for integrating Finvu authentication into your app, with seamless support for WebView-based flows and JavaScript bridging.

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
    implementation("com.finvu.android:finvuAuthenticationSDK:1.0.1") // Use the latest version
}
```

### 4. Add Network Security Config
Add the following attribute to your `<application>` tag in your `AndroidManifest.xml`:

```xml
<application
    ...
    android:networkSecurityConfig="@xml/otpless_network_security_config"
    ... >
    <!-- Other attributes and activities -->
</application>
```

---

## 🚀 Android Integration

### 1. Setup the WebView Bridge (Recommended)

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
        FinvuAuthenticationWrapper.instance.setupWebView(
            webView = webView,
            activity = this,
            scope = lifecycleScope
        )

        // Load your web app
        webView.loadUrl("https://your-web-app-url")
    }

    override fun onDestroy() {
        super.onDestroy()
        FinvuAuthenticationWrapper.instance.onDestroy()
    }
}
```

- **That's it!** The bridge is ready for your web app to use.

---

## 🌐 WebView/JavaScript Usage

Once the bridge is set up, your web app can call the following methods from JavaScript:

```javascript
window.finvu_authentication_bridge.initAuth(appId, callbackName);
window.finvu_authentication_bridge.startAuth(phoneNumber, callbackName);
window.finvu_authentication_bridge.verifyOtp(phoneNumber, otp, callbackName);
```

### Callback Flow & Status Handling

> **Important:** After you call `startAuth(phoneNumber, callbackName)`, the **same callback function** (e.g., `handleStartAuthResponse`) will be invoked for all subsequent statuses in the authentication flow. This includes `INITIATE`, `OTP_AUTO_READ`, and `SUCCESS` (with token), as well as any errors (`FAILURE`).
>
> - If OTP auto-read is successful, you may receive `OTP_AUTO_READ` and then `SUCCESS` (with token) automatically, all in the same callback.
> - If OTP auto-read is not possible, you will receive `INITIATE` and should prompt the user to enter the OTP, then call `verifyOtp`. The response to `verifyOtp` will be delivered to its own callback.
> - **In the case of silent authentication (`SILENT_AUTH`), wait for a `SUCCESS` status with a `token` in the same callback before proceeding. The flow is fully automatic and the token will be delivered in the same callback sequence.**

### Example: Handling Responses in JS

Define global callback functions to handle responses from the bridge:

```javascript
window.handleStartAuthResponse = (responseStr) => {
  const response = JSON.parse(responseStr);
  if (response.status === "FAILURE") {
    alert(response.errorMessage);
  } else if (response.status === "INITIATE") {
    // Prompt user for OTP
  } else if (response.status === "OTP_AUTO_READ") {
    // Auto-submit or show OTP
  } else if (response.status === "SUCCESS" && response.token) {
    // Use token for further API calls
  }
  // ... handle other statuses
};
```

### Example Integration (React/JS)

```jsx
<button onClick={() => window.finvu_authentication_bridge.initAuth("YOUR_APP_ID", "handleInitAuthResponse")}>Init Auth</button>
<button onClick={() => window.finvu_authentication_bridge.startAuth("MOBILE_NUMBER", "handleStartAuthResponse")}>Start Auth</button>
<input type="text" onChange={e => setOtp(e.target.value)} />
<button onClick={() => window.finvu_authentication_bridge.verifyOtp("MOBILE_NUMBER", otp, "handleVerifyOtpResponse")}>Verify OTP</button>
```

---

## 📤 Response Format & Status Reference

All responses to your JavaScript callback are sent as a **JSON string**. Parse it and check the `status` field to determine the result and next action.

### Possible Statuses & Actions

| Status              | When Returned                | Example JSON                                                                 | Typical Action for App/Web         |
|---------------------|-----------------------------|------------------------------------------------------------------------------|------------------------------------|
| SUCCESS             | SDK ready, token received    | `{ "status": "SUCCESS", "statusCode": "200", "token": "..." }`           | Proceed, use token                 |
| INITIATE            | After startAuth, OTP sent    | `{ "status": "INITIATE", "statusCode": "200", "authType": "OTP", "deliveryChannel": "SMS" }` | Prompt for OTP                     |
| OTP_AUTO_READ       | OTP auto-read                | `{ "status": "OTP_AUTO_READ", "statusCode": "200", "otp": "123456" }`     | Auto-submit or show OTP            |
| VERIFY              | OTP verified                 | `{ "status": "VERIFY", "statusCode": "200", "authType": "OTP" }`         | Proceed, show success              |
| DELIVERY_STATUS     | Delivery info update         | `{ "status": "DELIVERY_STATUS", ... }`                                     | Show delivery status               |
| FALLBACK_TRIGGERED  | Fallback flow triggered      | `{ "status": "FALLBACK_TRIGGERED", ... }`                                  | Handle fallback                    |
| FAILURE             | Any error                    | `{ "status": "FAILURE", "errorCode": "1002", "errorMessage": "..." }`     | Show error, allow retry            |
| SILENT_AUTH         | Silent authentication        | `{ "status": "VERIFY", "statusCode": "200", "authType": "SILENT_AUTH" }` | Wait for SUCCESS with token        |

#### Example: Silent Auth
- If `authType` is `SILENT_AUTH`, **wait for a `SUCCESS` status with a `token` in the same callback** before proceeding. The flow is fully automatic and the token will be delivered in the same callback sequence.

#### Example: OTP
- If `authType` is `OTP`, prompt the user to enter the OTP sent to their phone.

#### Example: Token
- If the response contains a `token`, use it for your backend or further API calls.

#### Example: Failure
- If `status` is `FAILURE`, show the `errorMessage` to the user and allow them to retry.

### Common Error Codes & Messages

| Error Code | When/Why                                 | Message                                                         |
|------------|------------------------------------------|-----------------------------------------------------------------|
| 1002       | Generic failure                          | Authentication failed, something went wrong.                    |
| 5003       | SDK initialization failed                | Authentication failed, SDK initialization failed.               |
| 9106       | Silent auth failed                       | Authentication failed, something went wrong.                    |
| (custom)   | OTP auto-read failed, token missing, etc | See errorMessage in response                                    |

---

## 🛡️ ProGuard

If you use ProGuard/R8, the SDK provides consumer rules for the required dependencies. No extra configuration is needed unless you have custom requirements.

---

## 📝 License
 
[MIT](./LICENSE)

---

## 💬 Support

For issues or feature requests, please open an [issue](https://github.com/Cookiejar-technologies/finvu-auth-sdk-android/issues).
