package com.naruto.lib.common.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.naruto.lib.common.Global
import com.naruto.lib.common.TopFunction.runInCoroutine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import java.util.Timer
import kotlin.concurrent.schedule

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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("MyData")

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

    fun getSetValue(key: String, def: Set<String> = setOf()): Flow<Set<String>> {
        return getValue(stringSetPreferencesKey(key), def)
    }

    suspend fun setSetValue(key: String, value: Set<String>) {
        setValue(stringSetPreferencesKey(key), value)
    }

    suspend fun writeSetValue(key: String, block: (Set<String>) -> Unit) {
        val preferencesKey = stringSetPreferencesKey(key)
        val set = getValue(preferencesKey, setOf()).first()
        block(set)
        setValue(preferencesKey, set)
    }

    inline fun <reified T> getOtherValue(key: String, def: T, crossinline transform: (String) -> T)
            : Flow<T> = getStringValue(key).map { if (it.isEmpty()) def else transform(it) }

    /**
     * 获取DataStore数据并监听变化
     * @param key String
     * @param func Function1<String, Flow<T>> 获取DataStore数据的方法，例如{ CommonDataStore.getLongValue(it, 0) }
     * @param callback Function1<T, Unit>
     */
    fun <T> listenDataStoreDataChange(
        key: String, func: DataStoreHelper.(String) -> Flow<T>, callback: (T) -> Unit
    ) {
        runInCoroutine { func(key).collect { callback(it);LogUtils.i("--->$key has changed：$it") } }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private fun <T> getValue(key: Preferences.Key<T>, def: T): Flow<T> {
        return dataStore.data.map { it[key] ?: def }.distinctUntilChanged()
    }

    suspend fun <T> setValue(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }
}