package com.naruto.lib.common.utils

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.view.Display
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

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

    /**
     * 设置沉浸式状态栏（界面内容延伸到状态栏）
     *
     * @param activity
     * @param stateBarTextColorBlack 状态栏图标文本是否设置为黑色
     */
    fun setToolBarTransparent(activity: AppCompatActivity, stateBarTextColorBlack: Boolean) {
        //5.x开始需要把颜色设置透明，否则导航栏会呈现系统默认的浅灰色
        val window = activity.window
        val decorView = window.decorView
        //两个 flag 要结合使用，表示让应用的主体内容占用系统状态栏的空间
        var option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        if (stateBarTextColorBlack) option = option or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        decorView.systemUiVisibility = option
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
        //导航栏颜色也可以正常设置
        //window.setNavigationBarColor(Color.TRANSPARENT)
    }

    /**
     * 设置状态栏内容（文本、图标）颜色（深色、浅色）
     * @param window Window
     * @param isStatusBarDark Boolean
     */
    fun setStatusBarContentColorMode(window: Window, isStatusBarDark: Boolean) {
        //boolean dark = ColorUtils.calculateLuminance(parseColor) >= 0.5;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            window.insetsController?.setSystemBarsAppearance(if (isStatusBarDark) 0 else mask, mask)
        } else {
            WindowCompat.getInsetsController(window, window.decorView)
                .isAppearanceLightStatusBars = !isStatusBarDark
        }
    }
}