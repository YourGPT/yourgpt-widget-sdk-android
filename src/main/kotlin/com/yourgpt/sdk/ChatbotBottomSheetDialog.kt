package com.yourgpt.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
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

/**
 * Per-dialog listener for chatbot events.
 * Mirrors iOS's YourGPTChatbotDelegate protocol.
 */
interface ChatbotDialogListener {
    fun chatbotDidReceiveMessage(message: Map<String, Any>)
    fun chatbotDidOpen()
    fun chatbotDidClose()
    fun chatbotDidFailWithError(error: String)
    fun chatbotDidStartLoading()
    fun chatbotDidFinishLoading()
}

class ChatbotBottomSheetDialog : BottomSheetDialogFragment() {

    override fun getTheme(): Int = com.google.android.material.R.style.Theme_Design_BottomSheetDialog

    private lateinit var webView: WebView
    private lateinit var configuration: YourGPTConfig
    private var eventListener: YourGPTEventListener? = null
    private val sdk = YourGPTSDKCore.getInstance()

    // UI State Management
    private lateinit var loadingView: View
    private lateinit var errorView: FrameLayout
    private lateinit var webViewContainer: FrameLayout
    private var isSDKReady = false

    /** Per-dialog listener. Mirrors iOS's YourGPTChatbotDelegate. */
    var dialogListener: ChatbotDialogListener? = null

    /** Custom loading view provider. Set before showing the dialog. */
    var customLoadingViewProvider: ((Context) -> View)? = null

