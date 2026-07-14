##────────────────────────────────────────────────────────────────────────────
##  WorkshopTech — Production R8/ProGuard rules (hardened)
##  Last reviewed: 2026-07-14
##────────────────────────────────────────────────────────────────────────────

##── General optimisation & obfuscation ──────────────────────────────────────
-optimizationpasses 5
-allowaccessmodification
-repackageclasses 'o'                  # collapse all packages into 'o' namespace
-overloadaggressively                  # rename methods aggressively

##── Debugging: keep source file + line numbers in stack traces ───────────────
##   (R8 maps are uploaded; remove these two lines for max APK size reduction)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute S

##── Kotlin ───────────────────────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.Lazy { *; }
-dontwarn kotlin.**
-dontnote kotlin.**

##── Kotlin Coroutines ────────────────────────────────────────────────────────
-keepclassmembernames class kotlinx.** { volatile <fields>; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

##── Room Database ────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keepclassmembers class * {
    @androidx.room.PrimaryKey *;
    @androidx.room.ColumnInfo *;
    @androidx.room.Embedded *;
    @androidx.room.Relation *;
    @androidx.room.Query *;
    @androidx.room.Insert *;
    @androidx.room.Update *;
    @androidx.room.Delete *;
    @androidx.room.Upsert *;
    @androidx.room.Transaction *;
}
-keep class com.workshoptech.data.entity.** { *; }
-keep class com.workshoptech.data.dao.**    { *; }
-keep class com.workshoptech.data.migration.** { *; }
-dontwarn androidx.room.**

##── Domain models & Result sealed class ─────────────────────────────────────
-keep class com.workshoptech.domain.model.** { *; }

##── Security layer (keep class names for reflection in EncryptedSharedPrefs) ─
-keep class com.workshoptech.security.** { *; }
-keep class androidx.security.crypto.**  { *; }
-dontwarn androidx.security.crypto.**

##── WorkManager ──────────────────────────────────────────────────────────────
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keepclassmembers class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-dontwarn androidx.work.**

##── Jetpack Compose ──────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
    @androidx.compose.runtime.Stable *;
    @androidx.compose.runtime.Immutable *;
}
-dontwarn androidx.compose.**

##── Lifecycle / ViewModel ────────────────────────────────────────────────────
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.ViewModelProvider$Factory { *; }
-dontwarn androidx.lifecycle.**

##── ML Kit ───────────────────────────────────────────────────────────────────
-keep class com.google.mlkit.**     { *; }
-keep class com.google.android.gms.vision.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**

##── TensorFlow Lite ─────────────────────────────────────────────────────────
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.**      { *; }
-dontwarn org.tensorflow.**

##── CameraX ─────────────────────────────────────────────────────────────────
-keep class androidx.camera.**  { *; }
-dontwarn androidx.camera.**

##── Gson ─────────────────────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.**                        { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

##── ExifInterface ────────────────────────────────────────────────────────────
-keep class androidx.exifinterface.** { *; }
-dontwarn androidx.exifinterface.**

##── Okio (used internally by some libs) ─────────────────────────────────────
-dontwarn okio.**

##── Remove all logging in release ───────────────────────────────────────────
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}
## Keep w/e/wtf for production crash visibility
## -assumenosideeffects class android.util.Log { public static *** w(...); }

##── Prevent reflection on sensitive classes ──────────────────────────────────
-keepclassmembernames class * {
    java.lang.Class class$(java.lang.String);
    java.lang.Class class$(java.lang.String, boolean);
}
