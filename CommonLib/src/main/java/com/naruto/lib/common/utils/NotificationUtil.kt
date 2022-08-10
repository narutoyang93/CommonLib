package com.naruto.lib.common.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.naruto.lib.common.Global

/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2022/8/5 0005
 * @Note
 */
object NotificationUtil {

    fun createAction(context: Context, icon: Int, title: CharSequence, action: String)
            : NotificationCompat.Action {
        val intent = PendingIntent.getBroadcast(
            context, icon, BroadcastUtil.createBroadcastIntent(context, action, null),
            IntentUtil.defaultPendingIntentFlag()
        )
        return NotificationCompat.Action(icon, title, intent)
    }

    /**
     * 创建通知渠道
     *
     * @param context
     * @return
     */
    fun createNotificationChannel(
        context: Context, channelNameEn: String? = null, channelDesc: String? = null
    ): String {
        val channelId = "${context.packageName}.notification.channel.${channelNameEn ?: "normal"}"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            NotificationChannel(channelId, Global.appName, importance).let {
                it.description = channelDesc ?: "默认渠道"
                it.setSound(null, null)
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .createNotificationChannel(it)
            }
        }
        return channelId
    }

    fun createNotificationBuilder(
        context: Context, channelId: String = createNotificationChannel(context)
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, channelId)
    }

    fun sendNotification(
        context: Context, notificationId: Int, builder: NotificationCompat.Builder
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && builder.build().smallIcon == null)
            context.runCatching {
                packageManager.getPackageInfo(packageName, 0).applicationInfo.icon
                    .let { builder.setSmallIcon(it) }
            }
        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    fun sendNotification(
        context: Context, notificationId: Int,
        channelId: String = createNotificationChannel(context),
        block: NotificationCompat.Builder.() -> Unit
    ) {
        val builder = createNotificationBuilder(context, channelId)
        block(builder)
        sendNotification(context, notificationId, builder)
    }
}