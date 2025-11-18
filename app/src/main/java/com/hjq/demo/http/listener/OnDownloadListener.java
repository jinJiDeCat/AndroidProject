package com.hjq.demo.http.listener;

import com.hjq.demo.http.model.DownloadInfo;

import okhttp3.Call;

/**
 * 下载监听
 */
public interface OnDownloadListener {

    default void onStart(Call call) {}

    default void onProgress(DownloadInfo info) {}

    default void onComplete(DownloadInfo info) {}

    default void onError(DownloadInfo info, Exception e) {}

    default void onEnd(Call call) {}
}
