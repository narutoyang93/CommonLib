package com.naruto.lib.common.TopFunction

import android.os.Build
import com.naruto.lib.common.utils.LogUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2022/8/26 0026
 * @Note
 */
fun now(): Long = System.currentTimeMillis()
fun todayDate(pattern: String = "yyyy/MM/dd"): String = SimpleDateFormat(pattern).format(Date())
fun currentDateTime(pattern: String = "yyyy/MM/dd HH:mm:ss"): String =
    SimpleDateFormat(pattern).format(Date())

fun isDomesticRom(): Boolean {
    return when (Build.MANUFACTURER.uppercase()) {
        "HUAWEI", "HONOR", "XIAOMI", "OPPO", "VIVO" -> true
        else -> false
    }
}

/**
 * 获取DataStore数据并监听变化
 * @param key String
 * @param func Function1<String, Flow<T>> 获取DataStore数据的方法，例如{ CommonDataStore.getLongValue(it, 0) }
 * @param callback Function1<T, Unit>
 */
fun <T> listenDataStoreDataChange(
    key: String, func: (String) -> Flow<T>, callback: (T) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        func(key).collect {
            callback(it)
            LogUtils.i("--->$key has changed：$it")
        }
    }
}