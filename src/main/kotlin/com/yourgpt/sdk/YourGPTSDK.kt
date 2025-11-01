package com.yourgpt.sdk

import android.content.Context
import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.flow.StateFlow

object YourGPTSDK {
    const val VERSION = "1.0.0"
    
    private val core = YourGPTSDKCore.getInstance()
    private var eventListener: YourGPTEventListener? = null
    
    suspend fun initialize(configuration: YourGPTConfig) {
        core.initialize(configuration)
    }
    
    fun setEventListener(listener: YourGPTEventListener?) {
        eventListener = listener
        ChatbotBottomSheetDialog.setEventListener(listener)
    }
    
    fun openChatbotBottomSheet(fragmentManager: FragmentManager, configuration: YourGPTConfig) {
        val bottomSheet = ChatbotBottomSheetDialog.newInstance(configuration)
        // Show the dialog but keep it hidden until loading is complete
        bottomSheet.show(fragmentManager, "ChatbotBottomSheet")
    }
    
    fun createChatbotBottomSheet(configuration: YourGPTConfig): ChatbotBottomSheetDialog {
        return ChatbotBottomSheetDialog.newInstance(configuration)
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
    
    fun destroy() {
        core.destroy()
    }
}