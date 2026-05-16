package com.niki914.nexus.agentic.mod.feat.hyper

import java.util.Collections
import java.util.WeakHashMap

/** 记录已注入的 Instruction 对象（弱引用），供 BlockNative*Hook 区分注入指令与原生指令。 */
class InjectedInstructionRegistry {
    private val instructions = Collections.synchronizedMap(WeakHashMap<Any, Boolean>())

    fun markInjected(instruction: Any) {
        instructions[instruction] = true
    }

    fun isInjected(instruction: Any?): Boolean {
        if (instruction == null) {
            return false
        }
        return instructions[instruction] == true
    }
}
