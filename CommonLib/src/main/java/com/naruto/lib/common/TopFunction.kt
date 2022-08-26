package com.naruto.lib.common.TopFunction

import android.os.Build
import android.util.SparseArray
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