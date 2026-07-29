plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.androidvisualqa.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.androidvisualqa.app"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // TODO(m6-final): wire real release keystore via CI secrets.
            // signingConfig = signingConfigs.getByName("release")
        }
    }

    // The baseline-prof.txt at src/main/ is auto-discovered by AGP.
    // Lane U's :benchmark module uses the androidx.baselineprofile plugin
    // to generate an updated profile that overlays this placeholder in CI.
    // TODO(m6-final): wire :benchmark generated profile via baselineProfile { from(...) }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // Project modules
    implementation(project(":annotation"))
    implementation(project(":core:model"))
    implementation(project(":core:geometry"))
    implementation(project(":capture:api"))
    implementation(project(":capture:pixels"))
    implementation(project(":capture:accessibility"))
    implementation(project(":matching"))
    implementation(project(":report"))
    implementation(project(":core:files"))
    implementation(project(":core:privacy"))
    implementation(project(":core:database"))
    implementation(project(":export:share"))
    implementation(project(":export:agent"))
    implementation(project(":export:github"))
    implementation(project(":export:jira"))

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime)

    // AndroidX
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation(libs.androidx.work.runtime.ktx)

    // Tests
    testImplementation(project(":core:testing"))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.room.testing)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
