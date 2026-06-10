package com.lumiroom.core.common.di

import com.lumiroom.core.common.dispatchers.Dispatcher
import com.lumiroom.core.common.dispatchers.LumiroomDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Hilt module that provides named [CoroutineDispatcher] bindings.
 *
 * Installed in [SingletonComponent] so the same dispatcher instances
 * are reused across the entire application lifetime.
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @Provides
    @Dispatcher(LumiroomDispatcher.IO)
    fun providesIODispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Dispatcher(LumiroomDispatcher.Default)
    fun providesDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Dispatcher(LumiroomDispatcher.Main)
    fun providesMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}
