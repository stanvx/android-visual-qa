# =============================================================================
# ProGuard / R8 rules for Android Visual QA — app module
# =============================================================================
# This file defines what R8 must NOT shrink, obfuscate, or optimise away.
# Rules are ordered by dependency layer: framework → serialisation → SDK
# contracts → model → internal modules.
#
# WHY EACH RULE EXISTS:
# - Compose: Framework classes are loaded reflectively by the Compose runtime.
# - Serialisation: Kotlinx.serialisation uses reflection and Companion patterns.
# - SDK API: Public SDK surface must remain callable by host apps.
# - Model: Serialised field names must survive R8 or JSON deserialisation breaks.
# =============================================================================

# ---------------------------------------------------------------------------
# Compose (Reflection-based runtime composition)
# ---------------------------------------------------------------------------
# Compose relies on reflection to find composable functions and manage slots.
# Without these keep rules, R8 strips the metadata Compose needs at runtime.
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# ---------------------------------------------------------------------------
# Kotlinx Serialization
# ---------------------------------------------------------------------------
# Serialization uses @Serializable annotations and generated Companion
# serializers.  R8 sees the annotation-only usage and removes the backing
# fields.  Keep annotation attributes, inner classes, and the serializer
# companion pattern.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class kotlinx.serialization.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---------------------------------------------------------------------------
# Public SDK contracts — the API surface host apps call
# ---------------------------------------------------------------------------
# The :sdk:compose and :sdk:compose-core modules expose a public API.
# Host apps need every `public` member to survive R8 so their compiled
# code links correctly after consuming our AAR.
-keep class com.androidvisualqa.sdk.compose.** { public *; }
-keep class com.androidvisualqa.sdk.composecore.** { public *; }

# ---------------------------------------------------------------------------
# Model schema classes
# ---------------------------------------------------------------------------
# Every @Serializable data class in the model module must keep its field
# names and constructors intact — JSON serialisation binds by name, not
# position.  Field-less objects also need keeping because serialization
# accesses them via reflection.
-keep @kotlinx.serialization.Serializable class com.androidvisualqa.model.** { *; }
-keepclassmembers class com.androidvisualqa.model.** { *; }

# ---------------------------------------------------------------------------
# Report module — JSONL persistence
# ---------------------------------------------------------------------------
# The report :report module reads/writes JSONL files.  If report classes
# are obfuscated, stored reports from past runs cannot be deserialised.
-keep class com.androidvisualqa.report.** { public *; }

# ---------------------------------------------------------------------------
# Export modules — share sheets, remote agents, issue trackers
# ---------------------------------------------------------------------------
# The :export:* modules communicate with Android share intents, remote APIs,
# and Jira/GitHub integrations.  Their public API must survive R8 because
# intents resolve by class name and API payloads preserve field names.
-keep class com.androidvisualqa.export.** { public *; }

# ---------------------------------------------------------------------------
# Capture modules — input sources
# ---------------------------------------------------------------------------
# The capture pipeline (AccessibilityService, pixel buffer, API interface)
# is invoked by the framework and must survive for reliable screenshot capture.
-keep class com.androidvisualqa.capture.** { public *; }

# ---------------------------------------------------------------------------
# Hilt / Dagger (if introduced in a future lane)
# ---------------------------------------------------------------------------
# Hilt generates factories and components at compile time that R8 sees as
# unused.  The safe default is silence warnings now; the keep rule is added
# when Hilt lands.
-dontwarn dagger.**

# ---------------------------------------------------------------------------
# Coroutines debug agent
# ---------------------------------------------------------------------------
# The coroutines debug agent attaches itself via instrumentation and is never
# on the release classpath.  Suppress the warning to avoid noise.
-dontwarn kotlinx.coroutines.debug.**

# ---------------------------------------------------------------------------
# General Android platform
# ---------------------------------------------------------------------------
# Keep general Android framework references that R8 may strip during
# optimisation passes.
-keepattributes Exceptions, InnerClasses, Signature, EnclosingMethod
-dontwarn android.**

# ---------------------------------------------------------------------------
# Kotlin stdlib internals
# ---------------------------------------------------------------------------
# Kotlin stdlib intrinsics (e.g. Intrinsics, kotlin.jvm.internal) are used
# by generated bytecode.  They are rarely stripped but we keep them to be safe.
-dontwarn kotlin.**

# ---------------------------------------------------------------------------
# Logging — never strip log calls in release; they are under a runtime flag
# ---------------------------------------------------------------------------
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
