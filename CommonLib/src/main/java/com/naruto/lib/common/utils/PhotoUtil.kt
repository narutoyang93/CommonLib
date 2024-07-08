package com.naruto.lib.common.utils

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.result.ActivityResult
import com.naruto.lib.common.base.BaseActivity

/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2023/12/5 0005
 * @Note
 */
object PhotoUtil {
    /**
     * 选择图片
     * @receiver BaseActivity
     * @param mimeType String
     * @param allowMulti Boolean
     * @param callback Function1<PhotoResult, Unit>
     */
    fun BaseActivity.selectPhoto(
        mimeType: String = "image/*", allowMulti: Boolean = false, callback: (SelectResult) -> Unit
    ) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = mimeType
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMulti)
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(IntentUtil.createChooserIntent("选择图片", intent))
            { result -> callback(SelectResult(result)) }
        }
    }

    class SelectResult(activityResult: ActivityResult) : PhotoResult(activityResult) {
        val fileUris = if (!isSuccess()) null
        else activityResult.data?.run {
            data?.let { listOf(it) } ?: (clipData?.run { List(itemCount) { getItemAt(it).uri } })
        }
    }

    open class PhotoResult(private val activityResult: ActivityResult) {
        fun isSuccess() = activityResult.resultCode == Activity.RESULT_OK
        fun thumbnail() =
            if (!isSuccess()) null else activityResult.data?.getParcelableExtra("data") as? Bitmap
    }
}