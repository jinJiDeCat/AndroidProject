package com.hjq.demo.download.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import com.hjq.demo.download.data.DownloadStatus;
import com.hjq.demo.download.data.VideoDownloadEntity;
import com.hjq.demo.download.data.VideoDownloadRepository;
import com.hjq.demo.download.model.VideoDownloadRequest;
import com.hjq.demo.download.ui.VideoDownloadActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.OkHttpClient;

public final class VideoDownloadService extends Service implements VideoDownloadManager.Callback {

    public static final String ACTION_START = "com.hjq.demo.action.START_DOWNLOAD_SERVICE";
    private static final String CHANNEL_ID = "video_download_channel";
    private static final int NOTIFICATION_ID = 1001;

    private VideoDownloadRepository mRepository;
    private VideoDownloadManager mManager;
    private DownloadBinder mBinder;
    private NotificationManager mNotificationManager;

    public static void start(Context context) {
        Intent intent = new Intent(context, VideoDownloadService.class);
        intent.setAction(ACTION_START);
        Context appContext = context.getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent);
        } else {
            appContext.startService(intent);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mRepository = new VideoDownloadRepository(this);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .build();
        mManager = new VideoDownloadManager(this, mRepository, okHttpClient, this);
        mBinder = new DownloadBinder();
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createChannelIfNeed();
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
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public void onActiveTaskChanged(int activeCount) {
        if (activeCount > 0) {
            Notification notification = buildNotification(activeCount);
            startForeground(NOTIFICATION_ID, notification);
        } else {
            stopForeground(true);
        }
    }

    private void createChannelIfNeed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "视频下载",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("视频下载进度通知");
            mNotificationManager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(int activeCount) {
        Intent intent = new Intent(this, VideoDownloadActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("视频下载")
                .setContentText("正在下载 " + activeCount + " 个任务")
                .setSmallIcon(R.drawable.download_ic)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
    }

    private void enqueueRequests(List<VideoDownloadRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }
        List<VideoDownloadEntity> entities = new ArrayList<>();
        for (VideoDownloadRequest request : requests) {
            VideoDownloadEntity entity = new VideoDownloadEntity();
            entity.setVideoId(request.getVideoId());
            entity.setSeriesId(request.getSeriesId());
            entity.setTitle(request.getTitle());
            entity.setUrl(request.getUrl());
            entity.setStatus(DownloadStatus.PENDING);
            entities.add(entity);
        }
        if (entities.size() == 1) {
            mRepository.insert(entities.get(0));
        } else {
            mRepository.insert(entities);
        }
        mManager.enqueue(entities);
    }

    public final class DownloadBinder extends Binder {

        public void enqueue(VideoDownloadRequest request) {
            if (request == null) {
                return;
            }
            enqueueRequests(Collections.singletonList(request));
        }

        public void enqueue(List<VideoDownloadRequest> requests) {
            enqueueRequests(requests);
        }

        public void pause(String videoId) {
            mManager.pause(videoId);
        }

        public void pauseAll() {
            mManager.pauseAll();
        }

        public void resume(String videoId) {
            mManager.resume(videoId);
        }

        public void resumeAll(List<String> videoIds) {
            mManager.resumeAll(videoIds);
        }

        public void delete(String videoId) {
            mManager.cancelAndDelete(videoId);
        }

        public void delete(List<String> videoIds) {
            mManager.cancelAndDelete(videoIds);
        }

        public LiveData<List<VideoDownloadEntity>> observeAll() {
            return mRepository.observeAllDownloads();
        }

        public LiveData<List<VideoDownloadEntity>> observeSeries(String seriesId) {
            return mRepository.observeBySeries(seriesId);
        }

        public VideoDownloadRepository getRepository() {
            return mRepository;
        }

        public long getAvailableStorageBytes() {
            return mManager.getAvailableStorageBytes();
        }
    }
}
