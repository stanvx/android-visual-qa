package com.androidvisualqa.sample.targetcompose

import com.androidvisualqa.sample.targetcompose.screens.CanvasScreenActivity
import com.androidvisualqa.sample.targetcompose.screens.DialogScreenActivity
import com.androidvisualqa.sample.targetcompose.screens.ImeScreenActivity
import com.androidvisualqa.sample.targetcompose.screens.LazyColumnScreenActivity
import com.androidvisualqa.sample.targetcompose.screens.MergedSemanticsScreenActivity
import com.androidvisualqa.sample.targetcompose.screens.NestedClickTargetsScreenActivity
import com.androidvisualqa.sample.targetcompose.screens.PasswordFieldScreenActivity
import com.androidvisualqa.sample.targetcompose.screens.RotationScreenActivity
import com.androidvisualqa.sample.targetcompose.screens.SecureScreenScreenActivity
import com.androidvisualqa.sample.targetcompose.screens.SplitScreenScreenActivity
import com.androidvisualqa.sample.targetcompose.screens.TabletLayoutScreenActivity
import com.androidvisualqa.sample.targetcompose.screens.WebViewScreenActivity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * JVM-level verification that every fixture-screen class compiles and
 * its type references resolve.
 *
 * These tests do not use Robolectric (which does not support
 * targetSdkVersion=36 yet). Compose UI render tests are deferred
 * to the instrumented test runner on API 36+ devices.
 */
class FixtureScreenCompileTest {

    @Test
    fun `MergedSemanticsScreenActivity class resolves`() {
        assertEquals(
            "com.androidvisualqa.sample.targetcompose.screens.MergedSemanticsScreenActivity",
            MergedSemanticsScreenActivity::class.qualifiedName,
        )
    }

    @Test
    fun `NestedClickTargetsScreenActivity class resolves`() {
        assertEquals(
            "com.androidvisualqa.sample.targetcompose.screens.NestedClickTargetsScreenActivity",
            NestedClickTargetsScreenActivity::class.qualifiedName,
        )
    }

    @Test
    fun `LazyColumnScreenActivity class resolves`() {
        assertEquals(
            "com.androidvisualqa.sample.targetcompose.screens.LazyColumnScreenActivity",
            LazyColumnScreenActivity::class.qualifiedName,
        )
    }

    @Test
    fun `WebViewScreenActivity class resolves`() {
        assertEquals(
            "com.androidvisualqa.sample.targetcompose.screens.WebViewScreenActivity",
            WebViewScreenActivity::class.qualifiedName,
        )
    }

    @Test
    fun `CanvasScreenActivity class resolves`() {
        assertEquals(
            "com.androidvisualqa.sample.targetcompose.screens.CanvasScreenActivity",
            CanvasScreenActivity::class.qualifiedName,
        )
    }

    @Test
    fun `PasswordFieldScreenActivity class resolves`() {
        assertEquals(
            "com.androidvisualqa.sample.targetcompose.screens.PasswordFieldScreenActivity",
            PasswordFieldScreenActivity::class.qualifiedName,
        )
    }

    @Test
    fun `DialogScreenActivity class resolves`() {
        assertEquals(
            "com.androidvisualqa.sample.targetcompose.screens.DialogScreenActivity",
            DialogScreenActivity::class.qualifiedName,
        )
    }

    @Test
    fun `ImeScreenActivity class resolves`() {
        assertEquals(
            "com.androidvisualqa.sample.targetcompose.screens.ImeScreenActivity",
            ImeScreenActivity::class.qualifiedName,
        )
    }

    @Test
    fun `SecureScreenScreenActivity class resolves`() {
        assertEquals(
            "com.androidvisualqa.sample.targetcompose.screens.SecureScreenScreenActivity",
            SecureScreenScreenActivity::class.qualifiedName,
        )
    }

    @Test
    fun `RotationScreenActivity class resolves`() {
        assertEquals(
            "com.androidvisualqa.sample.targetcompose.screens.RotationScreenActivity",
            RotationScreenActivity::class.qualifiedName,
        )
    }

    @Test
    fun `SplitScreenScreenActivity class resolves`() {
        assertEquals(
            "com.androidvisualqa.sample.targetcompose.screens.SplitScreenScreenActivity",
            SplitScreenScreenActivity::class.qualifiedName,
        )
    }

    @Test
    fun `TabletLayoutScreenActivity class resolves`() {
        assertEquals(
            "com.androidvisualqa.sample.targetcompose.screens.TabletLayoutScreenActivity",
            TabletLayoutScreenActivity::class.qualifiedName,
        )
    }
}
