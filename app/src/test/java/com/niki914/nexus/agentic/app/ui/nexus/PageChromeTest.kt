package com.niki914.nexus.agentic.app.ui.nexus

import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class PageChromeTest {
    @Test
    fun emptyContribution_hasNoBackRequestHandler() {
        assertNull(PageChromeContribution.Empty.onBackRequest)
    }

    @Test
    fun contribution_retainsBackRequestHandler() {
        val onBackRequest = {}
        val contribution = PageChromeContribution(onBackRequest = onBackRequest)

        assertSame(onBackRequest, contribution.onBackRequest)
    }
}
