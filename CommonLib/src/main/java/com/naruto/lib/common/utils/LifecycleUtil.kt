package com.naruto.lib.common.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.lang.ref.WeakReference

/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2021/9/1 0001
 * @Note
 */
object LifecycleUtil {
    /**
     * 添加监听
     *
     * @param lifecycleOwner
     * @param targetObject
     * @param lifecycleEvent
     * @param operation
     * @param <T>
     * @return 用于移除监听
    </T> */
    fun <T> addObserver(
        lifecycleOwner: LifecycleOwner, targetObject: T,
        lifecycleEvent: Lifecycle.Event, operation: T.() -> Unit
    ): () -> Unit {
        val wf = WeakReference(targetObject)
        val lifecycleObserver: LifecycleObserver =
            LifecycleEventObserver { _: LifecycleOwner?, event: Lifecycle.Event ->
                if (event == lifecycleEvent) wf.get()?.operation()
            }
        val lifecycle = lifecycleOwner.lifecycle.apply { addObserver(lifecycleObserver) }
        val wf_lifecycle = WeakReference(lifecycle)
        val wf_lifecycleObserver = WeakReference(lifecycleObserver)
        return {
            if (wf_lifecycle.get() != null) wf_lifecycle.get()!!
                .removeObserver(wf_lifecycleObserver.get()!!)
        }
    }

    /**
     * 添加 ON_DESTROY 监听
     *
     * @param lifecycleOwner
     * @param targetObject
     * @param operation
     * @param <T>
     * @return 用于移除监听
    </T> */
    fun <T> addDestroyObserver(
        lifecycleOwner: LifecycleOwner, targetObject: T, operation: T.() -> Unit
    ) = addObserver(lifecycleOwner, targetObject, Lifecycle.Event.ON_DESTROY, operation)
}