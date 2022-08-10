package com.naruto.lib.common.utils

import android.app.PendingIntent
import android.os.Build

/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2022/8/4 0004
 * @Note
 */
object IntentUtil {

    fun createPendingIntentFlag(flag: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE or flag else flag
    }

    fun defaultPendingIntentFlag(): Int {
        return createPendingIntentFlag(PendingIntent.FLAG_UPDATE_CURRENT)
    }

}