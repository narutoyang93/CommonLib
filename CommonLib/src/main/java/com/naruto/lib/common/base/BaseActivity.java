package com.naruto.lib.common.base;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;

import com.naruto.lib.common.Global;
import com.naruto.lib.common.R;
import com.naruto.lib.common.helper.PermissionHelper;
import com.naruto.lib.common.utils.DeviceInfoUtil;
import com.naruto.lib.common.utils.DialogFactory;
import com.naruto.lib.common.utils.LogUtils;

import java.util.List;

/**
 * @Purpose
 * @Author Naruto Yang
 * @CreateDate 2020/11/13 0013
 * @Note
 */
public abstract class BaseActivity extends AppCompatActivity implements BaseView {
    private final PermissionHelper.NormalActivityPermissionHelper permissionHelper = new PermissionHelper.NormalActivityPermissionHelper(this);
    public AlertDialog loadingDialog;//加载弹窗
    protected View rootView;//根布局，即getLayoutRes()返回的布局

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutRes());
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

    protected boolean keepFontSize() {
        return Global.INSTANCE.isKeepFontSize();
    }

    @Override
    public Resources getResources() {
        if (!keepFontSize()) return super.getResources();
        Resources resources = super.getResources();
        Configuration config = resources.getConfiguration();
        boolean needReset = false;
        if (config.fontScale != 1.0f) {
            LogUtils.INSTANCE.i("--->fontScale=" + config.fontScale);
            config.fontScale = 1.0f;
            needReset = true;
        }
        int defDensityDpi = DeviceInfoUtil.INSTANCE.getDefaultDisplayDensity();
        if (config.densityDpi != defDensityDpi && defDensityDpi != -1) {
            config.densityDpi = defDensityDpi;
            needReset = true;
        }
        //super.getResources().apply {updateConfiguration(config, displayMetrics)  }
        if (needReset) {
            config.uiMode = Configuration.UI_MODE_TYPE_UNDEFINED;
            return createConfigurationContext(config).getResources();
        }
        return resources;
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
            loadingDialog = DialogFactory.Companion.createDialog(this, R.layout.dialog_loading, null, R.style.LoadingDialogStyle);
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

    public PermissionHelper getPermissionHelper() {
        return permissionHelper;
    }

    /**
     * 检查并申请权限
     *
     * @param callback
     */
    public void doWithPermission(PermissionHelper.RequestPermissionsCallback callback) {
        permissionHelper.doWithPermission(callback);
    }

    /**
     * 检查权限
     *
     * @param permissions
     * @return
     */
    protected List<String> checkPermissions(String... permissions) {
        return permissionHelper.checkPermissions(permissions);
    }


    /**
     * startActivityForResult
     *
     * @param intent
     * @param callback
     */
    public void startActivityForResult(Intent intent, ActivityResultCallback<ActivityResult> callback) {
        permissionHelper.starActivityForResult(intent, callback);
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
