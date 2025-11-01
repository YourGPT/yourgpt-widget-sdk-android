package com.yourgpt.sdk

interface YourGPTEventListener {
    fun onMessageReceived(message: Map<String, Any>)
    fun onChatOpened()
    fun onChatClosed()
    fun onError(error: String)
    fun onLoadingStarted()
    fun onLoadingFinished()
}