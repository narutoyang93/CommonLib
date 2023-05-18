package com.naruto.lib.common.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.naruto.lib.common.R
import com.naruto.lib.common.TopFunction.getResString
import com.naruto.lib.common.base.ContextBridge
import com.naruto.lib.common.utils.DialogFactory.OnDialogButtonClickListener

/**
 * @Description 构建弹窗
 * @Author Naruto Yang
 * @CreateDate 2022/3/9 0009
 * @Note
 */
class DialogFactory {
    companion object {
        /**
         * 弹窗提示信息
         * @param context Context
         * @param message TextParam
         * @param contentGravityCenter Boolean
         * @param confirmText TextParam
         * @param onConfirmListener Function0<Unit>?
         * @param viewProcessor Function1<View, Unit>?
         * @return AlertDialog
         */
        @JvmOverloads
        fun showHintDialog(
            context: Context, message: String, title: String? = null,
            contentGravityCenter: Boolean = true,
            confirmText: String = getResString(R.string.text_confirm),
            onConfirmListener: (() -> Unit)? = null,
            viewProcessor: ((AlertDialog, View) -> Unit)? = null
        ): AlertDialog {
            val option = ActionDialogOption(
                content = message, confirmText = confirmText, title = title,
                confirmListener = if (onConfirmListener == null) null
                else OnDialogButtonClickListener { view, dialog -> onConfirmListener() },
                cancelText = null, contentGravityCenter = contentGravityCenter
            )

            val dialog = createActionDialog(context, option, viewProcessor)
            if (context !is Activity) dialog.showWithoutActivity() else dialog.show()
            return dialog
        }

        /**
         * 前往设置页面弹窗
         *
         * @param activity
         * @param message
         * @param intent
         * @param onCancel
         * @param activityResultCallback
         * @return
         */
        fun createGoSettingDialog(
            contextBridge: ContextBridge, title: String, message: String, intent: Intent,
            onCancel: () -> Unit, activityResultCallback: ActivityResultCallback<ActivityResult>
        ): AlertDialog {
            return createActionDialog(
                contextBridge.context, ActionDialogOption(
                    title = title, content = message,
                    confirmText = getResString(R.string.text_go_to_setting),
                    cancelListener = { _, _ -> onCancel() },
                    confirmListener = { _, _ ->
                        contextBridge.starActivityForResult(intent, activityResultCallback)
                    })
            )
        }

        /**
         * 构建弹窗
         * @param context Context
         * @param content TextParam
         * @param confirmListener OnDialogButtonClickListener
         * @param title TextParam?
         * @param viewProcessor Function2<AlertDialog, View, Unit>?
         * @return AlertDialog
         */
        @JvmOverloads
        fun createActionDialog(
            context: Context, content: String,
            confirmListener: OnDialogButtonClickListener,
            title: String? = null, viewProcessor: ((AlertDialog, View) -> Unit)? = null
        ): AlertDialog {
            val option = ActionDialogOption(
                title = title, content = content, confirmListener = confirmListener
            )
            return createActionDialog(context, option, viewProcessor)
        }

        /**
         * 构建弹窗
         * @param context Context
         * @param option ActionDialogOption
         * @param viewProcessor Function1<View, Unit>?
         * @return AlertDialog
         */
        @JvmOverloads
        fun createActionDialog(
            context: Context, option: ActionDialogOption,
            viewProcessor: ((AlertDialog, View) -> Unit)? = null
        ): AlertDialog {
            return createDialog(context, option.layoutResId, { dialog, view ->
                dialog.setCancelable(option.cancelable)
                //标题
                setText(view, option.titleViewId, option.title)
                //文本内容
                setText(view, option.contentViewId, option.content)
                if (!option.contentGravityCenter)
                    doWithTextView(view, option.contentViewId) { it.gravity = Gravity.LEFT }

                with(option) {
                    setActionButton(dialog, view, cancelViewId, cancelText, cancelListener)//取消按钮
                    setActionButton(dialog, view, confirmViewId, confirmText, confirmListener)//确定按钮
                    //中立（第三个）按钮
                    setActionButton(dialog, view, neutralViewId, neutralText, neutralListener)
                }

                viewProcessor?.invoke(dialog, view)
            }, option.themeResId)
        }


        /**
         *
         * @param context Context
         * @param layoutResId Int
         * @param viewProcessor Function2<AlertDialog, View, Unit>?用于对内容View进一步处理
         * @param themeResId Int
         * @return AlertDialog
         */
        @JvmOverloads
        fun createDialog(
            context: Context, @LayoutRes layoutResId: Int,
            viewProcessor: ((AlertDialog, View) -> Unit)? = null,
            themeResId: Int = R.style.DefaultDialogStyle
        ): AlertDialog {
            val builder = AlertDialog.Builder(context, themeResId)
            val view = LayoutInflater.from(context).inflate(layoutResId, null)
            val dialog = builder.setView(view).create()
            viewProcessor?.invoke(dialog, view)
            return dialog
        }


        /**
         * 显示原生弹窗
         *
         * @param context
         * @param title
         * @param content
         * @param cancelText
         * @param confirmText
         * @param onCancel
         * @param onConfirm
         */
        @JvmOverloads
        fun showNativeDialog(
            context: Context, title: String?, content: String, cancelText: String = "取消",
            confirmText: String = "确定", onCancel: Runnable? = null, onConfirm: Runnable
        ) {
            showNativeDialog(
                context, title, { setMessage(content) }, cancelText, confirmText,
                onCancel, onConfirm
            )
        }

        @JvmOverloads
        fun showNativeDialog(
            context: Context, title: String?, option: AlertDialog.Builder.() -> Unit,
            cancelText: String = "取消", confirmText: String = "确定",
            onCancel: Runnable? = null, onConfirm: Runnable
        ) {
            val dialog = AlertDialog.Builder(context, R.style.NativeDialogStyle)
                .setCancelable(false)
                .setNegativeButton(cancelText) { dialog: DialogInterface, which: Int ->
                    dialog.dismiss()
                    onCancel?.run()
                }
                .setPositiveButton(confirmText) { dialog: DialogInterface, which: Int ->
                    dialog.dismiss()
                    onConfirm.run()
                }.apply { if (!title.isNullOrEmpty()) setTitle(title);option() }.create()
            if (context !is Activity) dialog.showWithoutActivity() else dialog.show()
        }


        /**
         * 设置操作按钮
         * @param dialog AlertDialog
         * @param dialogView View
         * @param viewId Int
         * @param text TextParam?
         * @param onClickListener OnDialogButtonClickListener?
         */
        private fun setActionButton(
            dialog: AlertDialog, dialogView: View, viewId: Int, text: String?,
            onClickListener: OnDialogButtonClickListener?
        ) {
            setText(dialogView, viewId, text) {
                it.setOnClickListener { v ->
                    if (onClickListener == null || onClickListener.dismissible()) dialog.dismiss()
                    onClickListener?.onClick(v, dialog)
                }
            }
        }

        /**
         * 设置文本
         *
         * @param dialogView
         * @param textViewId
         * @param text
         */
        private fun setText(
            dialogView: View, textViewId: Int, text: String?, block: ((TextView) -> Unit)? = null
        ) {
            doWithTextView(dialogView, textViewId) { textView ->
                textView.isVisible = (text != null)
                if (text == null) return@doWithTextView
                textView.text = text
                block?.invoke(textView)
            }
        }

        private fun doWithTextView(
            dialogView: View, textViewId: Int, operation: (TextView) -> Unit
        ) {
            val textView = dialogView.findViewById<TextView>(textViewId)
            if (textView != null) operation(textView)
        }

    }


