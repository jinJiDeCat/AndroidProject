package com.hjq.demo.http.core;

import androidx.annotation.NonNull;

import com.hjq.demo.http.model.HttpMethod;

/**
 * 请求接口信息
 */
public interface RequestApi {

    /**
     * 接口地址
     */
    @NonNull
    String getApi();

    /**
     * 请求方法，默认 POST
     */
    @NonNull
    default HttpMethod getMethod() {
        return HttpMethod.POST;
    }
}
