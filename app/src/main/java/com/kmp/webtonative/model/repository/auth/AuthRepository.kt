package com.kmp.webtonative.model.repository.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth,
    private val credentialManager: CredentialManager
) {

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    suspend fun signInWithGoogle(context: Context, webClientId: String): Result<FirebaseUser> {
        return try {
            // Step 1 — Build the Google ID option
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // show all accounts, not just previously used
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false) // don't silently sign in without user interaction
                .build()

            // Step 2 — Build the credential request
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // Step 3 — Launch the credential picker (suspends until user picks)
            val credentialResponse = credentialManager.getCredential(
                request = request,
                context = context
            )

            // Step 4 — Extract the Google ID token from the response
            val credential = credentialResponse.credential
            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                return Result.failure(Exception("Unexpected credential type: ${credential.type}"))
            }

            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken

            // Step 5 — Exchange the ID token with Firebase
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(firebaseCredential).await()
            val user = result.user ?: return Result.failure(Exception("Firebase user was null after sign-in"))

            Result.success(user)

        } catch (e: GetCredentialException) {
            // User canceled, no accounts available, or play services issue
            Result.failure(e)
        } catch (e: GoogleIdTokenParsingException) {
            // Malformed token from Google
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }
}