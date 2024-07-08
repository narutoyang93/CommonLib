package com.naruto.lib.common.utils

import android.app.PendingIntent
import android.content.Intent
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

    fun createChooserIntent(title: String, intent0: Intent, vararg intentArray: Intent): Intent {
        val intent = Intent(Intent.ACTION_CHOOSER).apply {
            putExtra(Intent.EXTRA_INTENT, intent0)
            putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
        }
        return Intent.createChooser(intent, title)
    }
}