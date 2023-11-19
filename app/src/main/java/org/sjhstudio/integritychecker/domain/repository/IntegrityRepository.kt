package org.sjhstudio.integritychecker.domain.repository

import kotlinx.coroutines.flow.Flow

interface IntegrityRepository {
    suspend fun checkIntegrity(request: String): Flow<Map<String, Boolean>>
}