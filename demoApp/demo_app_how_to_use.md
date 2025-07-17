# Finvu Authentication SDK Demo App

This demo application showcases the integration and usage of the **Finvu Authentication SDK for Android**. The app demonstrates how to implement secure authentication flows using a WebView-based approach with the Finvu Authentication wrapper.

## 📱 What This Demo Shows

The demo app demonstrates:
- Integration of the Finvu Authentication SDK
- WebView-based authentication flow
- Secure authentication wrapper setup
- Real-time authentication with a test web application

## 🚀 Features

- **Simple UI**: Clean interface with a single button to initiate authentication
- **WebView Integration**: Secure WebView setup optimized for authentication flows
- **SDK Wrapper**: Demonstrates proper use of `FinvuAuthenticationWrapper`

## 📋 Prerequisites

Before running the demo app, ensure you have:

- **Android Studio** (latest stable version recommended)
- **Android SDK** with minimum API level 25 (Android 7.1)
- **GitHub access** to the Finvu SDK repository
- **Valid GitHub Personal Access Token** for Maven repository access

## 🛠️ Setup Instructions

### 1. Clone the Repository
```bash
git clone https://github.com/Cookiejar-technologies/finvu-auth-sdk-android
cd finvu-auth-sdk-android/demoApp
```

### 2. Configure GitHub Access
The demo app requires access to the private Finvu SDK Maven repository. Update the credentials in `settings.gradle.kts`:

```kotlin
maven {
    url = uri("https://maven.pkg.github.com/Cookiejar-technologies/finvu-auth-sdk-android")
    credentials {
        username = "your-github-username"
        password = "your-github-personal-access-token"
    }
}
```

### 3. Open in Android Studio
1. Open Android Studio
2. Select "Open an existing project"
3. Navigate to the `demoApp` folder
4. Wait for Gradle sync to complete

### 4. Build and Run
1. Connect an Android device or start an emulator
2. Click the "Run" button in Android Studio
3. The app will install and launch on your device

## 📱 How to Use the Demo

1. **Launch the App**: Open the FinvuAuthDemo app on your device
2. **Load Authentication**: Tap the "Load WebView" button in the center of the screen
3. **Authentication Flow**: The app will load the Finvu test authentication interface
4. **Complete Authentication**: Follow the authentication prompts in the WebView
5. **Success**: The authentication flow will complete and return results

## 🏗️ App Architecture

### Key Components

- **MainActivity.kt**: Main activity handling WebView setup and SDK integration
- **WebView Configuration**: Optimized settings for secure authentication
- **FinvuAuthenticationWrapper**: SDK wrapper managing authentication flow

### Dependencies

```kotlin
implementation("com.finvu.android:finvuauthenticationsdk:1.0.0")
```

### WebView Configuration
The app configures WebView with optimal settings for authentication:
- JavaScript enabled
- DOM storage enabled
- Mixed content compatibility
- Cache disabled for security
- Wide viewport support

## 🔧 Technical Specifications

- **Minimum SDK**: API 25 (Android 7.1)
- **Target SDK**: API 35 (Android 14)
- **Language**: Kotlin
- **UI Framework**: Android Views with Jetpack Compose support
- **Architecture**: Single Activity with WebView

## 🐛 Troubleshooting

### Common Issues

1. **Build Failures**
   - Verify GitHub credentials in `settings.gradle.kts`
   - Ensure your GitHub token has appropriate permissions
   - Check internet connectivity

2. **WebView Not Loading**
   - Verify device has internet access
   - Check if JavaScript is enabled in WebView settings
   - Ensure test URL is accessible

3. **Authentication Errors**
   - Verify SDK integration is correct
   - Check WebView configuration
   - Review logcat for detailed error messages

### Debugging Tips

- Enable WebView debugging for detailed logs
- Use Android Studio's debugger to step through authentication flow
- Check network requests in Chrome DevTools (if WebView debugging enabled)

## 📚 SDK Integration Reference

For detailed SDK documentation and integration guide, refer to:
- Main SDK documentation
- API reference guide
- Integration best practices

## 🤝 Support

For issues related to:
- **Demo App**: Check this README and troubleshooting section
- **SDK Integration**: Refer to main SDK documentation
- **Authentication Issues**: Contact Finvu support team

## 📄 License

This demo app is provided as part of the Finvu Authentication SDK package. Please refer to the main project license for terms and conditions.

---

**Note**: This is a demonstration application intended for development and testing purposes. For production implementation, follow the complete SDK integration guide and security best practices. 