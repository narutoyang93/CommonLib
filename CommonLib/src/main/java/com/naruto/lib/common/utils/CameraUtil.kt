package com.naruto.lib.common.utils

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.result.ActivityResult
import com.naruto.lib.common.Box
import com.naruto.lib.common.Global.toast
import com.naruto.lib.common.base.BaseActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2023/12/7 0007
 * @Note
 */
private const val DATE_TIME_FORMAT_DEFAULT = "yyyyMMddHHmmssSSS"

object CameraUtil {
    /**
     * 拍照
     * @param activity BaseActivity
     * @param options OutputOptions
     * @param callback Function1<TakePhotoResult?, Unit>
     */
    fun BaseActivity.openCamera(
        options: OutputOptions, callback: (TakePhotoResult?) -> Unit
    ) {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            createFile(options) { fileName, uri ->
                if (uri == null) {
                    toast("创建文件失败")
                    callback(null)
                    return@createFile
                }
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                startActivityForResult(takePictureIntent) { result ->
                    if (result.resultCode != Activity.RESULT_OK) {
                        callback(TakePhotoResult(null, null, result))
                        return@startActivityForResult
                    }
                    if (options.maxCompressSize > 0) { //压缩
                        showLoadingDialog("处理文件中，请稍候")
                        Thread {
                            val outputFileName: String = createOutputFileName(options)
                            createPublicSpaceFile(options.folderName, outputFileName)
                            { fileName0, uri0 ->
                                val box = Box(TakePhotoResult(fileName, uri, result))
                                if (uri0 == null) LogUtils.w("--->创建输出文件失败")
                                else compressPhoto(this, uri, uri0, options) { isSuccess ->
                                    if (isSuccess) {
                                        FileUtil.delete(uri)
                                        box.data = TakePhotoResult(fileName0, uri0, result)
                                    }
                                }
                                runOnUiThread { dismissLoadingDialog();callback(box.data) }
                            }
                        }.start()
                    } else callback(TakePhotoResult(fileName, uri, result))
                }
            }
        }
    }

    /**
     * 创建文件
     * @param options CameraOptions
     * @param callback Function2<String, Uri?, Unit>
     */
    private fun createFile(options: OutputOptions, callback: (String, Uri?) -> Unit) {
        val fileName: String
        var isPrivate = options.isPrivate
        if (options.maxCompressSize > 0) { //需要压缩，先创建临时文件
            isPrivate = true
            fileName = "temp_" + getCurrentTimeStr(DATE_TIME_FORMAT_DEFAULT) + ".jpg"
        } else fileName = createOutputFileName(options)
        if (isPrivate) { //应用私有目录
            val uri =
                FileUtil.createFileInExternalPrivateSpace(Environment.DIRECTORY_DCIM, fileName)
            callback(fileName, uri)
        } else { //系统公共目录
            createPublicSpaceFile(options.folderName, fileName, callback)
        }
    }

    private fun createPublicSpaceFile(
        folderName: String, fileName: String, callback: (String, Uri?) -> Unit
    ) {
        FileUtil.createImageFileInExternalPublicSpace(folderName, fileName)
        { callback(fileName, it) }
    }

    private fun createOutputFileName(options: OutputOptions): String {
        return options.fileNamePrefix + getCurrentTimeStr(options.timeStampFormat) + options.fileNameSuffix
    }

    private fun getCurrentTimeStr(format: String): String {
        return SimpleDateFormat(format, Locale.getDefault()).format(Date())
    }

    /**
     * 压缩图片
     */
    private fun compressPhoto(
        activity: BaseActivity, inputUri: Uri, outputUri: Uri, options: OutputOptions,
        callback: (Boolean) -> Unit
    ) {
        kotlin.runCatching {
            BitmapFactory.decodeStream(activity.contentResolver.openInputStream(inputUri))
        }.onFailure { it.printStackTrace() }.getOrNull()?.also { bm ->
            BitmapUtil.compressPicture(bm, options.maxCompressSize, Bitmap.CompressFormat.JPEG) {
                if (it == null) toast("照片压缩失败")
                else FileUtil.writeData(it, outputUri) { isSuccess ->
                    if (isSuccess) LogUtils.i("--->compressPhoto: 压缩成功")
                    else LogUtils.w("--->压缩结果保存失败")
                    callback(isSuccess)
                }
            }
        } ?: run { toast("读取照片失败");callback(false) }
    }

    /**
     * @Description
     * @Author Naruto Yang
     * @CreateDate 2023/12/7 0007
     * @Note
     */
    class OutputOptions(
        var isPrivate: Boolean = false, //存储在系统公共目录还是应用私有目录
        var folderName: String = "temp/", //文件夹名称
        var timeStampFormat: String = DATE_TIME_FORMAT_DEFAULT, //时间戳格式
        var fileNamePrefix: String = "IMG_", //文件名前缀
        var fileNameSuffix: String = ".jpg", //文件名后缀
        var maxCompressSize: Int = -1 //文件最大压缩体积（单位：b），当>0时生效，当原文件大于这个值便会自动压缩至小于这个值
    )

    /**
     * @Description
     * @Author Naruto Yang
     * @CreateDate 2023/12/7 0007
     * @Note
     */
    class TakePhotoResult(
        val fileName: String?,
        val fileUri: Uri?,
        activityResult: ActivityResult
    ) : PhotoUtil.PhotoResult(activityResult)

}