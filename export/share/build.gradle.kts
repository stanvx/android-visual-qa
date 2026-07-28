plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.androidvisualqa.export.share"
    compileSdk = 36

    defaultConfig {
        minSdk = 30
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
    // Project modules
    implementation(project(":core:model"))
    implementation(project(":core:files"))
    implementation(project(":report"))

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.documentfile)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Tests
    testImplementation(project(":core:testing"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.datetime)
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
