package com.niki914.nexus.h.util

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
        val properties = mapOf(
            "ro.mi.os.version.name" to getSystemProperty("ro.mi.os.version.name"),
            "ro.miui.ui.version.name" to getSystemProperty("ro.miui.ui.version.name"),
            "ro.build.version.oplusrom.display" to getSystemProperty("ro.build.version.oplusrom.display"),
            "ro.build.version.oplusrom" to getSystemProperty("ro.build.version.oplusrom"),
            "ro.build.version.opporom" to getSystemProperty("ro.build.version.opporom"),
            "ro.product.brand" to getSystemProperty("ro.product.brand"),
            "ro.product.manufacturer" to getSystemProperty("ro.product.manufacturer"),
            "ro.product.system.brand" to getSystemProperty("ro.product.system.brand"),
            "ro.product.system.manufacturer" to getSystemProperty("ro.product.system.manufacturer")
        ).mapValues { (_, value) -> value.normalizeSystemProperty() }

        xlog(
            "RootUtils.getOsFamily props=${
            properties.entries.joinToString { (key, value) -> "$key=$value" }
        }")

        val allValues = properties.values.joinToString(" ")

        val result = when {
            !properties["ro.mi.os.version.name"].isNullOrEmpty() -> OsFamily.HyperOS
            !properties["ro.build.version.oplusrom.display"].isNullOrEmpty() -> OsFamily.ColorOS
            !properties["ro.build.version.oplusrom"].isNullOrEmpty() -> OsFamily.ColorOS
            !properties["ro.build.version.opporom"].isNullOrEmpty() -> OsFamily.ColorOS
            "hyperos" in allValues || "miui" in allValues || "xiaomi" in allValues || "redmi" in allValues -> OsFamily.HyperOS
            "coloros" in allValues || "oplus" in allValues || "opporom" in allValues || "oppo" in allValues || "realme" in allValues || "oneplus" in allValues -> OsFamily.ColorOS
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
