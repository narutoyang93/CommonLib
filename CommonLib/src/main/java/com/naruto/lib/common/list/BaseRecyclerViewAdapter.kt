package com.naruto.lib.common.list

import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.util.forEach
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.naruto.lib.common.Extension.getOrPut
import com.naruto.lib.common.utils.setMyOnClickListener

/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2023/8/30 0030
 * @Note
 */
abstract class BaseRecyclerViewAdapter<T, E : VH<T>>(list: List<T>? = null) : Adapter<E>() {
    private val dataList: MutableList<T> = list?.toMutableList() ?: mutableListOf()
    private val clickListenerMap = SparseArray<SparseArray<OnItemClickListener<T>>>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): E {
        val view = LayoutInflater.from(parent.context)
            .inflate(getLayoutRes(viewType), parent, false)
        return onCreateViewHolder(view).also { initViewHolder(viewType, it) }
    }

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: E, position: Int) {
        holder.bindData(getData(position))
    }

    open fun getData(position: Int): T = dataList[position]

    open fun initViewHolder(viewType: Int, viewHolder: E) {
        viewHolder.dataGetter = { position -> getData(position) }
        clickListenerMap[viewType]?.let { viewHolder.setClickListener(it) }//item上多个view点击
    }

    abstract fun onCreateViewHolder(view: View): E

    @LayoutRes
    abstract fun getLayoutRes(viewType: Int): Int

    fun addClickListener(listenerMap: SparseArray<OnItemClickListener<T>>, viewType: Int = 0) {
        clickListenerMap.put(viewType, listenerMap)
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener<T>, viewType: Int = 0) {
        clickListenerMap.getOrPut(viewType) { SparseArray<OnItemClickListener<T>>() }
            .put(-1, onItemClickListener)
    }
}

/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2023/8/30 0030
 * @Note
 */
abstract class VH<T>(view: View) : RecyclerView.ViewHolder(view) {
    private val viewMap = SparseArray<View>()
    internal lateinit var dataGetter: (position: Int) -> T
    fun setClickListener(map: SparseArray<OnItemClickListener<T>>) {
        map.forEach { id, listener ->
            (if (id == -1) itemView else getView(id))?.setMyOnClickListener {
                listener.onClick(getData(adapterPosition), adapterPosition, it)
            }
        }
    }

    /**
     * 根据Id获取view
     *
     * @param id
     * @param <E>
     * @return
    </E> */
    protected open fun <V : View?> getView(id: Int): V? = viewMap.get(id) as? V
        ?: (itemView.findViewById<V>(id).also { viewMap.put(id, it) })

    protected fun getData(position: Int): T = dataGetter.invoke(position)

    abstract fun bindData(data: T)
}

interface OnItemClickListener<T> {
    fun onClick(data: T, position: Int, view: View)
}


/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2023/8/31 0031
 * @Note
 */
abstract class TextAdapter<T>(list: List<T>? = null) :
    BaseRecyclerViewAdapter<T, TextAdapter.TextVH<T>>(list) {
    override fun onCreateViewHolder(view: View): TextVH<T> = object : TextVH<T>(view) {
        override fun getText(data: T): String = this@TextAdapter.getText(data)
    }

    abstract fun getText(data: T): String

    abstract class TextVH<T>(view: View) : VH<T>(view) {
        override fun bindData(data: T) {
            getTextView().text = getText(data)
        }

        open fun getTextView(): TextView = itemView as TextView

        abstract fun getText(data: T): String
    }
}
