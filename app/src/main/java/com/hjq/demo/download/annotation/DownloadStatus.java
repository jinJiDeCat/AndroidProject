package com.hjq.demo.download.annotation;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 下载状态定义。
 */
@IntDef({
        DownloadStatus.IDLE,
        DownloadStatus.WAITING,
        DownloadStatus.DOWNLOADING,
        DownloadStatus.PAUSED,
        DownloadStatus.COMPLETED,
        DownloadStatus.FAILED,
        DownloadStatus.DELETED
})
@Retention(RetentionPolicy.SOURCE)
public @interface DownloadStatus {

    int IDLE = 0;
    int WAITING = 1;
    int DOWNLOADING = 2;
    int PAUSED = 3;
    int COMPLETED = 4;
    int FAILED = 5;
    int DELETED = 6;
}
