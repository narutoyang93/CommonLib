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
import android.util.SparseArray;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

import com.naruto.lib.common.base.ContextBridge;
import com.naruto.lib.common.utils.DialogFactory;
import com.naruto.lib.common.utils.LogUtils;

import java.lang.ref.WeakReference;
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
            String[] requestPermissionsArray = requestPermissionsList.toArray(new String[0]);
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
    public List<String> checkPermissions(String... permissions) {
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
                , "提示", requestPermissionReason + "，是否前往设置？", intent
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
        @SafeVarargs
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


    /**
     * @Description
     * @Author Naruto Yang
     * @CreateDate 2022/9/15 0015
     * @Note
     */
    public static abstract class ActivityPermissionHelper<T extends Activity> extends PermissionHelper {
        private final WeakReference<T> activityWF;

        public ActivityPermissionHelper(T activity) {
            activityWF = new WeakReference<>(activity);
        }

        public void doWithActivity(@NonNull Operation<T> operation, @NonNull Runnable onActivityNotFound) {
            if (activityWF.get() == null) onActivityNotFound.run();
            else operation.execute(activityWF.get());
        }

        @NonNull
        @Override
        public Context getContext() {
            return activityWF.get();
        }
    }


    /**
     * @Description
     * @Author Naruto Yang
     * @CreateDate 2022/9/15 0015
     * @Note 需在OnResume前初始化
     */
    public static class NormalActivityPermissionHelper extends ActivityPermissionHelper<ComponentActivity> {
        private final ActivityResultLauncher<String[]> permissionLauncher;//权限申请启动器
        private ActivityResultCallback<Map<String, Boolean>> permissionCallback;//权限申请回调
        private final ActivityResultLauncher<Intent> activityLauncher;//Activity启动器
        private ActivityResultCallback<ActivityResult> activityCallback;//Activity启动回调

        public NormalActivityPermissionHelper(ComponentActivity activity) {
            super(activity);
            //注册权限申请启动器
            permissionLauncher = activity.registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        permissionCallback.onActivityResult(result);
                        permissionCallback = null;
                    });

            //注册权限申请启动器
            activityLauncher = activity.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {//Android 12及以上，跳转到设置页面后，在设置界面可以改变位置信息使用权，如果从确切位置降级到大致位置，系统会重启应用的进程，则activityCallback==null。但依旧会走到这里，故需判断activityCallback是否为空
                        if (activityCallback != null) {
                            activityCallback.onActivityResult(result);
                            activityCallback = null;
                        }
                    });
        }

        @Override
        public void requestPermissions(@NonNull String[] permissions, @NonNull ActivityResultCallback<Map<String, Boolean>> callback) {
            if (permissionCallback != null) {
                LogUtils.INSTANCE.e("--->requestPermissions: ", new Exception("permissionCallback!=null"));
                return;
            }

            permissionCallback = callback;
            permissionLauncher.launch(permissions);
        }

        @Override
        public void starActivityForResult(@NonNull Intent intent, @NonNull ActivityResultCallback<ActivityResult> callback) {
            if (activityCallback != null) {
                LogUtils.INSTANCE.e("--->startActivityForResult: ", new Exception("ActivityCallback!=null"));
                return;
            }

            activityCallback = callback;
            activityLauncher.launch(intent);
        }
    }


    /**
     * @Description
     * @Author Naruto Yang
     * @CreateDate 2022/9/9 0009
     * @Note 需要在Activity的onActivityResult和onRequestPermissionsResult里调用LegacyActivityPermissionHelper中对应方法
     */
    public static class LegacyActivityPermissionHelper extends ActivityPermissionHelper<Activity> {
        private final SparseArray<ActivityResultCallback<ActivityResult>> activityResultCallbackQueue = new SparseArray<>();
        private final SparseArray<ActivityResultCallback<Map<String, Boolean>>> permissionsResultCallbackQueue = new SparseArray<>();

        public LegacyActivityPermissionHelper(Activity activity) {
            super(activity);
        }

        @Override
        @RequiresApi(api = Build.VERSION_CODES.M)
        public void requestPermissions(@NonNull String[] permissions, @NonNull ActivityResultCallback<Map<String, Boolean>> callback) {
            doWithActivity(activity -> {
                final int requestCode = createRequestCode();
                permissionsResultCallbackQueue.put(requestCode, callback);
                activity.requestPermissions(permissions, requestCode);
            }, () -> {
                Map<String, Boolean> map = new HashMap<>();
                for (String permission : permissions) {
                    map.put(permission, false);
                }
                callback.onActivityResult(map);
            });
        }


        @Override
        public void starActivityForResult(@NonNull Intent intent, @NonNull androidx.activity.result.ActivityResultCallback<ActivityResult> callback) {
            doWithActivity(activity -> {
                int requestCode = createRequestCode();
                activityResultCallbackQueue.put(requestCode, callback);
                activity.startActivityForResult(intent, requestCode);
            }, () -> callback.onActivityResult(new ActivityResult(Activity.RESULT_CANCELED, null)));
        }

        private int createRequestCode() {
            return (int) (System.currentTimeMillis() / 1000);
        }

        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            ActivityResultCallback<ActivityResult> callback = activityResultCallbackQueue.get(requestCode);
            if (callback == null) return;
            callback.onActivityResult(new ActivityResult(resultCode, data));
            activityResultCallbackQueue.remove(requestCode);
        }

        public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
            ActivityResultCallback<Map<String, Boolean>> callback = permissionsResultCallbackQueue.get(requestCode);
            if (callback == null) return;
            Map<String, Boolean> map = new HashMap<>();
            for (int i = 0; i < permissions.length; i++) {
                map.put(permissions[i], grantResults[i] == PackageManager.PERMISSION_GRANTED);
            }
            callback.onActivityResult(map);
            permissionsResultCallbackQueue.remove(requestCode);
        }
    }

    /**
     * @Description
     * @Author Naruto Yang
     * @CreateDate 2022/9/7 0007
     * @Note
     */
    public interface Operation<T> {
        void execute(T t);
    }
}
