# YourGPT Android SDK - Push Notification Setup

This guide explains how to enable push notifications in your Android app using the YourGPT SDK. When set up, your users will receive notifications for new messages from the YourGPT widget even when the app is in the background or closed.

## Features

- **Background Notifications**: Receive messages when the app is closed or in the background
- **Automatic Token Management**: FCM token is fetched, cached, and registered with the backend automatically
- **Two Modes**: Minimalist (auto-handles everything) or Advanced (custom handling)

## Prerequisites

1. A Firebase project linked to your Android app
2. Your YourGPT **widget UID**
3. Android 5.0 (API 21) or higher
4. Google Play Services on the target device

---

## Step 1: Firebase Configuration

1. Go to [Firebase Console](https://console.firebase.google.com) and create a project (or use an existing one)
2. Add your Android app (use your app's package name)
3. Download the `google-services.json` file
4. Place `google-services.json` in your app module directory (e.g., `app/`)

### Add the Google Services plugin

In your **project-level** `build.gradle`:

```gradle
buildscript {
    dependencies {
        classpath 'com.google.gms:google-services:4.4.0'
    }
}
```

In your **app-level** `build.gradle`:

```gradle
plugins {
    id 'com.android.application'
    id 'com.google.gms.google-services'
}
```

The YourGPT SDK already bundles Firebase Messaging — no additional Firebase dependencies are needed.

---

## Step 2: Configure Push Notifications on YourGPT Dashboard

Before your backend can send FCM notifications, you need to upload your Firebase credentials to the YourGPT dashboard.

1. Go to **Firebase Console** → **Project Settings** → **General** and note your **Project Number**
2. Go to **Firebase Console** → **Project Settings** → **Service Accounts**
3. Click **"Generate new private key"** and download the `.json` file
4. Log in to the [YourGPT Dashboard](https://app.yourgpt.ai)
5. Navigate to your chatbot's **Settings** → ** Notifications**
6. Enable the **Firebase Cloud Messaging** toggle
7. Enter your **Firebase Project Number**
8. Upload the service account JSON file you downloaded in step 3
9. Click **Save Credentials** — the dashboard will verify the credentials automatically

Once the status shows **"Configured"**, your YourGPT backend is ready to send push notifications.

---

## Step 3: Register the Notification Service

Add `YourGPTNotificationService` to your app's `AndroidManifest.xml` inside the `<application>` tag:

```xml
<!-- Firebase Messaging Service for YourGPT notifications -->
<service
    android:name="com.yourgpt.sdk.YourGPTNotificationService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

Also ensure you have the notification permission declared:

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

---

## Step 4: Initialize the SDK with Notifications

There are two ways to initialize: **Quick Setup** (recommended) or **Full Configuration**.

### Option A: Quick Setup (Recommended)

The simplest way — one line to enable everything:

```kotlin
import com.yourgpt.sdk.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize SDK with notifications in one line
        lifecycleScope.launch {
            YourGPTSDK.quickInitialize(this@MainActivity, "YOUR_WIDGET_UID")
        }
    }
}
```

This automatically:

- Initializes the SDK
- Fetches and caches the FCM token
- Creates the notification channel
- Enables minimalist notification handling

You can also use `YourGPTNotificationClient.quickSetup()` with a custom notification config:

```kotlin
val notifConfig = YourGPTNotificationConfig.builder()
    .setSmallIcon(R.drawable.ic_notification)  // Your app's notification icon
    .setSoundUri(Uri.parse("android.resource://${packageName}/raw/message_sound"))
    .build()

YourGPTNotificationClient.quickSetup(this, "YOUR_WIDGET_UID", notifConfig)
```

### Option B: Full Configuration

For more control over notification behavior:

```kotlin
import com.yourgpt.sdk.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Optional: customize notification icon, sound, vibration, etc.
        val notifConfig = YourGPTNotificationConfig.builder()
            .setSmallIcon(R.drawable.ic_notification)
            .setSoundUri(Uri.parse("android.resource://${packageName}/raw/message_sound"))
            .setVibrationPattern(longArrayOf(0, 300, 200, 300))
            .build()

        val config = YourGPTConfig(
            widgetUid = "YOUR_WIDGET_UID",
            enableNotifications = true,
            notificationMode = NotificationMode.MINIMALIST,  // or ADVANCED
            notificationConfig = notifConfig
        )

        lifecycleScope.launch {
            YourGPTSDK.initialize(this@MainActivity, config)
        }
    }
}
```

---

## Step 5: Request Notification Permission (Android 13+)

Android 13 (API 33) and above requires runtime permission for notifications. Add this to your activity:

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Notifications enabled
        } else {
            // User denied — notifications won't be shown
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ... SDK initialization ...

        requestNotificationPermission()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Already granted
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show explanation to user, then request
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}
```

