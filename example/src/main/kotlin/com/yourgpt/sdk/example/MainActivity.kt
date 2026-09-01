package com.yourgpt.sdk.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.yourgpt.sdk.YourGPTNotificationClient
import com.yourgpt.sdk.YourGPTNotificationConfig

/**
 * MainActivity demonstrating the simplest integration of YourGPT SDK
 * with push notifications
 */
class MainActivity : AppCompatActivity() {

    companion object {
        /** Replace with your actual YourGPT widget UID */
        const val WIDGET_UID = "your-widget-uid"
    }

    // Notification permission launcher for Android 13+
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notifications enabled!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notifications disabled. You won't receive messages when app is closed.", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // ===== SIMPLE INTEGRATION WITH CUSTOM ICON & SOUND =====

        // 1. Build notification config with custom icon and sound
        val notifConfig = YourGPTNotificationConfig.builder()
            .setSmallIcon(R.drawable.ic_notification)
            .setSoundUri(Uri.parse("android.resource://${packageName}/raw/notification_sound"))
            .build()

        // 2. Quick setup with YourGPT widget (Minimalist mode - handles everything automatically)
        YourGPTNotificationClient.quickSetup(
            context = this,
            widgetUid = WIDGET_UID,
            config = notifConfig
        )
        
        // 3. Request notification permission for Android 13+
        requestNotificationPermission()

        // ===== END OF INTEGRATION =====

        // Forward the intent to HomeScreenActivity (preserves notification extras)
        val homeIntent = Intent(this, HomeScreenActivity::class.java)
        intent?.let {
            homeIntent.action = it.action
            it.extras?.let { extras -> homeIntent.putExtras(extras) }
        }
        startActivity(homeIntent)
        finish()
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                else -> {
                    // Request permission
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}

/**
 * ALTERNATIVE: Advanced Mode Example
 * 
 * If you need more control over notifications, you can use advanced mode:
 * 
 * class MainActivityAdvanced : AppCompatActivity() {
 *     
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         
 *         // Initialize with advanced mode for custom handling
 *         YourGPTNotificationClient.initialize(
 *             context = this,
 *             widgetUid = "your-widget-uid",
 *             mode = NotificationMode.ADVANCED
 *         )
 *         
 *         // In advanced mode, you handle the notifications yourself
 *         // You can create a custom FirebaseMessagingService (see CustomNotificationService.kt)
 *         // and process notifications however you want
 *     }
 * }
 */