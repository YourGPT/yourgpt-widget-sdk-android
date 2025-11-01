# YourGPT Android SDK

A Kotlin SDK for integrating YourGPT chatbot widget as a full-screen view in Android applications.

## Quick Start

### Installation

Add the dependency to your app's `build.gradle` file:

```gradle
dependencies {
    implementation 'com.yourgpt:android-sdk:1.0.0'
    implementation 'androidx.webkit:webkit:1.8.0'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0' // For coroutines support
}
```

### Development Environment Setup

For local development and testing, see [DEV_SETUP.md](./DEV_SETUP.md) for detailed instructions on:
- Setting up Android Studio development environment
- Running the example app locally with Android emulators
- Testing on physical Android devices
- Debugging with Android Studio tools and profilers
- Performance testing and memory analysis

## Integration Guide

Follow these steps to integrate YourGPT SDK into your Android application:

### Step 1: Update `build.gradle` (App Module)

Add required dependencies to your app's `build.gradle` file:

```gradle
dependencies {
    implementation "org.jetbrains.kotlin:kotlin-stdlib:1.8.20"
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'
    implementation 'androidx.webkit:webkit:1.8.0'

    // YourGPT SDK
    implementation 'com.yourgpt:android-sdk:1.0.0'
}
```

### Step 2: Update `AndroidManifest.xml`

Add required permissions and register the ChatbotActivity:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Required permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:allowBackup="true"
        android:icon="@drawable/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.AppCompat.Light.DarkActionBar">

        <!-- Your main activity -->
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- YourGPT SDK ChatbotActivity - Required -->
        <activity
            android:name="com.yourgpt.sdk.ChatbotActivity"
            android:exported="false"
            android:theme="@style/Theme.AppCompat.Light.NoActionBar"
            android:hardwareAccelerated="true" />
    </application>

