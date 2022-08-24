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
     * @param lifecycleOwner LifecycleOwner
     * @param lifecycleEvent Event
     * @param callback Function0<Unit>
     * @return () -> Unit 用于移除监听
     */
    fun addObserver(
        lifecycleOwner: LifecycleOwner, lifecycleEvent: Lifecycle.Event, callback: () -> Unit
    ): () -> Unit {
        val lifecycleObserver: LifecycleObserver =
            LifecycleEventObserver { _: LifecycleOwner?, event: Lifecycle.Event ->
                if (event == lifecycleEvent) callback()
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
     * 添加监听(防止内存泄漏)
     *
     * @param lifecycleOwner
     * @param targetObject 回调时要处理的对象
     * @param lifecycleEvent
     * @param callback
     * @param <T>
     * @return 用于移除监听
    </T> */
    fun <T> addObserver(
        lifecycleOwner: LifecycleOwner, targetObject: T,
        lifecycleEvent: Lifecycle.Event, callback: T.() -> Unit
    ): () -> Unit {
        val wf = WeakReference(targetObject)
        return addObserver(lifecycleOwner, lifecycleEvent) { wf.get()?.callback() }
    }

    /**
     * 添加 ON_DESTROY 监听
     *
     * @param lifecycleOwner
     * @param targetObject
     * @param callback
     * @param <T>
     * @return 用于移除监听
    </T> */
    fun <T> addDestroyObserver(
        lifecycleOwner: LifecycleOwner, targetObject: T, callback: T.() -> Unit
    ) = addObserver(lifecycleOwner, targetObject, Lifecycle.Event.ON_DESTROY, callback)
}