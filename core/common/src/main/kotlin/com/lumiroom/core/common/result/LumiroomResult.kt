package com.lumiroom.core.common.result

/**
 * A discriminated union representing the outcome of a repository or use-case operation.
 *
 * Usage:
 * ```kotlin
 * when (result) {
 *     is LumiroomResult.Success -> render(result.data)
 *     is LumiroomResult.Error   -> showError(result.exception)
 *     is LumiroomResult.Loading -> showSpinner()
 * }
 * ```
 */
sealed class LumiroomResult<out T> {

    /** Operation succeeded with [data]. */
    data class Success<T>(val data: T) : LumiroomResult<T>()

    /** Operation failed with an [exception] and an optional user-facing [message]. */
    data class Error(
        val exception: Throwable,
        val message: String? = exception.localizedMessage,
    ) : LumiroomResult<Nothing>()

    /** Operation is in progress. Carries optional [progress] in range [0f, 1f]. */
    data class Loading(val progress: Float? = null) : LumiroomResult<Nothing>()
}

// ── Extensions ────────────────────────────────────────────────────────────────

/** Returns [block] result when [LumiroomResult.Success], else passes through. */
inline fun <T, R> LumiroomResult<T>.mapSuccess(
    block: (T) -> R,
): LumiroomResult<R> = when (this) {
    is LumiroomResult.Success -> LumiroomResult.Success(block(data))
    is LumiroomResult.Error   -> this
    is LumiroomResult.Loading -> this
}

/** Executes [block] when result is [LumiroomResult.Success]. */
inline fun <T> LumiroomResult<T>.onSuccess(block: (T) -> Unit): LumiroomResult<T> {
    if (this is LumiroomResult.Success) block(data)
    return this
}

/** Executes [block] when result is [LumiroomResult.Error]. */
inline fun <T> LumiroomResult<T>.onError(block: (Throwable) -> Unit): LumiroomResult<T> {
    if (this is LumiroomResult.Error) block(exception)
    return this
}

/** Returns the data or null if not [LumiroomResult.Success]. */
fun <T> LumiroomResult<T>.getOrNull(): T? =
    (this as? LumiroomResult.Success)?.data
