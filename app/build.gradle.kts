import org.gradle.kotlin.dsl.implementation

plugins {
    id("kotlin-kapt")
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)

}

android {
    namespace = "com.appgurjant.stickynotes"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.appgurjant.stickynotes"
        minSdk = 24
        targetSdk = 36
        versionCode = 21
        versionName = "4.0.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }
    kapt { generateStubs = true }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    flavorDimensions("version")
    productFlavors {
        create("dev") {
            dimension = "version"
            applicationId = "com.appgurjant.stickynotes.dev"
            versionNameSuffix = "-free"

        }
        create("prod") {
            dimension = "version"
        }
    }


}

dependencies {
    implementation(project(":data"))
    implementation(project(":domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    // Explicitly add Material 3 with version
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation(libs.androidx.material.icons.extended) // Material Icons Extended
    implementation(libs.androidx.navigation.compose)

    // kotlin coroutines for flow
    implementation(libs.kotlinx.coroutines.core)


    // Room Database
    implementation(libs.androidx.rooms.runtime)
    implementation(libs.ui.graphics)
    implementation(libs.androidx.foundation)

    kapt(libs.androidx.rooms.compiler)
    implementation(libs.androidx.rooms.ktx) // Kotlin Extensions and Coroutines support

    // hilt Dependencies
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose) // when you are adding view model in composable function
   // Optional, for Kotlin extensions

    // in App Update
    implementation(libs.app.update)
    implementation(libs.app.update.ktx)

    // firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation (libs.firebase.analytics)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.perf)
// GSON
    implementation(libs.gson)
    // camera
    implementation(libs.barcode.mlkit)
    implementation(libs.barcode.play)

    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.lottie.compose)


    // security
    // Security Crypto for encrypted preferences and file storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // For encrypted SharedPreferences
    implementation("androidx.security:security-crypto-ktx:1.1.0-alpha06")

    // For encrypting large files
    implementation("net.zetetic:android-database-sqlcipher:4.5.4" )
    implementation("androidx.sqlite:sqlite-ktx:2.3.1")

    implementation(libs.androidx.biometric)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.30.1")


}

