package ru.quandastudio.lpsserver.core

import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext

import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component


@Component
class ContextProvider : ApplicationContextAware {

    @Throws(BeansException::class)
    override fun setApplicationContext(applicationContext: ApplicationContext?) {
        Companion.applicationContext = applicationContext
    }

    companion object {
        private var applicationContext: ApplicationContext? = null

        fun <T> getBean(cls: Class<T>): T {
            return requireNotNull(applicationContext).getBean(cls)
        }
    }
}