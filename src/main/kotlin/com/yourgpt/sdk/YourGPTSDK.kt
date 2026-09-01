package com.yourgpt.sdk

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.flow.StateFlow

object YourGPTSDK {
    const val VERSION = "1.0.0"
    
    private val core = YourGPTSDKCore.getInstance()
    private var eventListener: YourGPTEventListener? = null
    private var applicationContext: Context? = null
    
    suspend fun initialize(context: Context, configuration: YourGPTConfig) {
        applicationContext = context.applicationContext
        core.initialize(configuration)
        
        // Initialize notifications based on mode
        if (configuration.enableNotifications && configuration.notificationMode != NotificationMode.DISABLED) {
            YourGPTNotificationClient.initialize(
                context = context,
                widgetUid = configuration.widgetUid,
                mode = configuration.notificationMode,
                config = configuration.notificationConfig
            )
        }
    }
    
    /**
     * Quick initialization for minimalist mode
     * Simplest way to get started with YourGPT SDK
     */
    suspend fun quickInitialize(context: Context, widgetUid: String) {
        val config = YourGPTConfig(
            widgetUid = widgetUid,
            enableNotifications = true,
            notificationMode = NotificationMode.MINIMALIST,
            autoRegisterToken = true
        )
        initialize(context, config)
    }

    fun setEventListener(listener: YourGPTEventListener?) {
        eventListener = listener
        ChatbotBottomSheetDialog.setEventListener(listener)
        YourGPTNotificationClient.setEventListener(listener)
    }
    
    fun openChatbotBottomSheet(fragmentManager: FragmentManager, configuration: YourGPTConfig) {
        val bottomSheet = ChatbotBottomSheetDialog.newInstance(configuration)
        bottomSheet.show(fragmentManager, "ChatbotBottomSheet")
    }

    /**
     * Create a standalone ChatbotBottomSheetDialog Fragment for custom embedding.
     * Mirrors iOS's createChatbotViewController().
     *
     * @param widgetUid The widget UID to use
     * @param customParams Optional custom parameters
     * @return A ChatbotBottomSheetDialog instance ready for presentation
     */
    fun createChatbotFragment(
        widgetUid: String,
        customParams: Map<String, String> = emptyMap()
    ): ChatbotBottomSheetDialog {
        val config = YourGPTConfig(
            widgetUid = widgetUid,
            customParams = customParams
        )
        return ChatbotBottomSheetDialog.newInstance(config)
    }

    /**
     * Open the chatbot widget using the configuration from initialize().
     * Simplest way to show the widget — one-liner after initialization.
     *
     * Example: YourGPTSDK.show(this)
     */
    fun show(activity: FragmentActivity) {
        val config = core.currentConfig
            ?: throw YourGPTError.NotInitialized()
        openChatbotBottomSheet(activity.supportFragmentManager, config)
    }

    /**
     * Open the chatbot widget and navigate directly to a specific session/conversation.
     * Useful for deep-linking from notifications in ADVANCED mode, or for
     * programmatic navigation to a known conversation.
     *
     * @param activity The FragmentActivity to show the widget in
     * @param sessionUid The session/conversation UID to open
     */
    fun openSession(activity: FragmentActivity, sessionUid: String) {
        val baseConfig = core.currentConfig
            ?: throw YourGPTError.NotInitialized()

        val configWithSession = baseConfig.withParams(mapOf("session_uid" to sessionUid))
        openChatbotBottomSheet(activity.supportFragmentManager, configWithSession)
    }

    suspend fun setUserContext(context: Map<String, Any>) {
        core.setUserContext(context)
    }
    
    val isReady: Boolean
        get() = core.isReady
    
    val currentState: YourGPTSDKState
        get() = core.currentState
    
    val stateFlow: StateFlow<YourGPTSDKState>
        get() = core.state
    
    fun buildWidgetUrl(additionalParams: Map<String, String> = emptyMap()): String {
        return core.buildWidgetUrl(additionalParams)
    }
    
    fun on(event: String, callback: (Any?) -> Unit) {
        core.on(event, callback)
    }
    
    fun off(event: String, callback: (Any?) -> Unit) {
        core.off(event, callback)
    }
    
    /**
     * Notify event listeners that a push was received without showing a notification.
     * Mirrors iOS's YourGPTNotificationClient.notifyPushReceived().
     */
    fun notifyPushReceived(data: Map<String, Any>) {
        eventListener?.onPushMessageReceived(data)
    }

    fun destroy() {
        core.destroy()
    }
}