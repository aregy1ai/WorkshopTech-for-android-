// ─── WorkshopTech — app/build.gradle.kts ───────────────────────────────────
// Target: Android 15 (API 35) | Kotlin 2.0.0 | Compose BOM 2024.09.00

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")   // Kotlin 2.x Compose compiler
    id("com.google.devtools.ksp")
}

android {
    namespace   = "com.workshoptech"
    compileSdk  = 35                            // Android 15

    defaultConfig {
        applicationId   = "com.workshoptech"
        minSdk          = 26                    // Android 8.0 — 96%+ device coverage
        targetSdk       = 35                    // Android 15
        versionCode     = 3
        versionName     = "1.3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Room schema export path for migration auditing
        ksp {
            arg("room.schemaLocation",   "$projectDir/schemas")
            arg("room.incremental",      "true")
            arg("room.generateKotlin",   "true")
        }
    }

    signingConfigs {
        // Release signing — secrets injected by CI or local keystore.properties
        create("release") {
            val props = java.util.Properties().also { p ->
                val f = rootProject.file("keystore.properties")
                if (f.exists()) p.load(f.inputStream())
            }
            storeFile     = props.getProperty("storeFile")?.let { file(it) }
            storePassword = props.getProperty("storePassword") ?: System.getenv("KEY_STORE_PASSWORD")
            keyAlias      = props.getProperty("keyAlias")      ?: System.getenv("KEY_ALIAS")
            keyPassword   = props.getProperty("keyPassword")   ?: System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            isDebuggable          = true
            isMinifyEnabled       = false
            applicationIdSuffix   = ".debug"
            versionNameSuffix     = "-debug"
            resValue("string", "app_mode", "DEBUG")
        }
        release {
            isDebuggable          = false
            isMinifyEnabled       = true
            isShrinkResources     = true
            signingConfig         = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            resValue("string", "app_mode", "RELEASE")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        // Global opt-ins — avoid per-file @OptIn noise
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
        )
    }

    buildFeatures {
        compose     = true
        buildConfig = true
    }

    // With org.jetbrains.kotlin.plugin.compose (Kotlin 2.x),
    // kotlinCompilerExtensionVersion is managed automatically — do NOT set it.

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues     = true
        }
    }
}

dependencies {
    // ── Compose BOM (manages all compose versions) ────────────────────────────
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // ── AndroidX Core ─────────────────────────────────────────────────────────
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")      // Splash Screen API
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-process:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.startup:startup-runtime:1.1.1")

    // ── Compose UI ────────────────────────────────────────────────────────────
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.runtime:runtime-livedata")

    // ── Navigation ────────────────────────────────────────────────────────────
    implementation("androidx.navigation:navigation-compose:2.8.3")

    // ── ViewModel + Lifecycle ─────────────────────────────────────────────────
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    // ── Room Database ─────────────────────────────────────────────────────────
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.room:room-paging:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // ── Paging 3 (for large case lists) ──────────────────────────────────────
    implementation("androidx.paging:paging-runtime-ktx:3.3.2")
    implementation("androidx.paging:paging-compose:3.3.2")

    // ── Camera ────────────────────────────────────────────────────────────────
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("androidx.camera:camera-video:1.3.4")

    // ── ML Kit ────────────────────────────────────────────────────────────────
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.mlkit:text-recognition-arabic:16.0.0")
    implementation("com.google.mlkit:object-detection:17.0.2")

    // ── TensorFlow Lite ───────────────────────────────────────────────────────
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")

    // ── Permissions ───────────────────────────────────────────────────────────
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // ── Location ──────────────────────────────────────────────────────────────
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // ── Image Loading ─────────────────────────────────────────────────────────
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-video:2.7.0")

    // ── Serialization / JSON ──────────────────────────────────────────────────
    implementation("com.google.code.gson:gson:2.11.0")

    // ── Background Work ───────────────────────────────────────────────────────
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // ── Security ─────────────────────────────────────────────────────────────
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.biometric:biometric:1.1.0")

    // ── Media / EXIF ─────────────────────────────────────────────────────────
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // ── Coroutines ────────────────────────────────────────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // ── DataStore (replacing any remaining SharedPreferences usage) ───────────
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ── Lottie animations ─────────────────────────────────────────────────────
    implementation("com.airbnb.android:lottie-compose:6.5.2")

    // ── Testing ───────────────────────────────────────────────────────────────
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("app.cash.turbine:turbine:1.1.0")         // Flow testing

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.work:work-testing:2.9.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
