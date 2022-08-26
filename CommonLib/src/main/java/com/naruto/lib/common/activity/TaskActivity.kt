package com.naruto.lib.common.activity

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import com.naruto.lib.common.R
import com.naruto.lib.common.base.BaseActivity
import com.naruto.lib.common.utils.LogUtils


open class TaskActivity : BaseActivity() {

    override fun init() {

/*        window.setGravity(Gravity.LEFT or Gravity.TOP)
        val params: WindowManager.LayoutParams = window.attributes
        params.x = 0
        params.y = 0
        params.height = 200
        params.width = 200
        window.attributes = params*/

        LogUtils.i("--->")
        intent.extras?.run {
            if (getBoolean(INTENT_KEY_IS_FROM_SYSTEM, true)) {//是系统启动的
                getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                    .takeIf { it != AppWidgetManager.INVALID_APPWIDGET_ID }?.let {
                        val resultValue = Intent()
                        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, it)
                        setResult(RESULT_OK, resultValue)//添加小部件时需要返回值
                        onAppWidgetLaunching()
                    }
            } else {
                LogUtils.w("--->非系统启动")
            }
        }
        isRunning = true
    }

    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
    }

    override fun getLayoutRes() = R.layout.activity_task

    /**
     * 小部件启动时（只有利用TaskActivity启动小部件才会回调）
     */
    open fun onAppWidgetLaunching() {}

    companion object {
        var isRunning = false
        private const val INTENT_KEY_IS_FROM_SYSTEM = "from_system"
        fun launch(context: Context, block: (Intent) -> Unit = {}) {
            val intent = Intent(context, TaskActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra(INTENT_KEY_IS_FROM_SYSTEM, false)
            block(intent)
            context.startActivity(intent)
        }
    }
}