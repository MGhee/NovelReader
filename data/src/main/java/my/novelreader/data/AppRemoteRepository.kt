package my.novelreader.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.novelreader.core.AppInternalState
import my.novelreader.core.Response
import my.novelreader.core.domain.AppVersion
import my.novelreader.core.domain.RemoteAppVersion
import my.novelreader.network.NetworkClient
import my.novelreader.network.toJson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRemoteRepository @Inject constructor(
    private val networkClient: NetworkClient,
    private val appInternalState: AppInternalState,
) {

    // Update checker disabled for personal fork
    // To enable, set to your own GitHub releases endpoint (e.g., https://api.github.com/repos/username/NovelReader/releases/latest)
    private val lastReleaseUrl = ""

    suspend fun getLastAppVersion(
    ): Response<RemoteAppVersion> = withContext(Dispatchers.Default) {
        return@withContext my.novelreader.network.tryConnect {
            val json = networkClient
                .get(lastReleaseUrl)
                .toJson()
                .asJsonObject

            RemoteAppVersion(
                version = AppVersion.fromString(json["tag_name"].asString),
                sourceUrl = json["html_url"].asString
            )
        }
    }

    fun getCurrentAppVersion() = AppVersion.fromString(appInternalState.versionName)
}