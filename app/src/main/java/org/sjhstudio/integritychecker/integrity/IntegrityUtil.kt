package org.sjhstudio.integritychecker.integrity

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

    fun getErrorText(e: Exception): String {
        return runCatching {
            val msg = e.message!!
            // Pretty junk way of getting the error code but it works
            val errorCode = msg.replace("\n".toRegex(), "").replace(":(.*)".toRegex(), "").toInt()
            when (errorCode) {
                IntegrityErrorCode.API_NOT_AVAILABLE -> {
                    """
                        Integrity API is not available.
                        
                        The Play Store version might be old, try updating it.
                    """.trimIndent()
                }

                IntegrityErrorCode.APP_NOT_INSTALLED -> {
                    """
                        The calling app is not installed.
                        
                        This shouldn't happen. If it does please open an issue on Github.
                    """.trimIndent()
                }

                IntegrityErrorCode.APP_UID_MISMATCH -> {
                    """
                        The calling app UID (user id) does not match the one from Package Manager.
                        
                        This shouldn't happen. If it does please open an issue on Github.
                    """.trimIndent()
                }

                IntegrityErrorCode.CANNOT_BIND_TO_SERVICE -> {
                    """
                        Binding to the service in the Play Store has failed.
                        
                        This can be due to having an old Play Store version installed on the device.
                    """.trimIndent()
                }

                IntegrityErrorCode.CLIENT_TRANSIENT_ERROR -> {
                    "There was a transient error in the client device."
                }

                IntegrityErrorCode.CLOUD_PROJECT_NUMBER_IS_INVALID -> {
                    "The provided cloud project number is invalid."
                }

                IntegrityErrorCode.GOOGLE_SERVER_UNAVAILABLE -> {
                    "Unknown internal Google server error."
                }

                IntegrityErrorCode.INTERNAL_ERROR -> {
                    "Unknown internal error."
                }

                IntegrityErrorCode.NETWORK_ERROR -> {
                    """
                        No available network is found.
                        
                        Please check your connection.
                    """.trimIndent()
                }

                IntegrityErrorCode.NONCE_IS_NOT_BASE64 -> {
                    """
                        Nonce is not encoded as a base64 web-safe no-wrap string.
                        
                        This shouldn't happen. If it does please open an issue on Github.
                    """.trimIndent()
                }

                IntegrityErrorCode.NONCE_TOO_LONG -> {
                    """
                        Nonce length is too long.
                        
                        This shouldn't happen. If it does please open an issue on Github.
                    """.trimIndent()
                }

                IntegrityErrorCode.NONCE_TOO_SHORT -> {
                    """
                        Nonce length is too short.
                        
                        This shouldn't happen. If it does please open an issue on Github.
                    """.trimIndent()
                }

                IntegrityErrorCode.NO_ERROR -> {
                    """
                        No error has occurred.
                        
                        If you ever get this, congrats, I have no idea what it means.
                    """.trimIndent()
                }

                IntegrityErrorCode.PLAY_SERVICES_NOT_FOUND -> {
                    """
                        Play Services is not available or version is too old.
                
                        Try installing or updating Google Play Services.
                    """.trimIndent()
                }

                IntegrityErrorCode.PLAY_SERVICES_VERSION_OUTDATED -> {
                    """
                        Play Services needs to be updated.
                
                        Try updating Google Play Services.
                    """.trimIndent()
                }

                IntegrityErrorCode.PLAY_STORE_ACCOUNT_NOT_FOUND -> {
                    """
                        No Play Store account is found on device.
                
                        Try logging into Play Store.
                    """.trimIndent()
                }

                IntegrityErrorCode.PLAY_STORE_NOT_FOUND -> {
                    """
                        No Play Store app is found on device or not official version is installed.
                
                        This app can't work without Play Store.
                    """.trimIndent()
                }

                IntegrityErrorCode.PLAY_STORE_VERSION_OUTDATED -> {
                    """
                        The Play Store needs to be updated.
                
                        Try updating Google Play Store.
                    """.trimIndent()
                }

                IntegrityErrorCode.TOO_MANY_REQUESTS -> {
                    """
                        The calling app is making too many requests to the API and hence is throttled.
                
                        This shouldn't happen. If it does please open an issue on Github.
                    """.trimIndent()
                }

                else -> "Unknown Error"
            }
        }.getOrNull() ?: return "Unknown Error"
    }
}