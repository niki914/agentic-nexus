package com.niki914.breeno.a.mod

import com.niki914.breeno.h.util.KVProvider
import kotlinx.coroutines.runBlocking

/**
 * 抽象的配置提供者基类，用于规范各个语音助手的配置包装器
 */
abstract class BaseConfigProvider {
    fun getString(key: String) = runBlocking { KVProvider.getString(key) }

    fun getBoolean(key: String) = runBlocking { KVProvider.getBoolean(key) }

    fun getInt(key: String) = runBlocking { KVProvider.getInt(key) }

    fun getList(key: String) = runBlocking { KVProvider.getList(key) }
}
