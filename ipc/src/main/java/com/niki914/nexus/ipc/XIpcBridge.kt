package com.niki914.nexus.ipc

import android.content.Context
import android.net.Uri
import android.os.Bundle
import com.niki914.nexus.ipc.store.StoreDescriptorRegistry
import com.niki914.nexus.ipc.store.XIpcStoreRepository
import java.io.FileNotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * `XIpcBridge` 是 `:ipc` 对外暴露的统一桥接入口，用来把"同一套能力在两个进程里运行"
 * 这件事压平到调用方几乎无感。
 *
 * 当前工程里，同一份业务代码会同时运行在两个 App / 进程中：
 * - Nexus 主 App 进程：真正拥有配置持久化能力和通知发送能力
 * - 宿主进程（例如被 Hook 的目标 App）：只能借助 `ContentProvider` 把请求转发回 Nexus
 *
 * 这里最容易让后人误解的一点是：宿主进程虽然也能拿到一个 `Context`，但这个 `Context`
 * 绝不能直接承担配置持久化和通知发送的最终执行职责。宿主 `Context` 在这里的唯一用途，
 * 是作为 IPC 发起端调用 `SettingsContentProvider`。真正的持久化和通知，最终都必须落到
 * Nexus 进程自己的 `Context` 上执行。
 *
 * 因此本类做的事情很单纯：
 * - 如果传入的是宿主 `Context`，就走 `ContentResolver.call()` 把请求转发给 Nexus
 * - 如果传入的是 Nexus `Context`，就直接调用本地 helper 完成持久化或通知
 *
 * 这样调用方只需要记住一条规则：始终把"当前手里的 `Context`"传进来即可，不需要自己关心
 * 现在是不是宿主进程、是否要走 provider、最终是谁在执行落盘或发通知。
 *
 * 额外约束：
 * - 本类只负责"路由"和少量 JSON 级别的读改写，不负责业务模型转换
 * - `XService` 之类的高层 facade 应该依赖本类，而不是直接碰 `ContentResolver.call()`
 * - `SettingsContentProvider` 仍然只是 IPC 边界；它负责分发，真正执行仍然下沉到 helper
 *
 * 设计意图是把"宿主多绕一跳、主 App 直接执行"统一成同一个 API 面，从而让跨进程和单进程
 * 的代码路径在语义上保持一致。
 */
object XIpcBridge {

    @Volatile
    private var cachedWebSettingsJson: String? = null
    private val webSettingsCacheMutex = Mutex()

    private sealed interface IpcCallResult {
        data class Success(val bundle: Bundle?) : IpcCallResult
        data object Unreachable : IpcCallResult
    }

    suspend fun readWebSettingsJson(context: Context): IpcReadResult {
        return readStoreJson(context, StoreDescriptorRegistry.WEB_SETTINGS_ID)
    }

    suspend fun writeWebSettingsJson(context: Context, json: String): IpcWriteResult {
        return writeStoreJsonFromOwner(context, StoreDescriptorRegistry.WEB_SETTINGS_ID, json)
    }

    suspend fun writeWebSetting(context: Context, key: String, value: Any?): IpcMutateResult {
        return mutateStoreJson(context, StoreDescriptorRegistry.WEB_SETTINGS_ID, key, value)
    }

    suspend fun readLocalSettingsJson(context: Context): IpcReadResult {
        return readJson(context, IpcContract.Store.LOCAL_SETTINGS)
    }

    suspend fun writeLocalSettingsJson(context: Context, json: String): IpcWriteResult {
        return writeJson(context, IpcContract.Store.LOCAL_SETTINGS, json)
    }

    suspend fun writeLocalSetting(context: Context, key: String, value: Any?): IpcMutateResult {
        return mutateSetting(context, IpcContract.Store.LOCAL_SETTINGS, key, value)
    }

    suspend fun readStoreJson(
        context: Context,
        storeId: String
    ): IpcReadResult {
        StoreDescriptorRegistry.resolveDynamic(storeId) ?: return IpcReadResult.NotFound
        if (storeId == StoreDescriptorRegistry.WEB_SETTINGS_ID) {
            cachedWebSettingsJson?.let { return IpcReadResult.Success(it) }
            return webSettingsCacheMutex.withLock {
                cachedWebSettingsJson?.let { return@withLock IpcReadResult.Success(it) }
                val result = readStoreJsonUncached(context, storeId)
                if (result is IpcReadResult.Success) {
                    cachedWebSettingsJson = result.json
                }
                result
            }
        }
        return readStoreJsonUncached(context, storeId)
    }

