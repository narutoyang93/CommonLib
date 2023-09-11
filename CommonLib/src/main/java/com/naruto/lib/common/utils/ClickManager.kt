package com.naruto.lib.common.utils

import android.util.Log
import android.view.View

/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2023/8/3 0003
 * @Note
 */
const val clickInterval: Long = 500

@Volatile
var lastClickTime: Long = 0

inline fun <T : View> T.setMyOnClickListener(crossinline block: ((T) -> Unit)) {
    setOnClickListener { v ->
        (v as T).doClick(block)
    }
}

fun <T : View> T.setMyOnClickListener(onClickListener: View.OnClickListener) {
    setOnClickListener { v ->
        (v as T).doClick { view -> onClickListener.onClick(view) }
    }
}

inline fun <T : View> T.doClick(block: ((T) -> Unit)) {
    val time = System.currentTimeMillis()
    if (time - lastClickTime >= clickInterval) {
        lastClickTime = time
        block(this)
    } else Log.e("ClickLimit", "--->setMyOnClickListener: ", Exception("$clickInterval ms内只允许一次点击事件"));
}