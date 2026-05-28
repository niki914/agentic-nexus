package com.niki914.nexus.agentic.runtime

import com.niki914.nexus.agentic.repo.XRepoRuntimeGateway
import com.niki914.nexus.agentic.runtime.settings.RuntimeBridge

fun createAppRuntimeBridge(): RuntimeBridge {
    return RuntimeBridge(
        settings = XRepoRuntimeGateway(),
        host = IpcRuntimeHostGateway(),
    )
}
