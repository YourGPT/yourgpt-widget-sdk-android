# YourGPT Android SDK - Internal Developer Documentation

## Architecture Overview

The Android SDK follows the MVVM pattern with reactive programming using Kotlin Coroutines and Flow:

```
YourGPTSDKCore (Business Logic)
├── Singleton pattern for global state management
├── StateFlow for reactive state updates
├── Coroutines for async operations
└── Event-driven communication

ChatbotActivity (UI Layer)
├── Lifecycle-aware components
├── Flow-based state observation
├── Loading/Error state management
└── WebView integration with JavaScript bridge
```

## Core Components

### 1. YourGPTSDKCore (Singleton)

**Location**: `src/main/kotlin/com/yourgpt/sdk/YourGPTSDKCore.kt`

**Purpose**: Central SDK management with reactive state handling

**Architecture Patterns**:
- Singleton pattern with thread-safe initialization
- StateFlow for reactive state management
- CoroutineScope with SupervisorJob for structured concurrency
- ConcurrentHashMap for thread-safe event handling

**State Management**:
```kotlin
data class YourGPTSDKState(
    val isInitialized: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val connectionState: YourGPTConnectionState = DISCONNECTED
)

enum class YourGPTConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, ERROR
}
```

**Key Features**:
- `StateFlow<YourGPTSDKState>` for reactive state updates
- Suspend functions for async operations
- Thread-safe event system with ConcurrentHashMap
- Immutable state objects with copy semantics

**Initialization Flow**:
1. `initialize(config)` called with `YourGPTConfiguration`
2. State transitions: `DISCONNECTED` → `CONNECTING`
3. Widget validation (async simulation)
4. Success: `CONNECTED` | Failure: `ERROR`
5. State changes emitted automatically via StateFlow

### 2. YourGPTConfiguration

**Purpose**: Immutable configuration data class with Parcelable support

**Features**:
- Parcelable for Intent extras
- Default value handling
- Debug mode for development

```kotlin
@Parcelize
data class YourGPTConfiguration(
    val widgetUid: String,              // Required
    val debug: Boolean = false          // Default: false
) : Parcelable
```

### 3. ChatbotActivity

**Location**: `src/main/kotlin/com/yourgpt/sdk/ChatbotActivity.kt`

**Purpose**: Activity wrapper with enhanced lifecycle management

**Component Architecture**:
```
AppCompatActivity
├── Lifecycle-aware coroutines (lifecycleScope)
├── StateFlow observation
├── Dynamic UI state management
├── WebView integration
└── Event listener pattern
```

**State Variables**:
- `isSDKReady`: SDK initialization completion
- `loadingView/errorView/webViewContainer`: UI state containers
- `configuration`: Parcelable configuration object

**Lifecycle Flow**:
```
onCreate()
    ↓
setupUI() → Create layout programmatically
    ↓
setupSDKObserver() → StateFlow collection
    ↓
initializeSDK() → Async SDK setup
    ↓
handleSDKStateChange() → Reactive UI updates
    ↓
setupWebView() → WebView creation (if ready)
    ↓
loadChatbot() → URL loading
    ↓
Bridge communication established
```

### 4. UI State Management

**Dynamic Layout Creation**:
```kotlin
private fun createMainLayout(): FrameLayout {
    // FrameLayout with overlapping views
    // - WebView container (hidden initially)
    // - Loading view (visible by default)
    // - Error view (hidden by default)
}
```

**State Transitions**:
- Loading → Error: SDK initialization fails
- Loading → WebView: SDK ready, load chatbot
- WebView → Error: WebView loading fails
- Any → Loading: Restart process

## Communication Architecture

### Reactive State Management
```kotlin
// StateFlow observation
lifecycleScope.launch {
    sdk.state.collect { state ->
        handleSDKStateChange(state)
    }
}
```

### Event System
```kotlin
// SDK-level events
core.on("sdk:initialized") { data -> }
core.on("sdk:stateChanged") { data -> }
core.on("sdk:error") { data -> }
```

### Bridge Communication
```kotlin
// Android → JavaScript
webView.evaluateJavascript("window.postMessage(...)")

// JavaScript → Android
webView.addJavascriptInterface(JavaScriptInterface(), "YourGPTNative")

@JavascriptInterface
fun postMessage(message: String) {
    // Handle messages from JavaScript
}
```

### Event Listener Pattern
```kotlin
interface YourGPTEventListener {
    fun onMessageReceived(message: Map<String, Any>)
    fun onChatOpened()
    fun onChatClosed()
    fun onError(error: String)
    fun onLoadingStarted()
    fun onLoadingFinished()
}
```

## Error Handling Strategy

### Exception Types
- `IllegalArgumentException`: Invalid configuration
- `IllegalStateException`: SDK not ready/initialized
- `Exception`: General errors (network, WebView, etc.)

### Error Propagation Flow
```
Error Source
    ↓
Suspend Function Exception
    ↓
try/catch in Coroutine
    ↓
StateFlow State Update
    ↓
UI Observer (collect)
    ↓
UI Update + Event Listener Callback
```

