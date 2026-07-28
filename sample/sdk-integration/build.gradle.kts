plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.androidvisualqa.sample.sdkintegration"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.androidvisualqa.sample.sdkintegration"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
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
    implementation(project(":sdk:compose"))
    implementation(project(":core:model"))
    implementation(project(":core:privacy"))          // needed for the PrivacyClassification typealias
    implementation(project(":core:geometry"))          // needed for Bounds types in KDoc

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.activity.compose)
}
