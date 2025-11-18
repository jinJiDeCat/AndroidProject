package com.hjq.demo.download.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;

import com.hjq.demo.R;
import com.hjq.demo.download.core.VideoDownloadManager;
import com.hjq.demo.download.db.VideoDownloadEntity;
import com.hjq.demo.download.model.DownloadRequest;

import java.util.List;

/**
 * 后台下载服务，提供 binder 供 UI 调用。
 */
public class VideoDownloadService extends Service {

    private static final String CHANNEL_ID = "video_download_channel";
    private static final int NOTIFICATION_ID = 0x1010;

    private final DownloadBinder mBinder = new DownloadBinder();
    private VideoDownloadManager mManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mManager = new VideoDownloadManager(this);
        startForeground(NOTIFICATION_ID, buildNotification());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mManager != null) {
            mManager.release();
        }
        stopForeground(true);
        super.onDestroy();
    }

    private Notification buildNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "视频下载",
                    NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(channel);
        }
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("视频下载")
                .setContentText("下载服务运行中")
                .setSmallIcon(R.mipmap.launcher_ic)
                .setOngoing(true)
                .build();
    }

    public class DownloadBinder extends Binder {

        public void startDownload(DownloadRequest request) {
            mManager.enqueue(request);
        }

        public void startDownloads(List<DownloadRequest> requests) {
            mManager.enqueue(requests);
        }

        public void pause(String downloadId) {
            mManager.pause(downloadId);
        }

        public void pauseAll() {
            mManager.pauseAll();
        }

        public void resume(String downloadId) {
            mManager.resume(downloadId);
        }

        public void resumeAll() {
            mManager.resumeAll();
        }

        public void delete(String downloadId) {
            mManager.delete(downloadId);
        }

        public void delete(List<String> downloadIds) {
            mManager.delete(downloadIds);
        }

        public LiveData<List<VideoDownloadEntity>> observeAll() {
            return mManager.observeAll();
        }

        public LiveData<List<VideoDownloadEntity>> observeSeries(String seriesId) {
            return mManager.observeSeries(seriesId);
        }
    }
}