    suspend fun mutateStoreJson(
        context: Context,
        storeId: String,
        path: String,
        value: Any?
    ): IpcMutateResult {
        StoreDescriptorRegistry.resolveDynamic(storeId)
            ?: return IpcMutateResult(IpcWriteResult.Unreachable, null)
        val valueJson = serializeValue(value)
        val result = if (shouldUseProvider(context)) {
            withContext(Dispatchers.IO) {
                mutateJsonViaProvider(context, storeId, path, valueJson)
            }
        } else {
            val updatedJson = XIpcStoreRepository.mutateJson(
                context = context,
                storeId = storeId,
                path = path,
                valueJson = valueJson
            )
            IpcMutateResult(IpcWriteResult.Success, updatedJson)
        }
        if (result.writeResult is IpcWriteResult.Success && result.updatedJson != null) {
            updateWebCacheIfNeeded(storeId, result.updatedJson)
        }
        return result
    }

    suspend fun writeStoreJsonFromOwner(
        context: Context,
        storeId: String,
        json: String
    ): IpcWriteResult {
        StoreDescriptorRegistry.resolveDynamic(storeId) ?: return IpcWriteResult.Unreachable
        if (shouldUseProvider(context)) {
            return writeStoreJsonViaProviderStream(context, storeId, json)
        }
        XIpcStoreRepository.writeJson(context, storeId, json)
        updateWebCacheIfNeeded(storeId, json)
        return IpcWriteResult.Success
    }

    private suspend fun writeStoreJsonViaProviderStream(
        context: Context,
        storeId: String,
        json: String
    ): IpcWriteResult {
        return withContext(Dispatchers.IO) {
            var delayMs = 250L
            repeat(4) { attempt ->
                try {
                    context.contentResolver
                        .openOutputStream(IpcContract.storeFileUri(storeId), "wt")
                        ?.use { output ->
                            output.write(json.toByteArray(Charsets.UTF_8))
                            output.flush()
                        }
                        ?: return@withContext IpcWriteResult.Unreachable
                    return@withContext if (waitForProviderWrite(context, storeId, json)) {
                        updateWebCacheIfNeeded(storeId, json)
                        IpcWriteResult.Success
                    } else {
                        IpcWriteResult.Unreachable
                    }
                } catch (e: IllegalArgumentException) {
                    if (!e.isUnknownAuthority() || attempt == 3) {
                        return@withContext IpcWriteResult.Unreachable
                    }
                } catch (_: SecurityException) {
                    return@withContext IpcWriteResult.Unreachable
                } catch (_: Exception) {
                    return@withContext IpcWriteResult.Unreachable
                }
                delay(delayMs)
                delayMs *= 2
            }
            IpcWriteResult.Unreachable
        }
    }

    private suspend fun waitForProviderWrite(
        context: Context,
        storeId: String,
        expectedJson: String
    ): Boolean {
        repeat(25) {
            if (readJsonViaProviderFile(context, storeId) == IpcReadResult.Success(expectedJson)) {
                return true
            }
            delay(20L)
        }
        return false
    }

    suspend fun readJson(
        context: Context,
        store: IpcContract.Store
    ): IpcReadResult {
        if (store == IpcContract.Store.WEB_SETTINGS) {
            cachedWebSettingsJson?.let { return IpcReadResult.Success(it) }
            return webSettingsCacheMutex.withLock {
                cachedWebSettingsJson?.let { return@withLock IpcReadResult.Success(it) }
                val result = readStoreJsonUncached(context, store)
                if (result is IpcReadResult.Success) {
                    cachedWebSettingsJson = result.json
                }
                result
            }
        }
        return readStoreJsonUncached(context, store)
    }

    suspend fun writeJson(
        context: Context,
        store: IpcContract.Store,
        json: String
    ): IpcWriteResult {
        val result = if (shouldUseProvider(context)) {
            IpcWriteResult.Unreachable
        } else {
            XIpcStoreRepository.writeJson(context, store, json)
            IpcWriteResult.Success
        }
        if (result is IpcWriteResult.Success) {
            updateWebCacheIfNeeded(store, json)
        }
        return result
    }

    suspend fun mutateSetting(
        context: Context,
        store: IpcContract.Store,
        key: String,
        value: Any?
    ): IpcMutateResult {
        val valueJson = serializeValue(value)
        val result = if (shouldUseProvider(context)) {
            withContext(Dispatchers.IO) {
                mutateJsonViaProvider(context, store, key, valueJson)
            }
        } else {
            val updatedJson = XIpcStoreRepository.mutateJson(
                context = context,
                store = store,
                path = key,
                valueJson = valueJson
            )
            IpcMutateResult(IpcWriteResult.Success, updatedJson)
        }
        if (result.writeResult is IpcWriteResult.Success && result.updatedJson != null) {
            updateWebCacheIfNeeded(store, result.updatedJson)
        }
        return result
    }

