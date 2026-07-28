plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.androidvisualqa.benchmark"
    compileSdk = 36

    defaultConfig {
        minSdk = 30
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = false
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // Project modules under test
    implementation(project(":app"))
    implementation(project(":matching"))
    implementation(project(":core:database"))
    implementation(project(":core:files"))
    implementation(project(":core:model"))
    implementation(project(":core:geometry"))
    implementation(project(":report"))

    // Macrobenchmark (includes BaselineProfileRule)
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.benchmark.junit4)

    // UI Automator
    implementation(libs.androidx.test.uiautomator)

    // Test infra
    implementation(libs.androidx.test.ext.junit.ktx)
    implementation(libs.androidx.test.ext.junit)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.test)

    // Datetime (used by SavingBenchmark via ReportHistoryIndex)
    implementation(libs.kotlinx.datetime)

    // Unit-test-only (JVM)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
