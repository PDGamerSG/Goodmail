package com.example.goodmail.data.repository

import android.content.Intent
import com.example.goodmail.data.local.datastore.AccountStore
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the Google sign-in session. The [GoogleAccountCredential] singleton it primes here is the
 * same instance injected wherever Gmail is called, so once an account is selected the whole app can
 * make authenticated Gmail requests with automatic token refresh.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val googleSignInClient: GoogleSignInClient,
    private val credential: GoogleAccountCredential,
    private val accountStore: AccountStore,
) {
    val accountEmail: Flow<String?> = accountStore.accountEmail

    /** Intent to launch the Google Sign-In + Gmail consent flow. */
    val signInIntent: Intent get() = googleSignInClient.signInIntent

    /** Re-prime the credential from a previously stored account. Returns the email, or null if none. */
    suspend fun restoreSession(): String? {
        val email = accountStore.accountEmail.first()
        if (email != null) credential.selectedAccountName = email
        return email
    }

    /** Parse the sign-in result; on success persist the email and prime the credential. */
    suspend fun handleSignInResult(data: Intent?): Result<String> = try {
        val account = GoogleSignIn.getSignedInAccountFromIntent(data)
            .getResult(ApiException::class.java)
        val email = account.email
        if (email == null) {
            Result.failure(IllegalStateException("Signed-in account has no email"))
        } else {
            credential.selectedAccountName = email
            accountStore.setAccountEmail(email)
            Result.success(email)
        }
    } catch (e: ApiException) {
        Result.failure(e)
    }

    suspend fun signOut() {
        runCatching { googleSignInClient.signOut().await() }
        credential.selectedAccountName = null
        accountStore.clear()
    }
}
