package org.sjhstudio.integritychecker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import org.sjhstudio.integritychecker.integrity.IntegrityTokenManager

@HiltAndroidApp
class MyApplication : Application() {

    @Inject
    lateinit var integrityTokenManager: IntegrityTokenManager

    override fun onCreate() {
        super.onCreate()
//        integrityTokenManager.prepareTokenProvider()
//        prepareIntegrityTokenProvider()
    }

//    private fun prepareIntegrityTokenProvider() {
//        // Create an instance of a manager.
//        val standardIntegrityManager = IntegrityManagerFactory.createStandard(applicationContext)
//        // Google Cloud project number
//        val cloudProjectNumber = 991949606441
//
//        standardIntegrityManager.prepareIntegrityToken(
//            PrepareIntegrityTokenRequest.builder()
//                .setCloudProjectNumber(cloudProjectNumber)
//                .build()
//        ).addOnSuccessListener { tokenProvider ->
//            Log.d("sjh", "prepareIntegrityTokenProvider :: ready for token provider!!")
//            integrityTokenProvider = tokenProvider
//        }.addOnFailureListener { e ->
//            Log.e("sjh", "prepareIntegrityTokenProvider :: fail to token provider >> " + IntegrityUtil.getErrorMessage(e))
//        }
//    }
//
//    companion object {
//        var integrityTokenProvider: StandardIntegrityTokenProvider? = null
//    }
}