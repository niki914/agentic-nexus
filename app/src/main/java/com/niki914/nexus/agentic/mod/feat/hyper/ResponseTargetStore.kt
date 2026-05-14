package com.niki914.nexus.agentic.mod.feat.hyper

import java.lang.ref.WeakReference

class ResponseTargetStore {
    private val lock = Any()
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
