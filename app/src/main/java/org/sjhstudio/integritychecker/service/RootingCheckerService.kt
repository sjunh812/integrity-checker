package org.sjhstudio.integritychecker.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.system.Os
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import org.sjhstudio.integritychecker.IRootingCheckerService

class RootingCheckerService : Service() {

    override fun onCreate() {
        super.onCreate()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private val binder = object : IRootingCheckerService.Stub() {
        override fun isMagiskPresent(): Boolean {
            Log.d("sjh", "Isolated UID:" + Os.getuid())
            var isMagiskPresent = false
            val file = File("/proc/self/mounts")
            try {
                var line = ""
                var count = 0
                FileInputStream(file).use { fis ->
                    BufferedReader(InputStreamReader(fis)).use { reader ->
                        while (reader.readLine()?.also { line = it } != null && count == 0) {
                            for (path in mountPaths) {
                                Log.d("sjh", "mountPath : $path // line : $line")
                                if (line.contains(path)) {
                                    count++
                                    break
                                }
                            }
                        }
                    }
                }
                Log.d("sjh", "Count of detected paths $count")
                if (count > 0) {
                    Log.d("sjh", "Magisk Found!!")
                    isMagiskPresent = true
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return isMagiskPresent
        }

    }

    companion object {
        private val mountPaths = arrayOf("magisk", "core/mirror", "core/img")
    }
}