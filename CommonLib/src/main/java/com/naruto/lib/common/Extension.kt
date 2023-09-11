package com.naruto.lib.common.Extension

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.SparseArray
import com.naruto.lib.common.utils.FileUtil
import kotlinx.coroutines.runBlocking
import java.nio.charset.Charset
import java.util.*

/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2022/8/26 0026
 * @Note
 */

fun <K, V> MutableMap<out K, V>.remove(map: Map<out K, V>) {
    for ((key, _) in map) {
        remove(key)
    }
}

fun <E> SparseArray<E>.putIfAbsent(key: Int, value: E) {
    get(key) ?: kotlin.run { put(key, value) }
}

fun <E> SparseArray<E>.getOrPut(key: Int, defaultValue: () -> E): E {
    return get(key) ?: defaultValue().also { put(key, it) }
}

private var AndroidID: String? = null

@SuppressLint("HardwareIds")
fun Context.getAndroidID(callback: (String) -> Unit) {
    if (AndroidID == null)
        AndroidID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            .takeIf { it != "9774d56d682e549c" }
    AndroidID?.also(callback) ?: (getUUIDFromFile("androidId.txt") { AndroidID = it;callback(it) })
}

/**
 * 获取保存在文件的UUID
 * @return String
 */
fun getUUIDFromFile(fileName: String, callback: (String) -> Unit) {
    val mediaType = FileUtil.MediaType.FILE
    FileUtil.getFileInExternalPublicSpace(mediaType, "", fileName)
        ?.also { uri ->
            FileUtil.readDataFromFile(uri) {
                val id = it?.run {
                    readBytes().toString(Charset.defaultCharset()).takeIf { s -> s.isNotEmpty() }
                } ?: createUUID(mediaType, fileName)
                callback(id)
            }
        }
        ?: callback(createUUID(mediaType, fileName))
}


fun createUUID(mediaType: FileUtil.MediaType, fileName: String): String {
    return UUID.randomUUID().toString().also {
        runBlocking {
            FileUtil.writeDataToExternalPublicSpaceFile(
                it.toByteArray(), mediaType, "", fileName, false, null
            )
        }
    }
}