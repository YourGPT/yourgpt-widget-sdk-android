package com.yourgpt.sdk

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import java.net.URL

/**
 * Helper class for common notification operations
 * Provides utility methods and notification builders
 */
object YourGPTNotificationHelper {
    
    private const val DEFAULT_CHANNEL_ID = "yourgpt_messages"
    private const val REPLY_ACTION_KEY = "key_reply"
    private const val KEY_TEXT_REPLY = "key_text_reply"
    
    /**
     * Create a simple notification
     */
    fun createSimpleNotification(
        context: Context,
        title: String,
        message: String,
        clickIntent: PendingIntent? = null,
        config: YourGPTNotificationConfig? = null
    ): NotificationCompat.Builder {
        val channelId = config?.channelId ?: DEFAULT_CHANNEL_ID
        val smallIcon = config?.smallIconRes ?: android.R.drawable.ic_dialog_info
        val priority = config?.priority ?: NotificationCompat.PRIORITY_HIGH
        val autoCancel = config?.autoCancel ?: true

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(priority)
            .setAutoCancel(autoCancel)
            .apply {
                clickIntent?.let { setContentIntent(it) }
                config?.largeIcon?.let { setLargeIcon(it) }

                if (config != null) {
                    // Sound (pre-O devices; on O+ sound is channel-level)
                    if (config.soundEnabled) {
                        val soundUri = config.soundUri
                            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        setSound(soundUri)
                    } else {
                        setSilent(true)
                    }

                    // Vibration (pre-O devices)
                    if (config.vibrationEnabled) {
                        setVibrate(config.vibrationPattern)
                    }

                    // LED
                    if (config.ledEnabled) {
                        setLights(config.ledColor, config.ledOnMs, config.ledOffMs)
                    }
                }
            }
    }
    
    /**
     * Create a rich notification with expandable content
     */
    fun createRichNotification(
        context: Context,
        title: String,
        message: String,
        bigText: String? = null,
        imageUrl: String? = null,
        clickIntent: PendingIntent? = null,
        config: YourGPTNotificationConfig? = null
    ): NotificationCompat.Builder {
        val builder = createSimpleNotification(context, title, message, clickIntent, config)
        
        // Add expandable big text
        bigText?.let {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(it))
        }
        
        // Add image if provided
        imageUrl?.let { url ->
            try {
                val image = BitmapFactory.decodeStream(URL(url).openConnection().getInputStream())
                builder.setLargeIcon(image)
                
                // If no big text, use big picture style
                if (bigText == null) {
                    builder.setStyle(NotificationCompat.BigPictureStyle()
                        .bigPicture(image)
                        .bigLargeIcon(null as Bitmap?))
                }
            } catch (e: Exception) {
                // Ignore image loading errors
            }
        }
        
        return builder
    }
    
    /**
     * Create a notification with reply action (for Android 7.0+)
     */
    fun createReplyNotification(
        context: Context,
        title: String,
        message: String,
        replyLabel: String = "Reply",
        clickIntent: PendingIntent? = null,
        replyIntent: PendingIntent
    ): NotificationCompat.Builder {
        val builder = createSimpleNotification(context, title, message, clickIntent)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
                .setLabel(replyLabel)
                .build()
            
            val replyAction = NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_send,
                replyLabel,
                replyIntent
            )
                .addRemoteInput(remoteInput)
                .build()
            
            builder.addAction(replyAction)
        }
        
        return builder
    }
    
    /**
     * Create a notification with multiple actions
     */
    fun createActionNotification(
        context: Context,
        title: String,
        message: String,
        actions: List<NotificationAction>,
        clickIntent: PendingIntent? = null
    ): NotificationCompat.Builder {
        val builder = createSimpleNotification(context, title, message, clickIntent)
        
        actions.forEach { action ->
            builder.addAction(
                action.icon,
                action.title,
                action.intent
            )
        }
        
        return builder
    }
    
    /**
     * Create a progress notification
     */
    fun createProgressNotification(
        context: Context,
        title: String,
        message: String,
        progress: Int = 0,
        indeterminate: Boolean = false
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, DEFAULT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setProgress(100, progress, indeterminate)
    }
    
    /**
     * Create a grouped notification summary
     */
    fun createGroupSummary(
        context: Context,
        groupKey: String,
        summaryText: String,
        config: YourGPTNotificationConfig? = null
    ): NotificationCompat.Builder {
        val channelId = config?.channelId ?: DEFAULT_CHANNEL_ID
        val smallIcon = config?.smallIconRes ?: android.R.drawable.ic_dialog_info

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIcon)
            .setContentTitle("YourGPT Messages")
            .setContentText(summaryText)
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setAutoCancel(config?.autoCancel ?: true)
    }
    
    /**
     * Show a notification
     */
    fun showNotification(
        context: Context,
        notificationId: Int,
        builder: NotificationCompat.Builder
    ) {
        val notificationManager = NotificationManagerCompat.from(context)
        
        // Check permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!notificationManager.areNotificationsEnabled()) {
                return
            }
        }
        
        notificationManager.notify(notificationId, builder.build())
    }
    
    /**
     * Cancel a notification
     */
    fun cancelNotification(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
    
    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }
    
    /**
     * Create or update a notification channel (Android O+)
     */
    fun createNotificationChannel(
        context: Context,
        channelId: String = DEFAULT_CHANNEL_ID,
        channelName: String = "YourGPT Messages",
        channelDescription: String = "Notifications from YourGPT widget",
        importance: Int = NotificationManager.IMPORTANCE_HIGH,
        soundUri: Uri? = null,
        vibrationEnabled: Boolean = true,
        vibrationPattern: LongArray? = null
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription

                if (soundUri != null) {
                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                    setSound(soundUri, audioAttributes)
                }

                enableVibration(vibrationEnabled)
                if (vibrationEnabled && vibrationPattern != null) {
                    this.vibrationPattern = vibrationPattern
                }
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Create a deep link intent for opening the widget
     */
    fun createWidgetDeepLink(
        context: Context,
        widgetUid: String,
        conversationId: String? = null
    ): Intent {
        return Intent(context, getMainActivityClass(context)).apply {
            action = "com.yourgpt.sdk.OPEN_WIDGET"
            putExtra("widget_uid", widgetUid)
            conversationId?.let { putExtra("conversation_id", it) }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }
    
    /**
     * Create a pending intent for notification clicks
     */
    fun createClickPendingIntent(
        context: Context,
        intent: Intent,
        requestCode: Int = 0
    ): PendingIntent {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        return PendingIntent.getActivity(context, requestCode, intent, flags)
    }
    
    /**
     * Extract reply text from a notification intent
     */
    fun getReplyText(intent: Intent): CharSequence? {
        return RemoteInput.getResultsFromIntent(intent)?.getCharSequence(KEY_TEXT_REPLY)
    }
    
    /**
     * Check if notifications are enabled
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    
    /**
     * Get the main activity class
     */
    private fun getMainActivityClass(context: Context): Class<*> {
        val packageManager = context.packageManager
        val launchIntent = packageManager.getLaunchIntentForPackage(context.packageName)
        val className = launchIntent?.component?.className
        
        return try {
            Class.forName(className ?: "")
        } catch (e: ClassNotFoundException) {
            // Fallback to a default activity class
            context.javaClass
        }
    }
    
    /**
     * Data class for notification actions
     */
    data class NotificationAction(
        val icon: Int,
        val title: String,
        val intent: PendingIntent
    )
    
    /**
     * Generate a unique notification ID based on timestamp
     */
    fun generateNotificationId(): Int {
        return System.currentTimeMillis().toInt()
    }
    
}