    /** Custom error view provider. Receives the error message. Set before showing the dialog. */
    var customErrorViewProvider: ((Context, String) -> View)? = null


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
            ?: throw YourGPTError.InvalidConfiguration("YourGPTConfig is required")

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
        // Set theme to remove default bottom sheet styling
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        makeBottomSheetFullScreen()
        setupBottomSheetBehavior()
        dialogListener?.chatbotDidOpen()
    }

    private fun setupBottomSheetBehavior() {
        dialog?.let { dialog ->
            if (dialog is BottomSheetDialog) {
                val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                bottomSheet?.let { sheet ->
                    // Set background to white without rounded corners for full screen
                    sheet.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white))

                    val behavior = BottomSheetBehavior.from(sheet)

                    // Disable dragging - only close via 'chatbot-close' event
                    behavior.isDraggable = false
                    behavior.isHideable = false
                    behavior.skipCollapsed = true

                    // Set to full screen height
                    val displayMetrics = resources.displayMetrics
                    val fullHeight = displayMetrics.heightPixels
                    behavior.peekHeight = fullHeight
                    behavior.maxHeight = fullHeight

                    // Start in expanded state
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED

                    // Force layout to ensure full height
                    sheet.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
                    sheet.requestLayout()
                }
            }
        }
    }


    private fun createMainLayout(): FrameLayout {
        val mainContainer = FrameLayout(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xFFFFFFFF.toInt())
        }

        // WebView container
        webViewContainer = FrameLayout(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(0, 0, 0, 0)
            visibility = View.GONE
        }

        // Loading view (custom or default)
        loadingView = customLoadingViewProvider?.invoke(requireContext()) ?: createDefaultLoadingView()

        // Error view container
        errorView = FrameLayout(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
        }

        mainContainer.addView(webViewContainer)
        mainContainer.addView(loadingView)
        mainContainer.addView(errorView)

        return mainContainer
    }

    private fun createDefaultLoadingView(): View {
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

    private fun createDefaultErrorView(message: String): View {
        val errorContainer = LinearLayout(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xFFFFFFFF.toInt())
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }

        val errorTitle = TextView(requireContext()).apply {
            text = "Connection Error"
            textSize = 18f
            setTextColor(0xFF333333.toInt())
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val errorText = TextView(requireContext()).apply {
            text = message
            textSize = 14f
            setTextColor(0xFFDC3545.toInt())
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 16
            }
        }

        val retryButton = Button(requireContext()).apply {
            text = "Try Again"
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 32
            }
            setOnClickListener { retryConnection() }
        }

        errorContainer.addView(errorTitle)
        errorContainer.addView(errorText)
        errorContainer.addView(retryButton)

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
                        dialogListener?.chatbotDidFailWithError(errorMessage)
                    }
                    else -> {
                        // Handle other states if needed
                    }
                }
            }
        }
    }

    private fun initializeSDK() {
        dialogListener?.chatbotDidStartLoading()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                sdk.initialize(configuration)
            } catch (error: Exception) {
                val errorMessage = "SDK initialization failed: ${error.message}"
                showErrorView(errorMessage)
                eventListener?.onError(errorMessage)
                dialogListener?.chatbotDidFailWithError(errorMessage)
            }
        }
    }

    private fun retryConnection() {
        hideErrorView()
        showLoadingView()
        initializeSDK()
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
        loadingView.visibility = View.GONE
        webViewContainer.visibility = View.GONE

        // Clear previous error content
        errorView.removeAllViews()

        // Use custom or default error view
        val content = customErrorViewProvider?.invoke(requireContext(), message)
            ?: createDefaultErrorView(message)
        errorView.addView(content)
        errorView.visibility = View.VISIBLE
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
            // Ensure WebView takes full space without padding
            setPadding(0, 0, 0, 0)
        }

        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            // Enable better text sizing for mobile
            layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
            // Set mobile user agent for better mobile experience
            userAgentString = "${userAgentString} YourGPT-Android-SDK"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                eventListener?.onLoadingStarted()
                dialogListener?.chatbotDidStartLoading()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                hideLoadingView()
                hideErrorView()
                webViewContainer.visibility = View.VISIBLE
                injectJavaScript()
                // Send cached FCM token to widget backend via JS bridge
                view?.let { YourGPTNotificationClient.registerTokenViaWebView(it) }
                // Navigate to specific session if opened from notification
                view?.let { navigateToSessionIfNeeded(it) }
                eventListener?.onLoadingFinished()
                dialogListener?.chatbotDidFinishLoading()
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
                dialogListener?.chatbotDidFailWithError(errorMessage)
            }
        }

        webView.webChromeClient = WebChromeClient()
        webView.addJavascriptInterface(JavaScriptInterface(), "YourGPTNative")

        webViewContainer.addView(webView)
    }

    private fun loadChatbot() {
        if (!isSDKReady) return

        try {
            val url = sdk.buildWidgetUrl(configuration.customParams)
            webView.loadUrl(url)
        } catch (error: Exception) {
            val errorMessage = "Failed to load chatbot: ${error.message}"
            showErrorView(errorMessage)
            eventListener?.onError(errorMessage)
            dialogListener?.chatbotDidFailWithError(errorMessage)
        }
    }

    private fun navigateToSessionIfNeeded(webView: WebView) {
        val sessionUid = configuration.customParams["session_uid"] ?: return

        val script = """
            (function() {
                window.postMessage({
                    type: 'open_session',
                    payload: {
                        session_uid: '$sessionUid'
                    }
                }, '*');
            })();
        """.trimIndent()

        webView.evaluateJavascript(script, null)
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

    // MARK: - Public Methods

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

    /**
     * Send session-specific data to the widget via JS bridge.
     * Mirrors iOS's setSessionData().
     */
    fun setSessionData(data: Map<String, Any>) {
        if (::webView.isInitialized) {
            val dataJson = JSONObject(data).toString()
            val script = """
                window.postMessage({
                    type: 'native:setSessionData',
                    payload: $dataJson
                }, '*');
            """.trimIndent()
            webView.evaluateJavascript(script, null)
        }
    }

    /**
     * Send visitor data to the widget via JS bridge, auto-enriched with device info.
     * Mirrors iOS's setVisitorData().
     */
    fun setVisitorData(data: Map<String, Any>) {
        if (::webView.isInitialized) {
            val enrichedData = data.toMutableMap().apply {
                put("platform", "Android")
                put("deviceModel", Build.MODEL)
                put("systemVersion", "Android ${Build.VERSION.RELEASE}")
                try {
                    val packageInfo = requireContext().packageManager
                        .getPackageInfo(requireContext().packageName, 0)
                    put("appVersion", packageInfo.versionName ?: "unknown")
                } catch (_: Exception) { }
            }
            val dataJson = JSONObject(enrichedData as Map<*, *>).toString()
            val script = """
                window.postMessage({
                    type: 'native:setVisitorData',
                    payload: $dataJson
                }, '*');
            """.trimIndent()
            webView.evaluateJavascript(script, null)
        }
    }

    /**
     * Send contact data to the widget via JS bridge.
     * Mirrors iOS's setContactData().
     */
    fun setContactData(data: Map<String, Any>) {
        if (::webView.isInitialized) {
            val dataJson = JSONObject(data).toString()
            val script = """
                window.postMessage({
                    type: 'native:setContactData',
                    payload: $dataJson
                }, '*');
            """.trimIndent()
            webView.evaluateJavascript(script, null)
        }
    }

    /**
     * Programmatically open the chat interface within the widget.
     * Mirrors iOS's openChat().
     */
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
        @android.webkit.JavascriptInterface
        fun postMessage(message: String) {
            try {
                // Handle plain string messages (like "chatbot-close")
                if (message == "chatbot-close") {
                    requireActivity().runOnUiThread {
                        eventListener?.onChatClosed()
                        dialogListener?.chatbotDidClose()
                        dismiss()
                    }
                    return
                }

                // Handle JSON messages
                val jsonObject = JSONObject(message)
                val type = jsonObject.getString("type")

                requireActivity().runOnUiThread {
                    when (type) {
                        // Message events
                        "message:received", "message:new" -> {
                            val payload = jsonObject.optJSONObject("payload")
                            if (payload != null) {
                                val messageMap = jsonToMap(payload)
                                eventListener?.onMessageReceived(messageMap)
                                dialogListener?.chatbotDidReceiveMessage(messageMap)
                            }
                        }
                        "message:sent" -> {
                            // Message sent confirmation
                        }

                        // Chat lifecycle events
                        "chat:opened", "widget:opened" -> {
                            eventListener?.onChatOpened()
                            dialogListener?.chatbotDidOpen()
                        }
                        "chat:closed", "widget:closed" -> {
                            eventListener?.onChatClosed()
                            dialogListener?.chatbotDidClose()
                            dismiss()
                        }
                        "chatbot-close" -> {
                            eventListener?.onChatClosed()
                            dialogListener?.chatbotDidClose()
                            dismiss()
                        }

                        // Connection events
                        "connection:established" -> {
                            // Connection established
                        }
                        "connection:lost" -> {
                            val payload = jsonObject.optJSONObject("payload")
                            val reason = payload?.optString("reason") ?: "Unknown"
                            val errorMsg = "Connection lost: $reason"
                            eventListener?.onError(errorMsg)
                            dialogListener?.chatbotDidFailWithError(errorMsg)
                        }
                        "connection:restored" -> {
                            // Connection restored
                        }

                        // User interaction events
                        "user:typing" -> {
                            // User is typing
                        }
                        "user:stopped_typing" -> {
                            // User stopped typing
                        }

                        // Escalation events
                        "escalation:to_human" -> {
                            // Escalated to human agent
                        }
                        "escalation:resolved" -> {
                            // Escalation resolved
                        }

                        // Error events
                        "error:occurred" -> {
                            val payload = jsonObject.optJSONObject("payload")
                            val errorMessage = payload?.optString("message") ?: "Unknown error"
                            eventListener?.onError(errorMessage)
                            dialogListener?.chatbotDidFailWithError(errorMessage)
                        }
                        "error:network" -> {
                            val payload = jsonObject.optJSONObject("payload")
                            val errorMessage = payload?.optString("message") ?: "Network error"
                            val msg = "Network error: $errorMessage"
                            eventListener?.onError(msg)
                            dialogListener?.chatbotDidFailWithError(msg)
                        }

                        // SDK lifecycle events
                        "sdk:initialized" -> {
                            // SDK initialized in WebView
                        }
                        "webview:loaded" -> {
                            // WebView content loaded
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

    private fun makeBottomSheetFullScreen() {
        dialog?.let { dialog ->
            dialog.window?.let { window ->
                // Make the dialog window full screen
                window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // Set status bar color to match the background
                window.statusBarColor = ContextCompat.getColor(requireContext(), android.R.color.white)

                // Remove dim background
                window.setDimAmount(0.5f)

                // Use default soft input mode
                window.setSoftInputMode(
                    android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                    android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
                )

                // Set flags to extend under status bar for true full screen
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()

        if (::webView.isInitialized) {
            webView.destroy()
        }
    }
}
