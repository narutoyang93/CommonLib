package com.naruto.commonlib.example

import android.app.Application
import com.naruto.lib.common.Global
import com.naruto.lib.common.commonLibInit


/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2022/8/20 0020
 * @Note
 */
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        commonLibInit()
        Global.appNameEN = Global.appName.toString()
        Global.isKeepFontSize = true
    }
}