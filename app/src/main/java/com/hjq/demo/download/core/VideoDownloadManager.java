package com.hjq.demo.download.core;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.hjq.demo.download.annotation.DownloadStatus;
import com.hjq.demo.download.db.VideoDownloadDao;
import com.hjq.demo.download.db.VideoDownloadEntity;
import com.hjq.demo.download.model.DownloadRequest;
import com.hjq.demo.download.repository.VideoDownloadRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import okhttp3.OkHttpClient;

/**
 * 下载调度器，负责多任务管理、暂停/继续/删除等操作。
 */
public class VideoDownloadManager implements DownloadTask.Listener {

    private static final int MAX_PARALLEL_DOWNLOAD = 3;

    private final Context mContext;
    private final VideoDownloadRepository mRepository;
    private final VideoDownloadDao mDao;
    private final OkHttpClient mOkHttpClient;

    private final ExecutorService mDownloadExecutor = Executors.newFixedThreadPool(MAX_PARALLEL_DOWNLOAD);
    private final ExecutorService mPrepareExecutor = Executors.newSingleThreadExecutor();
    private final Map<String, DownloadTask> mRunningTasks = new ConcurrentHashMap<>();

    public VideoDownloadManager(@NonNull Context context) {
        this.mContext = context.getApplicationContext();
        this.mRepository = new VideoDownloadRepository(this.mContext);
        this.mDao = mRepository.getDao();
        this.mOkHttpClient = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .build();
    }

    public void enqueue(DownloadRequest request) {
        if (request == null) {
            return;
        }
        mPrepareExecutor.execute(() -> {
            VideoDownloadEntity entity = buildEntity(request);
            mDao.insert(entity);
            startTask(entity);
        });
    }

