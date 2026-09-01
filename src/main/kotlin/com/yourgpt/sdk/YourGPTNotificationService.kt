package com.yourgpt.sdk

import android.content.Context
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class YourGPTNotificationService : FirebaseMessagingService() {
    
    companion object {
        const val CHANNEL_ID = "yourgpt_messages"
        const val CHANNEL_NAME = "YourGPT Messages"
        const val CHANNEL_DESCRIPTION = "Notifications for new messages from YourGPT widget"
        private const val TAG = "YourGPTNotificationService"
        
        // Notification extras
        const val EXTRA_WIDGET_UID = "widget_uid"
        const val EXTRA_MESSAGE_ID = "message_id"
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val EXTRA_SENDER_NAME = "sender_name"
        const val EXTRA_MESSAGE_CONTENT = "message_content"
        const val EXTRA_TIMESTAMP = "timestamp"
        
        private var tokenCallback: ((String) -> Unit)? = null
        private var messageCallback: ((Map<String, Any>) -> Unit)? = null
        
        fun setTokenCallback(callback: (String) -> Unit) {
            tokenCallback = callback
        }
        
        fun setMessageCallback(callback: (Map<String, Any>) -> Unit) {
            messageCallback = callback
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        // Use Helper to create notification channel with config if available
        val cfg = YourGPTNotificationClient.getNotificationConfig()
        if (cfg != null) {
            YourGPTNotificationHelper.createNotificationChannel(
                context = this,
                channelId = cfg.channelId,
                channelName = cfg.channelName,
                channelDescription = cfg.channelDescription,
                soundUri = if (cfg.soundEnabled) cfg.soundUri else null,
                vibrationEnabled = cfg.vibrationEnabled,
                vibrationPattern = cfg.vibrationPattern
            )
        } else {
            YourGPTNotificationHelper.createNotificationChannel(this)
        }
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        // Log token for debugging
        android.util.Log.d(TAG, "New FCM token received")

        // Store token locally in SharedPreferences
        storeToken(token)

        // Notify SDK about new token
        tokenCallback?.invoke(token)

        // Cache token in NotificationClient — it will be sent to the
        // backend securely via the WebView JS bridge next time the widget opens.
        if (YourGPTNotificationClient.isInitialized()) {
            YourGPTNotificationClient.cacheToken(token)
        }
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        android.util.Log.i(TAG, "========== FCM NOTIFICATION RECEIVED ==========")
        android.util.Log.i(TAG, "From: ${remoteMessage.from}")
        android.util.Log.i(TAG, "Message ID: ${remoteMessage.messageId}")
        android.util.Log.i(TAG, "Data payload: ${remoteMessage.data}")
        android.util.Log.i(TAG, "Notification payload: ${remoteMessage.notification?.let { "title='${it.title}' body='${it.body}'" } ?: "null"}")

        // First, try to handle with YourGPTNotificationClient (minimalist mode)
        if (YourGPTNotificationClient.isInitialized()) {
            android.util.Log.d(TAG, "Forwarding to YourGPTNotificationClient...")
            val handled = YourGPTNotificationClient.handleNotification(this, remoteMessage)
            if (handled) {
                android.util.Log.i(TAG, "Notification handled by YourGPTNotificationClient — notification will be shown")
                android.util.Log.i(TAG, "===============================================")
                return
            }
            android.util.Log.d(TAG, "YourGPTNotificationClient did not handle it — falling through to service logic")
        } else {
            android.util.Log.w(TAG, "YourGPTNotificationClient not initialized — handling in service directly")
        }

        // If not handled by client, proceed with normal flow
        val data = remoteMessage.data
        val notification = remoteMessage.notification

        // Parse YourGPT specific data - check for widget_uid or project_uid
        val widgetUid = data["widget_uid"] ?: data["project_uid"]

        android.util.Log.d(TAG, "widget_uid='$widgetUid'")

        // Identify YourGPT messages by presence of widget_uid or project_uid
        // The type field is not used for detection (backend sends description text in type)
        if (widgetUid != null) {
            android.util.Log.i(TAG, "Recognized as YourGPT widget message — showing rich notification")
            handleWidgetMessage(data, notification)
        } else if (notification != null) {
            // Handle standard FCM notification with notification payload
            android.util.Log.i(TAG, "Standard FCM notification payload — showing standard notification")
            showStandardNotification(notification)
        } else {
            android.util.Log.w(TAG, "Notification not recognized as YourGPT and has no notification payload — IGNORED")
        }
        
        // Notify callback listeners for advanced mode
        messageCallback?.invoke(data)
        android.util.Log.i(TAG, "===============================================")
    }
    
    private fun handleWidgetMessage(data: Map<String, String>, notification: RemoteMessage.Notification?) {
        // For advanced mode handling - minimalist mode is handled by NotificationClient
        // Check if notifications are enabled
        if (!YourGPTNotificationHelper.areNotificationsEnabled(this)) {
            return
        }
        
        // Extract message details based on notification format
        val messageId: String
        val conversationId: String?
        val senderName: String
        val messageContent: String
        val widgetUid: String
        val timestamp: Long
        
        // Detect YourGPT backend format by presence of session_uid or project_uid
        val isBackendFormat = data.containsKey("session_uid") || data.containsKey("project_uid")

        if (isBackendFormat) {
            // YourGPT backend format: reads title/body first, falls back to sender_name/message_content
            messageId = data["messageId"] ?: System.currentTimeMillis().toString()
            conversationId = data["session_uid"]
            senderName = notification?.title ?: data["title"] ?: data["sender_name"] ?: "YourGPT Assistant"
            messageContent = notification?.body ?: data["body"] ?: data["message_content"] ?: "New message"
            widgetUid = data["widget_uid"] ?: data["project_uid"] ?: return
            timestamp = System.currentTimeMillis()
        } else {
            // Old format fallback
            messageId = data["message_id"] ?: System.currentTimeMillis().toString()
            conversationId = data["conversation_id"]
            senderName = data["sender_name"] ?: notification?.title ?: "YourGPT Assistant"
            messageContent = data["message_content"] ?: notification?.body ?: "New message"
            widgetUid = data["widget_uid"] ?: return
            timestamp = data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()
        }
        
        // Get user config or fall back to defaults
        val config = YourGPTNotificationClient.getNotificationConfig() ?: YourGPTNotificationConfig()
        
        // Create notification
        showNotification(
            messageId = messageId,
            conversationId = conversationId,
            senderName = senderName,
            messageContent = messageContent,
            widgetUid = widgetUid,
            timestamp = timestamp,
            config = config
        )
    }
    
    private fun showNotification(
        messageId: String,
        conversationId: String?,
        senderName: String,
        messageContent: String,
        widgetUid: String,
        timestamp: Long,
        config: YourGPTNotificationConfig
    ) {
        // Check if notifications are permitted
        if (!YourGPTNotificationHelper.areNotificationsEnabled(this)) {
            return
        }

        // Create intent for notification click
        val clickIntent = YourGPTNotificationHelper.createWidgetDeepLink(
            context = this,
            widgetUid = widgetUid,
            conversationId = conversationId
        )

        val pendingIntent = YourGPTNotificationHelper.createClickPendingIntent(
            context = this,
            intent = clickIntent,
            requestCode = messageId.hashCode()
        )

        // Group key based on session_uid so same-session notifications thread together
        val groupKey = if (conversationId != null) "yourgpt_session_$conversationId" else "yourgpt_messages"

        // Build notification using Helper (config is applied inside createRichNotification)
        val notificationBuilder = YourGPTNotificationHelper.createRichNotification(
            context = this,
            title = senderName,
            message = messageContent,
            bigText = messageContent,
            clickIntent = pendingIntent,
            config = config
        ).apply {
            setGroup(groupKey)
            setWhen(timestamp)
            setShowWhen(true)
        }

        // Show notification using Helper
        YourGPTNotificationHelper.showNotification(
            context = this,
            notificationId = messageId.hashCode(),
            builder = notificationBuilder
        )

        // Create summary notification for this session thread
        if (conversationId != null) {
            val summaryBuilder = YourGPTNotificationHelper.createGroupSummary(
                context = this,
                groupKey = groupKey,
                summaryText = "New messages from $senderName",
                config = config
            )
            YourGPTNotificationHelper.showNotification(
                context = this,
                notificationId = "summary_$conversationId".hashCode(),
                builder = summaryBuilder
            )
        }
    }
    
    private fun showStandardNotification(notification: RemoteMessage.Notification) {
        val config = YourGPTNotificationClient.getNotificationConfig()
        val notificationBuilder = YourGPTNotificationHelper.createSimpleNotification(
            context = this,
            title = notification.title ?: "YourGPT",
            message = notification.body ?: "You have a new message",
            config = config
        )
        
        YourGPTNotificationHelper.showNotification(
            context = this,
            notificationId = System.currentTimeMillis().toInt(),
            builder = notificationBuilder
        )
    }
    private fun storeToken(token: String) {
        val sharedPrefs = getSharedPreferences("yourgpt_sdk_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putString("fcm_token", token)
            putLong("fcm_token_timestamp", System.currentTimeMillis())
            apply()
        }
    }
    
}