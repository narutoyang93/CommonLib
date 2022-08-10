package com.naruto.lib.common.base;

import android.app.Service;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

/**
 * @Description
 * @Author Naruto Yang
 * @CreateDate 2021/9/1 0001
 * @Note
 */
public abstract class BaseService extends Service implements LifecycleOwner {
    private LifecycleRegistry mLifecycleRegistry;

    @Override
    public void onCreate() {
        super.onCreate();
        mLifecycleRegistry = new LifecycleRegistry(this);
        mLifecycleRegistry.setCurrentState(Lifecycle.State.CREATED);
    }

    @Override
    public void onDestroy() {
        mLifecycleRegistry.setCurrentState(Lifecycle.State.DESTROYED);
        super.onDestroy();
    }


    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return mLifecycleRegistry;
    }
}
