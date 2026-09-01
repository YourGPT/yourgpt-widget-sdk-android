package com.yourgpt.sdk

interface YourGPTEventListener {
    // Widget events
    fun onMessageReceived(message: Map<String, Any>)
    fun onChatOpened()
    fun onChatClosed()
    fun onError(error: String)
    fun onLoadingStarted()
    fun onLoadingFinished()
    
    // Notification events
    fun onFCMTokenReceived(token: String) {}
    fun onPushMessageReceived(data: Map<String, Any>) {}
    fun onNotificationClicked(extras: Map<String, String>) {}
    fun onWidgetOpenRequested(widgetUid: String) {}
    fun onNotificationPermissionDenied() {}
    fun onNotificationPermissionGranted() {}
}