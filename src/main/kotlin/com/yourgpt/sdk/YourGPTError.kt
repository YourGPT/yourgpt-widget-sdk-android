package com.yourgpt.sdk

/**
 * Structured error types for the YourGPT SDK.
 * Mirrors iOS's YourGPTError enum with LocalizedError conformance.
 */
sealed class YourGPTError(message: String) : Exception(message) {

    /** Configuration is invalid or missing required fields. */
    class InvalidConfiguration(detail: String = "Invalid configuration")
        : YourGPTError("Invalid configuration: $detail")

    /** SDK has not been initialized. Call initialize() first. */
    class NotInitialized
        : YourGPTError("SDK not initialized. Call initialize() first.")

    /** SDK is not ready (still loading or in error state). */
    class NotReady
        : YourGPTError("SDK not ready")

    /** Failed to build a valid widget URL. */
    class InvalidURL
        : YourGPTError("Invalid URL")

    /** An error occurred in the WebView. */
    class WebViewError(detail: String = "WebView error")
        : YourGPTError("WebView error: $detail")
}