    public void enqueue(List<DownloadRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }
        mPrepareExecutor.execute(() -> {
            List<VideoDownloadEntity> entities = new ArrayList<>();
            for (DownloadRequest request : requests) {
                if (request == null) {
                    continue;
                }
                entities.add(buildEntity(request));
            }
            if (!entities.isEmpty()) {
                mDao.insert(entities);
                for (VideoDownloadEntity entity : entities) {
                    startTask(entity);
                }
            }
        });
    }

    public void resume(String downloadId) {
        if (TextUtils.isEmpty(downloadId)) {
            return;
        }
        mPrepareExecutor.execute(() -> {
            if (mRunningTasks.containsKey(downloadId)) {
                return;
            }
            VideoDownloadEntity entity = mDao.findById(downloadId);
            if (entity != null) {
                startTask(entity);
            }
        });
    }

    public void resumeAll() {
        mPrepareExecutor.execute(() -> {
            List<VideoDownloadEntity> all = mDao.findAllSync();
            for (VideoDownloadEntity entity : all) {
                if (entity == null || entity.getStatus() == DownloadStatus.COMPLETED) {
                    continue;
                }
                if (!mRunningTasks.containsKey(entity.getDownloadId())) {
                    startTask(entity);
                }
            }
        });
    }

    public void pause(String downloadId) {
        DownloadTask task = mRunningTasks.get(downloadId);
        if (task != null) {
            task.pause();
        } else {
            mRepository.updateStatus(downloadId, DownloadStatus.PAUSED);
        }
    }

    public void pauseAll() {
        for (DownloadTask task : mRunningTasks.values()) {
            task.pause();
        }
    }

    public void delete(String downloadId) {
        if (TextUtils.isEmpty(downloadId)) {
            return;
        }
        delete(Collections.singletonList(downloadId));
    }

    public void delete(List<String> downloadIds) {
        if (downloadIds == null || downloadIds.isEmpty()) {
            return;
        }
        mPrepareExecutor.execute(() -> {
            List<String> validIds = new ArrayList<>();
            for (String downloadId : downloadIds) {
                if (TextUtils.isEmpty(downloadId)) {
                    continue;
                }
                validIds.add(downloadId);
                DownloadTask task = mRunningTasks.remove(downloadId);
                if (task != null) {
                    task.cancel();
                }
                VideoDownloadEntity entity = mDao.findById(downloadId);
                if (entity != null && !TextUtils.isEmpty(entity.getFilePath())) {
                    FileUtil.del(entity.getFilePath());
                }
            }
            if (!validIds.isEmpty()) {
                mDao.deleteByIds(validIds);
            }
        });
    }

    public LiveData<List<VideoDownloadEntity>> observeAll() {
        return mRepository.observeAll();
    }

    public LiveData<List<VideoDownloadEntity>> observeSeries(String seriesId) {
        if (TextUtils.isEmpty(seriesId)) {
            return observeAll();
        }
        return mRepository.observeSeries(seriesId);
    }

    private void startTask(VideoDownloadEntity entity) {
        if (entity == null) {
            return;
        }
        String downloadId = entity.getDownloadId();
        if (mRunningTasks.containsKey(downloadId) || entity.isCompleted()) {
            return;
        }
        DownloadTask task = new DownloadTask(entity, mDao, mOkHttpClient, this);
        mRunningTasks.put(downloadId, task);
        mDownloadExecutor.execute(task);
    }

    private VideoDownloadEntity buildEntity(DownloadRequest request) {
        VideoDownloadEntity entity = mDao.findById(request.getDownloadId());
        if (entity == null) {
            entity = new VideoDownloadEntity();
            entity.setDownloadId(request.getDownloadId());
            entity.setCreateTime(System.currentTimeMillis());
        }
        entity.setSeriesId(request.getSeriesId());
        entity.setEpisodeId(request.getEpisodeId());
        entity.setVideoName(StrUtil.blankToDefault(request.getVideoName(), request.getDownloadId()));
        entity.setVideoUrl(request.getVideoUrl());
        entity.setCoverUrl(request.getCoverUrl());
        entity.setExtra(request.getExtra());
        boolean overridePath = !TextUtils.isEmpty(request.getTargetDir()) || !TextUtils.isEmpty(request.getFileName());
        if (TextUtils.isEmpty(entity.getFilePath()) || overridePath) {
            entity.setFilePath(generateFilePath(request));
        }
        entity.setStatus(DownloadStatus.WAITING);
        entity.setUpdateTime(System.currentTimeMillis());
        return entity;
    }

    private String generateFilePath(DownloadRequest request) {
        String targetDir = request.getTargetDir();
        if (StrUtil.isBlank(targetDir)) {
            File root = mContext.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
            if (root == null) {
                root = new File(mContext.getFilesDir(), "downloads");
            }
            targetDir = new File(root, "videos").getAbsolutePath();
        }
        File dir = FileUtil.file(targetDir);
        FileUtil.mkdir(dir);

        String fileName = request.getFileName();
        if (StrUtil.isBlank(fileName)) {
            fileName = request.getDownloadId() + ".mp4";
        }
        return new File(dir, fileName).getAbsolutePath();
    }

    @Override
    public void onTaskCompleted(@NonNull VideoDownloadEntity entity) {
        mRunningTasks.remove(entity.getDownloadId());
    }

    @Override
    public void onTaskFailed(@NonNull VideoDownloadEntity entity, @NonNull Throwable throwable) {
        mRunningTasks.remove(entity.getDownloadId());
    }

    @Override
    public void onTaskPaused(@NonNull VideoDownloadEntity entity) {
        mRunningTasks.remove(entity.getDownloadId());
    }

    @Override
    public void onTaskCanceled(@NonNull VideoDownloadEntity entity) {
        mRunningTasks.remove(entity.getDownloadId());
    }

    public void release() {
        for (DownloadTask task : mRunningTasks.values()) {
            task.cancel();
        }
        mRunningTasks.clear();
        mDownloadExecutor.shutdownNow();
        mPrepareExecutor.shutdownNow();
    }
}