    /**
     * @Purpose 弹窗所需配置的数据
     * @Author Naruto Yang
     * @CreateDate 2020/4/01 0001
     * @Note
     */
    data class ActionDialogOption(
        var title: String? = null,//标题
        val content: String,//内容
        var cancelText: String? = getResString(R.string.text_cancel),//取消按钮文本
        val confirmText: String = getResString(R.string.text_confirm),//确定按钮文本
        val neutralText: String? = null,//中立（第三个）按钮文本
        var contentGravityCenter: Boolean = true,//内容文本是否居中
        var cancelListener: OnDialogButtonClickListener? = null,
        var confirmListener: OnDialogButtonClickListener? = null,
        var neutralListener: OnDialogButtonClickListener? = null,//中立（第三个）按钮点击监听
        var cancelable: Boolean = cancelListener == null,
        @LayoutRes var layoutResId: Int = R.layout.dialog_normal, //布局
        @StyleRes var themeResId: Int = R.style.DefaultDialogStyle,
        @IdRes var titleViewId: Int = R.id.tv_title,
        @IdRes var contentViewId: Int = R.id.tv_content,
        @IdRes var cancelViewId: Int = R.id.btn_cancel,
        @IdRes var confirmViewId: Int = R.id.btn_confirm,
        @IdRes var neutralViewId: Int = R.id.btn_neutral//中立（第三个）按钮ID
    ) {
        constructor(content: String, confirmText: String)
                : this(null, content = content, confirmText = confirmText)
    }


    /**
     * @Description
     * @Author Naruto Yang
     * @CreateDate 2022/9/22 0022
     * @Note
     */
    @JvmDefaultWithoutCompatibility
    fun interface OnDialogButtonClickListener {
        fun onClick(view: View, dialog: Dialog)
        fun dismissible(): Boolean = true//是否让弹窗dismiss
    }
}

/**
 * 以系统弹窗的方式（不需要activity）弹出弹窗
 * @receiver AlertDialog
 */
fun AlertDialog.showWithoutActivity() {
    val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    else WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
    window?.setType(windowType)
    runCatching { show() }.onFailure { it.printStackTrace() }
}