plugins {
    id("loaderapp.jvm")
    id("loaderapp.lint")
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.loaderapp.core.ui"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
}
