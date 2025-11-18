package com.hjq.demo.http.callback;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;

import com.hjq.demo.http.core.TypeTokenUtils;
import com.hjq.demo.http.listener.OnHttpListener;

import java.lang.reflect.Type;

/**
 * 网络请求回调
 */
public abstract class HttpCallback<T> implements OnHttpListener<T> {

    private final LifecycleOwner mLifecycleOwner;
    private final Type mType;

    protected HttpCallback(@NonNull LifecycleOwner owner) {
        mLifecycleOwner = owner;
        Type type = TypeTokenUtils.resolve(getClass());
        if (type == null) {
            throw new IllegalArgumentException("Missing generic type for HttpCallback");
        }
        mType = type;
    }

    public LifecycleOwner getLifecycleOwner() {
        return mLifecycleOwner;
    }

    public Type getType() {
        return mType;
    }
}
