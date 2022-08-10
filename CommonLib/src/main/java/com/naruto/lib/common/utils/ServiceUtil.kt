package com.naruto.lib.common.utils

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import com.naruto.weather.utils.LogUtils.w

/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2021/6/24 0024
 * @Note
 */
object ServiceUtil {
    fun <T : Service> Context.stopService(serviceClass: Class<T>) {
        val intent = Intent(this, serviceClass)
        stopService(intent)
    }

    /**
     * 设置Service为前台服务
     * @param service Service
     * @param pendingIntent PendingIntent? 点击通知将执行的意图
     * @param notificationId Int 通知ID
     * @param iconRes Int 通知图标
     * @param block [@kotlin.ExtensionFunctionType] Function1<Builder, Unit>
     * @return NotificationCompat.Builder
     */
    fun setForegroundService(
        service: Service, pendingIntent: PendingIntent?, notificationId: Int,
        @DrawableRes iconRes: Int = -1, block: (NotificationCompat.Builder.() -> Unit)
    ): NotificationCompat.Builder {
        val builder = NotificationUtil.createNotificationBuilder(service)
            .setContentIntent(pendingIntent)
        if (iconRes == -1) w("--->不setSmallIcon将会导致title和contentText不显示")
        else builder.setSmallIcon(iconRes)

        block(builder)
        service.startForeground(notificationId, builder.build())
        return builder
    }

    /**
     * 设置Service为前台服务
     * @param service Service
     * @param pendingIntent PendingIntent? 点击通知将执行的意图
     * @param notificationId Int 通知ID
     * @param iconRes Int 通知图标
     * @param contentTitle String? 通知标题
     * @param contentText String? 通知内容
     * @param actions Array<out Action?> 通知操作按钮
     * @return NotificationCompat.Builder
     */
    fun setForegroundService(
        service: Service, pendingIntent: PendingIntent?, notificationId: Int,
        @DrawableRes iconRes: Int = -1, contentTitle: String? = null, contentText: String? = null,
        vararg actions: NotificationCompat.Action?
    ): NotificationCompat.Builder {
        return setForegroundService(service, pendingIntent, notificationId, iconRes) {
            contentTitle?.let { setContentTitle(it) }
            contentText?.let { setContentText(it) }
            if (actions.isNotEmpty()) actions.forEach { addAction(it) }
        }
    }
}