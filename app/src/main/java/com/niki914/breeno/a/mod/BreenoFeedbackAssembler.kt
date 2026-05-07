package com.niki914.breeno.a.mod

import com.niki914.breeno.h.util.call
import com.niki914.breeno.h.util.xlog

object BreenoFeedbackAssembler {
    fun attachIfNeeded(bean: Any) {
        val feedbackClassName = BreenoConfigProvider.feedbackInfoClass ?: return
        val footerClassName = BreenoConfigProvider.footerInfoClass ?: return
        val beanSetFeedbackInfoMethod = BreenoConfigProvider.beanSetFeedbackInfoMethod
        val feedbackSetFooterInfoMethod = BreenoConfigProvider.feedbackSetFooterInfoMethod
        val footerSetCopyFlagMethod = BreenoConfigProvider.footerInfoSetCopyFlagMethod
        val footerSetUpvoteFlagMethod = BreenoConfigProvider.footerInfoSetUpvoteFlagMethod

        val classLoader = bean.javaClass.classLoader
        val feedback = instantiate(feedbackClassName, classLoader) ?: return
        val footer = instantiate(footerClassName, classLoader) ?: return

        footer.call<Unit>(footerSetCopyFlagMethod, BreenoConfigProvider.feedbackCopyFlagEnabled)
        footer.call<Unit>(footerSetUpvoteFlagMethod, BreenoConfigProvider.feedbackUpvoteFlagEnabled)
        feedback.call<Unit>(feedbackSetFooterInfoMethod, footer)
        bean.call<Unit>(beanSetFeedbackInfoMethod, feedback)
    }

    private fun instantiate(className: String, classLoader: ClassLoader?): Any? = runCatching {
        val clazz = Class.forName(className, false, classLoader)
        clazz.getDeclaredConstructor().newInstance()
    }.onFailure {
        xlog("[BreenoFeedbackAssembler] instantiate failed: $className, ${it.message}")
    }.getOrNull()
}
