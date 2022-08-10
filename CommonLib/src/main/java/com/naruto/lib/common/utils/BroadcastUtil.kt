package com.naruto.lib.common.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.naruto.weather.utils.LogUtils

/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2022/8/8 0008
 * @Note
 */
object BroadcastUtil {

    /**
     * 创建广播Intent
     */
    fun createBroadcastIntent(context: Context, action: String, cls: Class<*>? = null): Intent {
        val intent = Intent(action)
        if (cls != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//8.0及以上的静态注册的广播需要指定component，非静态注册的广播cls必须为null
            intent.component = ComponentName(context, cls)
        }
        LogUtils.i("--->create action $action")
        return intent
    }

    /**
     * 发送本地广播
     */
    fun sendLocalBroadcast(context: Context, action: String, block: (Intent) -> Unit = {}) {
        doWithLocalBroadcast(context) {
            sendBroadcast(createBroadcastIntent(context, action).also { block(it) })
        }
    }

    fun doWithLocalBroadcast(context: Context, block: LocalBroadcastManager.() -> Unit) {
        block(LocalBroadcastManager.getInstance(context))
    }
}