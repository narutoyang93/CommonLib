package com.naruto.commonlib.example

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.naruto.lib.common.utils.DataStoreHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2023/5/6 0006
 * @Note
 */

inline fun <reified T> DataStoreHelper.getJsonValue(key: String, def: T)
        : Flow<T> =
    getOtherValue(key, def) { Gson().fromJson(it, object : TypeToken<T>() {}.type) }
//getOtherValue(key, def) { JSON.parseObject(json, object : TypeReference<T>() {}) }

suspend inline fun <reified T>
        DataStoreHelper.writeJsonValue(key: String, def: T, block: (T) -> Unit) {
    getJsonValue(key, def).first().let { block(it);setStringValue(key, Gson().toJson(it)) }
}