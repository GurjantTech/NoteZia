plugins {
    id("java-library")  // ✅ Correct: Pure Kotlin Module
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)   // ✅ Add this line
}



