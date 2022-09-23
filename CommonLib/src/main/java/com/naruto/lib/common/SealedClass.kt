package com.naruto.lib.common

import android.widget.TextView
import androidx.annotation.StringRes

/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2022/9/23 0023
 * @Note
 */


/**
 * @Description 用于文本传参
 * @Author Naruto Yang
 * @CreateDate 2022/9/22 0022
 * @Note
 */
sealed class TextParam {
    abstract fun setToTextView(textView: TextView)
}

class ResText(@StringRes val resId: Int) : TextParam() {
    override fun setToTextView(textView: TextView) {
        textView.setText(resId)
    }
}

class NormalText(val text: CharSequence) : TextParam() {
    override fun setToTextView(textView: TextView) {
        textView.text = text
    }
}