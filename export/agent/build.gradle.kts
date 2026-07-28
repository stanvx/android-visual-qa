plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:geometry"))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
