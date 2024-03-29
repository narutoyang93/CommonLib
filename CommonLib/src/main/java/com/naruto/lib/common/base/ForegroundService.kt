package com.naruto.lib.common.base

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.naruto.lib.common.Global
import com.naruto.lib.common.utils.NotificationUtil
import com.naruto.lib.common.utils.ServiceUtil


/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2021/12/6 0006
 * @Note
 */
abstract class ForegroundService : BaseService() {
    private val notificationManager by lazy { NotificationManagerCompat.from(this) }
    private lateinit var notificationBuilder: NotificationCompat.Builder
    protected open val notificationId = NotificationUtil.createNotificationId()

    override fun onCreate() {
        super.onCreate()
        notificationBuilder = ServiceUtil.setForegroundService(
            this, getPendingIntent(), notificationId, getNotificationIcon()
        ) { initNotification() }
    }

    /**
     * 初始化通知
     *
     * @return
     */
    protected open fun NotificationCompat.Builder.initNotification() {}

    /**
     * 更新通知
     *
     * @param operation
     */
    protected open fun updateNotification(operation: (NotificationCompat.Builder.() -> Unit)) {
        operation(notificationBuilder)
        notificationManager.notify(notificationId, notificationBuilder.build())
    }


    override fun onDestroy() {
        stopForeground(true)
        super.onDestroy()
    }

    protected abstract fun getPendingIntent(): PendingIntent?

    @DrawableRes
    protected open fun getNotificationIcon(): Int = Global.notificationIcon

    companion object {
        /**
         * 启动前台服务
         *
         * @param context
         * @param serviceIntent
         */
        fun launch(context: Context, serviceIntent: Intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }

        /**
         * 启动前台服务
         *
         * @param context
         * @param serviceClass
         */
        fun <T : ForegroundService> launch(
            context: Context, serviceClass: Class<T>, block: (Intent) -> Unit = {}
        ) {
            val intent = Intent(context, serviceClass).also { block(it) }
            launch(context, intent)
        }
    }
}