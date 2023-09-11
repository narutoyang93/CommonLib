package com.naruto.commonlib.example

import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.naruto.commonlib.example.databinding.ActivityMainBinding
import com.naruto.lib.common.Extension.getAndroidID
import com.naruto.lib.common.base.DataBindingActivity
import com.naruto.lib.common.helper.OpenMapRouteHelper
import com.naruto.lib.common.utils.LogUtils
import java.util.Timer
import kotlin.concurrent.schedule

private const val BROADCAST_ACTION_CANCEL_TASK = "cancel_task"
private const val EXTRA_KEY_NOTIFICATION_ID = "notification_id"

class MainActivity : DataBindingActivity<ActivityMainBinding>() {
    private val edittext by lazy { findViewById<EditText>(R.id.et_test) }
    private val openMapRouteHelper by lazy { OpenMapRouteHelper(this) }
    override fun init() {
        getAndroidID { findViewById<TextView>(R.id.tv_id).text = it }
        findViewById<Button>(R.id.btn_test).setOnClickListener {
//            openMapRouteHelper.openMapRoute(22.5442428589,113.950340271,"什么鬼地方")
            showLoadingDialog()
            Timer().schedule(2000){dismissLoadingDialog()}
        }
        findViewById<Button>(R.id.btn_add).setOnClickListener {

        }
    }

    override fun getLayoutRes(): Int = R.layout.activity_main

    override fun onResume() {
        super.onResume()
        LogUtils.i("--->")
    }

    override fun onStop() {
        super.onStop()
        LogUtils.i("--->")
        LogUtils.writeToFile()
    }

    override fun onPause() {
        super.onPause()
        LogUtils.i("--->")
    }

    override fun onDestroy() {
        LogUtils.i("--->")
        super.onDestroy()
    }

    /**
     * @Description
     * @Author Naruto Yang
     * @CreateDate 2023/5/6 0006
     * @Note
     */
    private class SimpleLruCache<K, V>(
        private val maxSize: Int,
        val map: LinkedHashMap<K, V> = linkedMapOf()
    ) {
        @Synchronized
        fun put(key: K, value: V) {
            when {
                map.isEmpty() -> {}
                map.containsKey(key) -> map.remove(key)
                map.size >= maxSize -> map.remove(map.entries.first().key)
            }
            map[key] = value
        }
    }
}