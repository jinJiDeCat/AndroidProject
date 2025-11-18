package com.hjq.demo.download.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.hjq.demo.download.annotation.DownloadStatus;
import com.hjq.demo.download.db.DownloadDatabase;
import com.hjq.demo.download.db.VideoDownloadDao;
import com.hjq.demo.download.db.VideoDownloadEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Download 数据操作封装，提供单条、多条插入和查询能力。
 */
public class VideoDownloadRepository {

    private final VideoDownloadDao mDao;
    private final ExecutorService mIoExecutor = Executors.newSingleThreadExecutor();

    public VideoDownloadRepository(@NonNull Context context) {
        this(DownloadDatabase.getInstance(context).videoDownloadDao());
    }

    public VideoDownloadRepository(@NonNull VideoDownloadDao dao) {
        this.mDao = dao;
    }

    public void insert(VideoDownloadEntity entity) {
        if (entity == null) {
            return;
        }
        mIoExecutor.execute(() -> {
            long now = System.currentTimeMillis();
            if (entity.getCreateTime() == 0) {
                entity.setCreateTime(now);
            }
            entity.setUpdateTime(now);
            mDao.insert(entity);
        });
    }

    public void insert(List<VideoDownloadEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        mIoExecutor.execute(() -> {
            long now = System.currentTimeMillis();
            List<VideoDownloadEntity> target = new ArrayList<>(entities.size());
            for (VideoDownloadEntity entity : entities) {
                if (entity == null) {
                    continue;
                }
                if (entity.getCreateTime() == 0) {
                    entity.setCreateTime(now);
                }
                entity.setUpdateTime(now);
                target.add(entity);
            }
            if (!target.isEmpty()) {
                mDao.insert(target);
            }
        });
    }

    public void updateStatus(String downloadId, @DownloadStatus int status) {
        mIoExecutor.execute(() -> mDao.updateStatus(downloadId, status, System.currentTimeMillis()));
    }

    public void delete(String downloadId) {
        mIoExecutor.execute(() -> mDao.deleteById(downloadId));
    }

    public void delete(List<String> downloadIds) {
        mIoExecutor.execute(() -> mDao.deleteByIds(downloadIds));
    }

    public LiveData<List<VideoDownloadEntity>> observeAll() {
        return mDao.observeAll();
    }

    public LiveData<List<VideoDownloadEntity>> observeSeries(String seriesId) {
        return mDao.observeBySeries(seriesId);
    }

    public VideoDownloadDao getDao() {
        return mDao;
    }

    public ExecutorService getIoExecutor() {
        return mIoExecutor;
    }
}
