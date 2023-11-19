package org.sjhstudio.integritychecker.integrity

import android.content.Context
import android.util.Log
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@Singleton
class IntegrityTokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun requestTokenProvider(): Flow<StandardIntegrityTokenProvider> = callbackFlow {
        val standardIntegrityManager = IntegrityManagerFactory.createStandard(context)
        standardIntegrityManager.prepareIntegrityToken(
            StandardIntegrityManager.PrepareIntegrityTokenRequest.builder()
                .setCloudProjectNumber(CLOUD_PROJECT_NUMBER)
                .build()
        ).addOnSuccessListener { tokenProvider: StandardIntegrityTokenProvider ->
            Log.d("sjh", "requestTokenProvider :: success")
            trySend(tokenProvider)
        }.addOnFailureListener { e ->
            Log.e("sjh", "requestTokenProvider :: fail")
            close(e)
        }

        awaitClose()
    }

    companion object {
        // 구글 클라우드 프로젝트 번호
        private const val CLOUD_PROJECT_NUMBER: Long = 991949606441
    }
}