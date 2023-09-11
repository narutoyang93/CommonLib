package com.naruto.lib.common

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.util.SparseArray
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.core.util.valueIterator
import com.naruto.lib.common.activity.TaskActivity
import com.naruto.lib.common.base.BaseActivity
import com.naruto.lib.common.helper.PermissionHelper
import com.naruto.lib.common.utils.LogUtils
import com.naruto.lib.common.utils.NotificationUtil
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * @Description 全局变量及方法
 * @Author Naruto Yang
 * @CreateDate 2022/7/6 0006
 * @Note
 */
object Global {
    private var currentActivityWF: WeakReference<Activity>? = null
    private val activityMap by lazy { SparseArray<Activity>() }
    private var mActivityStartCount = 0 //Activity 启动数量
    private val operationQueue by lazy { mutableListOf<(BaseActivity) -> Unit>() }

    /**
     * 通知图标
     */
    @DrawableRes
    var notificationIcon: Int = NotificationUtil.INVALID_ICON_RES
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
    lateinit var getMainModuleContext: () -> Context

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

    var isDebug: Boolean = true

    var isKeepFontSize: Boolean = false//是否屏蔽系统字体大小设置，保持字体大小不随系统改变

    fun toast(msg: String, shortDuration: Boolean = true) {
        runOnMainThread {
            val duration = if (shortDuration) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
            Toast.makeText(getMainModuleContext(), msg, duration).show()
        }
    }

    /**
     * 当前是否处于主线程
     *
     * @return
     */
    fun isMainThread(): Boolean {
        return Looper.myLooper() == Looper.getMainLooper()
    }

    fun runOnMainThread(block: () -> Unit) {
        if (isMainThread()) block() else MainScope().launch { block() }
    }

    /**
     * 利用当前活动的Activity执行操作
     *
     * @param operation
     * @return Activity的hashCode
     */
    fun doByActivity(operation: (BaseActivity) -> Unit) {
        val activity = currentActivityWF?.get()
        if (activity == null) {
            operationQueue.add(operation)
            TaskActivity.launch(getMainModuleContext())
        } else runOnMainThread { operation(activity as BaseActivity) }
    }

    /**
     * 通过当前Activity获取数据
     * @param operation Function1<Activity, T>
     * @param def Function0<T> 如果当前没有正在运行的activity，则以此提供默认值
     * @return T
     */
    fun <T> getDataByActivity(operation: (Activity) -> T, def: () -> T): T {
        return currentActivityWF?.get()?.let { operation(it) } ?: def()
    }

    /**
     * 执行需要权限的操作
     *
     * @param callback
     */
    fun doWithPermission(callback: PermissionHelper.RequestPermissionsCallback) {
        doByActivity { activity -> activity.doWithPermission(callback) }
    }

    /**
     * 关闭所有activity
     */
    fun finishAllActivity() {
        val list = mutableListOf<WeakReference<Activity>>()
        //Activity finish会触发activityMap执行remove，故不能直接在activityMap的forEach里面执行finish
        activityMap.valueIterator().forEach { list.add(WeakReference(it)) }
        list.forEach { it.get()?.finish() }
    }


    /**
     * 关闭 TaskActivity
     */
    fun finishTaskActivity() {
        if (operationQueue.isEmpty()) {
            var activity: Activity? = null
            activityMap.valueIterator().forEach {
                if (it is TaskActivity) {
                    activity = it//Activity finish会触发activityMap执行remove，故不能直接在forEach里面执行finish
                    return@forEach
                }
            }
            activity?.finish().run { LogUtils.i("--->") }
        }
    }

    /**
     * 获取在AndroidManifest.xml中配置的metaData
     * @param context Context
     * @param key String
     * @return Result<String?>
     */
    fun getMetaData(context: Context, key: String): Result<String?> =
        context.runCatching {
            packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                .metaData.getString(key)
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
            currentActivityWF = WeakReference(activity) //记录当前正在活动的activity
            while (operationQueue.isNotEmpty()) {
                operationQueue.removeAt(0).invoke(activity as BaseActivity)
            }
        }

        override fun onActivityPaused(activity: Activity) {}

        override fun onActivityStopped(activity: Activity) {
            mActivityStartCount--
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {
            activityMap.remove(activity.hashCode())
            currentActivityWF = null
        }
    }
}

/**
 * 初始化本模块
 * @receiver Application
 */
fun Application.commonLibInit() {
    if (hasInitialized) return
    hasInitialized = true
    object : CrashHandler() {
        override fun getContext(): Context = applicationContext
    }.init()
    Global.getMainModuleContext = { applicationContext }
    registerActivityLifecycleCallbacks(Global.MyActivityLifecycleCallbacks())//监听activity生命周期
    Global.isDebug = kotlin.runCatching {
        Class.forName("$packageName.BuildConfig").getField("DEBUG").get(null) as Boolean
    }.onFailure { Log.e("naruto", "--->reflect error", it) }.getOrDefault(true)
    LogUtils.init()
}

private var hasInitialized = false