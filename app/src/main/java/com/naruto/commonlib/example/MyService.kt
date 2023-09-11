package com.naruto.commonlib.example

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.naruto.lib.common.base.ForegroundService
import com.naruto.lib.common.utils.LogUtils
import kotlin.concurrent.thread


/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2022/10/31 0031
 * @Note
 */
class MyService : ForegroundService() {
    override fun getPendingIntent(): PendingIntent? = null

    override fun onBind(intent: Intent?): IBinder? = null

}