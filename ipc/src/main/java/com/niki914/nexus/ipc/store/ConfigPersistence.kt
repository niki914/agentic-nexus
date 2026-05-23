package com.niki914.nexus.ipc.store

import android.content.Context
import android.util.AtomicFile
import com.niki914.nexus.ipc.IpcContract
import java.io.File
import java.io.FileOutputStream

internal object ConfigPersistence {

    private const val WEB_SETTINGS_FILE_NAME = "web_settings.json"
    private const val LOCAL_SETTINGS_FILE_NAME = "local_settings.json"

    fun fileFor(context: Context, store: IpcContract.Store): File {
        val fileName = when (store) {
            IpcContract.Store.WEB_SETTINGS -> WEB_SETTINGS_FILE_NAME
            IpcContract.Store.LOCAL_SETTINGS -> LOCAL_SETTINGS_FILE_NAME
        }
        return File(context.filesDir, fileName)
    }

    fun readJson(context: Context, store: IpcContract.Store): String? {
        val file = fileFor(context, store)
        return if (file.exists()) file.readText(Charsets.UTF_8) else null
    }

    fun writeJson(context: Context, store: IpcContract.Store, json: String) {
        writeTextAtomically(fileFor(context, store), json)
    }

    private fun writeTextAtomically(target: File, text: String) {
        val atomicFile = AtomicFile(target)
        var stream: FileOutputStream? = null
        try {
            stream = atomicFile.startWrite()
            stream.write(text.toByteArray(Charsets.UTF_8))
            atomicFile.finishWrite(stream)
        } catch (t: Throwable) {
            stream?.let { atomicFile.failWrite(it) }
            throw t
        }
    }
}
