package org.sjhstudio.integritychecker

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.android.play.core.integrity.IntegrityTokenResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jose4j.jwe.JsonWebEncryption
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwx.JsonWebStructure
import org.jose4j.lang.JoseException
import org.json.JSONObject
import org.sjhstudio.integritychecker.domain.usecase.CheckIntegrityUseCase
import org.sjhstudio.integritychecker.integrity.model.IntegrityState
import org.sjhstudio.integritychecker.integrity.util.IntegrityUtil

@HiltViewModel
class MainViewModel @Inject constructor(
    private val checkIntegrityUseCase: CheckIntegrityUseCase
) : ViewModel() {

    private var _deviceIntegrityState = MutableStateFlow<IntegrityState>(IntegrityState.UnKnown)
    val deviceIntegrityState = _deviceIntegrityState.asStateFlow()

    private var _basicIntegrityState = MutableStateFlow<IntegrityState>(IntegrityState.UnKnown)
    val basicIntegrityState = _deviceIntegrityState.asStateFlow()

    private var _strongIntegrityState = MutableStateFlow<IntegrityState>(IntegrityState.UnKnown)
    val strongIntegrityState = _deviceIntegrityState.asStateFlow()

    private var _error = MutableSharedFlow<String>(onBufferOverflow = BufferOverflow.SUSPEND)
    val error = _error.asSharedFlow()

    fun initIntegrityState() {
        viewModelScope.launch {
            _deviceIntegrityState.emit(IntegrityState.UnKnown)
            _basicIntegrityState.emit(IntegrityState.UnKnown)
            _strongIntegrityState.emit(IntegrityState.UnKnown)
        }
    }

    fun checkIntegrity() {
        val request = "test"
        viewModelScope.launch {
            checkIntegrityUseCase(request)
                .onEach { map ->
                    if (map["MEETS_DEVICE_INTEGRITY"] == true) {
                        _deviceIntegrityState.emit(IntegrityState.Pass)
                    } else {
                        _deviceIntegrityState.emit(IntegrityState.Fail)
                    }

                    if (map["MEETS_BASIC_INTEGRITY"] == true) {
                        _basicIntegrityState.emit(IntegrityState.Pass)
                    } else {
                        _basicIntegrityState.emit(IntegrityState.Fail)
                    }

                    if (map["MEETS_STRONG_INTEGRITY"] == true) {
                        _strongIntegrityState.emit(IntegrityState.Pass)
                    } else {
                        _strongIntegrityState.emit(IntegrityState.Fail)
                    }
                }.catch { cause ->
                    Log.e("sjh", "checkIntegrity :: ERROR($cause)")
                    _error.emit(IntegrityUtil.getErrorMessage(cause))
                }.collect()
        }
    }

    fun requestOriginIntegrityToken(context: Context) {
        val nonce: String = IntegrityUtil.generateNonce(50)

        // Create an instance of a manager.
        val integrityManager = IntegrityManagerFactory.create(context)

        // Request the integrity token by providing a nonce.
        val integrityTokenResponse = integrityManager.requestIntegrityToken(
            IntegrityTokenRequest.builder()
                .setNonce(nonce)
                .build()
        )
        integrityTokenResponse.addOnSuccessListener { integrityTokenResponse1: IntegrityTokenResponse ->
            viewModelScope.launch {
                Log.d("sjh", "requestOriginIntegrityToken :: SUCCESS")
                val integrityToken = integrityTokenResponse1.token()
                decryptOriginToken(integrityToken)
            }
        }
        integrityTokenResponse.addOnFailureListener { e ->
            viewModelScope.launch {
                Log.e("sjh", "requestOriginIntegrityToken :: FAIL")
                _error.emit(IntegrityUtil.getErrorMessage(e))
            }
        }
    }

    private fun decryptOriginToken(integrityToken: String) {
        viewModelScope.launch {
            // base64OfEncodedDecryptionKey is provided through Play Console.
            val decryptionKeyBytes: ByteArray = Base64.decode(DECRYPTION_KEY, Base64.DEFAULT)

            // Deserialized encryption (symmetric) key.
            val decryptionKey: SecretKey = SecretKeySpec(
                decryptionKeyBytes,  /* offset= */
                0,
                decryptionKeyBytes.size,
                "AES"
            )

            // base64OfEncodedVerificationKey is provided through Play Console.
            val encodedVerificationKey: ByteArray = Base64.decode(VERIFICATION_KEY, Base64.DEFAULT)

            // Deserialized verification (public) key.
            var verificationKey: PublicKey?
            try {
                verificationKey = KeyFactory.getInstance("EC")
                    .generatePublic(X509EncodedKeySpec(encodedVerificationKey))
            } catch (e: InvalidKeySpecException) {
                _error.emit("Verification key error \n\n ${e.message.orEmpty()}")
                return@launch
            } catch (e: NoSuchAlgorithmException) {
                _error.emit("Verification key error \n\n ${e.message.orEmpty()}")
                return@launch
            }

            // some error occurred so return
            if (verificationKey == null) {
                _error.emit("Verification key is null")
                Log.e("sjh", "Verification key is null")
                return@launch
            }

            // JsonWebEncryption
            var jwe: JsonWebEncryption?
            try {
                jwe = JsonWebStructure
                    .fromCompactSerialization(integrityToken) as JsonWebEncryption
            } catch (e: JoseException) {
                _error.emit("JsonWebEncryption error \n\n ${e.message.orEmpty()}")
                return@launch
            }

            // some error occurred so return
            if (jwe == null) {
                _error.emit("JsonWebEncryption is null")
                return@launch
            }

            jwe.key = decryptionKey

            // This also decrypts the JWE token.
            var compactJws: String?
            try {
                compactJws = jwe.payload
            } catch (e: JoseException) {
                _error.emit("JsonWebEncryption payload error \n\n ${e.message.orEmpty()}")
                return@launch
            }

            // JsonWebSignature
            var jws: JsonWebSignature?
            try {
                jws = JsonWebStructure
                    .fromCompactSerialization(compactJws) as JsonWebSignature
            } catch (e: JoseException) {
                _error.emit("JsonWebSignature error \n\n ${e.message.orEmpty()}")
                return@launch
            }

            // some error occurred so return
            if (jws == null) {
                _error.emit("JsonWebSignature is null")
                return@launch
            }

            jws.key = verificationKey

            // get the json human readable string
            val jsonPlainVerdict: String = try {
                // This also verifies the signature.
                jws.payload
            } catch (e: JoseException) {
                _error.emit("JsonWebSignature payload error \n\n ${e.message.orEmpty()}")
                return@launch
            }

            Log.d("sjh", "decryption :: result >> $jsonPlainVerdict")

            checkOriginIntegrity(jsonPlainVerdict)
        }
    }

    private fun checkOriginIntegrity(payload: String) {
        viewModelScope.launch {
            payload.takeIf { it.isNotEmpty() }?.let {
                val json = JSONObject(it)

                if (json.has("error")) {
                    _error.emit("Response error")
                } else if (json.has("deviceIntegrity").not()) {
                    _error.emit("Response does not contain deviceIntegrity")
                } else {
                    val response = json.getJSONObject("deviceIntegrity").toString()
                    _deviceIntegrityState.emit(if (response.contains("MEETS_DEVICE_INTEGRITY")) IntegrityState.Pass else IntegrityState.Fail)
                    _basicIntegrityState.emit(if (response.contains("MEETS_BASIC_INTEGRITY")) IntegrityState.Pass else IntegrityState.Fail)
                    _strongIntegrityState.emit(if (response.contains("MEETS_STRONG_INTEGRITY")) IntegrityState.Pass else IntegrityState.Fail)
                }
            }
        }
    }

    companion object {
        private const val DECRYPTION_KEY = "h2g5pbGs0tu6MomIx6j0ShW661SDMuy4wuIOYbeZTUQ="
        private const val VERIFICATION_KEY = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAECxhkvBTgcvVUsX03E8QaigFu2uzFkuG+OnhZrMZkkjO9diHNEzXgyz0F2ZUiWUqRBR35vJmRd1ri2/7tNs7pAg=="
    }
}