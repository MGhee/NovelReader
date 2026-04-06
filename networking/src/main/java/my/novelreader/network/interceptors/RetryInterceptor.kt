package my.novelreader.network.interceptors

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import kotlin.math.min
import kotlin.random.Random

class RetryInterceptor : Interceptor {
    companion object {
        private const val MAX_RETRIES = 2
        private const val INITIAL_DELAY_MS = 500L
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        var exception: Exception? = null

        while (attempt < MAX_RETRIES) {
            try {
                val response = chain.proceed(chain.request())

                // Retry on server errors and too many requests
                if (response.code in listOf(429, 500, 502, 503, 504)) {
                    response.close()
                    if (attempt < MAX_RETRIES - 1) {
                        val retryAfter = response.header("Retry-After")?.toLongOrNull()
                        val delayMs = retryAfter?.times(1000) ?: getBackoffDelayMs(attempt)
                        Timber.d("Retrying request after $delayMs ms (attempt ${attempt + 1}/$MAX_RETRIES)")
                        Thread.sleep(delayMs)
                        attempt++
                        continue
                    }
                    return response
                }

                return response
            } catch (e: Exception) {
                exception = e
                if (attempt < MAX_RETRIES - 1 && isRetryable(e)) {
                    val delayMs = getBackoffDelayMs(attempt)
                    Timber.d("Retrying after exception: ${e.message} (attempt ${attempt + 1}/$MAX_RETRIES)")
                    Thread.sleep(delayMs)
                    attempt++
                } else {
                    throw e
                }
            }
        }

        throw exception ?: Exception("Max retries exceeded")
    }

    private fun getBackoffDelayMs(attempt: Int): Long {
        val baseDelay = INITIAL_DELAY_MS * (1L shl attempt) // Exponential: 1s, 2s, 4s
        val jitter = Random.nextLong(0, 1000)
        return baseDelay + jitter
    }

    private fun isRetryable(e: Exception): Boolean {
        return when (e) {
            is java.net.SocketException,
            is java.net.SocketTimeoutException,
            is java.net.ConnectException,
            is java.io.InterruptedIOException -> true
            else -> false
        }
    }
}
