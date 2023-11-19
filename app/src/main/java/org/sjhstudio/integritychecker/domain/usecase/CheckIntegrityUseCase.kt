package org.sjhstudio.integritychecker.domain.usecase

import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.sjhstudio.integritychecker.domain.repository.IntegrityRepository

@ViewModelScoped
class CheckIntegrityUseCase @Inject constructor(
    private val integrityRepository: IntegrityRepository
) {

    suspend operator fun invoke(request: String): Flow<Map<String, Boolean>> = integrityRepository.checkIntegrity(request)
}