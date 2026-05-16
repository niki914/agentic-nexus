package com.niki914.nexus.agentic.mod.feat.hyper

import java.lang.ref.WeakReference

/** 以 dialogId 为键缓存响应目标对象的弱引用，供 CaptureResponseTargetHook 写入、RenderTextStreamCardHook 读取。 */
class ResponseTargetStore {
    private val lock = Any() // TODO Mutex
    private val targets = linkedMapOf<String, WeakReference<Any>>()

    fun put(dialogId: String, target: Any) = synchronized(lock) {
        targets[dialogId] = WeakReference(target)
    }

    fun get(dialogId: String): Any? = synchronized(lock) {
        val reference = targets[dialogId] ?: return@synchronized null
        val target = reference.get()
        if (target == null) {
            targets.remove(dialogId)
        }
        target
    }

    fun clear(dialogId: String) = synchronized(lock) {
        targets.remove(dialogId)
    }
}
