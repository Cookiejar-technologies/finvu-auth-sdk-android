# Finvu Auth SDK — Android

**Version:** `1.1.2` · **Min SDK:** API 25 · **Kotlin:** 1.9.0+

Silent Network Authentication (SNA) and Device Binding SDK for Android, with WebView bridge support for web-based authentication flows.

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
    implementation("io.github.cookiejar-technologies:finvuauthenticationsdk:1.1.2")
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

Use this if your app handles the authentication flow entirely in Kotlin, without a WebView. The SDK offers two factors: **SNA** (Silent Network Authentication) and **Device Binding** (a hardware-backed key that proves "this user, on this device").

Every method returns its result through a `Result<JSONObject>` callback. On failure, `result.exceptionOrNull()` is a `FinvuAuthException` with `status`, `errorCode` and `errorMessage`.

```kotlin
import com.finvu.android.authenticationwrapper.FinvuAuthenticationNativeWrapper
import com.finvu.android.authenticationwrapper.models.FinvuAuthException
import com.finvu.android.authenticationwrapper.utils.FinvuAuthEnvironment

val nativeWrapper = FinvuAuthenticationNativeWrapper()

// Setup — call once, before any other method
nativeWrapper.setup(FinvuAuthEnvironment.PRODUCTION, activity, lifecycleScope)  // or DEVELOPMENT

// Cleanup — call when done or the user exits
nativeWrapper.onDestroy()
```

> `activity` must be a `FragmentActivity` (e.g. `AppCompatActivity`); device binding shows the system biometric prompt from it.

#### SNA

```kotlin
// 1. Init — call with the requestId from your backend
nativeWrapper.initSNA(mutableMapOf<String, Any>("requestId" to "REQUEST_ID")) { result ->
    result.onSuccess { json -> /* {"status":"SUCCESS","mcc":"404","mnc":"90"} — proceed to startSNA */ }
    result.onFailure { e -> val error = e as FinvuAuthException /* error.errorCode, error.errorMessage */ }
}

// 2. Start SNA — call with the SNA URL returned by your backend
nativeWrapper.startSNA("SNA_URL") { result ->
    result.onSuccess { json -> val snaToken = json.optString("snaToken") /* send to your backend */ }
    result.onFailure { e -> /* handle FinvuAuthException */ }
}
```

`initAuth` / `startAuth` are also supported and behave exactly like `initSNA` / `startSNA`.

The `requestId` passed to `initSNA` is attached to every SNA event the SDK logs.

#### Device Binding

Device binding creates a key pair in the Android Keystore. The private key never leaves the device and can only be used after the user unlocks with biometrics or the device PIN/pattern/password. Your backend stores the public key and verifies signatures with it.

**Typical flow**

1. **Check** — `getDeviceBindingState` with the `keyId` you stored for this user.
2. **Enroll** (state `NOT_ENROLLED` / `INVALIDATED`, or a new device) — `enrollDeviceBinding`, then store the returned `keyId` + `publicKey` on your backend against the user.
3. **Verify** — your backend issues a challenge; call `signDeviceBindingChallenge` with every `keyId` it has for the user; your backend verifies the returned signature.

> Call `enrollDeviceBinding` and `signDeviceBindingChallenge` on the main thread.

```kotlin
// 1. Check state — local, no prompt, no network. Omit keyId to only check device support.
nativeWrapper.getDeviceBindingState(mutableMapOf<String, Any>("keyId" to "STORED_KEY_ID")) { result ->
    result.onSuccess { json -> val state = json.getString("state") /* ENROLLED, NOT_ENROLLED, INVALIDATED, UNSUPPORTED, NO_SCREEN_LOCK */ }
}

// 2. Enroll — shows the device-lock prompt, then creates a new key
nativeWrapper.enrollDeviceBinding(
    mutableMapOf<String, Any>(
        "requestId" to "REQUEST_ID",
        "promptTitle" to "Verify it's you",                      // optional
        "promptSubtitle" to "Confirm to secure your account",    // optional
        "attestationChallenge" to "SERVER_NONCE",                // optional
    )
) { result ->
    result.onSuccess { json ->
        val keyId = json.getString("keyId")          // store on your backend
        val publicKey = json.getString("publicKey")  // store on your backend
    }
    result.onFailure { e -> /* handle FinvuAuthException — see error codes below */ }
}

// 3. Sign — shows the device-lock prompt, then signs the challenge
nativeWrapper.signDeviceBindingChallenge(
    mutableMapOf<String, Any>(
        "requestId" to "REQUEST_ID",
        "allowedKeyIds" to listOf("KEY_ID_1", "KEY_ID_2"),  // every keyId your backend has for this user
        "challenge" to "SERVER_CHALLENGE",
    )
) { result ->
    result.onSuccess { json -> val signature = json.getString("signature") /* send with keyId to your backend */ }
    result.onFailure { e -> /* handle FinvuAuthException — see error codes below */ }
}
```

