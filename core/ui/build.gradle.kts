plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.lumiroom.core.ui"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:common"))
    implementation(platform(libs.androidx.compose.bom))
    api(libs.bundles.compose.core)
    api(libs.androidx.compose.ui.tooling.preview)
    api(libs.lottie.compose)
    api(libs.coil.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
