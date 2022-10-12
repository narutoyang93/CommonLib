package com.naruto.lib.common.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.naruto.lib.common.Global
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2021/12/3 0003
 * @Note
 */
const val DEF_STRING = ""
const val DEF_INT = -1
const val DEF_LONG: Long = -1
const val DEF_FLOAT = -1.0f
const val DEF_DOUBLE = -1.0
const val DEF_BOOLEAN = false

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "MyData")

object CommonDataStore : DataStoreHelper(Global.getMainModuleContext().dataStore)

/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2022/4/28 0028
 * @Note
 */
open class DataStoreHelper(private val dataStore: DataStore<Preferences>) {
    fun getIntValue(key: String, def: Int = DEF_INT): Flow<Int> {
        return getValue(intPreferencesKey(key), def)
    }

    suspend fun setIntValue(key: String, value: Int) {
        setValue(intPreferencesKey(key), value)
    }

    fun getFloatValue(key: String, def: Float = DEF_FLOAT): Flow<Float> {
        return getValue(floatPreferencesKey(key), def)
    }

    suspend fun setFloatValue(key: String, value: Float) {
        setValue(floatPreferencesKey(key), value)
    }

    fun getLongValue(key: String, def: Long = DEF_LONG): Flow<Long> {
        return getValue(longPreferencesKey(key), def)
    }

    suspend fun setLongValue(key: String, value: Long) {
        setValue(longPreferencesKey(key), value)
    }

    fun getDoubleValue(key: String, def: Double = DEF_DOUBLE): Flow<Double> {
        return getValue(doublePreferencesKey(key), def)
    }

    suspend fun setDoubleValue(key: String, value: Double) {
        setValue(doublePreferencesKey(key), value)
    }

    fun getStringValue(key: String, def: String = DEF_STRING): Flow<String> {
        return getValue(stringPreferencesKey(key), def)
    }

    suspend fun setStringValue(key: String, value: String) {
        setValue(stringPreferencesKey(key), value)
    }

    fun getBooleanValue(key: String, def: Boolean = DEF_BOOLEAN): Flow<Boolean> {
        return getValue(booleanPreferencesKey(key), def)
    }

    suspend fun setBooleanValue(key: String, value: Boolean) {
        setValue(booleanPreferencesKey(key), value)
    }

    /**
     * 获取DataStore数据并监听变化
     * @param key String
     * @param func Function1<String, Flow<T>> 获取DataStore数据的方法，例如{ CommonDataStore.getLongValue(it, 0) }
     * @param callback Function1<T, Unit>
     */
    fun <T> listenDataStoreDataChange(
        key: String, func: DataStoreHelper.(String) -> Flow<T>, callback: (T) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            func(key).collect {
                callback(it)
                LogUtils.i("--->$key has changed：$it")
            }
        }
    }


    private fun <T> getValue(key: Preferences.Key<T>, def: T): Flow<T> {
        return dataStore.data.map { it[key] ?: def }
    }

    suspend fun <T> setValue(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }
}