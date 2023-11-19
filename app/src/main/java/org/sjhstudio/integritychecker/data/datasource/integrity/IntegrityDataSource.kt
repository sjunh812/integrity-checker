package org.sjhstudio.integritychecker.data.datasource.integrity

import android.content.Context
import android.util.Log
import com.data.dto.integrity.PlayIntegrityState
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.playintegrity.v1.PlayIntegrity
import com.google.api.services.playintegrity.v1.PlayIntegrityRequestInitializer
import com.google.api.services.playintegrity.v1.PlayIntegrityScopes
import com.google.api.services.playintegrity.v1.model.DecodeIntegrityTokenRequest
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import org.sjhstudio.integritychecker.R
import org.sjhstudio.integritychecker.integrity.util.IntegrityUtil
import org.sjhstudio.integritychecker.data.util.retrying

@Singleton
class IntegrityDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun requestIntegrityToken(
        request: String,
        tokenProvider: StandardIntegrityManager.StandardIntegrityTokenProvider
    ): Flow<String> = callbackFlow<String> {
        // 앱 서버에 요청할 값들을 requestHash 에 포함 (무결성 체크 용도로만 사용하므로 임의값 사용)
        val requestHash = request   // SHA256 필요
        val integrityTokenResponse = tokenProvider.request(
            StandardIntegrityManager.StandardIntegrityTokenRequest.builder()
                .setRequestHash(requestHash)
                .build()
        )
        integrityTokenResponse?.let { response ->
            response.addOnSuccessListener {
                Log.d("sjh", "requestIntegrityToken :: response success")
                trySend(it.token())
            }.addOnFailureListener { e ->
                Log.e("sjh", "requestIntegrityToken :: response fail")
                close(e)
            }
        }

        awaitClose {
            // release callback
        }
    }.retrying(
        retryCount = RETRY_COUNT,
        intervalMillis = RETRY_DELAY,
        useExponentialBackOff = true,
    ) { e ->
        IntegrityUtil.checkRetry(e)
    }

    suspend fun decryptToken(token: String): PlayIntegrityState {
        return withContext(Dispatchers.IO) {
            val decodeRequest = DecodeIntegrityTokenRequest().apply {
                integrityToken = token
            }
            val stream = context.resources.openRawResource(R.raw.credentials)
            val credentials = ServiceAccountCredentials
                .fromStream(stream)
                .createScoped(PlayIntegrityScopes.PLAYINTEGRITY)
            val requestInitializer: HttpRequestInitializer = HttpCredentialsAdapter(credentials)
            val httpTransport: HttpTransport = NetHttpTransport()
            val initializer: GoogleClientRequestInitializer = PlayIntegrityRequestInitializer()

            val playIntegrity = PlayIntegrity.Builder(httpTransport, JacksonFactory.getDefaultInstance(), requestInitializer)
                .setApplicationName("Play Integrity") // <-- Google cloud project associated with server
                .setGoogleClientRequestInitializer(initializer)
            val response = playIntegrity.build()
                .v1()
                .decodeIntegrityToken(context.packageName, decodeRequest)
                .execute()

            Gson().fromJson(response.tokenPayloadExternal.toString(), PlayIntegrityState::class.java) as PlayIntegrityState
        }
    }

    companion object {
        const val RETRY_COUNT = 3
        const val RETRY_DELAY = 5000L
    }
}