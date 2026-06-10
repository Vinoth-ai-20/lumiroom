package com.lumiroom.feature.auth.data.repository

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.FirebaseUser
import com.lumiroom.core.common.result.LumiroomResult
import com.lumiroom.feature.auth.domain.model.LumiroomUser
import com.lumiroom.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override val currentUser: Flow<LumiroomUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toLumiroomUser())
        }
        firebaseAuth.addAuthStateListener(authStateListener)
        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): LumiroomResult<LumiroomUser> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return LumiroomResult.Error(Exception("User not found"))
            LumiroomResult.Success(user.toLumiroomUser())
        } catch (e: Exception) {
            LumiroomResult.Error(e)
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String, displayName: String): LumiroomResult<LumiroomUser> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return LumiroomResult.Error(Exception("User not found"))
            
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
                
            user.updateProfile(profileUpdates).await()
            LumiroomResult.Success(user.toLumiroomUser().copy(displayName = displayName))
        } catch (e: Exception) {
            LumiroomResult.Error(e)
        }
    }

    override suspend fun signInAnonymously(): LumiroomResult<LumiroomUser> {
        return try {
            val result = firebaseAuth.signInAnonymously().await()
            val user = result.user ?: return LumiroomResult.Error(Exception("User not found"))
            LumiroomResult.Success(user.toLumiroomUser())
        } catch (e: Exception) {
            LumiroomResult.Error(e)
        }
    }

    override suspend fun signInWithGoogle(context: Context): LumiroomResult<LumiroomUser> {
        return try {
            val credentialManager = CredentialManager.create(context)
            
            // Server client ID from google-services.json oauth_client
            val serverClientId = "108844923426-1vm30ao4kvqph3vuvm28hqbg9bc2pnck.apps.googleusercontent.com"
            val rawNonce = UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.joinToString("") { "%02x".format(it) }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            handleSignIn(result)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Google Sign In failed", e)
            LumiroomResult.Error(e)
        }
    }

    private suspend fun handleSignIn(result: GetCredentialResponse): LumiroomResult<LumiroomUser> {
        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(authCredential).await()
                val user = authResult.user ?: return LumiroomResult.Error(Exception("User not found"))
                return LumiroomResult.Success(user.toLumiroomUser())
            } catch (e: Exception) {
                return LumiroomResult.Error(e)
            }
        }
        return LumiroomResult.Error(Exception("Invalid credential type"))
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    override suspend fun deleteAccount(): LumiroomResult<Unit> {
        return try {
            val user = firebaseAuth.currentUser ?: return LumiroomResult.Error(Exception("No user logged in"))
            user.delete().await()
            LumiroomResult.Success(Unit)
        } catch (e: Exception) {
            LumiroomResult.Error(e)
        }
    }

    private fun FirebaseUser.toLumiroomUser() = LumiroomUser(
        uid = this.uid,
        email = this.email,
        displayName = this.displayName,
        photoUrl = this.photoUrl?.toString(),
        isAnonymous = this.isAnonymous
    )
}
