plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.androidvisualqa.annotation"
    compileSdk = 36

    defaultConfig {
        minSdk = 30
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

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // Project dependencies (existing pure-Kotlin M0/M1 modules)
    implementation(project(":core:model"))
    implementation(project(":core:geometry"))
    implementation(project(":capture:api"))
    implementation(project(":capture:pixels"))
    implementation(project(":report"))
    implementation(project(":core:files"))

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime)

    // AndroidX Ink
    implementation(libs.androidx.ink.authoring)
    implementation(libs.androidx.ink.rendering)

    // Lifecycle (for ViewModel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Tests
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
