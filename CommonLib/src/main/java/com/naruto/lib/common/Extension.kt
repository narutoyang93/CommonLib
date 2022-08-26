package com.naruto.lib.common.Extension

import android.util.SparseArray

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