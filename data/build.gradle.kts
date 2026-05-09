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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.datastore.preferences)
    // Room Database
    implementation(libs.androidx.rooms.runtime)
    kapt(libs.androidx.rooms.compiler)
    implementation(libs.androidx.rooms.ktx) // Kotlin Extensions and Coroutines support

    // Firebase Firestore (cloud sync)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)

    // Google Identity (Credential Manager) for the standalone Google Sign-In flow.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.play.services.auth)
}
