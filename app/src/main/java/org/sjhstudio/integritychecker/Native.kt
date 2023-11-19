package org.sjhstudio.integritychecker

object Native {

    init {
        System.loadLibrary("native-lib")
    }

    val isMagiskPresentNative: Boolean
        external get
}