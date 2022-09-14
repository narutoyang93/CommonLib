package com.naruto.lib.common.utils

import android.Manifest
import android.app.Activity
import android.content.*
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Pair
import android.view.View
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider.getUriForFile
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.naruto.lib.common.Global
import com.naruto.lib.common.helper.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.*
import java.util.*


/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2022/1/17 0017
 * @Note
 */
object FileUtil {
    private val APP_FOLDER = Global.appNameEN + "/"

    /**
     * @param relativePath    相对根目录（/storage/emulated/0/）的路径，不以“/”开头，但以“/”结尾
     */
    fun createSAFIntent(
        operation: Operation, relativePath: String,
        fileName: String? = null, fileNameSuffix: String? = null
    ): Intent {
        var rp: String = relativePath
        while (rp.endsWith("/")) {
            rp = rp.substring(0, rp.length - 1)
        }
        val relativePath0 = rp.replace("/", "%2F")
        val uri =
            Uri.parse("content://com.android.externalstorage.documents/document/primary:$relativePath0")

        val action = when (operation) {
            Operation.CREATE -> {
                fileName ?: throw Exception("FileName is required.")
                Intent.ACTION_CREATE_DOCUMENT
            }
            Operation.OPEN -> Intent.ACTION_OPEN_DOCUMENT
            else -> Intent.ACTION_OPEN_DOCUMENT_TREE
        }
        return Intent(action).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            (fileNameSuffix ?: (fileName?.substringAfterLast(".") ?: "")).run {
                if (isNotEmpty()) type = getMimeTypeFromExtension(this)
            }
            putExtra(Intent.EXTRA_TITLE, fileName ?: "")
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
        }
    }

    /**
     * @param relativePath    相对应用私有目录（/.../APP_NAME_EN/）的路径，不以“/”开头，但以“/”结尾
     * 注意：暂未找到让SAF实现mkdirs的方法，故若相对路径不存在，会自动导向Download/文件夹
     */
    fun createSAFIntentForCreateFile(
        mediaType: MediaType, relativePath: String, fileName: String
    ): Intent {
        val rp = when (mediaType) {
            MediaType.AUDIO -> Environment.DIRECTORY_MUSIC
            MediaType.IMAGE -> Environment.DIRECTORY_PICTURES
            MediaType.VIDEO -> Environment.DIRECTORY_MOVIES
            MediaType.FILE -> Environment.DIRECTORY_DOCUMENTS
            MediaType.DOWNLOAD -> Environment.DIRECTORY_DOWNLOADS;
        } + "/${Global.appNameEN}/$relativePath"
        return createSAFIntent(Operation.CREATE, rp, fileName)
    }


    /**
     * 打开文件
     */
    fun openFile(
        activity: Activity, fileUri: Uri,
        fileName: String? = null, fileNameSuffix: String? = null
    ) {
        //获取文件file的MIME类型
        val type: String? = (fileNameSuffix ?: (fileName?.substringAfterLast(".") ?: "")).run {
            if (isNotEmpty()) getMimeTypeFromExtension(this)
            else throw Exception("FileName/fileNameSuffix is required.")
        }
        val intent = Intent()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.action = Intent.ACTION_VIEW
        intent.setDataAndType(fileUri, type)
        activity.startActivity(intent)
    }

    /**
     * 获取文件写入流
     *
     * @param uri      uri
     * @param isAppend 是否追加模式
     * @return 输入流
     */
    fun getOutputStream(uri: Uri?, isAppend: Boolean): OutputStream? {
        return uri?.let { getContentResolver().openOutputStream(it, if (isAppend) "wa" else "rw") }
    }

    /**
     * 写文件
     *
     * @param bytes
     * @param iOutputStream
     * @param callback      回调
     */
    private fun writeDataToExternalPublicSpaceFile(
        bytes: ByteArray, outputStreamProvider: (() -> OutputStream?),
        callback: ((Boolean) -> Unit)?
    ) {
        outputStreamProvider().use {
            val result = if (it == null) false else {
                it.write(bytes)
                it.flush()
                true
            }
            if (callback != null) callback(result)
        }
    }


    /**
     * 写文件
     */
    fun writeDataToExternalPublicSpaceFile(
        bytes: ByteArray, mediaType: MediaType, relativePath: String, fileName: String,
        isAppend: Boolean, callback: ((Boolean) -> Unit)?
    ) {
        val dataStoreKey = "$relativePath->$fileName"
        //系统中有可能已经存在同名文件A且当前app无法访问（例如卸载重装后），此时会创建新文件A(1)，但下次访问肯定还是无法访问A，为了避免创建A(2)，每次发现存在同名文件时记录原文件名与新文件名的映射
        val realFileName =
            runBlocking { FileDataStore.getStringValue(dataStoreKey, fileName).first() }
        val uri = getFileInExternalPublicSpace(mediaType, relativePath, realFileName)
        if (uri == null) {//找不到目标文件则创建新文件（有几种情况：1.系统不存在目标文件。2.系统已存在同名文件A且本app无法访问，此时会创建“A(1)”。3.“A(1)”也无法访问了，此时有可能连之前的同名文件A都没有了，可以尝试创建“A”）
            createFileInExternalPublicSpace(mediaType, relativePath, fileName) { uri0 ->
                uri0?.let {
                    CoroutineScope(Dispatchers.IO).launch {//判断新文件名是否与目标文件名是否一致，如果不一致，说明目标文件已无法访问，需记录原文件名与新文件名的映射
                        getContentResolver().query(
                            it, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null
                        ).use { cursor ->
                            if (cursor == null) {
                                LogUtils.e("--->找不到新文件")
                                return@use
                            }
                            val nameColumn =
                                cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                            var name: String? = null
                            while (cursor.moveToNext()) {
                                name = cursor.getString(nameColumn)
                                LogUtils.i("--->requireFileName=$realFileName；newFileName=$name")
                            }
                            if (name != null && name != realFileName)
                                FileDataStore.setStringValue(dataStoreKey, name)
                        }
                    }
                    //写入数据
                    writeDataToExternalPublicSpaceFile(
                        bytes, { getOutputStream(it, isAppend) }, callback
                    )
                }
            }
        } else writeDataToExternalPublicSpaceFile(
            bytes, { getOutputStream(uri, isAppend) }, callback
        )
    }


    /**
     * 截取视图保存文件
     */
    fun saveViewToFile(view: View, fileUri: Uri) {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        BufferedOutputStream(view.context.contentResolver.openOutputStream(fileUri)).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            it.flush()
            it.close()
        }
    }


    /**
     * 根据文件扩展名获取对应的 MimeType
     *
     * @param extension
     * @return
     */
    fun getMimeTypeFromExtension(extension: String?): String? {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension).also {
            LogUtils.i("--->extension=$extension;MimeType=$it")
        }
    }

    private fun getContentResolver(): ContentResolver {
        return getContext().contentResolver
    }

    /**
     * @param mediaType
     * @return
     */
    private fun getMediaStoreData(mediaType: MediaType): MediaData {
        var directory: String? = null
        var contentUri: Uri? = null
        when (mediaType) {
            MediaType.AUDIO -> {
                directory = Environment.DIRECTORY_MUSIC
                contentUri =
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
            MediaType.IMAGE -> {
                directory = Environment.DIRECTORY_PICTURES
                contentUri =
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
            MediaType.VIDEO -> {
                directory = Environment.DIRECTORY_MOVIES
                contentUri =
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
            MediaType.FILE -> {
                directory = Environment.DIRECTORY_DOCUMENTS
                contentUri = MediaStore.Files.getContentUri("external")
            }
            MediaType.DOWNLOAD -> {
                directory = Environment.DIRECTORY_DOWNLOADS
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                }
            }
        }
        return MediaData(directory = directory, contentUri = contentUri)
    }

    /**
     * 根据文件获取Uri
     *
     * @param file
     * @return
     */
    fun getUriForFile(file: File): Uri {
        val context: Context = getContext()
        return getUriForFile(context, context.packageName + ".app.fileProvider", file)
    }

    /**
     * 根据URI创建文件
     *
     * @param contentUri
     * @param relativePath
     * @param fileName
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private fun createFile(contentUri: Uri, relativePath: String, fileName: String): Uri? {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Downloads.DISPLAY_NAME, fileName)
        contentValues.put(
            MediaStore.Downloads.MIME_TYPE,
            getMimeTypeFromExtension(fileName.substringAfterLast("."))
        )
        contentValues.put(MediaStore.Downloads.DATE_TAKEN, System.currentTimeMillis())
        contentValues.put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
        return getContentResolver().insert(contentUri, contentValues)
    }

    /**
     * 创建文件
     *
     * @param folderPath 文件夹绝对路径
     * @param fileName
     * @return
     */
    private fun createFile(folderPath: String, fileName: String): Uri? {
        val storageDir = File(folderPath)
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            LogUtils.e("--->mkdirs失败")
            return null
        }
        val file = File(folderPath + fileName)
        try {
            if (!file.createNewFile()) return null
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return getUriForFile(file)
    }

    /**
     * 外部私有空间，卸载即删除，读写无需申请权限
     *
     * @param relativePath 相对路径，如：”download/picture/“
     * @return
     */
    fun getPathFromExternalPrivateSpace(relativePath: String): String {
        return getContext().getExternalFilesDir(null)!!.absolutePath +
                File.separator + relativePath
    }

    /**
     * 外部公共空间，读写需申请权限
     *
     * @param mediaType
     * @param relativePath
     * @return
     */
    fun getPathFromExternalPublicSpace(mediaType: MediaType, relativePath: String): String {
        val mediaData: MediaData = getMediaStoreData(mediaType)
        return getPathFromExternalPublicSpace(mediaData.directory!!, relativePath)
    }

    /**
     * 外部公共空间，读写需申请权限
     *
     * @param systemDirectory
     * @param relativePath
     * @return
     */
    private fun getPathFromExternalPublicSpace(systemDirectory: String, relativePath: String)
            : String {
        return getPathFromExternalPublicSpace(systemDirectory) + "/" + APP_FOLDER + relativePath
    }

    private fun getPathFromExternalPublicSpace(systemDirectory: String): String {
        return Environment.getExternalStoragePublicDirectory(systemDirectory).absolutePath
    }

    /**
     * 获取sd根目录下的相对路径
     *
     * @param systemDirectory
     * @param relativePath
     * @return
     */
    private fun getRelativePathInRoot(systemDirectory: String, relativePath: String): String {
        return "$systemDirectory/$APP_FOLDER$relativePath"
    }

    /**
     * 在外部私有空间创建文件
     *
     * @param relativePath
     * @param fileName
     * @return
     */
    fun createFileInExternalPrivateSpace(relativePath: String, fileName: String): Uri? {
        val folderPath: String =
            getPathFromExternalPrivateSpace(relativePath)
        return createFile(folderPath, fileName)
    }


    /**
     * 在外部公共存储空间创建文件
     *
     * @param mediaType
     * @param relativePath
     * @param fileName     文件名，需带后缀名
     * @return
     */
    private fun createFileInExternalPublicSpace(
        mediaType: MediaType, relativePath: String, fileName: String, callback: (Uri?) -> Unit
    ) {
        if (TextUtils.isEmpty(fileName)) {
            callback(null)
            return
        }
        val mediaData: MediaData = getMediaStoreData(mediaType)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            doWithStoragePermission {
                val folderPath: String =
                    getPathFromExternalPublicSpace(mediaData.directory!!, relativePath)
                callback(createFile(folderPath, fileName))
            }
        } else {
            try {
                val path = getRelativePathInRoot(mediaData.directory!!, relativePath)
                val uri: Uri? = createFile(mediaData.contentUri!!, path, fileName)
                callback(uri)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 在外部公共存储空间创建音频文件
     *
     * @param relativePath
     * @param fileName
     * @param callback
     */
    fun createAudioFileInExternalPublicSpace(
        relativePath: String, fileName: String, callback: (Uri?) -> Unit
    ) {
        createFileInExternalPublicSpace(MediaType.AUDIO, relativePath, fileName, callback)
    }

    /**
     * 在外部公共存储空间创建视频文件
     *
     * @param relativePath
     * @param fileName
     * @param callback
     */
    fun createVideoFileInExternalPublicSpace(
        relativePath: String, fileName: String, callback: (Uri?) -> Unit
    ) {
        createFileInExternalPublicSpace(MediaType.VIDEO, relativePath, fileName, callback)
    }

    /**
     * 在外部公共存储空间创建图像文件
     *
     * @param relativePath
     * @param fileName
     * @param callback
     */
    fun createImageFileInExternalPublicSpace(
        relativePath: String, fileName: String, callback: (Uri?) -> Unit
    ) {
        createFileInExternalPublicSpace(MediaType.IMAGE, relativePath, fileName, callback)
    }

    /**
     * 在外部公共存储空间创建文本文件
     *
     * @param relativePath
     * @param fileName
     * @param callback
     */
    fun createDocumentFileInExternalPublicSpace(
        relativePath: String, fileName: String, callback: (Uri?) -> Unit
    ) {
        createFileInExternalPublicSpace(MediaType.FILE, relativePath, fileName, callback)
    }

    /**
     * 在外部公共存储空间创建下载文件
     *
     * @param relativePath
     * @param fileName
     * @param callback
     */
    fun createDownloadFileInExternalPublicSpace(
        relativePath: String, fileName: String, callback: (Uri?) -> Unit
    ) {
        createFileInExternalPublicSpace(MediaType.DOWNLOAD, relativePath, fileName, callback)
    }

    /**
     * 获取外部公共存储空间文件
     *
     * @param mediaData
     * @param selection
     * @param selectionArgs
     * @param sortOrder
     * @param fileInfoCreator
     * @param <T>
     * @return
    </T> */
    fun <T> getFileInExternalPublicSpace(
        mediaData: MediaData, selection: String?, selectionArgs: Array<String>?, sortOrder: String?,
        fileInfoCreator: (MediaData) -> T
    ): List<T> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.DURATION,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.SIZE
        )
        val list: MutableList<T> = ArrayList()
        getContentResolver().query(
            mediaData.contentUri!!, projection, selection, selectionArgs, sortOrder
        ).use { cursor ->
            if (cursor == null) return list
            val idColumn: Int = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameColumn: Int = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val relativePathColumn: Int =
                cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
            val durationColumn: Int = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION)
            val createTimeColumn: Int =
                cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val sizeColumn: Int = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            while (cursor.moveToNext()) {
                val id: Long = cursor.getLong(idColumn)
                val name: String = cursor.getString(nameColumn)
                val relativePath: String = cursor.getString(relativePathColumn)
                val duration: Int = cursor.getInt(durationColumn)
                val size: Long = cursor.getInt(sizeColumn).toLong()
                val createTime: Long = cursor.getLong(createTimeColumn)
                val fileUri = ContentUris.withAppendedId(mediaData.contentUri, id)
                val data = MediaData(
                    id, fileUri, name, null, relativePath, duration, size, createTime
                )
                fileInfoCreator(data)?.run { list.add(this) }
            }
        }
        return list
    }

    /**
     * 获取外部公共空间的文件
     *
     * @param mediaType
     * @param relativePath
     * @param fileName
     * @return
     */
    fun getFileInExternalPublicSpace(
        mediaType: MediaType, relativePath: String, fileName: String
    ): Uri? {
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val folderPath: String = getPathFromExternalPublicSpace(mediaType, relativePath)
            getUriForFile(File(folderPath + fileName))
        } else {
            val mediaData: MediaData = getMediaStoreData(mediaType)
            val selection =
                MediaStore.MediaColumns.DISPLAY_NAME + "=? and " + MediaStore.MediaColumns.RELATIVE_PATH + "=?"
            val args = arrayOf(fileName, getRelativePathInRoot(mediaData.directory!!, relativePath))
            val list: List<Uri> =
                getFileInExternalPublicSpace(mediaData, selection, args, null) { it.fileUri!! }
            if (list.isEmpty()) null else list[0]
        }
    }

    /**
     * 执行需要存储权限的操作
     * 外部需判断 if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P)
     *
     * @param operation
     */
    fun doWithStoragePermission(autoRequest: Boolean = true, operation: () -> Unit) {
        Global.doWithPermission(object : PermissionHelper.RequestPermissionsCallback(
            Pair(null, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))
        ) {
            override fun onGranted() = operation()
        }.apply { setAutoRequest(autoRequest) })
    }

    /**
     * @Description
     * @Author Naruto Yang
     * @CreateDate 2022/1/24 0024
     * @Note
     */
    data class MediaData(
        val contentUri: Uri? = null,
        val directory: String? = null,
        val id: Long = 0,
        val fileUri: Uri? = null,
        val fileName: String? = null,
        val absolutePath: String? = null,
        val relativePath: String? = null,
        val duration: Int = 0,
        val size: Long = 0,
        val createTime: Long = 0
    ) {
        constructor (
            id: Long,
            fileUri: Uri?,
            fileName: String?,
            absolutePath: String?,
            relativePath: String?,
            duration: Int,
            size: Long,
            createTime: Long
        ) : this(
            null, null, id, fileUri, fileName, absolutePath, relativePath,
            duration, size, createTime
        )
    }

    /**
     * @Description
     * @Author Naruto Yang
     * @CreateDate 2022/1/17 0017
     * @Note
     */
    enum class Operation {
        CREATE, OPEN, SELECT_TREE
    }

    /**
     * @Description
     * @Author Naruto Yang
     * @CreateDate 2021/7/7 0007
     * @Note
     */
    enum class MediaType {
        AUDIO, VIDEO, IMAGE, FILE, DOWNLOAD
    }
}

private fun getContext(): Context = Global.getMainModuleContext()

private val Context.fileDataStore: DataStore<Preferences> by preferencesDataStore(name = "FileData")

private object FileDataStore : DataStoreHelper(getContext().fileDataStore)