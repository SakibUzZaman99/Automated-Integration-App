plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.localllmapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.localllmapp"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
        }
    }
}

dependencies {
    // Existing dependencies
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Kotlin Parcelize
    implementation("org.jetbrains.kotlin:kotlin-parcelize-runtime:1.9.10")

    // Material Design
    implementation(libs.material)

<<<<<<< Updated upstream
    // Firebase
=======
    // Firebase - using BOM for version management
>>>>>>> Stashed changes
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.auth.ktx)
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // AndroidX Core
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx.v1160)

    // Google Play Services
    implementation(libs.play.services.auth)

<<<<<<< Updated upstream
    // Compose Icons
=======
    // Compose Icons Extended
>>>>>>> Stashed changes
    implementation("androidx.compose.material:material-icons-extended:1.3.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

<<<<<<< Updated upstream
<<<<<<< Updated upstream
    // MediaPipe GenAI dependency (for when you're ready to implement)
    implementation("com.google.mediapipe:tasks-genai:0.10.14")
}
=======
    // MediaPipe GenAI
    implementation("com.google.mediapipe:tasks-genai:0.10.25")
    implementation("com.google.mediapipe:tasks-vision-image-generator:0.10.21")

    // Gmail API Dependencies (Stable for Android)
    implementation("com.google.api-client:google-api-client-android:1.35.0") {
=======
    // MediaPipe GenAI for LLM
    implementation("com.google.mediapipe:tasks-genai:0.10.25")
    implementation("com.google.mediapipe:tasks-vision-image-generator:0.10.21")

    // Gmail API Dependencies
    implementation("com.google.api-client:google-api-client-android:2.2.0") {
>>>>>>> Stashed changes
        exclude(group = "org.apache.httpcomponents")
        exclude(group = "com.google.guava")
    }
    implementation("com.google.apis:google-api-services-gmail:v1-rev110-1.25.0") {
        exclude(group = "org.apache.httpcomponents")
        exclude(group = "com.google.guava")
    }
    implementation("com.google.http-client:google-http-client-gson:1.43.3")
<<<<<<< Updated upstream
=======

    // Required for Gmail API
>>>>>>> Stashed changes
    implementation("com.google.guava:guava:31.1-android")

    // OkHttp for network requests (Telegram API)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // JSON handling
    implementation("org.json:json:20230227")

    // Lifecycle components for Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Activity Result API
<<<<<<< Updated upstream
    implementation("androidx.activity:activity-ktx:1.9.0")
}
>>>>>>> Stashed changes
=======
    implementation("androidx.activity:activity-ktx:1.8.2")

    // WorkManager for scheduled tasks (optional, for future scheduling feature)
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}
>>>>>>> Stashed changes