---

## Step 6: Handle Notification Clicks

When a user taps a notification, the SDK can automatically open the widget. Add click handling in your launcher activity:

```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ... SDK initialization ...

        // Handle notification click that launched the app
        handleNotificationIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Handle notification click when app is already running
        intent?.let { handleNotificationIntent(it) }
    }

    private fun handleNotificationIntent(intent: Intent) {
        // Let the SDK handle it — opens the widget automatically
        if (YourGPTNotificationClient.handleNotificationClick(this, intent)) {
            return
        }

        // Optional: custom handling for the OPEN_WIDGET action
        if (intent.action == "com.yourgpt.sdk.OPEN_WIDGET") {
            // Navigate to your chat screen or open the widget manually
            val config = YourGPTConfig(widgetUid = "YOUR_WIDGET_UID")
            YourGPTSDK.openChatbotBottomSheet(supportFragmentManager, config)
        }
    }
}
```

---

## Step 7: Open the Widget at Least Once

The FCM token is registered with the YourGPT backend **through the WebView JS bridge** when the widget is opened. Until the widget is opened at least once, the backend won't know where to send notifications.

```kotlin
// Open the widget (e.g., on a button click)
val config = YourGPTConfig(widgetUid = "YOUR_WIDGET_UID")
YourGPTSDK.openChatbotBottomSheet(supportFragmentManager, config)
```

After the widget loads, the SDK automatically sends the cached FCM token to the backend. Subsequent token refreshes are also sent automatically the next time the widget is opened.

---

## How It Works

Here's the full notification flow:

```
1. App starts → SDK initializes → FCM token fetched and cached locally
2. User opens widget → Token sent to YourGPT backend via WebView JS bridge
3. New message on backend → FCM data message sent to device
4. YourGPTNotificationService receives message → YourGPTNotificationClient handles it
5. Notification displayed → User taps → Widget opens via handleNotificationClick()
```

### Token Registration Flow

```
App Init
  └→ YourGPTNotificationClient.initialize()
       └→ Fetches FCM token via FirebaseMessaging
       └→ Caches token in memory

Widget Opened
  └→ ChatbotBottomSheetDialog WebView loads
       └→ YourGPTNotificationClient.registerTokenViaWebView(webView)
            └→ Sends token to backend via window.postMessage()
```

---

## Notification Modes

| Mode         | Description                                                                              | Use Case                                |
| ------------ | ---------------------------------------------------------------------------------------- | --------------------------------------- |
| `MINIMALIST` | Auto-handles everything: display, grouping, click actions                                | Most apps — zero custom code needed     |
| `ADVANCED`   | SDK identifies YourGPT notifications but does not display them; your app handles display | Apps that need custom notification UI   |
| `DISABLED`   | No notification handling                                                                 | Apps that don't want push notifications |

### Setting the Mode

```kotlin
// Via config during initialization
val config = YourGPTConfig(
    widgetUid = "YOUR_WIDGET_UID",
    enableNotifications = true,
    notificationMode = NotificationMode.MINIMALIST  // or ADVANCED, DISABLED
)

// Or change at runtime
YourGPTNotificationClient.setNotificationMode(
    NotificationMode.ADVANCED
)
```

---

## Advanced: Notification Configuration

You can customize notification appearance using `YourGPTNotificationConfig`. The config can be passed through either `quickSetup()` or the full `YourGPTConfig`:

