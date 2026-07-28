package com.androidvisualqa.testing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * JUnit 5 extension that installs a [StandardTestDispatcher] and exposes
 * [runTest] semantics.
 *
 * Usage:
 * ```kotlin
 * @RegisterExtension
 * val coroutines = CoroutinesTestRule()
 *
 * @Test
 * fun example() = coroutines.runTest {
 *     // virtual time is controlled by the test dispatcher
 * }
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class)
public class CoroutinesTestRule : BeforeEachCallback, AfterEachCallback {

    public val scheduler: TestCoroutineScheduler = TestCoroutineScheduler()
    public val testDispatcher: TestDispatcher = StandardTestDispatcher(scheduler)
    private val testScope: TestScope = TestScope(testDispatcher)

    /**
     * Executes [block] inside [kotlinx.coroutines.test.runTest] using this
     * rule's dispatcher.
     */
    public fun runTest(block: suspend TestScope.() -> Unit): Unit =
        runTest(testDispatcher) { block() }

    override fun beforeEach(context: ExtensionContext?) {
        // Reset scheduler state before every test
        scheduler.advanceUntilIdle()
    }

    override fun afterEach(context: ExtensionContext?) {
        // Ensure no lingering coroutines leak between tests
        scheduler.advanceUntilIdle()
    }
}
