package com.androidvisualqa.sdk.compose

import android.content.pm.PackageInfo
import com.androidvisualqa.sdk.composecore.InMemorySdkRegistry
import com.androidvisualqa.sdk.composecore.SdkComponentDescriptor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AndroidSdkEvidenceProducerTest {

    @Test
    fun `snapshot returns empty components for empty registry`() = runTest {
        val registry = InMemorySdkRegistry()
        val context = RuntimeEnvironment.getApplication()
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

        val producer = AndroidSdkEvidenceProducer(
            registry = registry,
            packageInfo = packageInfo,
            isDebuggable = true,
            buildType = "debug",
            gitSha = "abc123def",
        )

        val evidence = producer.snapshot()
        assertTrue("Expected no components for empty registry", evidence.components.isEmpty())
    }

    @Test
    fun `snapshot returns registered components`() = runTest {
        val registry = InMemorySdkRegistry()
        val context = RuntimeEnvironment.getApplication()
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

        registry.register(
            SdkComponentDescriptor(
                stableId = "test.component.one",
                semantics = mapOf("variant" to "primary"),
            )
        )
        registry.register(
            SdkComponentDescriptor(
                stableId = "test.component.two",
                semantics = mapOf("variant" to "secondary"),
            )
        )

        val producer = AndroidSdkEvidenceProducer(
            registry = registry,
            packageInfo = packageInfo,
            isDebuggable = true,
            buildType = "debug",
        )

        val evidence = producer.snapshot()
        assertEquals("Expected 2 components", 2, evidence.components.size)

        val ids = evidence.components.map { it.componentId.value }.sorted()
        assertEquals(listOf("test.component.one", "test.component.two"), ids)
    }
}
