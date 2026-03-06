package com.yourgpt.sdk

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Parcelable
import androidx.core.app.NotificationCompat
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class YourGPTNotificationConfig(
    // Basic settings
    val notificationsEnabled: Boolean = true,
    val smallIconRes: Int = android.R.drawable.ic_dialog_info,
    val largeIcon: @RawValue Bitmap? = null,
    
    // Sound settings
    val soundEnabled: Boolean = true,
    val soundUri: Uri? = null, // null uses default notification sound
    
    // Vibration settings
    val vibrationEnabled: Boolean = true,
    val vibrationPattern: LongArray = longArrayOf(0, 250, 250, 250),
    
    // LED settings
    val ledEnabled: Boolean = true,
    val ledColor: Int = Color.BLUE,
    val ledOnMs: Int = 300,
    val ledOffMs: Int = 3000,
    
    // Priority and importance
    val priority: Int = NotificationCompat.PRIORITY_HIGH,
    
    // Grouping
    val groupMessages: Boolean = true,
    val groupKey: String = "com.yourgpt.sdk.MESSAGES",
    
    // Actions
    val showReplyAction: Boolean = true,
    val replyIconRes: Int = android.R.drawable.ic_menu_send,

    // Auto-cancel
    val autoCancel: Boolean = true,
    
    // Quiet hours (in 24-hour format)
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: Int = 22, // 10 PM
    val quietHoursEnd: Int = 8,     // 8 AM
    
    // Message preview
    val showMessagePreview: Boolean = true,
    val maxPreviewLength: Int = 100,
    
    // Stacking
    val stackNotifications: Boolean = true,
    val maxNotificationStack: Int = 5,
    
    // Channel settings (for Android O+)
    val channelId: String = "yourgpt_messages",
    val channelName: String = "YourGPT Messages",
    val channelDescription: String = "Notifications for new messages from YourGPT widget",
    
    // Custom data
    val customExtras: Map<String, String> = emptyMap()
) : Parcelable {
    
    fun isInQuietHours(): Boolean {
        if (!quietHoursEnabled) return false
        
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        
        return if (quietHoursStart > quietHoursEnd) {
            // Quiet hours span midnight (e.g., 22:00 to 08:00)
            currentHour >= quietHoursStart || currentHour < quietHoursEnd
        } else {
            // Quiet hours within same day (e.g., 08:00 to 22:00)
            currentHour in quietHoursStart until quietHoursEnd
        }
    }
    
    fun shouldShowNotification(): Boolean {
        return notificationsEnabled && !isInQuietHours()
    }
    
    fun getProcessedMessageContent(message: String): String {
        return if (showMessagePreview) {
            if (message.length > maxPreviewLength) {
                message.substring(0, maxPreviewLength) + "..."
            } else {
                message
            }
        } else {
            "New message"
        }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as YourGPTNotificationConfig
        
        if (notificationsEnabled != other.notificationsEnabled) return false
        if (smallIconRes != other.smallIconRes) return false
        if (largeIcon != other.largeIcon) return false
        if (soundEnabled != other.soundEnabled) return false
        if (soundUri != other.soundUri) return false
        if (vibrationEnabled != other.vibrationEnabled) return false
        if (!vibrationPattern.contentEquals(other.vibrationPattern)) return false
        if (ledEnabled != other.ledEnabled) return false
        if (ledColor != other.ledColor) return false
        if (ledOnMs != other.ledOnMs) return false
        if (ledOffMs != other.ledOffMs) return false
        if (priority != other.priority) return false
        if (groupMessages != other.groupMessages) return false
        if (groupKey != other.groupKey) return false
        if (showReplyAction != other.showReplyAction) return false
        if (replyIconRes != other.replyIconRes) return false
        if (autoCancel != other.autoCancel) return false
        if (quietHoursEnabled != other.quietHoursEnabled) return false
        if (quietHoursStart != other.quietHoursStart) return false
        if (quietHoursEnd != other.quietHoursEnd) return false
        if (showMessagePreview != other.showMessagePreview) return false
        if (maxPreviewLength != other.maxPreviewLength) return false
        if (stackNotifications != other.stackNotifications) return false
        if (maxNotificationStack != other.maxNotificationStack) return false
        if (channelId != other.channelId) return false
        if (channelName != other.channelName) return false
        if (channelDescription != other.channelDescription) return false
        if (customExtras != other.customExtras) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = notificationsEnabled.hashCode()
        result = 31 * result + smallIconRes
        result = 31 * result + (largeIcon?.hashCode() ?: 0)
        result = 31 * result + soundEnabled.hashCode()
        result = 31 * result + (soundUri?.hashCode() ?: 0)
        result = 31 * result + vibrationEnabled.hashCode()
        result = 31 * result + vibrationPattern.contentHashCode()
        result = 31 * result + ledEnabled.hashCode()
        result = 31 * result + ledColor
        result = 31 * result + ledOnMs
        result = 31 * result + ledOffMs
        result = 31 * result + priority
        result = 31 * result + groupMessages.hashCode()
        result = 31 * result + groupKey.hashCode()
        result = 31 * result + showReplyAction.hashCode()
        result = 31 * result + replyIconRes
        result = 31 * result + autoCancel.hashCode()
        result = 31 * result + quietHoursEnabled.hashCode()
        result = 31 * result + quietHoursStart
        result = 31 * result + quietHoursEnd
        result = 31 * result + showMessagePreview.hashCode()
        result = 31 * result + maxPreviewLength
        result = 31 * result + stackNotifications.hashCode()
        result = 31 * result + maxNotificationStack
        result = 31 * result + channelId.hashCode()
        result = 31 * result + channelName.hashCode()
        result = 31 * result + channelDescription.hashCode()
        result = 31 * result + customExtras.hashCode()
        return result
    }
    
    companion object {
        fun builder() = Builder()
        
        class Builder {
            private var notificationsEnabled = true
            private var smallIconRes = android.R.drawable.ic_dialog_info
            private var largeIcon: Bitmap? = null
            private var soundEnabled = true
            private var soundUri: Uri? = null
            private var vibrationEnabled = true
            private var vibrationPattern = longArrayOf(0, 250, 250, 250)
            private var ledEnabled = true
            private var ledColor = Color.BLUE
            private var ledOnMs = 300
            private var ledOffMs = 3000
            private var priority = NotificationCompat.PRIORITY_HIGH
            private var groupMessages = true
            private var groupKey = "com.yourgpt.sdk.MESSAGES"
            private var showReplyAction = true
            private var replyIconRes = android.R.drawable.ic_menu_send
            private var autoCancel = true
            private var quietHoursEnabled = false
            private var quietHoursStart = 22
            private var quietHoursEnd = 8
            private var showMessagePreview = true
            private var maxPreviewLength = 100
            private var stackNotifications = true
            private var maxNotificationStack = 5
            private var channelId = "yourgpt_messages"
            private var channelName = "YourGPT Messages"
            private var channelDescription = "Notifications for new messages from YourGPT widget"
            private var customExtras = emptyMap<String, String>()
            
            fun setNotificationsEnabled(enabled: Boolean) = apply { notificationsEnabled = enabled }
            fun setSmallIcon(resId: Int) = apply { smallIconRes = resId }
            fun setLargeIcon(bitmap: Bitmap?) = apply { largeIcon = bitmap }
            fun setSoundEnabled(enabled: Boolean) = apply { soundEnabled = enabled }
            fun setSoundUri(uri: Uri?) = apply { soundUri = uri }
            fun setVibrationEnabled(enabled: Boolean) = apply { vibrationEnabled = enabled }
            fun setVibrationPattern(pattern: LongArray) = apply { vibrationPattern = pattern }
            fun setLedEnabled(enabled: Boolean) = apply { ledEnabled = enabled }
            fun setLedColor(color: Int) = apply { ledColor = color }
            fun setLedTiming(onMs: Int, offMs: Int) = apply { 
                ledOnMs = onMs
                ledOffMs = offMs
            }
            fun setPriority(priority: Int) = apply { this.priority = priority }
            fun setGroupMessages(enabled: Boolean) = apply { groupMessages = enabled }
            fun setGroupKey(key: String) = apply { groupKey = key }
            fun setShowReplyAction(show: Boolean) = apply { showReplyAction = show }
            fun setReplyIcon(resId: Int) = apply { replyIconRes = resId }
            fun setAutoCancel(enabled: Boolean) = apply { autoCancel = enabled }
            fun setQuietHours(enabled: Boolean, startHour: Int = 22, endHour: Int = 8) = apply {
                quietHoursEnabled = enabled
                quietHoursStart = startHour
                quietHoursEnd = endHour
            }
            fun setMessagePreview(show: Boolean, maxLength: Int = 100) = apply {
                showMessagePreview = show
                maxPreviewLength = maxLength
            }
            fun setStackNotifications(enabled: Boolean, maxStack: Int = 5) = apply {
                stackNotifications = enabled
                maxNotificationStack = maxStack
            }
            fun setChannel(id: String, name: String, description: String) = apply {
                channelId = id
                channelName = name
                channelDescription = description
            }
            fun setCustomExtras(extras: Map<String, String>) = apply { customExtras = extras }
            
            fun build() = YourGPTNotificationConfig(
                notificationsEnabled = notificationsEnabled,
                smallIconRes = smallIconRes,
                largeIcon = largeIcon,
                soundEnabled = soundEnabled,
                soundUri = soundUri,
                vibrationEnabled = vibrationEnabled,
                vibrationPattern = vibrationPattern,
                ledEnabled = ledEnabled,
                ledColor = ledColor,
                ledOnMs = ledOnMs,
                ledOffMs = ledOffMs,
                priority = priority,
                groupMessages = groupMessages,
                groupKey = groupKey,
                showReplyAction = showReplyAction,
                replyIconRes = replyIconRes,
                autoCancel = autoCancel,
                quietHoursEnabled = quietHoursEnabled,
                quietHoursStart = quietHoursStart,
                quietHoursEnd = quietHoursEnd,
                showMessagePreview = showMessagePreview,
                maxPreviewLength = maxPreviewLength,
                stackNotifications = stackNotifications,
                maxNotificationStack = maxNotificationStack,
                channelId = channelId,
                channelName = channelName,
                channelDescription = channelDescription,
                customExtras = customExtras
            )
        }
    }
}