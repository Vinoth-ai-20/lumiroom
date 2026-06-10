package com.lumiroom.feature.auth.domain.repository

import android.content.Context
import com.lumiroom.core.common.result.LumiroomResult
import com.lumiroom.feature.auth.domain.model.LumiroomUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<LumiroomUser?>
    
    suspend fun signInWithEmail(email: String, password: String): LumiroomResult<LumiroomUser>
    suspend fun signUpWithEmail(email: String, password: String, displayName: String): LumiroomResult<LumiroomUser>
    suspend fun signInAnonymously(): LumiroomResult<LumiroomUser>
    suspend fun signInWithGoogle(context: Context): LumiroomResult<LumiroomUser>
    suspend fun signOut()
    suspend fun deleteAccount(): LumiroomResult<Unit>
}
