package org.sjhstudio.integritychecker

internal object Native {

    init {
        System.loadLibrary("native-lib")
    }

    val isMagiskPresentNative: Boolean
        external get
}