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
        versionCode = 29
        versionName = "4.0.8"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            // For ndk-build, instead use the ndkBuild block.
            cmake {
                // Passes optional arguments to CMake.
                arguments += listOf("-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON")
            }
        }
    }
    signingConfigs {
        create("release") {
            storeFile = file("/home/gurjantsingh/Desktop/MobileApps/NotezyApp/NotezyApp/app/stickyanimationnote.jks")
            storePassword = "123456gG"
            keyAlias = "stickyanimationnote"
            keyPassword = "123456gG"
        }
    }
    packagingOptions {
        jniLibs {
            useLegacyPackaging = true
        }
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
    flavorDimensions += listOf("version")

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
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.material.icons.extended) // Material Icons Extended
    implementation(libs.androidx.navigation.compose)


    // Room Database
    implementation(libs.androidx.rooms.runtime)
    implementation(libs.ui.graphics)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.compose.runtime)

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
    implementation(libs.androidx.security.crypto)

    // For encrypted SharedPreferences
    implementation(libs.androidx.security.crypto.ktx)

    implementation(libs.androidx.sqlite.ktx)

    implementation(libs.androidx.biometric)
    implementation(libs.accompanist.systemuicontroller)

    implementation(libs.play.services.ads)

    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)






}

