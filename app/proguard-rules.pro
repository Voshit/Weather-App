# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes *Annotation*

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- FIREBASE & MODELS ---
# Keep data models used by Firestore serialization
-keep class com.weatherapp.app.model.** { *; }

# Keep logic helpers (FirestoreHelper)
-keep class com.weatherapp.app.logic.** { *; }

# Keep Database entities and DAOs (Room)
-keep class com.weatherapp.app.db.** { *; }

# Keep Firebase SDKs from being stripped aggressively
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep standard enum methods
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}