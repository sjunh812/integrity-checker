package org.sjhstudio.integritychecker.service

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.system.Os
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.Scanner
import org.sjhstudio.integritychecker.IRootingCheckerService


class RootingCheckerService : Service() {

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private val binder = object : IRootingCheckerService.Stub() {
        override fun isMagiskPresent(): Boolean {
            Log.d("sjh", "Isolated UID:" + Os.getuid())
            var isMagiskPresent = false
            val file = File("/proc/self/mounts")
            // mounts
            // mountinfo
            // mountstats
            try {
                var line = ""
                var count = 0
                FileInputStream(file).use { fis ->
                    BufferedReader(InputStreamReader(fis)).use { reader ->
                        while (reader.readLine()?.also { line = it } != null && count == 0) {
                            for (path in mountPaths) {
//                                Log.d("sjh", "mountPath : $path // line : $line")
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
                } else {
                    if (
                        checkForRWPaths()
//                        checkForDangerousProps()
//                        || checkForSuBinary()
//                        || checkForBusyBoxBinary()
//                        || checkSuExists()
//                        || detectTestKeys()
                    ) {
                        Log.d("sjh", "Magisk Found!!")
                        return true
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return isMagiskPresent
        }
    }

    private fun mountReader(): List<String>? {
        return try {
            val inputstream = Runtime.getRuntime().exec("mount").inputStream ?: return null
            val propVal = Scanner(inputstream).useDelimiter("\\A").next()
            propVal.split("\n".toRegex()).dropLastWhile { it.isEmpty() }
        } catch (e: IOException) {
//            Log.e(e)
            null
        } catch (e: NoSuchElementException) {
//            Log.e(e)
            null
        }
    }

    /**
     * When you're root you can change the permissions on common system directories, this method checks if any of these patha Const.pathsThatShouldNotBeWritable are writable.
     * @return true if one of the dir is writable
     */
    fun checkForRWPaths(): Boolean {
        var result = false

        //Run the command "mount" to retrieve all mounted directories
        val lines = mountReader()
            ?: // Could not read, assume false;
            return false

        //The SDK version of the software currently running on this hardware device.
        val sdkVersion = Build.VERSION.SDK_INT
        /**
         *
         * In devices that are running Android 6 and less, the mount command line has an output as follow:
         *
         * <fs_spec_path> <fs_file> <fs_spec> <fs_mntopts>
         *
         * where :
         * - fs_spec_path: describes the path of the device or remote filesystem to be mounted.
         * - fs_file: describes the mount point for the filesystem.
         * - fs_spec describes the block device or remote filesystem to be mounted.
         * - fs_mntopts: describes the mount options associated with the filesystem. (E.g. "rw,nosuid,nodev" )
         *
        </fs_mntopts></fs_spec></fs_file></fs_spec_path> */
        /** In devices running Android which is greater than Marshmallow, the mount command output is as follow:
         *
         * <fs_spec> <ON> <fs_file> <TYPE> <fs_vfs_type> <(fs_mntopts)>
         *
         * where :
         * - fs_spec describes the block device or remote filesystem to be mounted.
         * - fs_file: describes the mount point for the filesystem.
         * - fs_vfs_type: describes the type of the filesystem.
         * - fs_mntopts: describes the mount options associated with the filesystem. (E.g. "(rw,seclabel,nosuid,nodev,relatime)" )
        </fs_vfs_type></TYPE></fs_file></ON></fs_spec> */
        for (line in lines) {

            // Split lines into parts
            val args = line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (sdkVersion <= Build.VERSION_CODES.M && args.size < 4 || sdkVersion > Build.VERSION_CODES.M && args.size < 6) {
                // If we don't have enough options per line, skip this and log an error
                Log.e("sjh", "Error formatting mount line: $line")
                continue
            }
            var mountPoint: String
            var mountOptions: String
            /**
             * To check if the device is running Android version higher than Marshmallow or not
             */
            if (sdkVersion > Build.VERSION_CODES.M) {
                mountPoint = args[2]
                mountOptions = args[5]
            } else {
                mountPoint = args[1]
                mountOptions = args[3]
            }
            for (pathToCheck in pathsThatShouldNotBeWritable) {
                if (mountPoint.equals(pathToCheck, ignoreCase = true)) {
                    /**
                     * If the device is running an Android version above Marshmallow,
                     * need to remove parentheses from options parameter;
                     */
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                        mountOptions = mountOptions.replace("(", "")
                        mountOptions = mountOptions.replace(")", "")
                    }

                    // Split options out and compare against "rw" to avoid false positives
                    for (option in mountOptions.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                        if (option.equals("rw", ignoreCase = true)) {
                            Log.e("sjh", "$pathToCheck path is mounted with rw permissions! $line")
                            result = true
                            break
                        }
                    }
                }
            }
        }
        return result
    }

    val pathsThatShouldNotBeWritable = arrayOf(
        "/system",
        "/system/bin",
        "/system/sbin",
        "/system/xbin",
        "/vendor/bin",
        "/sbin",
        "/etc"
    )

    private fun propsReader(): List<String>? {
        try {
            val input = Runtime.getRuntime().exec("getprop").inputStream ?: return null
            val propVal = Scanner(input).useDelimiter("\\A").next()
            return propVal.split("\n")
        } catch (e: IOException) {
            Log.e("sjh", e.message.orEmpty())
        } catch (e: NoSuchElementException) {
            Log.e("sjh", e.message.orEmpty())
        }
        return null
    }

    private fun checkForDangerousProps(): Boolean {
        val dangerousProps = mutableMapOf(
            "ro.debuggable" to "1",
            "ro.secure" to "0"
        )
        var result = false
        var lines = propsReader() ?: return false
        propsReader()?.let {
            lines.forEach { line ->
                dangerousProps.keys.forEach { key ->
                    if (line.contains(key)) {
                        var badValue = dangerousProps[key]
                        badValue = "[$badValue]"
                        if (line.contains(badValue)) {
                            Log.e("sjh", "$key = $badValue detected!")
                            result = true
                        }
                    }
                }
            }
        } ?: return false
        return result
    }

    fun checkOTACerts() : Boolean{
        val otaPath = "/etc/security/otacerts.zip"
        val file = File(otaPath)
        return file.exists()
    }


    private val binaryPaths = arrayOf(
        "/data/local/",
        "/data/local/bin/",
        "/data/local/xbin/",
        "/sbin/",
        "/su/bin/",
        "/system/bin/",
        "/system/bin/.ext/",
        "/system/bin/failsafe/",
        "/system/sd/xbin/",
        "/system/usr/we-need-root/",
        "/system/xbin/",
        "/system/app/Superuser.apk",
        "/cache",
        "/data",
        "/dev"
    )



    /**
     * @param filename - check for this existence of this
     * @return true if exists
     */
    private fun checkForBinary(filename: String): Boolean {
        for (path in binaryPaths) {
            val f = File(path, filename)
            val fileExists = f.exists()
            if (fileExists) {
                return true
            }
        }
        return false
    }

    /**
     * A variation on the checking for SU, this attempts a 'which su'
     * different file system check for the su binary
     * @return true if su exists
     */
    private fun checkSuExists(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("/system /xbin/which", "su"))
            val `in` = BufferedReader(
                InputStreamReader(process.inputStream)
            )
            val line = `in`.readLine()
            process.destroy()
            line != null
        } catch (e: Exception) {
            process?.destroy()
            false
        }
    }


    private fun checkForSuBinary(): Boolean {
        val detected = checkForBinary("su")
        Log.e("sjh", "checkForSuBinary :: detected? $detected")
        return detected // function is available below
    }

    private fun checkForBusyBoxBinary(): Boolean {
        val detected = checkForBinary("busybox")
        Log.e("sjh", "checkForBusyBoxBinary :: detected? $detected")
        return detected //function is available below
    }

    fun a(): Boolean {
        // 2 or ApplicationInfo.FLAG_DEBUGGABLE
        val detected = (applicationContext.applicationInfo.flags and 2) != 0
        Log.e("sjh", "a :: detected? $detected")
        return detected
    }

    private fun detectTestKeys(): Boolean {
        val buildTags = Build.TAGS
        val detected = buildTags != null && buildTags.contains("test-keys")
        Log.e("sjh", "detectTestKeys :: detected? $detected")
        return detected
    }

    companion object {
        private val mountPaths = arrayOf("magisk", "core/mirror", "core/img")
    }
}