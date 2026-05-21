package com.niki914.nexus.agentic.app.ui.infra.nav

import androidx.compose.runtime.Stable

@Stable
class Navigator<P : Page> internal constructor(
    private val controller: NavigationController<P>,
) {
    fun push(page: P) {
        controller.push(page)
    }

    fun pop(): Boolean {
        return controller.pop()
    }
}
