package my.novelreader.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

sealed class GoogleAuthResult {
    data class Success(
        val idToken: String,
        val displayName: String?,
        val email: String?,
        val photoUrl: String?,
    ) : GoogleAuthResult()

    data class Error(val message: String) : GoogleAuthResult()
}

@Singleton
class GoogleAuthManager @Inject constructor() {

    suspend fun signIn(activityContext: Context, webClientId: String): GoogleAuthResult {
        return try {
            Timber.d("GoogleAuthManager: Starting sign-in with clientId=$webClientId")

            val credentialManager = CredentialManager.create(activityContext)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            Timber.d("GoogleAuthManager: Calling getCredential...")
            val result = credentialManager.getCredential(activityContext, request)
            Timber.d("GoogleAuthManager: Got credential response")
            val credential = result.credential

            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken

            Timber.d("GoogleAuthManager: sign-in successful")

            GoogleAuthResult.Success(
                idToken = idToken,
                displayName = googleIdTokenCredential.displayName,
                email = googleIdTokenCredential.id,
                photoUrl = googleIdTokenCredential.profilePictureUri?.toString(),
            )
        } catch (e: Exception) {
            Timber.e(e, "GoogleAuthManager: sign-in failed")
            GoogleAuthResult.Error(e.message ?: "Sign-in failed")
        }
    }
}
