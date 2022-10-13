package com.naruto.lib.common.view

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2022/10/11 0011
 * @Note
 */
class MySwitch : SwitchMaterial {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    var toggleDispatcher: ToggleDispatcher? = null

    override fun toggle() {
        toggleDispatcher?.onToggle { if (it) super.toggle() } ?: super.toggle()
    }


    /**
     * @Description 某些情况下切换开关状态需要进行一些额外操作（例如申请权限），操作成功才能允许切换
     * @Author Naruto Yang
     * @CreateDate 2022/10/12 0012
     * @Note
     */
    fun interface ToggleDispatcher {
        /**
         *
         * @param callback Function1<Boolean, Unit> 回调，boolean:是否允许切换
         */
        fun onToggle(callback: (Boolean) -> Unit)
    }
}