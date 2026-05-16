package com.niki914.nexus.agentic.mod.feat

data class HookTarget(
    val ownerClass: String,
    val methodName: String,
    val methodParams: List<String>,
    val hookTiming: String? = null,
    val hookKind: String? = null,
    val returnType: String? = null
)
