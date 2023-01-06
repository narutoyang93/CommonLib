package com.naruto.lib.common.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
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
import androidx.documentfile.provider.DocumentFile
import com.naruto.lib.common.Global
import com.naruto.lib.common.NormalText
import com.naruto.lib.common.TopFunction.runInCoroutine
import com.naruto.lib.common.helper.PermissionHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.single
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
    val permissions = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
    )
    private val APP_FOLDER = Global.appNameEN + "/"
    val SELECTION_SPECIFY_FILE = MediaStore.MediaColumns.DISPLAY_NAME + "=? and " +
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.MediaColumns.RELATIVE_PATH + "=?"
            else MediaStore.Images.Media.DATA + " like ?"

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
                    runInCoroutine {//判断新文件名是否与目标文件名是否一致，如果不一致，说明目标文件已无法访问，需记录原文件名与新文件名的映射
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
     * 读取文件内容
     * @param uri Uri
     * @param block Function1<InputStream?, Unit>
     */
    fun readDataFromFile(uri: Uri, block: (InputStream?) -> Unit) {
        val func = { getContentResolver().openInputStream(uri).use { block(it) } }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            doWithStoragePermission({ func() }, { block(null) })
        } else func()
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
    fun getMediaStoreData(mediaType: MediaType): MediaData {
        var directory: String? = null
        var contentUri: Uri? = null
        when (mediaType) {
            MediaType.AUDIO -> {
                directory = Environment.DIRECTORY_MUSIC
                contentUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
            MediaType.IMAGE -> {
                directory = Environment.DIRECTORY_PICTURES
                contentUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            MediaType.VIDEO -> {
                directory = Environment.DIRECTORY_MOVIES
                contentUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
            MediaType.FILE -> {
                val volume = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.VOLUME_EXTERNAL_PRIMARY else "external_primary"
                directory = Environment.DIRECTORY_DOCUMENTS
                contentUri = MediaStore.Files.getContentUri(volume)
            }
            MediaType.DOWNLOAD -> {
                directory = Environment.DIRECTORY_DOWNLOADS
                contentUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else MediaStore.Files.getContentUri("external_primary")
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
    fun getUriForFile(file: File): Uri? {
        if (!file.exists()) return null
        val context: Context = getContext()
        return getUriForFile(context, context.packageName + ".app.fileProvider", file)
    }

    /**
     * 根据Uri获取File
     *
     * @param uri
     * @return
     */
    private fun getFileByUri(uri: Uri): File {
        return File(uri.path!!)
    }

    /**
     * 判断字符串是否是Uri
     */
    fun isUri(str: String): Boolean {
        return if (TextUtils.isEmpty(str)) false
        else str.startsWith(ContentResolver.SCHEME_CONTENT + "://")
                || str.startsWith(ContentResolver.SCHEME_ANDROID_RESOURCE + "://")
                || str.startsWith(ContentResolver.SCHEME_FILE + "://")
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
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        contentValues.put(
            MediaStore.MediaColumns.MIME_TYPE,
            getMimeTypeFromExtension(fileName.substringAfterLast("."))
        )
        contentValues.put(MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis())
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
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
    private fun getRelativePathInRoot(systemDirectory: String, relativePath: String): String =
        "$systemDirectory/$APP_FOLDER$relativePath"


    /**
     * 获取sd根目录下的相对路径
     *
     * @param mediaType
     * @param relativePath
     * @return
     */
    fun getRelativePathInRoot(mediaType: MediaType, relativePath: String): String =
        getRelativePathInRoot(getMediaStoreData(mediaType).directory!!, relativePath)


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
            doWithStoragePermission({
                val folderPath: String =
                    getPathFromExternalPublicSpace(mediaData.directory!!, relativePath)
                callback(createFile(folderPath, fileName))
            },{callback(null)})
        } else {
            kotlin.runCatching {
                val path = getRelativePathInRoot(mediaData.directory!!, relativePath)
                callback(createFile(mediaData.contentUri!!, path, fileName))
            }.onFailure { it.printStackTrace() }
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
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val relativePathColumn =
                cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION)
            val createTimeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val relativePath = cursor.getString(relativePathColumn)
                val duration = cursor.getInt(durationColumn)
                val size = cursor.getInt(sizeColumn).toLong()
                val createTime = cursor.getLong(createTimeColumn)
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
            val selection = SELECTION_SPECIFY_FILE
            val args = arrayOf(fileName, getRelativePathInRoot(mediaData.directory!!, relativePath))
            val list: List<Uri> =
                getFileInExternalPublicSpace(mediaData, selection, args, null) { it.fileUri!! }
            if (list.isEmpty()) null else list[0]
        }
    }


    /**
     * 获取外部公共空间的文件
     *
     * @param mediaType
     * @param relativePath
     * @param myFileFilter
     * @param fileInfoCreator
     * @param callback
     * @param <T>
    </T> */
    fun <T> getFileInExternalPublicSpace(
        mediaType: MediaType, relativePath: String, myFileFilter: MyFileFilter?,
        fileInfoCreator: (MediaData) -> T, callback: (List<T>) -> Unit
    ) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val folderPath: String = getPathFromExternalPublicSpace(mediaType, relativePath)
            getFileInExternalSpace(folderPath, myFileFilter, fileInfoCreator, callback)
        } else {
            val data = getMediaStoreData(mediaType)
            val selection = MediaStore.MediaColumns.RELATIVE_PATH + "=?"
            val args = arrayOf(getRelativePathInRoot(data.directory!!, relativePath))

            myFileFilter?.doForMediaStore(selection, args) { f_slc, f_args ->
                callback(getFileInExternalPublicSpace(data, f_slc, f_args, null, fileInfoCreator))
            }
        }
    }


    /**
     * 获取外部非公共空间的文件
     *
     * @param relativePath    相对根目录（/storage/emulated/0/）的路径，不以“/”开头，但以“/”结尾
     * @param fileInfoCreator
     * @param callback
     * @param <T>
    </T> */
    @SuppressLint("CheckResult")
    fun <T> getFileInExternalNonPublicSpace(
        relativePath: String, fileInfoCreator: (MediaData) -> T, callback: (List<T>) -> Unit
    ) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val folderPath = Environment.getExternalStorageDirectory().toString() + "/$relativePath"
            getFileInExternalSpace(folderPath, null, fileInfoCreator, callback)
            return
        }
        val spKey = "treeUri_$relativePath"
        val operation: (DocumentFile) -> Unit = { df ->
            val list: MutableList<T> = ArrayList()
            var mediaData: MediaData
            df.listFiles().forEach { f ->
                mediaData = MediaData(
                    fileUri = f.uri, fileName = f.name, size = f.length(),
                    createTime = f.lastModified()
                )
                list.add(fileInfoCreator(mediaData))
            }
            callback(list)
        }

        runInCoroutine {
            val treeUriString = FileDataStore.getStringValue(spKey).single()
            if (!TextUtils.isEmpty(treeUriString)) {
                kotlin.runCatching {
                    val treeUri = Uri.parse(treeUriString)
                    //检查权限
                    getContentResolver().takePersistableUriPermission(
                        treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    DocumentFile.fromTreeUri(getContext(), treeUri)
                }.onFailure { it.printStackTrace() }.getOrNull()?.let { df ->
                    operation(df);return@runInCoroutine
                }
            }

            //前往授权
            val message = "由于当前系统限制，访问外部非共享文件需获取局部访问权限，即将打开设置页面，请在打开的页面点击底部按钮"
            val confirmListener = object : DialogFactory.OnDialogButtonClickListener {
                override fun onClick(view: View, dialog: Dialog) {
                    var rp = relativePath
                    while (rp.endsWith("/")) {
                        rp = rp.substring(0, rp.length - 1)
                    }
                    val relativePath0 = rp.replace("/", "%2F")
                    val uri =
                        Uri.parse("content://com.android.externalstorage.documents/document/primary:$relativePath0")
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
                    Global.doByActivity { activity ->
                        activity.startActivityForResult(intent) { result ->
                            if (result.resultCode != Activity.RESULT_OK) return@startActivityForResult
                            val treeUri = result.data?.data ?: return@startActivityForResult
                            val df = DocumentFile.fromTreeUri(getContext(), treeUri)
                                ?: return@startActivityForResult
                            val uriStr = treeUri.toString()
                            if (uriStr.endsWith(relativePath0)) {
                                runInCoroutine { FileDataStore.setStringValue(spKey, uriStr) }
                                //永久保存获取的目录权限
                                getContentResolver().takePersistableUriPermission(
                                    treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                                operation(df)
                            } else activity.runOnUiThread {
                                val msg = "授权文件夹与目标文件夹不一致，请重新设置（设置局部权限时请勿选择其他文件夹）"
                                DialogFactory.createActionDialog(
                                    activity, NormalText(msg), this, NormalText("操作失败")
                                ).show()
                            }
                        }
                    }
                }
            }
            //弹窗
            Global.doByActivity { activity ->
                activity.runOnUiThread {
                    DialogFactory.createActionDialog(
                        activity, NormalText(message), confirmListener, NormalText("提示")
                    ).show()
                }
            }
        }
    }


    /**
     * 获取外部空间的文件
     *
     * @param folderPath
     * @param myFileFilter
     * @param fileInfoCreator
     * @param callback
     * @param <T>
    </T> */
    private fun <T> getFileInExternalSpace(
        folderPath: String, myFileFilter: MyFileFilter?, fileInfoCreator: (MediaData) -> T,
        callback: (List<T>) -> Unit
    ) {
        assert(Build.VERSION.SDK_INT != Build.VERSION_CODES.Q) { "Android 10 不支持" }
        Global.doWithPermission(object : PermissionHelper.RequestPermissionsCallback(
            Pair(null, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        ) {
            override fun onGranted() {
                val list: MutableList<T> = ArrayList()
                val folder = File(folderPath)
                val files: Array<File>? = if (myFileFilter == null) folder.listFiles()
                else { //有过滤条件
                    if (myFileFilter.filenameFilter != null)
                        folder.listFiles(myFileFilter.filenameFilter)
                    else folder.listFiles(myFileFilter.fileFilter)
                }
                var mediaData: MediaData
                files?.takeIf { files.isNotEmpty() }?.forEach { f ->
                    mediaData = MediaData(
                        0, getUriForFile(f), f.name, f.absolutePath, null, 0, f.length(),
                        f.lastModified()
                    )
                    list.add(fileInfoCreator(mediaData))
                }
                callback(list)
            }
        })
    }


    /**
     * 删除外部公共空间的文件
     *
     * @param mediaType
     * @param selection
     * @param selectionArgs
     * @return
     */
    fun deleteFileInExternalPublicSpace(
        mediaType: MediaType, selection: String, selectionArgs: Array<String>?
    ): Int {
        return getContentResolver()
            .delete(getMediaStoreData(mediaType).contentUri!!, selection, selectionArgs)
    }

    /**
     * 删除外部公共空间的文件
     * @param mediaType MediaType
     * @param relativePath String
     * @param filter MyFileFilter
     * @return Boolean
     */
    fun deleteFileInExternalPublicSpace(
        mediaType: MediaType, relativePath: String, filter: MyFileFilter
    ): Boolean {
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            File(getPathFromExternalPublicSpace(mediaType, relativePath)).takeIf { it.exists() }
                ?.run {
                    filter.fileFilter?.let { listFiles(it) } ?: listFiles(filter.filenameFilter!!)
                        ?.let { files ->
                            doWithStoragePermission({ files.forEach { it.delete() } })
                        }
                }
            true
        } else {
            val selection = MediaStore.MediaColumns.RELATIVE_PATH + "=?"
            val args = arrayOf(getRelativePathInRoot(mediaType, relativePath))

            filter.doForMediaStore(selection, args) { full_selection, full_args ->
                deleteFileInExternalPublicSpace(mediaType, full_selection, full_args) > 0
            }
        }
    }

    /**
     * 删除外部公共空间的文件
     *
     * @param mediaType
     * @param relativePath
     * @param fileName
     * @return
     */
    fun deleteFileInExternalPublicSpace(
        mediaType: MediaType, relativePath: String, fileName: String
    ): Boolean {
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P)
            delete((getPathFromExternalPublicSpace(mediaType, relativePath) + fileName))
        else {
            val args = arrayOf(fileName, getRelativePathInRoot(mediaType, relativePath))
            deleteFileInExternalPublicSpace(mediaType, SELECTION_SPECIFY_FILE, args) > 0
        }
    }

    /**
     * 删除外部公共空间的文件夹
     *
     * @param mediaType
     * @param relativePath
     * @return
     */
    fun deleteFolderInExternalPublicSpace(mediaType: MediaType, relativePath: String): Boolean {
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val folderPath: String = getPathFromExternalPublicSpace(mediaType, relativePath)
            delete(folderPath)
        } else {
            val selection = MediaStore.MediaColumns.RELATIVE_PATH + "=?"
            val args = arrayOf(getRelativePathInRoot(mediaType, relativePath))
            deleteFileInExternalPublicSpace(mediaType, selection, args) > 0
        }
    }


    /**
     * 更新外部存储空间的文件
     *
     * @param mediaType
     * @param selection
     * @param selectionArgs
     * @param updateValues
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    fun updateFileInExternalPublicSpace(
        mediaType: MediaType, selection: String?, selectionArgs: Array<String>?,
        updateValues: ContentValues?
    ): Boolean {
        val mediaData = getMediaStoreData(mediaType)
        val uriList: List<Uri> =
            getFileInExternalPublicSpace(mediaData, selection, selectionArgs, null) { it.fileUri!! }
        return kotlin.runCatching {
            uriList.forEach { updateFileInExternalPublicSpace(it, updateValues) };true
        }.onFailure { it.printStackTrace() }.getOrDefault(false)
    }

    fun updateFileInExternalPublicSpace(fileUri: Uri?, updateValues: ContentValues?): Boolean =
        getContentResolver().update(fileUri!!, updateValues, null, null) > 0

    /**
     * 重命名
     *
     * @param mediaType
     * @param relativePath
     * @param oldFileName
     * @param newFileName
     * @return
     */
    fun renameFileInExternalPublicSpace(
        mediaType: MediaType, relativePath: String, oldFileName: String, newFileName: String
    ): Boolean {
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val folderPath: String = getPathFromExternalPublicSpace(mediaType, relativePath)
            val old = File(folderPath + oldFileName)
            old.renameTo(File(folderPath + newFileName))
        } else {
            val args = arrayOf(oldFileName, getRelativePathInRoot(mediaType, relativePath))
            val updateValues = ContentValues()
            updateValues.put(MediaStore.MediaColumns.DISPLAY_NAME, newFileName)
            updateFileInExternalPublicSpace(mediaType, SELECTION_SPECIFY_FILE, args, updateValues)
        }
    }


    /**
     * 删除文件 path或Uri
     */
    fun delete(filePath: String): Boolean {
        if (filePath.isEmpty()) return true
        return if (isUri(filePath)) delete(Uri.parse(filePath)) else delete(File(filePath))
    }

    /**
     *
     * @param file File
     * @return Boolean
     */
    fun delete(file: File): Boolean {
        if (!file.exists()) return true
        if (file.isDirectory) { //目录
            //先把目录下的文件都删除了，再删除目录
            file.list()?.let { children ->
                val size = children.size
                for (i in 0 until size) {
                    if (!delete(File(file, children[i]))) return false
                }
            }
        }
        return file.delete()
    }

    /**
     * 删除Uri对应的资源
     */
    fun delete(uri: Uri): Boolean {
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val file: File = getFileByUri(uri)
            if (file.exists()) file.delete() else true
        } else getContentResolver().delete(uri, null, null) > 0
    }


    /**
     * 执行需要存储权限的操作
     * 外部需判断 if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P)
     *
     * @param operation
     */
    fun doWithStoragePermission(
        operation: () -> Unit, onDenied: (() -> Unit)? = null, autoRequest: Boolean = true
    ) {
        Global.doWithPermission(
            object : PermissionHelper.RequestPermissionsCallback(Pair(null, permissions)) {
                override fun onGranted() = operation()
                override fun onDenied(context: Context?, deniedPermissions: MutableList<String>?) {
                    onDenied?.invoke()
                    super.onDenied(context, deniedPermissions)
                }
            }.apply { setAutoRequest(autoRequest) })
    }


    /**
     * @Description 文件过滤
     * @Author Naruto Yang
     * @CreateDate 2021/7/18 0018
     * @Note
     */
    class MyFileFilter {
        var fileFilter: FileFilter? = null
        var filenameFilter: FilenameFilter? = null
        var selection: String
        var selectionArgs: Array<String>?

        constructor(fileFilter: FileFilter, selection: String, selectionArgs: Array<String>?) {
            this.fileFilter = fileFilter
            this.selection = selection
            this.selectionArgs = selectionArgs
        }

        constructor(
            filenameFilter: FilenameFilter, selection: String, selectionArgs: Array<String>?
        ) {
            this.filenameFilter = filenameFilter
            this.selection = selection
            this.selectionArgs = selectionArgs
        }

        inline fun <T> doForMediaStore(
            def_selection: String, def_args: Array<String>,
            block: (full_selection: String, full_args: Array<String>) -> T
        ): T {
            var full_selection = def_selection
            var full_args = def_args.clone()
            if (selection.isNotEmpty()) { //有过滤条件
                if (!selection.trim().lowercase().startsWith("and")) full_selection += " and "
                full_selection += selection
                selectionArgs?.takeIf { it.isNotEmpty() }?.let { full_args += it }//合并参数
            }
            return block(full_selection, full_args)
        }
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