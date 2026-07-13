##-- Room Database --##
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.PrimaryKey *;
    @androidx.room.ColumnInfo *;
    @androidx.room.Embedded *;
    @androidx.room.Relation *;
}
-keep class com.workshoptech.data.entity.** { *; }
-keep class com.workshoptech.data.dao.** { *; }

##-- Kotlin coroutines --##
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

##-- Gson --##
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

##-- ML Kit --##
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

##-- TensorFlow Lite --##
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

##-- WorkManager --##
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

##-- Domain models --##
-keep class com.workshoptech.domain.model.** { *; }

##-- Crash reporting stack traces --##
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
