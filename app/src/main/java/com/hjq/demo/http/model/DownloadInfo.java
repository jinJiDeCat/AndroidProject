package com.hjq.demo.http.model;

import java.io.File;

/**
 * 下载信息
 */
public final class DownloadInfo {

    private final File mFile;
    private final long mTotalBytes;
    private final long mDownloadedBytes;

    public DownloadInfo(File file, long totalBytes, long downloadedBytes) {
        mFile = file;
        mTotalBytes = totalBytes;
        mDownloadedBytes = downloadedBytes;
    }

    public File getFile() {
        return mFile;
    }

    public long getTotalBytes() {
        return mTotalBytes;
    }

    public long getDownloadSize() {
        return mDownloadedBytes;
    }

    public int getDownloadProgress() {
        if (mTotalBytes <= 0) {
            return 0;
        }
        return (int) (mDownloadedBytes * 100f / mTotalBytes);
    }
}
