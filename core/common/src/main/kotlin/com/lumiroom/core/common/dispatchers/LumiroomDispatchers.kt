package com.lumiroom.core.common.dispatchers

import javax.inject.Qualifier

/**
 * Qualifier annotations for named Kotlin Coroutine dispatchers.
 *
 * Inject via:
 * ```kotlin
 * @Inject constructor(
 *     @Dispatcher(LumiroomDispatcher.IO) private val ioDispatcher: CoroutineDispatcher
 * )
 * ```
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val lumiroomDispatcher: LumiroomDispatcher)

enum class LumiroomDispatcher {
    /** [kotlinx.coroutines.Dispatchers.Default] — CPU-intensive work (JSON parsing, sorting). */
    Default,

    /** [kotlinx.coroutines.Dispatchers.IO] — I/O operations (DB, network, file access). */
    IO,

    /** [kotlinx.coroutines.Dispatchers.Main] — UI thread operations. */
    Main,
}
