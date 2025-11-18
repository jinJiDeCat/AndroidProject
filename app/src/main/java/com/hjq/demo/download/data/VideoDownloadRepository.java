package com.hjq.demo.download.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class VideoDownloadRepository {

    private final VideoDownloadDao mDao;
    private final ExecutorService mDiskExecutor = Executors.newSingleThreadExecutor();

    public VideoDownloadRepository(@NonNull Context context) {
        mDao = VideoDownloadDatabase.getInstance(context).videoDownloadDao();
    }

    public ExecutorService getDiskExecutor() {
        return mDiskExecutor;
    }

    public LiveData<List<VideoDownloadEntity>> observeAllDownloads() {
        return mDao.observeAll();
    }

    public LiveData<List<VideoDownloadEntity>> observeBySeries(@NonNull String seriesId) {
        return mDao.observeBySeries(seriesId);
    }

    public void insert(VideoDownloadEntity entity) {
        mDiskExecutor.execute(() -> {
            long now = System.currentTimeMillis();
            entity.setCreateTime(now);
            entity.setUpdateTime(now);
            mDao.insert(entity);
        });
    }

    public void insert(List<VideoDownloadEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        mDiskExecutor.execute(() -> {
            long now = System.currentTimeMillis();
            for (VideoDownloadEntity entity : entities) {
                entity.setCreateTime(now);
                entity.setUpdateTime(now);
            }
            mDao.insert(entities);
        });
    }

    public void update(VideoDownloadEntity entity) {
        mDiskExecutor.execute(() -> {
            entity.setUpdateTime(System.currentTimeMillis());
            mDao.update(entity);
        });
    }

    public void updateProgress(String videoId, long downloadedBytes, long totalBytes, DownloadStatus status) {
        mDiskExecutor.execute(() -> mDao.updateProgress(videoId, downloadedBytes, totalBytes, status, System.currentTimeMillis()));
    }

    public void delete(VideoDownloadEntity entity) {
        if (entity == null) {
            return;
        }
        mDiskExecutor.execute(() -> mDao.delete(entity));
    }

    public void deleteByIds(List<String> videoIds) {
        if (videoIds == null || videoIds.isEmpty()) {
            return;
        }
        mDiskExecutor.execute(() -> mDao.deleteByIds(new ArrayList<>(videoIds)));
    }

    @WorkerThread
    public VideoDownloadEntity findByIdSync(String videoId) {
        return mDao.findById(videoId);
    }
}
