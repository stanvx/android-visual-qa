plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    // Exposed to consumers — these are testing utilities
    api(libs.kotlinx.coroutines.test)
    api(libs.junit.jupiter)
    api("org.junit.platform:junit-platform-launcher") {
        because("JUnit Platform launcher for JUnit 5 test execution")
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
