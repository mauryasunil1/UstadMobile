package com.ustadmobile.core.test.viewmodeltest

import app.cash.turbine.test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlin.time.Duration

/**
 * Shorthand for filter { condition }.test {
 *   awaitItem()
 * }
 */
suspend fun <T> Flow<T>.assertItemReceived(
    timeout: Duration? = null,
    name: String? = null,
    filterBlock: (T) -> Boolean
) {
    filter(filterBlock).test(timeout = timeout, name = name) {
        awaitItem()
        cancelAndIgnoreRemainingEvents()
    }
}
