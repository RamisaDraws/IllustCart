plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.illustcart"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.illustcart"
        minSdk = 27
        targetSdk = 36
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

    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    //Firebase BOM
    implementation(platform(libs.firebase.bom))

    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.firestore)

    //Firebase Cloud Messaging
    implementation("com.google.firebase:firebase-messaging-ktx:24.1.2")


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("com.soundcloud.android:android-crop:1.0.1@aar")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("com.facebook.shimmer:shimmer:0.5.0")

    // For image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.squareup.picasso:picasso:2.8")

    //Google Auth Library for FCM V1 API (OAuth2)
    implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")

    //google play services for location
    implementation("com.google.android.gms:play-services-location:21.0.1")


    //OkHttp for HTTP requests
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    //Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    //Coroutines (for async operations)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}