**Parameters**

| Method | Key | Required | Description |
|---|---|---|---|
| `getDeviceBindingState` | `keyId` | No | `keyId` from a previous enroll. Omit to only check whether the device supports device binding. |
| `enrollDeviceBinding` | `requestId` | Recommended | Your backend's request id; attached to the logged events. |
| | `promptTitle`, `promptSubtitle` | No | Text on the device-lock prompt. |
| | `attestationChallenge` | No | Server nonce. When set, the response includes a Keystore attestation chain proving the key is hardware-backed. |
| `signDeviceBindingChallenge` | `allowedKeyIds` | Yes | List of every `keyId` your backend has for this user (a single string is also accepted). The SDK signs with whichever exists on this device. |
| | `challenge` | Yes | Server-issued challenge. Signed exactly as its UTF-8 bytes. |
| | `requestId` | Recommended | Your backend's request id; attached to the logged events. |
| | `promptTitle`, `promptSubtitle` | No | Text on the device-lock prompt. |

`entityId` may also be passed to `enrollDeviceBinding` / `signDeviceBindingChallenge` and is attached to the logged events.

**Success responses**

```json
// getDeviceBindingState — keyId only when state is ENROLLED or INVALIDATED
{"status":"SUCCESS","state":"ENROLLED","keyId":"6f1c2a9e-3b7d-4c1a-9e2f-8a5b3c7d1e90"}

// enrollDeviceBinding — attestation only when attestationChallenge was passed and the device supports it
{"status":"SUCCESS","keyId":"6f1c2a9e-...","publicKey":"MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE...","algorithm":"ES256","attestation":"MIIC...,MIIC..."}

// signDeviceBindingChallenge
{"status":"SUCCESS","keyId":"6f1c2a9e-...","signature":"MEUCIQD...","algorithm":"ES256"}
```

| State | Meaning | What to do |
|---|---|---|
| `ENROLLED` | Key exists and is usable | Sign |
| `NOT_ENROLLED` | No key for this `keyId` on this device (or no `keyId` passed) | Enroll |
| `INVALIDATED` | Key exists but is no longer usable (e.g. a new fingerprint was added) | Enroll again and replace the `keyId` on your backend |
| `UNSUPPORTED` | Device cannot do device binding | Use another factor |
| `NO_SCREEN_LOCK` | No PIN/pattern/password set | Ask the user to set a screen lock |

**Error codes** (`FinvuAuthException.errorCode`)

| errorCode | Method | When | Suggested handling |
|---|---|---|---|
| `DEVICE_NOT_SECURE` | enroll | No screen lock on the device | Ask the user to set a screen lock |
| `DEVICE_BINDING_UNSUPPORTED` | enroll | Device has no usable secure hardware/authenticator | Use another factor |
| `DEVICE_BINDING_NOT_ENROLLED` | sign | None of `allowedKeyIds` exist on this device, or the list is empty | Treat as a new device: verify the user another way, then enroll |
| `DEVICE_BINDING_KEY_INVALIDATED` | sign | The key was invalidated (biometrics changed) and has been removed | Enroll again and replace the `keyId` |
| `DEVICE_BINDING_USER_CANCELLED` | enroll, sign | User dismissed the prompt | Let the user retry |
| `DEVICE_BINDING_AUTH_FAILED` | enroll, sign | Prompt failed (e.g. too many attempts) | Show the message; retry later |
| `DEVICE_BINDING_ENROLL_FAILED` | enroll | Key could not be created | Retry, then fall back |
| `DEVICE_BINDING_SIGN_FAILED` | sign | Signing failed | Retry, then fall back |
| `INVALID_CHALLENGE` | sign | `challenge` missing or empty | Fix the request |
| `SESSION_NOT_INITIALIZED` | all | `setup` was not called | Call `setup` first |

**Backend verification**

- Store `keyId` and `publicKey` per user **per device**; a user may have several.
- `publicKey` is a Base64 X.509 SubjectPublicKeyInfo (EC P-256). Verify `signature` (Base64, DER-encoded ECDSA) as **ES256 / SHA256withECDSA** over the UTF-8 bytes of the challenge you issued.
- After re-enrolling on the same device, replace that device's old `keyId`; on a new device, add the new one.

#### Capabilities

```kotlin
val capabilities = nativeWrapper.getCapabilities()
// {"status":"SUCCESS","sdkVersion":"1.1.2","platform":"android","bridgeVersion":2,"factors":["SNA","DEVICE_BINDING"],"supportedMethods":[...]}
```

---

## Support

support@cookiejar.co.in · [finvu.in](https://finvu.in)
