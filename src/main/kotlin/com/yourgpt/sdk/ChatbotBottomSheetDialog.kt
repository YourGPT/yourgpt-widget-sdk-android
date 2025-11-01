package com.yourgpt.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.json.JSONObject

class ChatbotBottomSheetDialog : BottomSheetDialogFragment() {
    
    private lateinit var webView: WebView
    private lateinit var configuration: YourGPTConfig
    private var eventListener: YourGPTEventListener? = null
    private val sdk = YourGPTSDKCore.getInstance()
    
    // UI State Management
    private lateinit var loadingView: View
    private lateinit var errorView: FrameLayout
    private lateinit var webViewContainer: FrameLayout
    private var isSDKReady = false
    
    companion object {
        private const val ARG_CONFIGURATION = "arg_configuration"
        
        fun newInstance(configuration: YourGPTConfig): ChatbotBottomSheetDialog {
            return ChatbotBottomSheetDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_CONFIGURATION, configuration)
                }
            }
        }
        
        private var globalEventListener: YourGPTEventListener? = null
        
        fun setEventListener(listener: YourGPTEventListener?) {
            globalEventListener = listener
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        @Suppress("DEPRECATION")
        configuration = arguments?.getParcelable(ARG_CONFIGURATION) as? YourGPTConfig
            ?: throw IllegalArgumentException("YourGPTConfig is required")
        
        eventListener = globalEventListener
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return createMainLayout()
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSDKObserver()
        initializeSDK()
    }
    
    override fun onStart() {
        super.onStart()
        setupBottomSheetBehavior()
    }

    private fun setupBottomSheetBehavior() {
        dialog?.let { dialog ->
            if (dialog is BottomSheetDialog) {
                val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                bottomSheet?.let { sheet ->
                    // Add border radius to top corners
                    val drawable = GradientDrawable().apply {
                        setColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                        cornerRadii = floatArrayOf(
                            48f, 48f, // top-left (16dp in pixels)
                            48f, 48f, // top-right (16dp in pixels)
                            0f, 0f,   // bottom-right
                            0f, 0f    // bottom-left
                        )
                    }
                    sheet.background = drawable

                    val behavior = BottomSheetBehavior.from(sheet)

                    // Calculate screen dimensions
                    val displayMetrics = resources.displayMetrics
                    val screenHeight = displayMetrics.heightPixels

                    // Disable dragging - only close via 'chatbot-close' event
                    behavior.isDraggable = false
                    behavior.isHideable = false

                    // Set peek height to 90% of screen
                    behavior.peekHeight = (screenHeight * 0.9).toInt()

                    // Set max height to 90% of screen
                    behavior.maxHeight = (screenHeight * 0.9).toInt()

                    // Start in expanded state
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }
    }
    
    private fun createMainLayout(): FrameLayout {
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        
        val mainContainer = FrameLayout(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                (screenHeight * 0.9).toInt() // Limit height to 90% of screen
            )
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        
        // WebView container
        webViewContainer = FrameLayout(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
        }
        
        // Loading view
        loadingView = createLoadingView()
        
        // Error view
        errorView = createErrorView()
        
        mainContainer.addView(webViewContainer)
        mainContainer.addView(loadingView)
        mainContainer.addView(errorView)
        
        return mainContainer
    }
    
    private fun createLoadingView(): View {
        val loadingContainer = FrameLayout(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        
        val progressBar = ProgressBar(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER
            }
        }
        
        val loadingText = TextView(requireContext()).apply {
            text = "Loading YourGPT..."
            textSize = 16f
            setTextColor(0xFF333333.toInt())
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER
                topMargin = 100 // Position below progress bar
            }
        }
        
        loadingContainer.addView(progressBar)
        loadingContainer.addView(loadingText)
        
        return loadingContainer
    }
    
    private fun createErrorView(): FrameLayout {
        val errorContainer = FrameLayout(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xFFFFFFFF.toInt())
            visibility = View.GONE
        }
        
        val errorText = TextView(requireContext()).apply {
            text = "Failed to load chatbot"
            textSize = 16f
            setTextColor(0xFFDC3545.toInt())
            gravity = android.view.Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER
            }
        }
        
        errorContainer.addView(errorText)
        
        return errorContainer
    }
    
    private fun setupSDKObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            sdk.state.collect { state ->
                when (state.connectionState) {
                    YourGPTConnectionState.CONNECTED -> {
                        isSDKReady = true
                        setupWebView()
                        loadChatbot()
                    }
                    YourGPTConnectionState.ERROR -> {
                        val errorMessage = state.error ?: "Unknown error occurred"
                        showErrorView(errorMessage)
                        eventListener?.onError(errorMessage)
                    }
                    else -> {
                        // Handle other states if needed
                    }
                }
            }
        }
    }
    
    private fun initializeSDK() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                sdk.initialize(configuration)
            } catch (error: Exception) {
                val errorMessage = "SDK initialization failed: ${error.message}"
                showErrorView(errorMessage)
                eventListener?.onError(errorMessage)
            }
        }
    }
    
    private fun showLoadingView() {
        loadingView.visibility = View.VISIBLE
        webViewContainer.visibility = View.GONE
        errorView.visibility = View.GONE
    }
    
    private fun hideLoadingView() {
        loadingView.visibility = View.GONE
    }
    
    private fun showErrorView(message: String) {
        errorView.visibility = View.VISIBLE
        webViewContainer.visibility = View.GONE
        loadingView.visibility = View.GONE
        
        // Update error message
        val errorText = errorView.getChildAt(0) as? TextView
        errorText?.text = message
    }
    
    private fun hideErrorView() {
        errorView.visibility = View.GONE
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        if (!isSDKReady) return
        
        webView = WebView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(false)
        }
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                eventListener?.onLoadingStarted()
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                hideLoadingView()
                hideErrorView()
                webViewContainer.visibility = View.VISIBLE
                injectJavaScript()
                eventListener?.onLoadingFinished()
            }
            
            @Deprecated("Deprecated in Java")
            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                val errorMessage = "WebView error: $description"
                showErrorView(errorMessage)
                eventListener?.onError(errorMessage)
            }
        }
        
        webView.webChromeClient = WebChromeClient()
        webView.addJavascriptInterface(JavaScriptInterface(), "YourGPTNative")
        
        webViewContainer.addView(webView)
    }
    
    private fun loadChatbot() {
        if (!isSDKReady) return
        
        try {
            val url = sdk.buildWidgetUrl()
            webView.loadUrl(url)
        } catch (error: Exception) {
            val errorMessage = "Failed to load chatbot: ${error.message}"
            showErrorView(errorMessage)
            eventListener?.onError(errorMessage)
        }
    }
    
    private fun injectJavaScript() {
        val script = """
            window.addEventListener('message', function(event) {
                if (event.data) {
                    if (typeof event.data === 'object') {
                        YourGPTNative.postMessage(JSON.stringify(event.data));
                    } else if (typeof event.data === 'string') {
                        YourGPTNative.postMessage(event.data);
                    }
                }
            });

            window.nativeBridge = {
                sendMessage: function(message) {
                    window.postMessage({ type: 'native:sendMessage', payload: message }, '*');
                },
                setUserContext: function(context) {
                    window.postMessage({ type: 'native:setUserContext', payload: context }, '*');
                }
            };
        """.trimIndent()

        webView.evaluateJavascript(script, null)
    }
    
    fun sendMessage(message: String) {
        if (::webView.isInitialized) {
            val script = """
                window.postMessage({
                    type: 'sendMessage',
                    payload: '$message'
                }, '*');
            """.trimIndent()
            
            webView.evaluateJavascript(script, null)
        }
    }
    
    fun setUserContext(context: Map<String, Any>) {
        if (::webView.isInitialized) {
            val contextJson = JSONObject(context).toString()
            val script = """
                window.postMessage({
                    type: 'setUserContext',
                    payload: $contextJson
                }, '*');
            """.trimIndent()
            
            webView.evaluateJavascript(script, null)
        }
    }
    
    fun openChat() {
        if (::webView.isInitialized) {
            val script = """
                window.postMessage({
                    type: 'openChat'
                }, '*');
            """.trimIndent()
            
            webView.evaluateJavascript(script, null)
        }
    }
    
    private inner class JavaScriptInterface {
        @JavascriptInterface
        fun postMessage(message: String) {
            try {
                // Handle plain string messages (like "chatbot-close")
                if (message == "chatbot-close") {
                    requireActivity().runOnUiThread {
                        eventListener?.onChatClosed()
                        dismiss()
                    }
                    return
                }

                // Handle JSON messages
                val jsonObject = JSONObject(message)
                val type = jsonObject.getString("type")

                requireActivity().runOnUiThread {
                    when (type) {
                        "message:new" -> {
                            val payload = jsonObject.getJSONObject("payload")
                            val messageMap = jsonToMap(payload)
                            eventListener?.onMessageReceived(messageMap)
                        }
                        "chat:opened" -> {
                            eventListener?.onChatOpened()
                        }
                        "chat:closed" -> {
                            eventListener?.onChatClosed()
                            dismiss()
                        }
                    }
                }
            } catch (e: Exception) {
                eventListener?.onError("Error parsing message: ${e.message}")
            }
        }
    }
    
    private fun jsonToMap(jsonObject: JSONObject): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        val keys = jsonObject.keys()
        
        while (keys.hasNext()) {
            val key = keys.next()
            val value = jsonObject.get(key)
            map[key] = value
        }
        
        return map
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        if (::webView.isInitialized) {
            webView.destroy()
        }
    }
}