```kotlin
val notificationConfig = YourGPTNotificationConfig.builder()
    .setSmallIcon(R.drawable.ic_notification)
    .setSoundUri(Uri.parse("android.resource://${packageName}/raw/message_sound"))
    .setVibrationPattern(longArrayOf(0, 300, 200, 300))
    .build()

// Option 1: Via quickSetup
YourGPTNotificationClient.quickSetup(this, "YOUR_WIDGET_UID", notificationConfig)

// Option 2: Via full config
val config = YourGPTConfig(
    widgetUid = "YOUR_WIDGET_UID",
    enableNotifications = true,
    notificationConfig = notificationConfig
)
lifecycleScope.launch {
    YourGPTSDK.initialize(this@MainActivity, config)
}
```

### Custom Notification Icon

Create a notification icon (monochrome, white-on-transparent) and place it in your drawable resources (e.g., `res/drawable/ic_notification.xml` or `res/drawable-xxhdpi/ic_notification.png`):

```kotlin
val config = YourGPTNotificationConfig.builder()
    .setSmallIcon(R.drawable.ic_notification)  // Status bar icon
    .setLargeIcon(myBitmap)                    // Optional: expanded notification icon
    .build()
```

### Custom Notification Sound

Place your sound file in `res/raw/` (e.g., `res/raw/message_sound.mp3`):

```kotlin
val config = YourGPTNotificationConfig.builder()
    .setSoundUri(Uri.parse("android.resource://${packageName}/raw/message_sound"))
    .build()
```

To disable sound:

```kotlin
val config = YourGPTNotificationConfig.builder()
    .setSoundEnabled(false)
    .build()
```

> **Android 8.0+ (API 26) Note:** On Android O and above, notification sound and vibration are **channel-level** settings. Once a notification channel is created, the OS will not update its sound/vibration settings on subsequent calls. If you change the custom sound after the app has already run, users will need to either reinstall the app or clear app data for the new sound to take effect. Alternatively, use a different `channelId` via `.setChannel()` to create a fresh channel with the new settings.

### Available Configuration Options

| Option                                | Default              | Description                                       |
| ------------------------------------- | -------------------- | ------------------------------------------------- |
| `setNotificationsEnabled(Boolean)`    | `true`               | Enable/disable notifications                      |
| `setSmallIcon(Int)`                   | System default       | Notification icon resource                        |
| `setLargeIcon(Bitmap?)`               | `null`               | Large icon shown in expanded notification         |
| `setSoundEnabled(Boolean)`            | `true`               | Play sound on notification                        |
| `setSoundUri(Uri?)`                   | System default       | Custom notification sound                         |
| `setVibrationEnabled(Boolean)`        | `true`               | Vibrate on notification                           |
| `setVibrationPattern(LongArray)`      | `[0, 250, 250, 250]` | Custom vibration pattern                          |
| `setLedEnabled(Boolean)`              | `true`               | LED indicator                                     |
| `setLedColor(Int)`                    | `Color.BLUE`         | LED color                                         |
| `setPriority(Int)`                    | `PRIORITY_HIGH`      | Notification priority                             |
| `setAutoCancel(Boolean)`              | `true`               | Dismiss notification on tap                       |
| `setQuietHours(Boolean, Int, Int)`    | Disabled             | Suppress notifications during hours (24h format)  |
| `setMessagePreview(Boolean, Int)`     | `true`, 100 chars    | Show message preview in notification              |
| `setChannel(String, String, String)`  | `yourgpt_messages`   | Custom notification channel ID, name, description |
| `setGroupMessages(Boolean)`           | `true`               | Group notifications by conversation               |
| `setStackNotifications(Boolean, Int)` | `true`, 5            | Stack notifications with a max count              |

---

## SDK Methods Reference

### Notification Identification

```kotlin
// Check if a RemoteMessage is from YourGPT
// Useful in Advanced mode to filter YourGPT notifications from other FCM messages
val isYourGPT = YourGPTNotificationClient.isYourGPTNotification(remoteMessage)

// Let the SDK handle an incoming notification
// Returns true if handled (MINIMALIST mode), false otherwise
val handled = YourGPTNotificationClient.handleNotification(context, remoteMessage)
```