### Multi-Layer Error Handling
1. **SDK Core Level**: Configuration validation, async operations
2. **WebView Level**: Page loading, navigation errors
3. **Activity Level**: Lifecycle, state management
4. **Application Level**: User-facing error display

## Performance Considerations

### Memory Management
- Singleton prevents multiple SDK instances
- Proper coroutine scope management with SupervisorJob
- WebView cleanup in onDestroy
- StateFlow subscription cleanup via lifecycle

### Concurrency
```kotlin
// Structured concurrency with lifecycleScope
lifecycleScope.launch {
    try {
        sdk.initialize(configuration)
    } catch (error: Exception) {
        // Handle error
    }
}
```

### Threading
- Main thread for UI updates (`runOnUiThread`)
- Background thread for SDK operations
- StateFlow ensures thread-safe state updates

## UI Implementation Details

### Programmatic Layout Creation
```kotlin
private fun createLoadingView(): View {
    val container = FrameLayout(this)
    val progressBar = ProgressBar(this)
    val textView = TextView(this)
    // Layout with constraints
}
```

### View State Management
```kotlin
private fun showLoadingView() {
    loadingView.visibility = View.VISIBLE
    errorView.visibility = View.GONE
    webViewContainer.visibility = View.GONE
}
```

### WebView Configuration
```kotlin
with(webView.settings) {
    javaScriptEnabled = true
    domStorageEnabled = true
    loadWithOverviewMode = true
    useWideViewPort = true
    setSupportZoom(false)
}
```

## Intent-Based Communication

### Activity Launch
```kotlin
companion object {
    fun createIntent(context: Context, configuration: YourGPTConfiguration): Intent {
        return Intent(context, ChatbotActivity::class.java).apply {
            putExtra(EXTRA_CONFIGURATION, configuration)
        }
    }
}
```

### Configuration Passing
```kotlin
// Parcelable support for complex objects
configuration = intent.getParcelableExtra(EXTRA_CONFIGURATION)
    ?: throw IllegalArgumentException("YourGPTConfiguration is required")
```

## Debug & Development Features

### Debug Mode
```kotlin
YourGPTConfiguration(debug = true) // Enables detailed logging
```

### Logging
```kotlin
private fun log(message: String) {
    if (config?.debug == true) {
        android.util.Log.d("YourGPTSDK", message)
    }
}
```

### Development Tools
- Android Log statements for state transitions
- Flow debugging with logging operators
- Error tracking with event listeners
- WebView debugging support

## Extension Points

### Custom Event Handling
```kotlin
// Custom event handlers
YourGPTSDK.on("custom:event") { data ->
    handleCustomEvent(data)
}
```

### Configuration Extensions
- Theme customization with enum
- Additional URL parameters
- Custom base URLs
- Environment-specific settings

## Build Configuration

### Dependencies
```gradle
dependencies {
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.webkit:webkit:1.8.0'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
}
```

### Permissions
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### ProGuard Rules
```proguard
-keep class com.yourgpt.sdk.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
```

## Testing Strategy

### Unit Testing
- SDK initialization logic
- Configuration validation
- URL building functions
- State management
- Event system functionality

### UI Testing
- Activity lifecycle
- Loading state display
- Error state handling
- WebView integration
- Intent handling

### Integration Testing
- End-to-end initialization flow
- Bridge communication
- StateFlow state updates
- Memory management

### Manual Testing Checklist
- [ ] SDK initializes with valid/invalid configurations
- [ ] Loading states display correctly
- [ ] Error states show appropriate messages
- [ ] WebView loads and communicates
- [ ] Activity recreation works correctly
- [ ] Memory usage remains stable
- [ ] Coroutines clean up properly

## Common Issues & Solutions

### WebView Not Loading
- Check internet permissions in manifest
- Verify widget URL validity
- Test with debug mode enabled
- Validate network connectivity

### SDK Initialization Fails
- Validate widgetUid format
- Check coroutine context
- Review StateFlow updates
- Test configuration parameters

### Bridge Communication Issues
- Verify JavaScript injection timing
- Check @JavascriptInterface annotation
- Validate message format
- Test with debug logging

### Memory Leaks
- Proper coroutine scope cleanup
- WebView reference management
- StateFlow subscription lifecycle
- Activity context handling

## Development Workflow

### Local Development
1. Use example app for testing
2. Enable debug mode for detailed logging
3. Test with various configurations
4. Verify error handling paths
5. Test activity recreation scenarios

### Build Process
1. Kotlin compilation
2. AAR generation
3. Dependency resolution
4. ProGuard optimization

### Release Process
1. Version bump in build.gradle
2. Update CHANGELOG.md
3. Test on devices and emulators
4. Publish to Maven/JitPack
5. Update documentation

## Platform-Specific Considerations

### Android Lifecycle
- Handle configuration changes
- Manage activity recreation
- Proper resource cleanup
- Background/foreground transitions

### API Level Compatibility
- Minimum SDK 21 (Android 5.0)
- WebView compatibility checks
- Permission handling
- Runtime permissions if needed

### Performance Optimization
- Lazy WebView initialization
- Efficient state updates
- Memory-conscious bitmap handling
- Network request optimization