package com.naruto.lib.common.utils

import android.util.Log
import com.naruto.lib.common.Extension.remove
import com.naruto.lib.common.Global
import com.naruto.lib.common.TopFunction.currentDateTime
import com.naruto.lib.common.TopFunction.runInCoroutine
import com.naruto.lib.common.TopFunction.todayDate

/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2021/12/15 0015
 * @Note
 */
object LogUtils {
    private const val DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss:SSS"
    private val defTag by lazy {
        Global.runCatching { appNameEN }.getOrDefault(Global.appName).toString()
    }
    private val logMap by lazy { mutableMapOf<String, StringBuilder>() }
    private fun log(msg: String, block: ((String, String) -> Unit)) {
        Throwable().stackTrace[2].run {
            //block(className, "$msg[$className.$methodName($fileName:$lineNumber)]")
            val content = "[${toString()}]$msg"
            if (Global.isDebug) block(defTag, content)
            else {
                val stringBuilder = logMap.getOrPut(getTodayDate()) { StringBuilder() }
                stringBuilder.append("${currentDateTime(DATETIME_FORMAT)} $content\n")
            }
        }
    }

    fun v(msg: String) {
        log(msg) { tag, m -> Log.v(tag, m) }
    }

    fun d(msg: String) {
        log(msg) { tag, m -> Log.d(tag, m) }
    }

    fun i(msg: String) {
        log(msg) { tag, m -> Log.i(tag, m) }
    }

    fun w(msg: String) {
        log(msg) { tag, m -> Log.w(tag, m) }
    }

    fun e(msg: String) {
        log(msg) { tag, m -> Log.e(tag, m) }
    }

    fun e(msg: String, tr: Throwable) {
        log(msg) { tag, m -> Log.e(tag, m, tr) }
    }

    fun writeToFile() {
        if (Global.isDebug) return
        runInCoroutine {
            for ((date, stringBuilder) in logMap) {
                if (stringBuilder.isNotEmpty()) {
                    val backupSB = StringBuilder(stringBuilder)//备份
                    stringBuilder.clear()
                    FileUtil.writeDataToExternalPublicSpaceFile(
                        backupSB.toString().toByteArray(), FileUtil.MediaType.FILE, "log/",
                        "$date.txt", true
                    ) { if (!it) stringBuilder.insert(0, backupSB) }
                }
            }
            logMap.remove(logMap.filter { it.key != getTodayDate() && it.value.isEmpty() })
        }
    }

    private fun getTodayDate() = todayDate("yyyy-MM-dd")
}