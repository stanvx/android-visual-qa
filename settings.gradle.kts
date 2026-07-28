pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "android-visual-qa"

// M0 modules
include(":core:model")
include(":core:geometry")
include(":core:testing")
include(":capture:api")

// Future modules (uncomment when needed):
// include(":core:bitmap")
// include(":core:report")
// include(":capture:device")
// include(":capture:emulator")
// include(":capture:writer")
// include(":compare:api")
// include(":compare:reference")
// include(":compare:diff")
// include(":compare:threshold")
// include(":ui:common")
// include(":ui:capture")
// include(":ui:compare")
// include(":ui:report")
// include(":instrumentation:common")
// include(":instrumentation:runner")
// include(":instrumentation:junit5")
// include(":cli:app")
