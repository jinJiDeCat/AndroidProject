package com.hjq.demo.http.core;

import com.hjq.demo.http.callback.HttpCallback;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 泛型工具类
 */
public final class TypeTokenUtils {

    private TypeTokenUtils() {}

    public static Type resolve(Class<?> clazz) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            Type genericSuperclass = current.getGenericSuperclass();
            if (genericSuperclass instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
                Type rawType = parameterizedType.getRawType();
                if (rawType instanceof Class && HttpCallback.class.isAssignableFrom((Class<?>) rawType)) {
                    return parameterizedType.getActualTypeArguments()[0];
                }
            }
            if (genericSuperclass instanceof Class) {
                current = (Class<?>) genericSuperclass;
            } else {
                break;
            }
        }
        return null;
    }
}
