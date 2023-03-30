package com.naruto.lib.common.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.naruto.lib.common.Global
import com.naruto.lib.common.TopFunction.isDomesticRom

/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2022/8/5 0005
 * @Note
 */
object NotificationUtil {
    @DrawableRes
    internal const val INVALID_ICON_RES = 0

    fun createAction(
        context: Context, icon: Int, title: CharSequence, action: String,
        block: (Intent.() -> Unit)? = null
    ): NotificationCompat.Action {
        val intent = BroadcastUtil.createBroadcastIntent(context, action, null)
        block?.invoke(intent)
        val pendingIntent = PendingIntent.getBroadcast(
            context, icon, intent, IntentUtil.defaultPendingIntentFlag()
        )
        return NotificationCompat.Action(icon, title, pendingIntent)
    }

    /**
     * 创建通知渠道
     *
     * @param context
     * @return
     */
    fun createNotificationChannel(
        context: Context, channelNameEn: String? = null, channelDesc: String? = null,
        block: (NotificationChannel.() -> Unit)? = null
    ): String {
        val channelId = "${context.packageName}.notification.channel.${channelNameEn ?: "normal"}"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            NotificationChannel(channelId, Global.appName, importance).let {
                it.description = channelDesc ?: "默认渠道"
                block?.invoke(it)
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .createNotificationChannel(it)
            }
        }
        return channelId
    }

    fun createNotificationBuilder(
        context: Context, channelId: String = createNotificationChannel(context)
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, channelId).apply {
            if (!isDomesticRom() || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)//初始化时设置默认图标，允许外部覆盖
                Global.notificationIcon.let { if (it != INVALID_ICON_RES) setSmallIcon(it) }
        }
    }

    fun sendNotification(
        context: Context, notificationId: Int, builder: NotificationCompat.Builder
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && builder.build().smallIcon == null)
            context.runCatching {//发送前检测到未设置图标，要么是外部设置图标错误（可能性很低），要么就是Global.notificationIcon==INVALID_ICON_RES，所以此处只能用应用图标
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