    suspend fun postNotification(
        context: Context,
        title: String,
        content: String,
        uri: String?
    ): IpcWriteResult {
        return if (shouldUseProvider(context)) {
            val result = withContext(Dispatchers.IO) {
                callProvider(
                    context = context,
                    method = IpcContract.Method.POST_NOTIFICATION,
                    extras = bridgeBundleOf(
                        IpcContract.Field.TITLE to title,
                        IpcContract.Field.CONTENT to content,
                        IpcContract.Field.URI to uri
                    )
                )
            }
            when (result) {
                is IpcCallResult.Success -> {
                    if (result.bundle?.isSuccessBundle() == true) {
                        IpcWriteResult.Success
                    } else {
                        IpcWriteResult.Unreachable
                    }
                }
                is IpcCallResult.Unreachable -> IpcWriteResult.Unreachable
            }
        } else {
            withContext(Dispatchers.IO) {
                XNotificationBridge.post(context, title, content, uri)
            }
            IpcWriteResult.Success
        }
    }

    private suspend fun readStoreJsonUncached(
        context: Context,
        store: IpcContract.Store
    ): IpcReadResult {
        return if (shouldUseProvider(context)) {
            withContext(Dispatchers.IO) {
                readJsonViaProviderFile(context, store)
            }
        } else {
            IpcReadResult.Success(XIpcStoreRepository.readJson(context, store))
        }
    }

    private suspend fun readStoreJsonUncached(
        context: Context,
        storeId: String
    ): IpcReadResult {
        return if (shouldUseProvider(context)) {
            withContext(Dispatchers.IO) {
                readJsonViaProviderFile(context, storeId)
            }
        } else {
            IpcReadResult.Success(XIpcStoreRepository.readJson(context, storeId))
        }
    }

    private suspend fun readJsonViaProviderFile(
        context: Context,
        store: IpcContract.Store
    ): IpcReadResult {
        return readJsonViaProviderFile(context, store.fileUri, store.storeId)
    }

    private fun validatedProviderJson(
        storeId: String,
        json: String
    ): IpcReadResult {
        val descriptor = StoreDescriptorRegistry.resolveDynamic(storeId) ?: return IpcReadResult.Unreachable
        return if (runCatching { JSONObject(json) }.isSuccess) {
            IpcReadResult.Success(json)
        } else {
            IpcReadResult.Success(descriptor.defaultJson)
        }
    }

    private suspend fun readJsonViaProviderFile(
        context: Context,
        storeId: String
    ): IpcReadResult {
        return readJsonViaProviderFile(context, IpcContract.storeFileUri(storeId), storeId)
    }

