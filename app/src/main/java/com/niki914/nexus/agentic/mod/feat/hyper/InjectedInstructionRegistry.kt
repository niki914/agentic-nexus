package com.niki914.nexus.agentic.mod.feat.hyper

import java.util.Collections
import java.util.WeakHashMap

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
