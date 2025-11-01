package com.yourgpt.sdk.example

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yourgpt.sdk.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), YourGPTEventListener {
    
    private lateinit var openBottomSheetButton: Button
    private lateinit var statusText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        setupUI()
        setupSDK()
        initializeSDK()
    }
    
    private fun setupUI() {
        openBottomSheetButton = findViewById(R.id.btn_open_bottom_sheet)
        statusText = findViewById(R.id.tv_status)
        
        openBottomSheetButton.setOnClickListener {
            openChatbotBottomSheet()
        }
        
        // Disable button until SDK is ready
        openBottomSheetButton.isEnabled = false
        statusText.text = "SDK Status: Initializing..."
    }
    
    private fun setupSDK() {
        YourGPTSDK.setEventListener(this)
        
        // Observe SDK state changes
        lifecycleScope.launch {
            YourGPTSDK.stateFlow.collect { state ->
                updateUIForSDKState(state)
            }
        }
    }
    
    private fun initializeSDK() {
        val configuration = YourGPTConfig(
            widgetUid = "69dd8b5d-d4bf-444c-a40f-732d15248ae9",
        )
        
        lifecycleScope.launch {
            try {
                YourGPTSDK.initialize(configuration)
            } catch (error: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "SDK initialization failed: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private fun updateUIForSDKState(state: YourGPTSDKState) {
        runOnUiThread {
            statusText.text = "SDK Status: ${state.connectionState.name.lowercase().replaceFirstChar { it.uppercase() }}"
            
            when (state.connectionState) {
                YourGPTConnectionState.CONNECTED -> {
                    statusText.setTextColor(0xFF28A745.toInt()) // Green
                    openBottomSheetButton.isEnabled = true
                }
                YourGPTConnectionState.CONNECTING -> {
                    statusText.setTextColor(0xFFFFC107.toInt()) // Orange
                    openBottomSheetButton.isEnabled = false
                }
                YourGPTConnectionState.ERROR -> {
                    statusText.setTextColor(0xFFDC3545.toInt()) // Red
                    openBottomSheetButton.isEnabled = false
                    state.error?.let {
                        statusText.text = "SDK Error: $it"
                    }
                }
                YourGPTConnectionState.DISCONNECTED -> {
                    statusText.setTextColor(0xFF6C757D.toInt()) // Gray
                    openBottomSheetButton.isEnabled = false
                }
            }
        }
    }
    
    private fun openChatbotBottomSheet() {
        val configuration = YourGPTConfig(
            widgetUid = "69dd8b5d-d4bf-444c-a40f-732d15248ae9",
        )
        
        YourGPTSDK.openChatbotBottomSheet(supportFragmentManager, configuration)
    }
    
    // YourGPTEventListener implementation
    override fun onMessageReceived(message: Map<String, Any>) {
        runOnUiThread {
            Toast.makeText(
                this,
                "New message: $message",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    override fun onChatOpened() {
        runOnUiThread {
            Toast.makeText(this, "Chat opened", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onChatClosed() {
        runOnUiThread {
            Toast.makeText(this, "Chat closed", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onError(error: String) {
        runOnUiThread {
            Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onLoadingStarted() {
        // Loading started - no toast message needed
    }
    
    override fun onLoadingFinished() {
        // Loading finished - no toast message needed
    }
}