### Notification Click Handling

```kotlin
// Handle notification tap — opens the widget automatically
// Call in both onCreate() and onNewIntent()
val handled = YourGPTNotificationClient.handleNotificationClick(activity, intent)
```

### Token Management

```kotlin
// Get the cached FCM token (synchronous)
val token = YourGPTNotificationClient.getCachedToken()

// Fetch a fresh token from Firebase (suspend function)
lifecycleScope.launch {
    val token = YourGPTNotificationClient.getFirebaseToken()
}

// Reset token — deletes current token and fetches a new one
// Useful when user logs out/in
YourGPTNotificationClient.resetToken(context)
```

### Widget

```kotlin
// Open the YourGPT widget programmatically
YourGPTNotificationClient.openWidget(activity)

// Open the widget and navigate to a specific conversation
YourGPTSDK.openSession(activity, sessionUid = "conversation-uid")
```

### Push Event Forwarding

```kotlin
// Notify event listeners that a push was received without showing a notification.
// Useful in custom/advanced notification handling.
YourGPTSDK.notifyPushReceived(data)
```

### State & Mode

```kotlin
// Check if notification client is initialized
val ready = YourGPTNotificationClient.isInitialized()

// Get current notification mode
val mode = YourGPTNotificationClient.getNotificationMode()

// Change notification mode at runtime
YourGPTNotificationClient.setNotificationMode(
    NotificationMode.ADVANCED
)
```

### Notification Utilities

```kotlin
// Check if notifications are enabled on the device
val enabled = YourGPTNotificationHelper.areNotificationsEnabled(context)

// Cancel all YourGPT notifications
YourGPTNotificationHelper.cancelAllNotifications(context)

// Cancel a specific notification by ID
YourGPTNotificationHelper.cancelNotification(context, notificationId)

// Create or update the notification channel (Android 8.0+)
YourGPTNotificationHelper.createNotificationChannel(context)

// Create channel with custom sound and vibration
YourGPTNotificationHelper.createNotificationChannel(
    context = context,
    channelId = "my_channel",
    channelName = "My Messages",
    channelDescription = "Custom notification channel",
    soundUri = Uri.parse("android.resource://${packageName}/raw/my_sound"),
    vibrationEnabled = true,
    vibrationPattern = longArrayOf(0, 300, 200, 300)
)
```

---

## Advanced Mode: Custom Notification Handling

If you use `ADVANCED` mode, the SDK identifies YourGPT notifications but does **not** display them — your app handles display. This gives you full control over styling, actions, and message routing.

### When to Use Advanced Mode

- Custom notification styling beyond the SDK defaults
- Different handling for different message types (urgent, promotional, etc.)
- Integration with your own backend alongside YourGPT
- Custom actions on notifications
- Analytics or logging of notification events

### Custom FirebaseMessagingService

Create your own service that replaces the SDK's built-in one:

