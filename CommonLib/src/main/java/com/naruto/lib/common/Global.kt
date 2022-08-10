package com.naruto.lib.common

import android.app.Activity
import android.app.Application
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SparseArray
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.core.util.forEach
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.naruto.lib.common.activity.TaskActivity
import com.naruto.lib.common.base.BaseActivity
import com.naruto.lib.common.utils.LogUtils
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*

/**
 * @Description 全局变量及方法
 * @Author Naruto Yang
 * @CreateDate 2022/7/6 0006
 * @Note
 */
object Global {
    private var hasInitialized = false
    private var currentActivity: WeakReference<Activity>? = null
    private val activityMap by lazy { SparseArray<Activity>() }
    private var mActivityStartCount = 0 //Activity 启动数量
    private val operationQueue by lazy { mutableListOf<(BaseActivity) -> Unit>() }

    /**
     * 通知图标
     */
    @DrawableRes
    var notificationIcon: Int = -1
/*        get() {
            if (field == -1) throw Throwable("属性未初始化，请在主Module的Application中为其赋值")
            return field
        }*/

    /**
     * 应用英文名或缩写，用于创建文件夹等，需在主Module的Application中为其赋值
     */
    lateinit var appNameEN: String

    /**
     * 主Module的全局Context，需在主Module的Application中调用commonLibInit()初始化
     */
    internal lateinit var getMainModuleContext: () -> Context

    /**
     * 应用名
     */
    val appName: CharSequence? by lazy {
/*        getMainModuleContext().run {
            getString(resources.getIdentifier("app_name", "string", packageName))
        }*/
        getMainModuleContext().runCatching {
            getString(packageManager.getPackageInfo(packageName, 0).applicationInfo.labelRes)
        }.getOrNull()
    }


    /**
     * 初始化本模块
     * @receiver Application
     */
    fun Application.commonLibInit() {
        object : CrashHandler() {
            override fun getContext(): Context = applicationContext
        }.init()
        getMainModuleContext = { applicationContext }
        registerActivityLifecycleCallbacks(MyActivityLifecycleCallbacks())//监听activity生命周期
        hasInitialized = true
    }

    fun toast(msg: String) = Toast.makeText(getMainModuleContext(), msg, Toast.LENGTH_SHORT).show()

    /**
     * 利用当前活动的Activity执行操作
     *
     * @param operation
     * @return Activity的hashCode
     */
    fun doByActivity(operation: (BaseActivity) -> Unit) {
        val activity = currentActivity?.get()
        if (activity == null) {
            operationQueue.add(operation)
            TaskActivity.launch(getMainModuleContext())
        } else operation(activity as BaseActivity)
    }

    /**
     * 执行需要权限的操作
     *
     * @param callBack
     */
    fun doWithPermission(callBack: BaseActivity.RequestPermissionsCallBack) {
        doByActivity { activity -> activity.doWithPermission(callBack) }
    }

    /**
     * 关闭所有activity
     */
    fun finishAllActivity() {
        val list = mutableListOf<WeakReference<Activity>>()
        activityMap.forEach { key, value -> list.add(WeakReference(value)) }//Activity finish会触发activityMap执行remove，故不能直接在forEach里面执行finish
        list.forEach { it.get()?.finish() }
    }


    /**
     * @Description
     * @Author Naruto Yang
     * @CreateDate 2022/7/7 0007
     * @Note
     */
    class MyActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            activityMap.put(activity.hashCode(), activity)
        }

        override fun onActivityStarted(activity: Activity) {
            mActivityStartCount++
        }

        override fun onActivityResumed(activity: Activity) {
            LogUtils.i("--->activity=$activity")
            currentActivity = WeakReference(activity) //记录当前正在活动的activity
            while (operationQueue.isNotEmpty()) {
                operationQueue[0].invoke(activity as BaseActivity)
                operationQueue.removeAt(0)
            }
        }

        override fun onActivityPaused(activity: Activity) {}

        override fun onActivityStopped(activity: Activity) {
            mActivityStartCount--
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {
            activityMap.remove(activity.hashCode())
            currentActivity = null
        }
    }
}

fun now(): Long = System.currentTimeMillis()
fun todayDate(pattern: String = "yyyy/MM/dd"): String = SimpleDateFormat(pattern).format(Date())
fun currentDateTime(pattern: String = "yyyy/MM/dd HH:mm:ss"): String =
    SimpleDateFormat(pattern).format(Date())

fun createPendingIntentFlag(flag: Int): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        PendingIntent.FLAG_IMMUTABLE or flag
    else flag
}

fun <K, V> MutableMap<out K, V>.remove(map: Map<out K, V>) {
    for ((key, _) in map) {
        remove(key)
    }
}

fun <E> SparseArray<E>.putIfAbsent(key: Int, value: E) {
    get(key) ?: kotlin.run { put(key, value) }
}


/**
 * 创建广播Intent
 */
fun createBroadcastIntent(context: Context, action: String, cls: Class<*>? = null): Intent {
    val intent = Intent(action)
    if (cls != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//8.0及以上的静态广播需要指定component
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
        LogUtils.i("--->action=$action")
        sendBroadcast(createBroadcastIntent(context, action).also { block(it) })
    }
}

fun doWithLocalBroadcast(context: Context, block: LocalBroadcastManager.() -> Unit) {
    block(LocalBroadcastManager.getInstance(context))
}

fun isDomesticRom(): Boolean {
    return when (Build.MANUFACTURER.uppercase()) {
        "HUAWEI", "HONOR", "XIAOMI", "OPPO", "VIVO" -> true
        else -> false
    }
}