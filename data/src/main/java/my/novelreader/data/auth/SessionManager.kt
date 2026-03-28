package my.novelreader.data.auth

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import my.novelreader.core.appPreferences.AppPreferences
import my.novelreader.core.Response
import my.novelreader.network.NetworkClient
import my.novelreader.network.ScraperNetworkClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Serializable
data class GoogleTokenResponse(
    val sessionToken: String,
    val email: String,
    val name: String?,
    val picture: String?,
)

@Singleton
class SessionManager @Inject constructor(
    private val appPreferences: AppPreferences,
    private val networkClient: NetworkClient,
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun exchangeGoogleToken(
        serverUrl: String,
        idToken: String,
    ): Response<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "$serverUrl/api/auth/google-token"
            Timber.d("SessionManager: exchanging Google token at $url")

            val requestBody = """{"idToken":"$idToken"}""".toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val client = (networkClient as? ScraperNetworkClient)?.client
            if (client == null) {
                Timber.e("SessionManager: networkClient is not ScraperNetworkClient")
                return@withContext Response.Error("Invalid network client", Exception())
            }

            val response = client.newCall(request).execute()
            Timber.d("SessionManager: token exchange response code=${response.code}")

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                Timber.e("SessionManager: token exchange failed: ${response.code} $errorBody")
                return@withContext Response.Error("Token exchange failed: ${response.code}", Exception())
            }

            val body = response.body?.string() ?: ""
            val jsonElement = json.parseToJsonElement(body)
            val responseData = json.decodeFromJsonElement(GoogleTokenResponse.serializer(), jsonElement)

            // Store session token and user info
            appPreferences.SYNC_SESSION_TOKEN.value = responseData.sessionToken
            appPreferences.SYNC_USER_EMAIL.value = responseData.email
            appPreferences.SYNC_USER_DISPLAY_NAME.value = responseData.name ?: ""
            appPreferences.SYNC_USER_PHOTO_URL.value = responseData.picture ?: ""

            Timber.d("SessionManager: token exchange successful, user=${responseData.email}")
            Response.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "SessionManager: unexpected error during token exchange")
            Response.Error(e.message ?: "Token exchange error", e)
        }
    }

    fun getEffectiveToken(): String {
        val sessionToken = appPreferences.SYNC_SESSION_TOKEN.value
        if (sessionToken.isNotBlank()) return sessionToken
        // Fallback to legacy API key for backward compatibility
        return appPreferences.SYNC_API_KEY.value
    }

    fun isLoggedIn(): Boolean {
        return appPreferences.SYNC_SESSION_TOKEN.value.isNotBlank()
    }

    fun logout() {
        appPreferences.SYNC_SESSION_TOKEN.value = ""
        appPreferences.SYNC_USER_EMAIL.value = ""
        appPreferences.SYNC_USER_DISPLAY_NAME.value = ""
        appPreferences.SYNC_USER_PHOTO_URL.value = ""
        Timber.d("SessionManager: user logged out")
    }

    fun getUserEmail(): String = appPreferences.SYNC_USER_EMAIL.value
    fun getUserDisplayName(): String = appPreferences.SYNC_USER_DISPLAY_NAME.value
    fun getUserPhotoUrl(): String = appPreferences.SYNC_USER_PHOTO_URL.value
}