```kotlin
package com.yourapp

import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.yourgpt.sdk.NotificationMode
import com.yourgpt.sdk.YourGPTNotificationClient
import com.yourgpt.sdk.YourGPTNotificationHelper

class CustomNotificationService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "CustomNotificationService"
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize in ADVANCED mode
        YourGPTNotificationClient.initialize(
            context = applicationContext,
            widgetUid = "YOUR_WIDGET_UID",
            mode = NotificationMode.ADVANCED
        )
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Cache token — it will be sent to the backend via WebView JS bridge
        YourGPTNotificationClient.cacheToken(token)
        // Optionally send to your own backend too
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        if (YourGPTNotificationClient.isYourGPTNotification(remoteMessage)) {
            handleYourGPTMessage(remoteMessage)
        } else {
            // Handle other (non-YourGPT) notifications
        }
    }

    private fun handleYourGPTMessage(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        val senderName = data["title"] ?: "YourGPT Assistant"
        val messageContent = data["body"] ?: "New message"
        val messageId = data["messageId"] ?: System.currentTimeMillis().toString()
        val conversationId = data["session_uid"]

        // Create click intent that opens the widget
        val clickIntent = YourGPTNotificationHelper.createWidgetDeepLink(
            context = this,
            widgetUid = data["widget_uid"] ?: "YOUR_WIDGET_UID",
            conversationId = conversationId
        )
        val pendingIntent = YourGPTNotificationHelper.createClickPendingIntent(
            context = this,
            intent = clickIntent,
            requestCode = messageId.hashCode()
        )

        // Build your custom notification (pass config for icon, sound, etc.)
        val notifConfig = YourGPTNotificationClient.getNotificationConfig()
        val builder = YourGPTNotificationHelper.createRichNotification(
            context = this,
            title = senderName,
            message = messageContent,
            bigText = messageContent,
            clickIntent = pendingIntent,
            config = notifConfig
        ).apply {
            // Additional custom styling
            color = getColor(R.color.colorPrimary)
        }

        YourGPTNotificationHelper.showNotification(
            context = this,
            notificationId = messageId.hashCode(),
            builder = builder
        )
    }
}
```

### AndroidManifest.xml for Custom Service

Replace the SDK's built-in service with your custom one:

```xml
<!-- Remove or replace the SDK service -->
<service
    android:name=".CustomNotificationService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

> **Note:** Only one `FirebaseMessagingService` can handle `MESSAGING_EVENT` per app. If you register a custom service, do **not** also register `com.yourgpt.sdk.YourGPTNotificationService`.

---

## Complete Example

```kotlin
class MainActivity : AppCompatActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> /* handle result */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Initialize SDK with notifications
        lifecycleScope.launch {
            YourGPTSDK.quickInitialize(this@MainActivity, "YOUR_WIDGET_UID")
        }

        // 2. Request notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // 3. Handle notification click
        handleNotificationIntent(intent)

        // 4. Open widget on button click
        findViewById<Button>(R.id.btn_open_chat).setOnClickListener {
            val config = YourGPTConfig(widgetUid = "YOUR_WIDGET_UID")
            YourGPTSDK.openChatbotBottomSheet(supportFragmentManager, config)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleNotificationIntent(it) }
    }

    private fun handleNotificationIntent(intent: Intent) {
        YourGPTNotificationClient.handleNotificationClick(this, intent)
    }
}
```

---

## Testing

1. Install the app on a physical device (FCM may not work on emulators without Google Play Services)
2. Grant notification permission when prompted
3. Open the widget at least once (so the FCM token is registered with the backend)
4. Close the app
5. Send a test message through the YourGPT dashboard

---

## Troubleshooting

### Notifications not received

1. Verify Firebase credentials are uploaded and showing **"Configured"** on the YourGPT Dashboard (Settings → Push Notifications)
2. Verify `google-services.json` is in the correct location and matches your package name
3. Confirm the `YourGPTNotificationService` is declared in your `AndroidManifest.xml`
4. Check that notification permission is granted (Settings > Apps > Your App > Notifications)
5. Ensure the widget was opened at least once after SDK initialization (for token registration)
6. Check logcat for `YourGPTNotificationClient` or `YourGPTNotificationService` logs

### Notifications received but not displayed

1. Ensure `POST_NOTIFICATIONS` permission is granted on Android 13+
2. Check that the notification channel `yourgpt_messages` is not disabled in device settings
3. Verify `notificationMode` is not set to `DISABLED`

### Widget doesn't open on notification tap

1. Ensure `handleNotificationClick()` is called in both `onCreate()` and `onNewIntent()`
2. Verify your launcher activity handles the `com.yourgpt.sdk.OPEN_WIDGET` action

### Token not registered

1. The FCM token is sent via the WebView JS bridge — the widget must be opened at least once
2. Check logcat for `"FCM token successfully sent to widget backend"` message
3. If the token was refreshed while the widget was closed, it will be re-sent next time the widget opens

## Support

For issues or questions, please refer to the main [README](README.md) or contact YourGPT support.
