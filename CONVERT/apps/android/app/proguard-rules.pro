# ===================================================================
# ProGuard & R8 Optimization / Obfuscation Rules
# Application: AI Downloader (com.nextaitechnology.antidetect)
# Security Grade: Hardened Production
# ===================================================================

# 1. Giữ nguyên các chú thích cần thiết cho Reflection và Javascript Bridge
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# 2. Bảo vệ tuyệt đối JavascriptInterface cho Web Sniffer Engine
# R8 không được phép đổi tên hoặc xóa các hàm giao tiếp với WebView JavaScript
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.nextaitechnology.antidetect.feature.browser.HomeFragment$AndroidSnifferBridge {
    public <methods>;
}

# 3. AndroidX Media3 (ExoPlayer Modern Suite)
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# 4. OkHttp3 & Okio Networking Stack
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# 5. Kotlin Coroutines & Flow
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# 6. Data Models (Phục vụ lưu trữ, StateFlow & UI Binding)
-keep class com.nextaitechnology.antidetect.core.model.** { *; }
-keepclassmembers class com.nextaitechnology.antidetect.core.model.** { *; }

# 7. Core Cryptographic Engine (AES-256-GCM Vault)
-keep class com.nextaitechnology.antidetect.core.security.** { *; }
-keepclassmembers class com.nextaitechnology.antidetect.core.security.** { *; }

# 8. Google Mobile Ads (AdMob)
-keep public class com.google.android.gms.ads.** {
    public *;
}
-keep public class com.google.ads.** {
    public *;
}

# 9. Firebase Services (Analytics, Messaging)
-dontwarn com.google.firebase.**
-keep class com.google.firebase.** { *; }

# 10. AndroidX Security Crypto (EncryptedSharedPreferences)
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**
