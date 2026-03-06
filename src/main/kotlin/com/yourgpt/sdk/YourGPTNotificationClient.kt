package com.yourgpt.sdk

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.webkit.WebView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Simplified client for handling YourGPT notifications
 */
object YourGPTNotificationClient {
    
    private const val TAG = "YourGPTNotificationClient"
    private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    private var widgetUid: String? = null
    private var notificationMode: NotificationMode = NotificationMode.MINIMALIST
    private var isInitialized = false
    private var cachedFcmToken: String? = null
    private var isTokenRegisteredViaWebView = false
    private var appContext: Context? = null
    private var notificationConfig: YourGPTNotificationConfig? = null
    private var eventListener: YourGPTEventListener? = null

    /**
     * Set the event listener for notification events
     */
    @JvmStatic
    fun setEventListener(listener: YourGPTEventListener?) {
        eventListener = listener
    }
    
    /**
     * Initialize notification client with minimal configuration
     * This is the simplest way to enable notifications
     * 
     * @param context Application or Activity context
     * @param widgetUid Your widget UID
     * @param mode Notification handling mode (default: MINIMALIST)
     */
    @JvmStatic
    @JvmOverloads
    fun initialize(
        context: Context,
        widgetUid: String,
        mode: NotificationMode = NotificationMode.MINIMALIST,
        config: YourGPTNotificationConfig? = null
    ) {
        this.widgetUid = widgetUid
        this.notificationMode = mode
        this.isInitialized = true
        this.appContext = context.applicationContext
        this.notificationConfig = config

        // Persist widgetUid so the service can self-initialize when the app is killed
        val prefs = context.getSharedPreferences("yourgpt_sdk_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("widget_uid", widgetUid)
            .apply()

        // Restore cached FCM token from SharedPreferences if available
        val storedToken = prefs.getString("fcm_token", null)
        if (storedToken != null && cachedFcmToken == null) {
            cachedFcmToken = storedToken
            Log.d(TAG, "Restored FCM token from SharedPreferences")
        }

        Log.d(TAG, "Initialized with widget: $widgetUid, mode: $mode")

        // Request notification permission on Android 13+ (minimalist mode)
        if (mode == NotificationMode.MINIMALIST) {
            requestNotificationPermissionIfNeeded(context)
        }

        // Auto-fetch and cache token if in minimalist mode
        if (mode == NotificationMode.MINIMALIST) {
            CoroutineScope(Dispatchers.IO).launch {
                val token = getFirebaseToken()
                token?.let { cacheToken(it) }
                Log.d(TAG, "FCM token cached: ${token != null}")
            }
        }
    }
    
    /**
     * Check if a RemoteMessage is from YourGPT
     * 
     * @param remoteMessage The Firebase message to check
     * @return true if message is from YourGPT
     */
    @JvmStatic
    fun isYourGPTNotification(remoteMessage: RemoteMessage): Boolean {
        val data = remoteMessage.data

        Log.d(TAG, "isYourGPTNotification check — data keys: ${data.keys}  widgetUid=$widgetUid")

        // Priority 1: widget_uid (exact match with configured widgetUid)
        if (data.containsKey("widget_uid")) {
            val match = data["widget_uid"] == widgetUid
            Log.d(TAG, "Matched on widget_uid: widget_uid=${data["widget_uid"]}  match=$match")
            return match
        }

        // Priority 2: project_uid (fallback)
        if (data.containsKey("project_uid")) {
            val match = data["project_uid"] == widgetUid
            Log.d(TAG, "Matched on project_uid: project_uid=${data["project_uid"]}  match=$match")
            return match
        }

        Log.d(TAG, "Not a YourGPT notification — no matching keys found")
        return false
    }
    
    /**
     * Handle incoming notification in minimalist mode
     * Automatically shows notification and opens widget on tap
     * 
     * @param context Context
     * @param remoteMessage The Firebase message
     * @return true if handled, false otherwise
     */
    @JvmStatic
    fun handleNotification(context: Context, remoteMessage: RemoteMessage): Boolean {
        if (!isInitialized) {
            Log.w(TAG, "handleNotification: client not initialized — skipping")
            return false
        }

        if (notificationMode == NotificationMode.DISABLED) {
            Log.i(TAG, "handleNotification: mode is DISABLED — notification suppressed")
            return false
        }

        val isYourGPT = isYourGPTNotification(remoteMessage)
        Log.i(TAG, "handleNotification: isYourGPTNotification=$isYourGPT  mode=$notificationMode  data=${remoteMessage.data}")

        if (!isYourGPT) {
            Log.d(TAG, "handleNotification: not a YourGPT notification — not handled here")
            return false
        }

        // Notify listener about incoming push message (fires for both MINIMALIST and ADVANCED modes)
        eventListener?.onPushMessageReceived(remoteMessage.data.toMap())

        when (notificationMode) {
            NotificationMode.MINIMALIST -> {
                Log.i(TAG, "handleNotification: MINIMALIST mode — showing notification automatically")
                showNotificationAndHandleClick(context, remoteMessage)
                return true
            }
            NotificationMode.ADVANCED -> {
                Log.i(TAG, "handleNotification: ADVANCED mode — delegating to app callback")
                return false
            }
            NotificationMode.DISABLED -> {
                return false
            }
        }
    }
    
    /**
     * Cache FCM token locally. The token will be sent to the YourGPT backend
     * securely through the WebView JS bridge when the widget is opened,
     * avoiding the need for a public API endpoint.
     *
     * @param token The FCM token to cache
     */
    @JvmStatic
    fun cacheToken(token: String) {
        cachedFcmToken = token
        isTokenRegisteredViaWebView = false

        // Persist to SharedPreferences so the token survives app restarts
        appContext?.getSharedPreferences("yourgpt_sdk_prefs", Context.MODE_PRIVATE)
            ?.edit()
            ?.putString("fcm_token", token)
            ?.putLong("fcm_token_timestamp", System.currentTimeMillis())
            ?.apply()

        Log.d(TAG, "FCM token cached, will register via WebView when widget opens")

        // Notify listener about new FCM token
        eventListener?.onFCMTokenReceived(token)
    }

    /**
     * Send the cached FCM token to the widget backend through the WebView JS bridge.
     * This is called automatically when the widget WebView finishes loading.
     *
     * @param webView The widget WebView instance
     */
    @JvmStatic
    fun registerTokenViaWebView(webView: WebView) {
        val token = cachedFcmToken
        val uid = widgetUid

        if (token == null || uid == null) {
            Log.d(TAG, "No cached token or widgetUid to register via WebView")
            return
        }

        if (isTokenRegisteredViaWebView) {
            Log.d(TAG, "Token already registered via WebView for this session")
            return
        }

        val script = """
            (function() {
                window.postMessage({
                    type: 'register_fcm_token',
                    payload: {
                        token: '$token',
                        platform: 'android',
                        widget_uid: '$uid'
                    }
                }, '*');
            })();
        """.trimIndent()

        Log.i(TAG, "Sending FCM token via WebView JS bridge — token=$token")
        webView.evaluateJavascript(script) { result ->
            isTokenRegisteredViaWebView = true
            Log.i(TAG, "FCM token successfully sent to widget backend — result=$result")
        }
    }

    /**
     * Get the cached FCM token
     */
    @JvmStatic
    fun getCachedToken(): String? = cachedFcmToken
    
    /**
     * Get current Firebase token
     */
    @JvmStatic
    suspend fun getFirebaseToken(): String? {
        return try {
            // Check if Firebase is initialized
            if (!isFirebaseAvailable()) {
                Log.w(TAG, "Firebase is not initialized. Notifications will not work.")
                return null
            }
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Firebase token", e)
            null
        }
    }
    
    /**
     * Check if Firebase is available and initialized
     */
    @JvmStatic
    private fun isFirebaseAvailable(): Boolean {
        return try {
            com.google.firebase.FirebaseApp.getInstance()
            true
        } catch (e: IllegalStateException) {
            false
        }
    }
    
    /**
     * Reset token and re-register with backend
     * Useful when user logs out/in
     */
    @JvmStatic
    fun resetToken(context: Context) {
        if (!isFirebaseAvailable()) {
            Log.w(TAG, "Firebase not available - cannot reset token")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Delete current token
                FirebaseMessaging.getInstance().deleteToken().await()

                // Get new token and cache it
                val newToken = getFirebaseToken()
                newToken?.let { cacheToken(it) }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset token", e)
            }
        }
    }
    
