package com.niki914.nexus.agentic.mod.feat.oppo

import com.niki914.nexus.h.util.call
import com.niki914.nexus.h.util.xTry

object BreenoFeedbackAssembler {
    fun attachIfNeeded(bean: Any) {
        val feedbackClassName = BreenoConfigProvider.RenderCard.feedbackInfoClass ?: return
        val footerClassName = BreenoConfigProvider.RenderCard.footerInfoClass ?: return
        val beanSetFeedbackInfoMethod = BreenoConfigProvider.RenderCard.beanSetFeedbackInfoMethod ?: return
        val feedbackSetFooterInfoMethod = BreenoConfigProvider.RenderCard.feedbackSetFooterInfoMethod ?: return
        val footerSetCopyFlagMethod = BreenoConfigProvider.RenderCard.footerInfoSetCopyFlagMethod ?: return
        val footerSetUpvoteFlagMethod = BreenoConfigProvider.RenderCard.footerInfoSetUpvoteFlagMethod ?: return

        val classLoader = bean.javaClass.classLoader
        val feedback = instantiate(feedbackClassName, classLoader) ?: return
        val footer = instantiate(footerClassName, classLoader) ?: return

        footer.call<Unit>(footerSetCopyFlagMethod, BreenoConfigProvider.RenderCard.feedbackCopyFlagEnabled)
        footer.call<Unit>(footerSetUpvoteFlagMethod, BreenoConfigProvider.RenderCard.feedbackUpvoteFlagEnabled)
        feedback.call<Unit>(feedbackSetFooterInfoMethod, footer)
        bean.call<Unit>(beanSetFeedbackInfoMethod, feedback)
    }

    private fun instantiate(className: String, classLoader: ClassLoader?): Any? = xTry {
        val clazz = Class.forName(className, false, classLoader)
        clazz.getDeclaredConstructor().newInstance()
    }
}
