package org.sjhstudio.integritychecker.integrity.util

import com.google.android.play.core.integrity.model.IntegrityErrorCode
import kotlin.math.floor

object IntegrityUtil {

    fun generateNonce(length: Int): String {
        var nonce = ""
        val allowed = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        for (i in 0 until length) {
            nonce += allowed[floor(Math.random() * allowed.length).toInt()].toString()
        }
        return nonce
    }

    private fun getErrorCode(throwable: Throwable) =
        throwable.message?.replace("\n".toRegex(), "")?.replace(":(.*)".toRegex(), "")?.toIntOrNull() ?: 0

    /* Handling error message with IntegrityErrorCode */
    fun getErrorMessage(throwable: Throwable): String {
        return runCatching {
            when (getErrorCode(throwable)) {
                // Integrity API를 사용할 수 없습니다.
                // (Integrity API가 사용 설정되지 않았거나 Play 스토어 버전이 오래되었을 수 있습니다.)
                IntegrityErrorCode.API_NOT_AVAILABLE -> "Error : API_NOT_AVAILABLE"

                // 사용 가능한 네트워크를 찾을 수 없습니다.
                IntegrityErrorCode.NETWORK_ERROR -> "Error : NETWORK_ERROR"

                // 기기에서 공식 Play 스토어 앱을 찾을 수 없습니다.
                IntegrityErrorCode.PLAY_STORE_NOT_FOUND -> "Error : PLAY_STORE_NOT_FOUND"

                // Play 스토어 앱을 업데이트해야 합니다.
                IntegrityErrorCode.PLAY_STORE_VERSION_OUTDATED -> "Error : PLAY_STORE_VERSION_OUTDATED"

                // 기기에서 Play 스토어 계정을 찾을 수 없습니다.
                // (이 오류 코드는 지원되지 않는 이전 Play 스토어 버전에만 사용됩니다.)
                IntegrityErrorCode.PLAY_STORE_ACCOUNT_NOT_FOUND -> "Error : PLAY_STORE_ACCOUNT_NOT_FOUND"

                // Play 스토어의 서비스에 바인딩할 수 없습니다.
                // (기기에 이전 Play 스토어 버전이 설치되어 있기 때문일 수 있습니다.)
                IntegrityErrorCode.CANNOT_BIND_TO_SERVICE -> "Error : CANNOT_BIND_TO_SERVICE"

                // 호출 앱이 설치되어 있지 않습니다.
                IntegrityErrorCode.APP_NOT_INSTALLED -> "Error : APP_NOT_INSTALLED"

                // Play 서비스를 사용할 수 없거나 업데이트해야 합니다.
                IntegrityErrorCode.PLAY_SERVICES_NOT_FOUND -> "Error : PLAY_SERVICES_NOT_FOUND"

                // Play 서비스를 업데이트해야 합니다.
                IntegrityErrorCode.PLAY_SERVICES_VERSION_OUTDATED -> "Error : PLAY_SERVICES_VERSION_OUTDATED"

                // 호출 앱이 API에 너무 많은 요청을 해 제한되었습니다. (성공 처리)
                IntegrityErrorCode.TOO_MANY_REQUESTS -> ""

                // 알 수 없는 내부 Google 서버 오류입니다.
                // 재시도 로직 (3회 / 지연시간: 5초 > 10초 > 20초)
                IntegrityErrorCode.GOOGLE_SERVER_UNAVAILABLE -> "Error : GOOGLE_SERVER_UNAVAILABLE"

                // 클라이언트 기기에 일시적인 오류가 발생했습니다.
                // 재시도 로직 (3회 / 지연시간: 5초 > 10초 > 20초)
                IntegrityErrorCode.CLIENT_TRANSIENT_ERROR -> "Error : CLIENT_TRANSIENT_ERROR"

                // 알 수 없는 내부 오류입니다.
                // 재시도 로직 (3회 / 지연시간: 5초 > 10초 > 20초)
                IntegrityErrorCode.INTERNAL_ERROR -> "Error : INTERNAL_ERROR"

                // nonce 길이가 너무 짧습니다.
                // (nonce는 최소 16바이트(base64 인코딩 전)여야 합니다.)
                IntegrityErrorCode.NONCE_TOO_SHORT -> "Error : NONCE_TOO_SHORT"

                // nonce 길이가 너무 깁니다.
                // (nonce는 base64 인코딩 전에 500바이트 미만이어야 합니다.)
                IntegrityErrorCode.NONCE_TOO_LONG -> "Error : NONCE_TOO_LONG"

                // nonce 형식이 base64, 웹 안전, 줄바꿈 없음이 아닙니다.
                IntegrityErrorCode.NONCE_IS_NOT_BASE64 -> "Error : NONCE_IS_NOT_BASE64"

                // 입력한 클라우드 프로젝트 번호가 잘못되었습니다.
                IntegrityErrorCode.CLOUD_PROJECT_NUMBER_IS_INVALID -> "Error : CLOUD_PROJECT_NUMBER_IS_INVALID"

                // 호출 앱 UID(사용자 ID)가 패키지 관리자의 UID와 일치하지 않습니다.
                IntegrityErrorCode.APP_UID_MISMATCH -> "Error : APP_UID_MISMATCH"

                else -> "Error : Unknown"
            }
        }.getOrElse {
            "Error : Unknown\n${it.message.orEmpty()}"
        }
    }

    /* Check Throwable need to retry */
    fun checkRetry(throwable: Throwable): Boolean {
        val errorCode = getErrorCode(throwable)
        return errorCode == IntegrityErrorCode.TOO_MANY_REQUESTS
                || errorCode == IntegrityErrorCode.GOOGLE_SERVER_UNAVAILABLE
                || errorCode == IntegrityErrorCode.CLIENT_TRANSIENT_ERROR
                || errorCode == IntegrityErrorCode.INTERNAL_ERROR
    }
}