package com.naruto.lib.common.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.naruto.lib.common.R
import com.naruto.lib.common.list.OnItemClickListener
import com.naruto.lib.common.list.TextAdapter

/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2023/8/30 0030
 * @Note
 */
object PopupWindowFactory {
    /**
     * 创建选择弹窗
     * @param title String
     * @param rootView ViewGroup
     * @param list List<TextData>
     * @param onItemClickListener OnItemClickListener<TextData>
     * @return PopupWindow
     */
    fun <T> createSelectPopupWindow(
        title: String, rootView: ViewGroup, list: List<T>, getTextFunc: (T) -> String,
        onItemClickListener: OnItemClickListener<T>
    ): PopupWindow {
        val context = rootView.context
        val view = LayoutInflater.from(context).inflate(R.layout.pop_select, rootView, false)
        view.findViewById<TextView>(R.id.tv_title).text = title
        val popWindow = PopupWindow(
            view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, true
        )
        popWindow.animationStyle = R.style.PopupWindowAnimation
        popWindow.elevation = context.resources.getDimension(R.dimen.dp_10)
        //列表
        view.findViewById<RecyclerView>(R.id.recyclerView).run {
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            adapter = object : TextAdapter<T>(list) {
                override fun getText(data: T): String = getTextFunc.invoke(data)
                override fun getLayoutRes(viewType: Int): Int = R.layout.item_simple
            }.apply {
                setOnItemClickListener(object : OnItemClickListener<T> {
                    override fun onClick(data: T, position: Int, view: View) {
                        popWindow.dismiss();onItemClickListener.onClick(data, position, view)
                    }
                })
            }
        }
        return popWindow
    }
}