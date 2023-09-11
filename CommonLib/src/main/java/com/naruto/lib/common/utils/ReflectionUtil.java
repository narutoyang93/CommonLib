package com.naruto.lib.common.utils;

import java.lang.reflect.Field;

/**
 * @Purpose 反射工具类
 * @Author Naruto Yang
 * @CreateDate 2019/11/12 0012
 * @Note
 */
public class ReflectionUtil {
    /**
     * 获取属性
     *
     * @param target
     * @param filedName
     * @return
     */
    public static Field getField(Object target, String filedName) throws NoSuchFieldException {
        return getField(target.getClass(), filedName);
    }

    /**
     * 获取属性
     *
     * @param clazz
     * @param filedName
     * @return
     */
    public static Field getField(Class<?> clazz, String filedName) throws NoSuchFieldException {
        Field field = clazz.getDeclaredField(filedName);
        field.setAccessible(true);
        return field;
    }

    /**
     * 获取属性值
     *
     * @param target
     * @param filedName
     * @param <T>
     * @return
     */
    public static <T> T getFieldValue(Object target, String filedName) throws NoSuchFieldException, IllegalAccessException {
        return (T) getField(target, filedName).get(target);
    }

    /**
     * 设置属性值
     *
     * @param target
     * @param filedName
     * @param value
     * @param <T>
     */
    public static <T> void setFieldValue(Object target, String filedName, T value) throws NoSuchFieldException, IllegalAccessException {
        getField(target, filedName).set(target, value);
    }

}
