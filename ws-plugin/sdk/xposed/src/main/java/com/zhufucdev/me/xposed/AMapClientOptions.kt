package com.zhufucdev.me.xposed

import java.util.WeakHashMap

/** Capture submitted options without retaining the client or mutable option object. */
internal class AMapClientOptions {
    private val offsets = WeakHashMap<Any, Boolean>()

    @Synchronized
    fun set(client: Any, option: Any?) {
        if (option == null) return
        offsets[client] = option.javaClass.getMethod("isOffset").invoke(option) as Boolean
    }

    @Synchronized
    fun offset(client: Any): Boolean = offsets[client] ?: true
}
