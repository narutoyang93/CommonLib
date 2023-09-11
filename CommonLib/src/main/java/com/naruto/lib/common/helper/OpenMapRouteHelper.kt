package com.naruto.lib.common.helper

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.annotation.FloatRange
import androidx.appcompat.app.AppCompatActivity
import com.naruto.lib.common.list.OnItemClickListener
import com.naruto.lib.common.utils.DialogFactory
import com.naruto.lib.common.utils.LifecycleUtil
import com.naruto.lib.common.utils.PopupWindowFactory

/**
 * @Description 启动地图规划路线
 * @Author Naruto Yang
 * @CreateDate 2023/9/5 0005
 * @Note
 */
class OpenMapRouteHelper(private val activity: AppCompatActivity) {
    private val rootView = activity.window.decorView.findViewById(android.R.id.content) as ViewGroup

    private val transportTypePopupWindow by lazy {
        createSelectPopupWindow(
            "选择交通方式", TransportType.values().asList(), { it.text }, { openMapRoute(it) })
    }

    private val destroyCallback =
        LifecycleUtil.addDestroyObserver(activity, this) { destroy() }

    private lateinit var destination: LatLng //目的地经纬度

    private lateinit var destinationName: String //目的地名称

    /**
     * 打开地图规划路线
     * @param latitude Double
     * @param longitude Double
     * @param destinationName String 目的地名称，用于UI显示
     */
    fun openMapRoute(
        @FloatRange(from = 0.0) latitude: Double,
        @FloatRange(from = 0.0) longitude: Double,
        destinationName: String
    ) {
        this.destination = LatLng(latitude, longitude)
        this.destinationName = destinationName
        showPopupWindow(transportTypePopupWindow)
    }

    /**
     * 显示选择弹窗
     */
    private fun showPopupWindow(popupWindow: PopupWindow) {
        popupWindow.showAtLocation(rootView, Gravity.BOTTOM, 0, 0)
    }

    /**
     * 创建选择弹窗
     * @param title String
     * @param list List<T>
     * @param getTextFunc Function1<T, String>
     * @param onClickListener Function1<T, Unit>
     * @return PopupWindow
     */
    private fun <T> createSelectPopupWindow(
        title: String, list: List<T>, getTextFunc: (T) -> String, onClickListener: (T) -> Unit
    ): PopupWindow {
        return PopupWindowFactory.createSelectPopupWindow(title, rootView, list, getTextFunc,
            object : OnItemClickListener<T> {
                override fun onClick(data: T, position: Int, view: View) {
                    onClickListener(data)
                }
            })
    }

    /**
     * 打开百度地图规划路线
     */
    private fun openMapRoute(transportType: TransportType) {
        val packageName = activity.packageName
        val list = mutableListOf<MapClient>()
        //百度
        object : MapClient("百度地图", "com.baidu.BaiduMap") {
            override fun createIntent(): Intent = Intent().apply {
                data =
                    Uri.parse("baidumap://map/direction?origin=我的位置&destination=name:$destinationName|latlng:${destination.latitude},${destination.longitude}&coord_type=gcj02&mode=${transportType.baiDuValue}&sy=3&src=$packageName")
            }
        }.let { list.add(it) }
        //高德
        object : MapClient("高德地图", "com.autonavi.minimap") {
            override fun createIntent(): Intent = Intent().apply {
                action = Intent.ACTION_VIEW
                addCategory(Intent.CATEGORY_DEFAULT)
                data =
                    Uri.parse("amapuri://route/plan/?sid=&slat=&slon=&sname=我的位置&did=&dlat=${destination.latitude}&dlon=${destination.longitude}&dname=$destinationName&dev=0&t=${transportType.gaoDeValue}&sourceApplication=${packageName}")
            }
        }.let { list.add(it) }

        val enableList = list.filter { it.hasInstalled(activity) }
        when (enableList.size) {
            0 -> createSelectPopupWindow("选择导航APP", list, { it.appName }) {
                DialogFactory.createActionDialog(
                    activity, "您尚未安装${it.appName}app，是否前往安装？",
                    { _, _ ->
                        val uri = Uri.parse("market://details?id=" + "com.baidu.BaiduMap")
                        startActivity(Intent(Intent.ACTION_VIEW, uri), "启动应用市场失败")
                    }).show()
            }.let { showPopupWindow(it) }

            1 -> enableList[0].let { startActivity(it.intent, "唤起${it.appName}失败") }
            else -> createSelectPopupWindow("选择导航APP", list, { it.appName })
            { startActivity(it.intent, "唤起${it.appName}失败") }.let { showPopupWindow(it) }
        }
    }

    /**
     * 启动activity
     * @param intent Intent
     * @param errorMsgTitle String
     */
    private fun startActivity(intent: Intent, errorMsgTitle: String) {
        runCatching {
            activity.startActivity(intent)
        }.onFailure { e ->
            e.printStackTrace()
            DialogFactory.showHintDialog(activity, e.message ?: "未知异常", errorMsgTitle)
        }
    }

    private fun destroy() {
        destroyCallback()
    }

    /**
     * @Description
     * @Author Naruto Yang
     * @CreateDate 2023/8/10 0010
     * @Note
     */
    private abstract class MapClient(val appName: String, val appPackageName: String) {
        val intent: Intent by lazy { createIntent() }
        protected abstract fun createIntent(): Intent
        fun hasInstalled(context: Context): Boolean = context.packageManager.runCatching {
            getPackageInfo(appPackageName, PackageManager.GET_ACTIVITIES) != null
        }.onFailure { it.printStackTrace() }.getOrDefault(false)
    }

    /**
     * @Description 交通方式
     * @Author Naruto Yang
     * @CreateDate 2023/8/8 0008
     * @Note
     */
    private enum class TransportType(
        val text: String, val baiDuValue: String, val gaoDeValue: Int
    ) {
        DRIVING("驾车", "driving", 0), TRANSIT("公共交通", "transit", 1),
        WALKING("步行", "walking", 2), RIDING("骑行", "riding", 3)
    }

    data class LatLng(val latitude: Double, val longitude: Double)

}