plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.exo.styleswap"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.exo.outfitly"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "1.0"

        // Try-On API config (overridable). Kept here so it is not hard-coded in source.
        buildConfigField("String", "TRYON_BASE_URL", "\"https://exostudio24--exo-image-ai-fastapi-app.modal.run\"")
        buildConfigField("String", "TRYON_API_KEY", "\"a8F3xK9mP2qW7rT4vY1nC6dL0sH5jBzE\"")
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

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // First-open flow: splash, language/survey lists, onboarding pager
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // On-device person detection (pose + labeling + face) to validate the photo has a person
    implementation("com.google.mlkit:pose-detection:18.0.0-beta5")
    implementation("com.google.mlkit:image-labeling:17.0.9")
    implementation("com.google.mlkit:face-detection:16.1.7")

    // CameraX
    val cameraxVersion = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
}
