package com.naruto.lib.common.base

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.annotation.RequiresApi

/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2022/9/7 0007
 * @Note
 */
interface ContextBridge {
    val context: Context

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun requestPermissions(
        permissions: Array<String>, callback: ActivityResultCallback<Map<String, Boolean>>
    )

    fun starActivityForResult(intent: Intent, callback: ActivityResultCallback<ActivityResult>)
}