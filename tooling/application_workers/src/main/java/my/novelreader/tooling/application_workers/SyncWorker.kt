package my.novelreader.tooling.application_workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.novelreader.data.SyncRepository
import my.novelreader.core.Response
import timber.log.Timber
import java.util.concurrent.TimeUnit

@HiltWorker
internal class SyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val syncRepository: SyncRepository,
) : CoroutineWorker(context, workerParameters) {

    companion object {
        const val TAG = "Sync"
        const val TAG_MANUAL = "SyncManual"
        private const val DATA_SERVER_URL = "serverUrl"
        private const val DATA_AUTH_TOKEN = "authToken"

        fun createPeriodicRequest(
            serverUrl: String,
            authToken: String = "",
            repeatIntervalHours: Int = 24,
        ): PeriodicWorkRequest {
            val builder = PeriodicWorkRequestBuilder<SyncWorker>(
                repeatInterval = repeatIntervalHours.toLong(),
                repeatIntervalTimeUnit = TimeUnit.HOURS,
            )

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            return builder
                .addTag(TAG)
                .setConstraints(constraints)
                .setInitialDelay(30, TimeUnit.MINUTES)
                .setInputData(createInputData(serverUrl, authToken))
                .build()
        }

        fun createManualRequest(serverUrl: String, authToken: String = ""): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<SyncWorker>()
                .addTag(TAG_MANUAL)
                .setInputData(createInputData(serverUrl, authToken))
                .build()
        }

        private fun createInputData(serverUrl: String, authToken: String) = Data.Builder()
            .putString(DATA_SERVER_URL, serverUrl)
            .putString(DATA_AUTH_TOKEN, authToken)
            .build()
    }

    override suspend fun doWork(): Result {
        val serverUrl = inputData.getString(DATA_SERVER_URL) ?: return Result.failure()
        val authToken = inputData.getString(DATA_AUTH_TOKEN) ?: ""

        Timber.d("SyncWorker: starting sync with $serverUrl")

        return try {
            withContext(Dispatchers.IO) {
                val result = syncRepository.syncWithServer(serverUrl, authToken)

                when (result) {
                    is Response.Success -> {
                        Timber.d("SyncWorker: sync successful: ${result.data}")
                        Result.success()
                    }
                    is Response.Error -> {
                        Timber.e(result.exception, "SyncWorker: ${result.message}")
                        Result.retry()
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SyncWorker: unexpected error")
            Result.retry()
        }
    }
}
