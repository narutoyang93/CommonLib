package com.naruto.commonlib.example

import android.Manifest
import android.provider.MediaStore
import android.util.Pair
import android.widget.Button
import android.widget.Toast
import com.naruto.lib.common.TopFunction.currentDateTime
import com.naruto.lib.common.TopFunction.todayDate
import com.naruto.lib.common.base.BaseActivity
import com.naruto.lib.common.helper.PermissionHelper
import com.naruto.lib.common.utils.FileUtil
import com.naruto.lib.common.utils.LogUtils
import java.io.File
import java.io.FilenameFilter
import java.util.Arrays

class MainActivity : BaseActivity() {
    private val fileNames = arrayOf("naruto", "sasuke", "sakura", "kakashi")
    private var i = 0

    override fun init() {
        findViewById<Button>(R.id.btn_create).setOnClickListener {
            FileUtil.writeDataToExternalPublicSpaceFile(
                "dcjidvihvb icj".toByteArray(), FileUtil.MediaType.FILE,
                "test/", "${fileNames[i++ % fileNames.size]}.txt", true, null
            )
        }

        findViewById<Button>(R.id.btn_delete).setOnClickListener {
/*            contentResolver.query(FileUtil.getMediaStoreData(FileUtil.MediaType.FILE).contentUri!!,
                null, *//*MediaStore.MediaColumns.DATA+" like %? ", arrayOf("/test"),*//*null,null, null)?.use { cursor ->
                val i_displayName=cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val i_data=cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                while (cursor.moveToNext()){
                    LogUtils.i("--->displayName=${cursor.getString(i_displayName)};data=${cursor.getString(i_data)}")
                }
            }*/

            val filter = FileUtil.MyFileFilter(
                { dir, name -> name.contains("naruto") },
                MediaStore.MediaColumns.DISPLAY_NAME + " like ? ", arrayOf("naruto%")
            )
            FileUtil.deleteFileInExternalPublicSpace(FileUtil.MediaType.FILE, "test/", filter)
        }
    }

    override fun getLayoutRes(): Int = R.layout.activity_main
}