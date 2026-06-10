package com.lumiroom.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Attaches Firebase ID token as Bearer authorization header on every API request.
 * Token is fetched lazily from Firebase Auth current user.
 *
 * TODO: Implement actual token injection in Milestone 1 when Firebase Auth is set up.
 */
@Singleton
class AuthInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            // .header("Authorization", "Bearer ${getFirebaseToken()}")
            .build()
        return chain.proceed(request)
    }
}
