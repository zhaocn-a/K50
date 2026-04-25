# DynamicOasis-K40 ProGuard Rules

# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep application class
-keep class com.dynamicoasis.app.DynamicOasisApp { *; }

# Keep service classes
-keep class com.dynamicoasis.app.service.** { *; }

# Keep receiver classes
-keep class com.dynamicoasis.app.receiver.** { *; }

# Keep manager classes
-keep class com.dynamicoasis.app.manager.** { *; }

# Keep utility classes
-keep class com.dynamicoasis.app.utils.** { *; }

# Keep UI classes
-keep class com.dynamicoasis.app.ui.** { *; }

# AndroidX
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# Material Components
-keep class com.google.android.material.** { *; }

# Keep Parcelable classes
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep R classes
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Keep annotation classes
-keep class * extends java.lang.annotation.Annotation { *; }

# Kotlin
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
