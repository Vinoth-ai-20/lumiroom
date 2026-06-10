package com.lumiroom.core.ui.utils

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Extension function for ViewModel to safely launch coroutines with a default exception handler.
 * This prevents the entire application from crashing due to unhandled exceptions in ViewModels.
 */
fun ViewModel.safeLaunch(
    onError: ((Throwable) -> Unit)? = null,
    block: suspend CoroutineScope.() -> Unit
) {
    val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Log.e("SafeLaunch", "Unhandled Coroutine Exception in ${this::class.java.simpleName}: ${exception.message}", exception)
        onError?.invoke(exception)
    }
    viewModelScope.launch(exceptionHandler) {
        block()
    }
}
