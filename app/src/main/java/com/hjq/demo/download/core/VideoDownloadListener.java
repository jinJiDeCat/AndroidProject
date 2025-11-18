package com.hjq.demo.download.core;

import com.hjq.demo.download.data.DownloadStatus;

public interface VideoDownloadListener {

    void onStatusChanged(String videoId, DownloadStatus status, long downloadedBytes, long totalBytes);

    void onProgress(String videoId, long downloadedBytes, long totalBytes);
}
