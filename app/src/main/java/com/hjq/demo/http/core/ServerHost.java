package com.hjq.demo.http.core;

import android.text.TextUtils;

import androidx.annotation.NonNull;

/**
 * 服务器配置
 */
public interface ServerHost {

    @NonNull
    String getHost();

    @NonNull
    default String getPath() {
        return "";
    }

    @NonNull
    default String getBaseUrl() {
        String host = getHost();
        if (!host.endsWith("/")) {
            host += "/";
        }
        String path = getPath();
        if (TextUtils.isEmpty(path)) {
            return host;
        }
        if (!path.endsWith("/")) {
            path += "/";
        }
        return host + path;
    }
}
