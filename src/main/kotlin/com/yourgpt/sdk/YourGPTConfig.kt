package com.yourgpt.sdk

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * YourGPT SDK Configuration Constants
 *
 * This file contains all the configuration constants used across the Android SDK.
 * DO NOT modify the base URL without proper coordination with the backend team.
 */
object YourGPTSDKConfig {
    
    /**
     * Widget API endpoint configuration
     */
    object Endpoints {
        /**
         * Base widget URL - DO NOT CHANGE without coordination
         */
        const val WIDGET_BASE = "https://widget.yourgpt.ai"
        
        /**
         * Constructs the full widget URL with the provided widget UID
         * Format: https://widget.yourgpt.ai/{widgetUid}
         * Example: https://widget.yourgpt.ai/232d2602-7cbd-4f6a-87eb-21058599d594
         */
        fun widgetURL(widgetUid: String): String {
            return "$WIDGET_BASE/$widgetUid"
        }
        
        /**
         * Constructs widget URL with query parameters
         */
        fun widgetURLWithParams(widgetUid: String, params: Map<String, String>): Uri {
            val baseUrl = widgetURL(widgetUid)
            val builder = Uri.parse(baseUrl).buildUpon()
            
            params.forEach { (key, value) ->
                builder.appendQueryParameter(key, value)
            }
            
            return builder.build()
        }
    }
    
    /**
     * SDK metadata
     */
    object SDK {
        const val VERSION = "1.0.0"
        const val PLATFORM = "Android"
        const val NAME = "YourGPT Android SDK"
    }
    
    /**
     * Default configuration values
     */
    object Defaults {
        const val DEBUG = false
        const val TIMEOUT_MS = 30_000L
    }
}


/**
 * Configuration data class for initializing the SDK
 */
@Parcelize
data class YourGPTConfig(
    /** Widget UID - required */
    val widgetUid: String,
    /** Enable debug logging */
    val debug: Boolean = YourGPTSDKConfig.Defaults.DEBUG,
    /** Custom parameters to pass to the widget */
    val customParams: Map<String, String> = emptyMap()
) : Parcelable {
    /**
     * Creates a copy of this config with updated parameters
     */
    fun withParams(additionalParams: Map<String, String>): YourGPTConfig {
        val mergedParams = customParams + additionalParams
        return copy(customParams = mergedParams)
    }
}

/**
 * Utility class for building widget URLs and managing configuration
 */
class YourGPTConfigBuilder(private val config: YourGPTConfig) {
    
    /**
     * Generates query parameters for the widget URL
     */
    private fun getQueryParams(): Map<String, String> {
        val params = mutableMapOf<String, String>()
        
        // Add custom parameters
        params.putAll(config.customParams)
        
        // Add SDK metadata
        params["sdk"] = YourGPTSDKConfig.SDK.PLATFORM
        params["sdkVersion"] = YourGPTSDKConfig.SDK.VERSION
        
        // Add mobile parameter
        params["mobileWebView"] = "true"
        
        return params
    }
    
    /**
     * Builds the complete widget URL with all parameters
     */
    fun buildWidgetURL(): Uri {
        return YourGPTSDKConfig.Endpoints.widgetURLWithParams(
            config.widgetUid,
            getQueryParams()
        )
    }
    
    /**
     * Gets the configuration object
     */
    fun getConfig(): YourGPTConfig = config
    
    /**
     * Updates the configuration with additional parameters
     */
    fun withParams(additionalParams: Map<String, String>): YourGPTConfigBuilder {
        return YourGPTConfigBuilder(config.withParams(additionalParams))
    }
}

/**
 * Creates a new configuration builder instance
 */
fun createConfig(config: YourGPTConfig): YourGPTConfigBuilder {
    return YourGPTConfigBuilder(config)
}