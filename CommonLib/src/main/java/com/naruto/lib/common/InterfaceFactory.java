package com.naruto.lib.common;

/**
 * @Purpose
 * @Author Naruto Yang
 * @CreateDate 2019/11/5 0005
 * @Note
 */
public class InterfaceFactory {

    public interface SimpleOperation {
        void done();
    }

    public interface Operation<T> {
        void done(T t);
    }

    public interface Func<T,R> {
        R execute(T t);
    }

    public interface Func0<T> {
        T execute();
    }

    /**
     * @Description 数据加载接口
     * @Author Naruto Yang
     * @CreateDate 2021/7/26 0026
     * @Note
     */
    public interface SimpleDataLoader {
        void getData();
    }

    /**
     * @Description 数据加载接口
     * @Author Naruto Yang
     * @CreateDate 2021/7/26 0026
     * @Note
     */
    public interface DataLoader<T> {
        void getData(T param);
    }

    /**
     * @Description 回调
     * @Author Naruto Yang
     * @CreateDate 2021/7/28 0028
     * @Note
     */
    public interface SimpleCallback {
        void onCallback();
    }

    /**
     * @Description 回调
     * @Author Naruto Yang
     * @CreateDate 2021/7/28 0028
     * @Note
     */
    public interface Callback<T> {
        void onCallback(T data);
    }

    /**
     * @Description 回调
     * @Author Naruto Yang
     * @CreateDate 2021/7/28 0028
     * @Note
     */
    public interface Callback2<T> {
        void onCallback(T data, Throwable throwable);
    }


    /**
     * @Description 提取数据
     * @Author Naruto Yang
     * @CreateDate 2021/8/6 0006
     * @Note
     */
    public interface DataExtractor<T, D> {
        D getData(T t);
    }
}