    private suspend fun readJsonViaProviderFile(
        context: Context,
        uri: Uri,
        storeId: String
    ): IpcReadResult {
        var delayMs = 250L
        repeat(4) { attempt ->
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return IpcReadResult.NotFound
                val json = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                return if (json.isBlank()) {
                    IpcReadResult.NotFound
                } else {
                    validatedProviderJson(storeId, json)
                }
            } catch (_: FileNotFoundException) {
                return IpcReadResult.NotFound
            } catch (e: IllegalArgumentException) {
                if (!e.isUnknownAuthority() || attempt == 3) {
                    return IpcReadResult.Unreachable
                }
            } catch (_: SecurityException) {
                return IpcReadResult.Unreachable
            }
            delay(delayMs)
            delayMs *= 2
        }
        return IpcReadResult.Unreachable
    }

    private suspend fun writeJsonViaProvider(
        context: Context,
        store: IpcContract.Store,
        json: String
    ): IpcWriteResult {
        return when (val result = callProvider(
            context = context,
            method = store.writeMethod,
            extras = bridgeBundleOf(store.payloadField to json)
        )) {
            is IpcCallResult.Success -> {
                if (result.bundle?.isSuccessBundle() == true) {
                    IpcWriteResult.Success
                } else {
                    IpcWriteResult.Unreachable
                }
            }
            is IpcCallResult.Unreachable -> IpcWriteResult.Unreachable
        }
    }

    private suspend fun mutateJsonViaProvider(
        context: Context,
        store: IpcContract.Store,
        key: String,
        valueJson: String
    ): IpcMutateResult {
        when (callProvider(
            context = context,
            method = store.mutateMethod,
            extras = bridgeBundleOf(
                IpcContract.Field.PATH to key,
                IpcContract.Field.VALUE_JSON to valueJson
            )
        )) {
            is IpcCallResult.Unreachable -> return IpcMutateResult(IpcWriteResult.Unreachable, null)
            is IpcCallResult.Success -> { /* continue to read back */ }
        }
        val readResult = readJsonViaProviderFile(context, store)
        val json = (readResult as? IpcReadResult.Success)?.json ?: EMPTY_JSON
        return IpcMutateResult(IpcWriteResult.Success, json)
    }

    private suspend fun mutateJsonViaProvider(
        context: Context,
        storeId: String,
        path: String,
        valueJson: String
    ): IpcMutateResult {
        val callResult = callProvider(
            context = context,
            method = IpcContract.Method.MUTATE_STORE,
            extras = bridgeBundleOf(
                IpcContract.Field.STORE_ID to storeId,
                IpcContract.Field.PATH to path,
                IpcContract.Field.VALUE_JSON to valueJson
            )
        )
        when (callResult) {
            is IpcCallResult.Unreachable -> return IpcMutateResult(IpcWriteResult.Unreachable, null)
            is IpcCallResult.Success -> {
                if (callResult.bundle?.isSuccessBundle() != true) {
                    return IpcMutateResult(IpcWriteResult.Unreachable, null)
                }
            }
        }

        val readResult = readJsonViaProviderFile(context, storeId)
        val json = (readResult as? IpcReadResult.Success)?.json ?: EMPTY_JSON
        return IpcMutateResult(IpcWriteResult.Success, json)
    }

    private suspend fun callProvider(
        context: Context,
        method: IpcContract.Method,
        extras: Bundle? = null
    ): IpcCallResult {
        var delayMs = 250L
        repeat(4) { attempt ->
            try {
                return IpcCallResult.Success(
                    context.contentResolver.call(
                        IpcContract.CONTENT_URI,
                        method.wireName,
                        null,
                        extras
                    )
                )
            } catch (e: IllegalArgumentException) {
                if (!e.isUnknownAuthority() || attempt == 3) {
                    return IpcCallResult.Unreachable
                }
            } catch (_: SecurityException) {
                return IpcCallResult.Unreachable
            }
            delay(delayMs)
            delayMs *= 2
        }
        return IpcCallResult.Unreachable
    }

    private fun Throwable.isUnknownAuthority(): Boolean {
        return message?.contains("Unknown authority", ignoreCase = true) == true
    }

    private fun updateWebCacheIfNeeded(
        store: IpcContract.Store,
        json: String
    ) {
        if (store == IpcContract.Store.WEB_SETTINGS) {
            cachedWebSettingsJson = json
        }
    }

    private fun updateWebCacheIfNeeded(
        storeId: String,
        json: String
    ) {
        if (storeId == StoreDescriptorRegistry.WEB_SETTINGS_ID) {
            cachedWebSettingsJson = json
        }
    }

    private fun shouldUseProvider(context: Context): Boolean {
        return when (XValues.getAppTypeOf(context)) {
            XValues.AppType.Host -> true
            XValues.AppType.Me -> false
            XValues.AppType.Unknown -> throw IllegalStateException(
                "XIpcBridge does not support package=${context.packageName}"
            )
        }
    }

    private fun serializeValue(value: Any?): String {
        return when (val jsonValue = toJsonValue(value)) {
            JSONObject.NULL -> "null"
            is String -> JSONObject.quote(jsonValue)
            else -> jsonValue.toString()
        }
    }

    private fun bridgeBundleOf(vararg pairs: Pair<IpcContract.Field, Any?>): Bundle {
        return Bundle().apply {
            pairs.forEach { (field, value) ->
                when (value) {
                    null -> putString(field.wireName, null)
                    is String -> putString(field.wireName, value)
                    is Boolean -> putBoolean(field.wireName, value)
                    else -> error("Unsupported bundle type for field=${field.name}")
                }
            }
        }
    }

    private fun Bundle.isSuccessBundle(): Boolean {
        return getBoolean(IpcContract.Field.SUCCESS.wireName)
    }

    private const val EMPTY_JSON = "{}"

    private fun toJsonValue(value: Any?): Any {
        return when (value) {
            null -> JSONObject.NULL
            is JSONObject -> value
            is JSONArray -> value
            is Boolean, is Number, is String -> value
            is Map<*, *> -> JSONObject().apply {
                value.forEach { (key, nestedValue) ->
                    if (key != null) {
                        put(key.toString(), toJsonValue(nestedValue))
                    }
                }
            }

            is Iterable<*> -> JSONArray().apply {
                value.forEach { put(toJsonValue(it)) }
            }

            is Array<*> -> JSONArray().apply {
                value.forEach { put(toJsonValue(it)) }
            }

            else -> value.toString()
        }
    }
}
