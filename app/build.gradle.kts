plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.workshoptech"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.workshoptech"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        vectorDrawables { useSupportLibrary = true }
        ksp { arg("room.schemaLocation", "$projectDir/schemas") }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }

    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }

    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }

    aaptOptions { noCompress("tflite") }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)

    // AndroidX Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Room + KSP
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Paging 3
    implementation("androidx.room:room-paging:2.6.1")
    implementation("androidx.paging:paging-runtime-ktx:3.3.2")
    implementation("androidx.paging:paging-compose:3.3.2")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

    // CameraX
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // ML Kit
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // TFLite
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    // Coil
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Gson
    implementation("com.google.code.gson:gson:2.11.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Accompanist
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")

    // ExifInterface
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
