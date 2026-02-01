# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   https://developer.android.com/guide/developing/tools/proguard

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep USB Serial library
-keep class com.hoho.android.usbserial.** { *; }
-keepclassmembers class com.hoho.android.usbserial.** { *; }

# Keep our classes
-keep class com.smartatendimentos.traccar.** { *; }
-keepclassmembers class com.smartatendimentos.traccar.** { *; }

# Keep Kotlin coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }

# Keep Google Play Services
-keep class com.google.android.gms.** { *; }
-keepclassmembers class com.google.android.gms.** { *; }
