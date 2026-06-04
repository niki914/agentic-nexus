package com.niki914.nexus.agentic.app.ui.nexus

import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class PageChromeTest {
    @Test
    fun emptyContribution_hasNoBackHandler() {
        assertNull(PageChromeContribution.Empty.backHandler)
    }

    @Test
    fun contribution_retainsBackHandler() {
        val backHandler = PageBackHandler(
            shouldConsumeBack = { true },
            onConsumeBack = {},
        )
        val contribution = PageChromeContribution(backHandler = backHandler)

        assertSame(backHandler, contribution.backHandler)
    }
}
