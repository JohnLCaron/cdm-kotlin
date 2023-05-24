@file:OptIn(ExperimentalCoroutinesApi::class)

package com.sunya.testdata

import io.kotest.property.PropTestConfig
import io.kotest.property.ShrinkingMode
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Kotest requires its properties to be executed as a suspending function. To make this all work,
 * we're using [kotlinx.coroutines.test.runTest] to do it. Note that this internal `runTest`
 * function requires that it be called *at most once per test method*. It's fine to put multiple
 * asserts or `forAll` calls or whatever else inside the `runTest` lambda body.
 */
fun runTest(f: suspend () -> Unit) {
    // another benefit of having this wrapper code: we don't have to have the OptIn thing
    // at the top of every unit test file
    kotlinx.coroutines.test.runTest { f() }
}

/**
 * Property-based testing can run slowly. This will speed things up by turning off shrinking and
 * using fewer iterations. Typical usage:
 * ```
 * forAll(propTestFastConfig, Arb.x(), Arb.y()) { x, y -> ... }
 * ```
 */
val propTestFastConfig =
    PropTestConfig(maxFailure = 1, shrinkingMode = ShrinkingMode.Off, iterations = 10)

/**
 * If we know we can afford more effort to run a property test, this will spend extra time
 * trying more inputs and will put more effort into shrinking any counterexamples. Typical usage:
 * ```
 * forAll(propTestSlowConfig, Arb.x(), Arb.y()) { x, y -> ... }
 * ```
 */
val propTestSlowConfig =
    PropTestConfig(iterations = 1000)
