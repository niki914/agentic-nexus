package com.niki914.nexus.h.util

import android.os.Build
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

object RootUtils {
    enum class OsFamily {
        ColorOS,
        HyperOS,
        Unknown
    }

    fun runCommand(command: String): String? {
        var process: Process? = null
        var os: DataOutputStream? = null
        var reader: BufferedReader? = null
        try {
            process = Runtime.getRuntime().exec("su")
            os = DataOutputStream(process.outputStream)
            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()

            reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            process.waitFor()
            return output.toString().trim()
        } catch (e: Exception) {
            xlog("RootUtils: Error running command '$command': ${e.message}")
            return null
        } finally {
            try {
                os?.close()
                reader?.close()
                process?.destroy()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun getPackageVersionCode(packageName: String): Long? {
        // 使用 dumpsys package 获取版本号，这在 root 下非常可靠
        val output = runCommand("dumpsys package $packageName | grep versionCode") ?: return null
        // 匹配格式如: versionCode=127400 minSdk=...
        val regex = Regex("versionCode=(\\d+)")
        val matchResult = regex.find(output)
        return matchResult?.groupValues?.get(1)?.toLongOrNull()
    }

    fun getOsFamily(): OsFamily {
        val manufacturer = Build.MANUFACTURER.trim().lowercase()
        val o = setOf("oppo", "oneplus", "realme")
        val result = when {
            manufacturer in o -> OsFamily.ColorOS
            manufacturer.contains("xiaomi") -> OsFamily.HyperOS
            else -> OsFamily.Unknown
        }

        xlog("RootUtils.getOsFamily result=$result")
        return result
    }

    private fun getSystemProperty(key: String): String? {
        return runCommand("getprop $key")?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun String?.normalizeSystemProperty(): String {
        return this?.trim()?.lowercase().orEmpty()
    }
}
