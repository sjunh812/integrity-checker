package org.sjhstudio.integritychecker.data.repository.integrity

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import org.sjhstudio.integritychecker.data.datasource.integrity.IntegrityDataSource
import org.sjhstudio.integritychecker.domain.repository.IntegrityRepository
import org.sjhstudio.integritychecker.integrity.IntegrityTokenManager

@Singleton
class IntegrityRepositoryImpl @Inject constructor(
    private val integrityTokenManager: IntegrityTokenManager,
    private val integrityDataSource: IntegrityDataSource
) : IntegrityRepository {

    override suspend fun checkIntegrity(request: String): Flow<Map<String, Boolean>> =
        integrityTokenManager.requestTokenProvider()
            .flatMapConcat { tokenProvider ->
                integrityDataSource.requestIntegrityToken(request, tokenProvider)
            }.map { token ->
                // TODO. 서버에 token 복호화 요청 및 payload 응답
                // 현재 로컬에서 진행 (서버로직 구현)
                val playIntegrityState = integrityDataSource.decryptToken(token)
                playIntegrityState.deviceIntegrity?.let {
                    val recognitionVerdict = it.deviceRecognitionVerdict
                    mapOf(
                        "MEETS_DEVICE_INTEGRITY" to recognitionVerdict.contains("MEETS_DEVICE_INTEGRITY"),
                        "MEETS_BASIC_INTEGRITY" to recognitionVerdict.contains("MEETS_BASIC_INTEGRITY"),
                        "MEETS_STRONG_INTEGRITY" to recognitionVerdict.contains("MEETS_STRONG_INTEGRITY")
                    )
                } ?: mapOf(
                    "MEETS_DEVICE_INTEGRITY" to false,
                    "MEETS_BASIC_INTEGRITY" to false,
                    "MEETS_STRONG_INTEGRITY" to false
                )
            }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class IntegrityRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindsIntegrityRepository(integrityRepositoryImpl: IntegrityRepositoryImpl): IntegrityRepository
}