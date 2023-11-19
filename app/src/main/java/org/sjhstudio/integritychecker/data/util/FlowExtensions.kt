package org.sjhstudio.integritychecker.data.util

import android.util.Log
import kotlin.math.pow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.retryWhen

suspend inline fun <T> Flow<T>.retrying(
    retryCount: Int,
    intervalMillis: Long,
    useExponentialBackOff: Boolean = false,
    crossinline retryCheck: (Throwable) -> Boolean = { true }
): Flow<T> =
    retryWhen { cause, attempt ->
        if (retryCheck.invoke(cause) && attempt < retryCount) {
            Log.e("sjh", "retry $attempt")
            delay(intervalMillis.takeIf { useExponentialBackOff }?.times((2.0).pow(attempt.toInt()).toInt()) ?: intervalMillis)
            true
        } else {
            false
        }
    }