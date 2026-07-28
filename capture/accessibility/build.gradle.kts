plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.androidvisualqa.accessibility"
    compileSdk = 36

    defaultConfig {
        minSdk = 30
        // consume from the host app
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
    implementation(project(":capture:api"))
    implementation(project(":core:geometry"))
    implementation(project(":core:model"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)

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
