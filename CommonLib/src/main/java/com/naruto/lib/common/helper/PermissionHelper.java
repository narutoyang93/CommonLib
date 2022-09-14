package com.naruto.lib.common.helper;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

import com.naruto.lib.common.base.ContextBridge;
import com.naruto.lib.common.utils.DialogFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kotlin.Unit;

/**
 * @Description 处理权限请求
 * @Author Naruto Yang
 * @CreateDate 2022/9/7 0007
 * @Note
 */
public abstract class PermissionHelper implements ContextBridge {

    /**
     * 检查并申请权限
     *
     * @param callback
     */
    public void doWithPermission(RequestPermissionsCallback callback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {//6.0以下系统无需动态申请权限
            if (callback != null) callback.onGranted();
            return;
        }
        //检查权限
        List<String> requestPermissionsList = checkPermissions(callback.getRequestPermissions());//记录需要申请的权限
        if (requestPermissionsList.isEmpty()) {//均已授权
            callback.onGranted();
        } else if (callback.autoRequest) {//申请
            String[] requestPermissionsArray = requestPermissionsList.toArray(new String[requestPermissionsList.size()]);
            requestPermissions(requestPermissionsArray, result -> {
                        Context context = getContext();
                        List<String> refuseList = new ArrayList<>();//被拒绝的权限
                        List<String> shouldShowReasonList = new ArrayList<>();//需要提示申请理由的权限，即没有被设置“不再询问”的权限
                        String permission;
                        String requestPermissionReason = null;
                        for (Map.Entry<String, Boolean> entry : result.entrySet()) {
                            if (entry.getValue()) continue;
                            refuseList.add(permission = entry.getKey());
                            if (TextUtils.isEmpty(requestPermissionReason))
                                requestPermissionReason = callback.permissionMap.get(permission);
                            if (context instanceof Activity && ((Activity) context).shouldShowRequestPermissionRationale(permission))
                                shouldShowReasonList.add(permission);
                        }
                        if (refuseList.contains(Manifest.permission.ACCESS_FINE_LOCATION)) {//Android 12 定位权限允许单独授权，若已授权ACCESS_COARSE_LOCATION，则拒绝ACCESS_FINE_LOCATION后无需再次询问
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                refuseList.remove(Manifest.permission.ACCESS_FINE_LOCATION);
                            }
                        }
                        if (refuseList.isEmpty()) {//全部已授权
                            callback.onGranted();
                        } else {//被拒绝
                            if (TextUtils.isEmpty(requestPermissionReason)) {
                                callback.onDenied(context, refuseList);//直接执行拒绝后的操作
                            } else {//弹窗
                                if (shouldShowReasonList.isEmpty()) //被设置“不再询问”
                                    showGoToSettingPermissionDialog(callback, refuseList, requestPermissionReason);//弹窗引导前往设置页面
                                else
                                    showPermissionRequestReasonDialog(callback, refuseList, requestPermissionReason);//弹窗显示申请原因并重新请求权限
                            }
                        }
                    }
            );
        }
    }

    /**
     * 检查权限
     *
     * @param permissions
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    protected List<String> checkPermissions(String... permissions) {
        List<String> list = new ArrayList<>();
        for (String p : permissions) {
            if (getContext().checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {//未授权，记录下来
                list.add(p);
            }
        }
        return list;
    }

    /**
     * 显示引导设置权限弹窗
     *
     * @param callback
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void showGoToSettingPermissionDialog(RequestPermissionsCallback callback, List<String> deniedPermissions, String requestPermissionReason) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
        intent.setData(uri);

        DialogFactory.Companion.makeGoSettingDialog(this
                , requestPermissionReason + "，是否前往设置？", intent
                , () -> {
                    callback.onDenied(getContext(), deniedPermissions);
                    return Unit.INSTANCE;
                }
                , result -> {
                    if (checkPermissions(callback.getRequestPermissions()).isEmpty()) {//已获取权限
                        callback.onGranted();
                    } else {//被拒绝
                        callback.onDenied(getContext(), deniedPermissions);
                    }
                }).show();
    }

    /**
     * 显示申请权限理由
     *
     * @param callback
     * @return
     */
    private void showPermissionRequestReasonDialog(RequestPermissionsCallback callback, List<String> deniedPermissions, String requestPermissionReason) {
        DialogFactory.DialogData dialogData = new DialogFactory.DialogData();
        dialogData.setTitle("提示");
        dialogData.setContent(requestPermissionReason);
        dialogData.setCancelText("取消");
        dialogData.setConfirmText("授予");
        dialogData.setCancelListener((v) -> callback.onDenied(getContext(), deniedPermissions));
        dialogData.setConfirmListener((v) -> doWithPermission(callback));
        AlertDialog dialog = DialogFactory.Companion.makeSimpleDialog(getContext(), dialogData);
        dialog.show();
    }


    /**
     * @Purpose 申请权限后处理接口
     * @Author Naruto Yang
     * @CreateDate 2019/12/19
     * @Note
     */
    public static abstract class RequestPermissionsCallback {
        public Map<String, String> permissionMap = new HashMap<>();//key为权限，value为申请原因
        public boolean autoRequest = true;//是否自动申请权限

        /**
         * @param permissionMap //key为申请原因,value为权限数组
         */
        public RequestPermissionsCallback(Map<String, String[]> permissionMap) {
            for (Map.Entry<String, String[]> entry : permissionMap.entrySet()) {
                for (String permission : entry.getValue()) {
                    this.permissionMap.put(permission, entry.getKey());
                }
            }
        }


        /**
         * @param permissionPairs pair(申请理由，权限)
         */
        public RequestPermissionsCallback(Pair<String, String[]>... permissionPairs) {
            for (Pair<String, String[]> pair : permissionPairs) {
                for (String s : pair.second) {
                    permissionMap.put(s, pair.first);
                }
            }
        }

        public RequestPermissionsCallback(String[] permissions) {
            for (String p : permissions) {
                permissionMap.put(p, null);
            }
        }

        public void setAutoRequest(boolean autoRequest) {
            this.autoRequest = autoRequest;
        }

        public String[] getRequestPermissions() {
            return permissionMap.keySet().toArray(new String[0]);
        }

        /**
         * 已授权
         */
        public abstract void onGranted();

        /**
         * 被拒绝
         *
         * @param context
         */
        public void onDenied(Context context, List<String> deniedPermissions) {
            Toast.makeText(context, "授权失败", Toast.LENGTH_SHORT).show();
        }

    }
}