    /**
     * Handle notification click to open widget
     * 
     * @param activity The activity to open widget from
     * @param intent The notification intent
     * @return true if handled
     */
    @JvmStatic
    fun handleNotificationClick(activity: FragmentActivity, intent: Intent): Boolean {
        if (!isInitialized || notificationMode != NotificationMode.MINIMALIST) {
            return false
        }
        
        val action = intent.action
        val widgetUid = intent.getStringExtra("widget_uid")
        
        if (action == "com.yourgpt.sdk.OPEN_WIDGET" && widgetUid == this.widgetUid) {
            // Collect intent extras as a map for the listener
            val extras = mutableMapOf<String, String>()
            intent.extras?.let { bundle ->
                for (key in bundle.keySet()) {
                    bundle.getString(key)?.let { extras[key] = it }
                }
            }
            eventListener?.onNotificationClicked(extras)

            val conversationId = intent.getStringExtra("conversation_id")
            openWidget(activity, conversationId)
            return true
        }
        
        return false
    }
    
    /**
     * Open YourGPT widget, optionally navigating to a specific session.
     *
     * @param activity FragmentActivity to show widget in
     * @param sessionUid Optional session UID to open directly (e.g. from a notification)
     */
    @JvmStatic
    @JvmOverloads
    fun openWidget(activity: FragmentActivity, sessionUid: String? = null) {
        if (!isInitialized || widgetUid == null) {
            Log.w(TAG, "Cannot open widget - not initialized")
            return
        }

        eventListener?.onWidgetOpenRequested(widgetUid!!)

        val customParams = if (sessionUid != null) {
            mapOf("session_uid" to sessionUid)
        } else {
            emptyMap()
        }

        val config = YourGPTConfig(
            widgetUid = widgetUid!!,
            enableNotifications = true,
            customParams = customParams
        )

        YourGPTSDK.openChatbotBottomSheet(activity.supportFragmentManager, config)
    }
    
