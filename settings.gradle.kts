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

// M1 modules
include(":core:files")
include(":capture:pixels")

include(":report")

// M1 Android modules
include(":annotation")
include(":app")

// M2 Android modules
include(":capture:accessibility")

// M2 modules
include(":matching")

// M3 modules
include(":core:privacy")
include(":core:database")

// M4 modules
include(":sdk:compose-core")
include(":sdk:compose")
include(":sample:target-compose")
include(":sample:sdk-integration")

// M5 modules
include(":export:share")
include(":export:agent")
include(":export:github")
include(":export:jira")

// Future modules (uncomment when needed):
// include(":core:bitmap")
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
