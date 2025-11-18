package com.hjq.demo.http.listener;

import okhttp3.Call;

/**
 * 网络请求状态监听
 */
public interface OnHttpListener<T> {

    default void onStart(Call call) {}

    default void onSucceed(T result) {}

    default void onFail(Exception e) {}

    default void onEnd(Call call) {}
}
