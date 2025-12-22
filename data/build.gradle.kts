plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")

}
android {
    namespace = "com.app.notezy"
    compileSdk = 36
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
    kapt { generateStubs = true }
}


dependencies {
    implementation(project(":domain"))

    // Room Database
    implementation(libs.androidx.rooms.runtime)
    kapt(libs.androidx.rooms.compiler)
    implementation(libs.androidx.rooms.ktx) // Kotlin Extensions and Coroutines support

}
