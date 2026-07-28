plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.androidvisualqa.sdk.compose"
    compileSdk = 36

    defaultConfig {
        minSdk = 30

        buildConfigField("String", "SDK_BUILD_TYPE", "\"release\"")
        buildConfigField("String", "SDK_GIT_SHA", "\"unknown\"")
        buildConfigField("boolean", "SDK_DEBUGGABLE", "false")
    }

    buildFeatures {
        compose = true
        buildConfig = true
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
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    api(project(":sdk:compose-core"))       // public API surface includes these types
    implementation(project(":core:model"))
    implementation(project(":core:geometry"))
    implementation(project(":core:privacy"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.runtime)

    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.11.4")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("robolectric.screenshot", "false")
    systemProperty("robolectric.logging", "stdout")
}