</manifest>
```

### Step 3: Create Your Activity (e.g., `MainActivity.kt`)

Implement `YourGPTEventListener` and initialize the SDK:

```kotlin
package com.yourapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yourgpt.sdk.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), YourGPTEventListener {

    private lateinit var openChatButton: Button
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupUI()
        setupSDK()
        initializeSDK()
    }

    private fun setupUI() {
        openChatButton = findViewById(R.id.btn_open_chat)
        statusText = findViewById(R.id.tv_status)

        openChatButton.setOnClickListener {
            openChatbot()
        }

        // Disable button until SDK is ready
        openChatButton.isEnabled = false
        statusText.text = "SDK Status: Initializing..."
    }

    private fun setupSDK() {
        // Set event listener
        YourGPTSDK.setEventListener(this)

        // Observe SDK state changes
        lifecycleScope.launch {
            YourGPTSDK.stateFlow.collect { state ->
                updateUIForSDKState(state)
            }
        }
    }

    private fun initializeSDK() {
        // Configure SDK with your widget UID
        val configuration = YourGPTConfig(
            widgetUid = "your-widget-uid-here",  // Replace with your actual widget UID
        )

        // Initialize SDK
        lifecycleScope.launch {
            try {
                YourGPTSDK.initialize(configuration)
            } catch (error: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "SDK initialization failed: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun updateUIForSDKState(state: YourGPTSDKState) {
        runOnUiThread {
            statusText.text = "SDK Status: ${state.connectionState.name.lowercase().replaceFirstChar { it.uppercase() }}"

            when (state.connectionState) {
                YourGPTConnectionState.CONNECTED -> {
                    statusText.setTextColor(0xFF28A745.toInt()) // Green
                    openChatButton.isEnabled = true
                }
                YourGPTConnectionState.CONNECTING -> {
                    statusText.setTextColor(0xFFFFC107.toInt()) // Orange
                    openChatButton.isEnabled = false
                }
                YourGPTConnectionState.ERROR -> {
                    statusText.setTextColor(0xFFDC3545.toInt()) // Red
                    openChatButton.isEnabled = false
                    state.error?.let {
                        statusText.text = "SDK Error: $it"
                    }
                }
                YourGPTConnectionState.DISCONNECTED -> {
                    statusText.setTextColor(0xFF6C757D.toInt()) // Gray
                    openChatButton.isEnabled = false
                }
            }
        }
    }

    private fun openChatbot() {
        val configuration = YourGPTConfig(
            widgetUid = "your-widget-uid-here",  // Replace with your actual widget UID
        )

        YourGPTSDK.openChatbot(this, configuration)
    }

    // YourGPTEventListener implementation
    override fun onMessageReceived(message: Map<String, Any>) {
        runOnUiThread {
            Toast.makeText(
                this,
                "New message: $message",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onChatOpened() {
        runOnUiThread {
            Toast.makeText(this, "Chat opened", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onChatClosed() {
        runOnUiThread {
            Toast.makeText(this, "Chat closed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onError(error: String) {
        runOnUiThread {
            Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
        }
    }

    override fun onLoadingStarted() {
        runOnUiThread {
            Toast.makeText(this, "Loading started", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onLoadingFinished() {
        runOnUiThread {
            Toast.makeText(this, "Loading finished", Toast.LENGTH_SHORT).show()
        }
    }
}
```

### Step 4: Create Layout File (`res/layout/activity_main.xml`)

Create a simple layout with a button to open the chatbot:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp"
    android:gravity="center">

    <TextView
        android:id="@+id/tv_status"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="SDK Status: Initializing..."
        android:textSize="16sp"
        android:layout_marginBottom="32dp" />

    <Button
        android:id="@+id/btn_open_chat"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Open YourGPT Chat"
        android:textSize="16sp"
        android:padding="16dp" />

</LinearLayout>
```

## Configuration Options

### YourGPTConfig

```kotlin
val configuration = YourGPTConfig(
    widgetUid = "your-widget-uid",    // Required: Your YourGPT widget UID
    debug = true                      // Optional: Enable debug logs (default: false)
)
```

## SDK Methods

### Initialize SDK

```kotlin
lifecycleScope.launch {
    try {
        YourGPTSDK.initialize(configuration)
    } catch (error: Exception) {
        // Handle initialization error
    }
}
```

### Open Chatbot

```kotlin
// Direct method
YourGPTSDK.openChatbot(context, configuration)

// Or create intent for custom handling
val intent = YourGPTSDK.createChatbotIntent(context, configuration)
startActivity(intent)
```

### Set Event Listener

```kotlin
YourGPTSDK.setEventListener(this) // 'this' implements YourGPTEventListener
```

### Observe SDK State

```kotlin
lifecycleScope.launch {
    YourGPTSDK.stateFlow.collect { state ->
        when (state.connectionState) {
            YourGPTConnectionState.CONNECTED -> {
                // SDK is ready
            }
            YourGPTConnectionState.CONNECTING -> {
                // SDK is connecting
            }
            YourGPTConnectionState.ERROR -> {
                // Handle error: state.error
            }
            YourGPTConnectionState.DISCONNECTED -> {
                // SDK is disconnected
            }
        }
    }
}
```

## Event Listener Interface

Implement `YourGPTEventListener` to receive SDK events:

```kotlin
interface YourGPTEventListener {
    fun onMessageReceived(message: Map<String, Any>)  // New message from chatbot
    fun onChatOpened()                                 // Chat interface opened
    fun onChatClosed()                                 // Chat interface closed
    fun onError(error: String)                         // Error occurred
    fun onLoadingStarted()                             // Loading started
    fun onLoadingFinished()                            // Loading finished
}
```

## SDK States

The SDK provides real-time state updates through `YourGPTSDKState`:

- **CONNECTED**: SDK is connected and ready to use
- **CONNECTING**: SDK is initializing/connecting
- **DISCONNECTED**: SDK is disconnected
- **ERROR**: An error occurred (check `state.error` for details)

## Requirements

- Android API level 21 (Android 5.0) or higher
- Kotlin 1.8.0 or higher
- AndroidX libraries

## ProGuard/R8

If you're using code obfuscation, add these rules to your `proguard-rules.pro`:

```proguard
-keep class com.yourgpt.sdk.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
```

## Events

The widget sends these events via the event listener:
- `onMessageReceived` - New message received from chatbot
- `onChatOpened` - Chat interface opened
- `onChatClosed` - Chat interface closed
- `onError` - Error occurred during operation