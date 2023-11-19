//package org.sjhstudio.integritychecker
//
//import android.app.ZygotePreload
//import android.content.pm.ApplicationInfo
//import android.os.Build
//import androidx.annotation.RequiresApi
//
//@RequiresApi(api = Build.VERSION_CODES.Q)
//class AppZygotePreload : ZygotePreload {
//    override fun doPreload(appInfo: ApplicationInfo) {
//        System.loadLibrary("native-lib")
//    }
//}