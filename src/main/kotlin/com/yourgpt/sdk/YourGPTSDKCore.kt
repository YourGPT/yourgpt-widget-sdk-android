package com.yourgpt.sdk

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap

enum class YourGPTConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, ERROR
}

data class YourGPTSDKState(
    val isInitialized: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val connectionState: YourGPTConnectionState = YourGPTConnectionState.DISCONNECTED
)

class YourGPTSDKCore private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: YourGPTSDKCore? = null
        
        fun getInstance(): YourGPTSDKCore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: YourGPTSDKCore().also { INSTANCE = it }
            }
        }
    }
    
    private var config: YourGPTConfig? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val _state = MutableStateFlow(YourGPTSDKState())
    val state: StateFlow<YourGPTSDKState> = _state.asStateFlow()
    
    private val eventListeners = ConcurrentHashMap<String, MutableList<(Any?) -> Unit>>()
    
    suspend fun initialize(config: YourGPTConfig) {
        log("Initializing SDK with widgetUid: ${config.widgetUid}")
        
        if (config.widgetUid.isEmpty()) {
            throw IllegalArgumentException("widgetUid is required for initialization")
        }
        
        _state.value = _state.value.copy(
            isLoading = true,
            error = null,
            connectionState = YourGPTConnectionState.CONNECTING
        )
        
        try {
            this.config = config
            
            // Validate widget
            validateWidget()
            
            _state.value = _state.value.copy(
                isInitialized = true,
                isLoading = false,
                connectionState = YourGPTConnectionState.CONNECTED
            )
            
            emit("sdk:initialized", config)
            log("SDK initialized successfully")
            
        } catch (error: Exception) {
            val errorMessage = "Failed to initialize SDK: ${error.message}"
            
            _state.value = _state.value.copy(
                isLoading = false,
                error = errorMessage,
                connectionState = YourGPTConnectionState.ERROR
            )
            
            emit("sdk:error", errorMessage)
            throw error
        }
    }
    
    private suspend fun validateWidget() {
        config?.let { config ->
            // Simulate widget validation with delay
            delay(1000)
            
            if (config.widgetUid.length < 3) {
                throw IllegalArgumentException("Invalid widget UID")
            }
        } ?: throw IllegalStateException("SDK not configured")
    }
    
    fun buildWidgetUrl(additionalParams: Map<String, String> = emptyMap()): String {
        val config = this.config ?: throw IllegalStateException("SDK not initialized")
        
        if (!isReady) {
            throw IllegalStateException("SDK not ready")
        }
        
        val configBuilder = createConfig(config)
        val urlBuilder = if (additionalParams.isNotEmpty()) {
            configBuilder.withParams(additionalParams)
        } else {
            configBuilder
        }
        
        return urlBuilder.buildWidgetURL().toString()
    }
    
    val isReady: Boolean
        get() = _state.value.let { 
            it.isInitialized && !it.isLoading && it.error == null 
        }
    
    val currentConfig: YourGPTConfig?
        get() = config
    
    val currentState: YourGPTSDKState
        get() = _state.value
    
    // Event system
    fun on(event: String, callback: (Any?) -> Unit) {
        eventListeners.getOrPut(event) { mutableListOf() }.add(callback)
    }
    
    fun off(event: String, callback: (Any?) -> Unit) {
        eventListeners[event]?.remove(callback)
    }
    
    private fun emit(event: String, data: Any? = null) {
        eventListeners[event]?.forEach { callback ->
            try {
                callback(data)
            } catch (e: Exception) {
                log("Error in event callback: ${e.message}")
            }
        }
    }
    
    suspend fun setUserContext(context: Map<String, Any>) {
        log("Setting user context: $context")
        emit("sdk:userContextSet", context)
    }
    
    suspend fun updateConfig(newConfig: YourGPTConfig) {
        if (!_state.value.isInitialized) {
            throw IllegalStateException("SDK not initialized")
        }
        
        this.config = newConfig
        emit("sdk:configUpdated", newConfig)
    }
    
    private fun log(message: String) {
        if (config?.debug == true) {
            android.util.Log.d("YourGPTSDK", message)
        }
    }
    
    fun destroy() {
        log("Destroying SDK instance")
        scope.cancel()
        config = null
        _state.value = YourGPTSDKState()
        eventListeners.clear()
        INSTANCE = null
    }
}