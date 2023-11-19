package com.data.dto.integrity

/*
{
  requestDetails: { ... }
  appIntegrity: { ... }
  deviceIntegrity: { ... }
  accountDetails: { ... }
}
*/
data class PlayIntegrityState(
    val requestDetails: RequestDetails? = null,
    val appIntegrity: AppIntegrity? = null,
    val deviceIntegrity: DeviceIntegrity? = null,
    val accountDetails: AccountDetails? = null,
) {

    data class RequestDetails(
        val requestPackageName: String? = null,                     // Application package name this attestation was requested for. Note that this field might be spoofed in the middle of the request.
        val requestHash: String? = null,                            // Request hash provided by the developer.
        val timestampMillis: Long? = null                           // The timestamp in milliseconds when the integrity token was prepared (computed on the server).
    )

    data class AppIntegrity(
        val appRecognitionVerdict: String? = null,                  // PLAY_RECOGNIZED, UNRECOGNIZED_VERSION, or UNEVALUATED.
        val packageName: String? = null,                            // The package name of the app. This field is populated iff appRecognitionVerdict != UNEVALUATED.
        val certificateSha256Digest: List<String> = emptyList(),    // The sha256 digest of app certificates. This field is populated iff appRecognitionVerdict != UNEVALUATED.
        val versionCode: String? = null                             // The version of the app. This field is populated iff appRecognitionVerdict != UNEVALUATED.
    )

    data class DeviceIntegrity(
        val deviceRecognitionVerdict: List<String> = emptyList()    // "MEETS_DEVICE_INTEGRITY" is one of several possible values.
    )

    data class AccountDetails(
        val appLicensingVerdict: String? = null                     // This field can be LICENSED, UNLICENSED, or UNEVALUATED.
    )
}