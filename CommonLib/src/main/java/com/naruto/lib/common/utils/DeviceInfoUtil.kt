package com.naruto.lib.common.utils

import android.annotation.SuppressLint
import android.view.Display

/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2022/11/25 0025
 * @Note
 */
object DeviceInfoUtil {
    private var defDensityDpi: Int = 0

    /**
     * 获取手机出厂时默认的densityDpi
     * @return Int
     */
    @SuppressLint("PrivateApi")
    fun getDefaultDisplayDensity(): Int {
        if (defDensityDpi == 0) defDensityDpi = kotlin.runCatching {
            val aClass = Class.forName("android.view.WindowManagerGlobal")
            val wms = aClass.getMethod("getWindowManagerService")
                .apply { isAccessible = true }.invoke(aClass)
            wms.javaClass.getMethod("getInitialDisplayDensity", Int::class.javaPrimitiveType)
                .apply { isAccessible = true }.invoke(wms, Display.DEFAULT_DISPLAY) as Int
        }.getOrDefault(-1)
        return defDensityDpi
    }
}