package com.naruto.lib.common.base;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;

import com.naruto.lib.common.R;
import com.naruto.lib.common.helper.PermissionHelper;
import com.naruto.lib.common.utils.DialogFactory;
import com.naruto.lib.common.utils.LogUtils;

import java.util.Map;

/**
 * @Purpose
 * @Author Naruto Yang
 * @CreateDate 2020/11/13 0013
 * @Note
 */
public abstract class BaseActivity extends AppCompatActivity implements BaseView {
    private ActivityResultLauncher<String[]> permissionLauncher;//权限申请启动器
    private ActivityResultCallback<Map<String, Boolean>> permissionCallback;//权限申请回调
    private ActivityResultLauncher<Intent> activityLauncher;//Activity启动器
    private ActivityResultCallback<ActivityResult> activityCallback;//Activity启动回调
    private PermissionHelper permissionHelper = null;


    public AlertDialog loadingDialog;//加载弹窗
    protected View rootView;//根布局，即getLayoutRes()返回的布局

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutRes());
        //注册权限申请启动器
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions()
                , result -> {
                    permissionCallback.onActivityResult(result);
                    permissionCallback = null;
                });
        //注册Activity启动器
        activityLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult()
                , result -> {//Android 12及以上，跳转到设置页面后，在设置界面可以改变位置信息使用权，如果从确切位置降级到大致位置，系统会重启应用的进程，则activityCallback==null。但依旧会走到这里，故需判断activityCallback是否为空
                    if (activityCallback != null) {
                        activityCallback.onActivityResult(result);
                        activityCallback = null;
                    }
                });

        rootView = ((ViewGroup) getWindow().getDecorView().findViewById(android.R.id.content)).getChildAt(0);

        init();
    }

    @Override
    public View getRootView() {
        return rootView;
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public LifecycleOwner getLifecycleOwner() {
        return this;
    }

    @Override
    public Context getContext() {
        return this;
    }

    /**
     * 展示等待对话框
     */
    public void showLoadingDialog() {
        showLoadingDialog(getString(R.string.hint_loading));
    }

    /**
     * 展示等待对话框
     */
    public void showLoadingDialog(String msg) {
        if (loadingDialog == null) {
            loadingDialog = DialogFactory.Companion.makeSimpleDialog(this, R.layout.dialog_loading);
            loadingDialog.getWindow().setDimAmount(0f);//移除遮罩层
            loadingDialog.setCancelable(false);
        }
        if (!TextUtils.isEmpty(msg)) loadingDialog.setMessage(msg);
        if (!loadingDialog.isShowing()) {
            loadingDialog.show();
        }
    }

    /**
     * 隐藏等待对话框
     */
    public void dismissLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    public void updateLoadingDialogMsg(String msg) {
        if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.setMessage(msg);
    }

    /**
     * 检查并申请权限
     *
     * @param callback
     */
    public void doWithPermission(PermissionHelper.RequestPermissionsCallback callback) {
        if (permissionHelper == null) permissionHelper = new PermissionHelper() {
            @NonNull
            @Override
            public Context getContext() {
                return BaseActivity.this;
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
                BaseActivity.this.startActivityForResult(intent, callback);
            }
        };

        permissionHelper.doWithPermission(callback);
    }


    /**
     * startActivityForResult
     *
     * @param intent
     * @param callback
     */
    public void startActivityForResult(Intent intent, ActivityResultCallback<ActivityResult> callback) {
        if (activityCallback != null) {
            LogUtils.INSTANCE.e("--->startActivityForResult: ", new Exception("ActivityCallback!=null"));
            return;
        }
        activityCallback = callback;
        activityLauncher.launch(intent);
    }


    public void startActivity(Class<? extends Activity> activityClass) {
        startActivity(new Intent(this, activityClass));
    }

    /**
     * 启动其他页面
     *
     * @param activityClass
     */
    protected void launchActivity(Class<? extends Activity> activityClass) {
        Intent intent = new Intent(this, activityClass);
        startActivity(intent);
    }

}
