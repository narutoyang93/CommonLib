package com.naruto.lib.common

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.SparseArray
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.core.util.valueIterator
import com.naruto.lib.common.activity.TaskActivity
import com.naruto.lib.common.base.BaseActivity
import com.naruto.lib.common.utils.LogUtils
import java.lang.ref.WeakReference

/**
 * @Description 全局变量及方法
 * @Author Naruto Yang
 * @CreateDate 2022/7/6 0006
 * @Note
 */
object Global {
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

    var isDebug: Boolean = true

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
            currentActivity = null
        }
    }
}

/**
 * 初始化本模块
 * @receiver Application
 */
fun Application.commonLibInit() {
    object : CrashHandler() {
        override fun getContext(): Context = applicationContext
    }.init()
    Global.getMainModuleContext = { applicationContext }
    registerActivityLifecycleCallbacks(Global.MyActivityLifecycleCallbacks())//监听activity生命周期
    Global.isDebug = kotlin.runCatching {
        Class.forName("$packageName.BuildConfig").getField("DEBUG").get(null) as Boolean
    }.getOrDefault(true)
}