    /**
     * Get current notification mode
     */
    @JvmStatic
    fun getNotificationMode(): NotificationMode {
        return notificationMode
    }
    
    /**
     * Update notification mode
     */
    @JvmStatic
    fun setNotificationMode(mode: NotificationMode) {
        this.notificationMode = mode
        
        if (mode == NotificationMode.DISABLED && isFirebaseAvailable()) {
            // Unsubscribe from topics
            widgetUid?.let {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("widget_$it")
            }
        }
    }
    
    /**
     * Check if client is initialized
     */
    @JvmStatic
    fun isInitialized(): Boolean {
        return isInitialized
    }

    /**
     * Get the current notification config
     */
    @JvmStatic
    fun getNotificationConfig(): YourGPTNotificationConfig? = notificationConfig
    
    /**
     * Call this from your Activity's permission result callback to notify the listener.
     *
     * Example:
     * ```
     * val launcher = registerForActivityResult(RequestPermission()) { granted ->
     *     YourGPTNotificationClient.onPermissionResult(granted)
     * }
     * ```
     *
     * @param granted true if POST_NOTIFICATIONS permission was granted
     */
    @JvmStatic
    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            eventListener?.onNotificationPermissionGranted()
        } else {
            eventListener?.onNotificationPermissionDenied()
        }
    }

    private fun showNotificationAndHandleClick(context: Context, remoteMessage: RemoteMessage) {
        val cfg = notificationConfig ?: YourGPTNotificationConfig()

        // Respect quiet hours and enabled flag
        if (!cfg.shouldShowNotification()) return

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        // Extract notification data based on format
        val messageContent: String
        val senderName: String
        val widgetUidToUse: String
        val conversationId: String?
        val messageId: String
        
        // Detect YourGPT backend format by presence of widget_uid or project_uid
        val isBackendFormat = data.containsKey("widget_uid") || data.containsKey("project_uid")

        if (isBackendFormat) {
            // YourGPT backend format: reads title/body first, falls back to sender_name/message_content
            senderName = notification?.title ?: data["title"] ?: data["sender_name"] ?: "YourGPT"
            messageContent = notification?.body ?: data["body"] ?: data["message_content"] ?: "New message"
            widgetUidToUse = data["widget_uid"] ?: data["project_uid"] ?: widgetUid ?: return
            conversationId = data["session_uid"] ?: data["conversation_id"]
            messageId = data["messageId"] ?: data["message_id"] ?: remoteMessage.messageId ?: System.currentTimeMillis().toString()
        } else {
            // Unknown format fallback
            messageContent = data["message_content"] ?: notification?.body ?: "New message"
            senderName = data["sender_name"] ?: notification?.title ?: "YourGPT"
            widgetUidToUse = widgetUid ?: return
            conversationId = data["conversation_id"]
            messageId = data["message_id"] ?: System.currentTimeMillis().toString()
        }
        
        // Create deep link intent
        val clickIntent = YourGPTNotificationHelper.createWidgetDeepLink(
            context = context,
            widgetUid = widgetUidToUse,
            conversationId = conversationId
        )
        
        val pendingIntent = YourGPTNotificationHelper.createClickPendingIntent(
            context = context,
            intent = clickIntent,
            requestCode = messageId.hashCode()
        )
        
        // Group key based on session_uid so same-session notifications thread together
        val groupKey = if (conversationId != null) "yourgpt_session_$conversationId" else cfg.groupKey

        // Process message content per config (preview truncation)
        val processedMessage = cfg.getProcessedMessageContent(messageContent)

        // Create and show notification using Helper
        val notificationBuilder = YourGPTNotificationHelper.createSimpleNotification(
            context = context,
            title = senderName,
            message = processedMessage,
            clickIntent = pendingIntent,
            config = cfg
        ).apply {
            setGroup(groupKey)
        }

        // Show the notification
        YourGPTNotificationHelper.showNotification(
            context = context,
            notificationId = messageId.hashCode(),
            builder = notificationBuilder
        )

        // Show/update summary notification for this session thread
        if (conversationId != null) {
            val summaryBuilder = YourGPTNotificationHelper.createGroupSummary(
                context = context,
                groupKey = groupKey,
                summaryText = "New messages from $senderName",
                config = cfg
            )
            YourGPTNotificationHelper.showNotification(
                context = context,
                notificationId = "summary_$conversationId".hashCode(),
                builder = summaryBuilder
            )
        }

        Log.i(TAG, "Notification displayed — title='$senderName'  body='$processedMessage'  group='$groupKey'  id=${messageId.hashCode()}")
    }
    
    /**
     * Quick setup method for one-line initialization
     * Perfect for simple integrations
     * 
     * Example:
     * YourGPTNotificationClient.quickSetup(this, "your-widget-uid")
     */
    @JvmStatic
    @JvmOverloads
    fun quickSetup(context: Context, widgetUid: String, config: YourGPTNotificationConfig? = null) {
        initialize(context, widgetUid, NotificationMode.MINIMALIST, config)

        // Create notification channel for Android 8.0+
        val cfg = config
        if (cfg != null) {
            YourGPTNotificationHelper.createNotificationChannel(
                context = context,
                channelId = cfg.channelId,
                channelName = cfg.channelName,
                channelDescription = cfg.channelDescription,
                soundUri = if (cfg.soundEnabled) cfg.soundUri else null,
                vibrationEnabled = cfg.vibrationEnabled,
                vibrationPattern = cfg.vibrationPattern
            )
        } else {
            YourGPTNotificationHelper.createNotificationChannel(context)
        }
        
        // Permission is already requested in initialize() for minimalist mode
    }

    /**
     * Request POST_NOTIFICATIONS permission on Android 13+ if not already granted.
     * Only works when the context is an Activity.
     */
    private fun requestNotificationPermissionIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && context is Activity) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "Requesting POST_NOTIFICATIONS permission")
                ActivityCompat.requestPermissions(
                    context,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            } else {
                Log.d(TAG, "POST_NOTIFICATIONS permission already granted")
            }
        }
    }
}