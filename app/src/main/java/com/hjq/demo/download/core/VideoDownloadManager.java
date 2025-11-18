package com.hjq.demo.download.core;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;

import androidx.annotation.NonNull;

import com.hjq.demo.download.data.DownloadStatus;
import com.hjq.demo.download.data.VideoDownloadEntity;
import com.hjq.demo.download.data.VideoDownloadRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import okhttp3.OkHttpClient;

public final class VideoDownloadManager {

    public interface Callback {
        void onActiveTaskChanged(int activeCount);
    }

    private final Context mContext;
    private final VideoDownloadRepository mRepository;
    private final OkHttpClient mOkHttpClient;
    private final ExecutorService mWorkerExecutor;
    private final Map<String, VideoDownloadTask> mRunningTasks = new ConcurrentHashMap<>();
    private final Callback mCallback;
    private final File mStorageRoot;
    private final VideoDownloadTask.StorageChecker mStorageChecker = requiredBytes -> hasEnoughStorage(requiredBytes);

    public VideoDownloadManager(@NonNull Context context,
                                @NonNull VideoDownloadRepository repository,
                                @NonNull OkHttpClient okHttpClient,
                                @NonNull Callback callback) {
        mContext = context.getApplicationContext();
        mRepository = repository;
        mOkHttpClient = okHttpClient;
        mCallback = callback;
        mWorkerExecutor = Executors.newFixedThreadPool(3);
        File root = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (root == null) {
            root = context.getFilesDir();
        }
        mStorageRoot = FileUtil.mkdir(root);
    }

    public void enqueue(VideoDownloadEntity entity) {
        if (entity == null) {
            return;
        }
        mWorkerExecutor.execute(() -> startTask(entity));
    }

    public void enqueue(List<VideoDownloadEntity> entities) {
        if (entities == null) {
            return;
        }
        for (VideoDownloadEntity entity : entities) {
            enqueue(entity);
        }
    }

    public void resume(String videoId) {
        if (StrUtil.isBlank(videoId)) {
            return;
        }
        mRepository.getDiskExecutor().execute(() -> {
            VideoDownloadEntity entity = mRepository.findByIdSync(videoId);
            if (entity != null) {
                enqueue(entity);
            }
        });
    }

    public void resumeAll(List<String> videoIds) {
        if (videoIds == null) {
            return;
        }
        for (String videoId : videoIds) {
            resume(videoId);
        }
    }

    public void pause(String videoId) {
        VideoDownloadTask task = mRunningTasks.get(videoId);
        if (task != null) {
            task.pause();
        }
    }

    public void pauseAll() {
        for (VideoDownloadTask task : mRunningTasks.values()) {
            task.pause();
        }
    }

    public void cancelAndDelete(String videoId) {
        VideoDownloadTask task = mRunningTasks.remove(videoId);
        if (task != null) {
            task.cancel();
        }
        mRepository.getDiskExecutor().execute(() -> {
            VideoDownloadEntity entity = mRepository.findByIdSync(videoId);
            if (entity != null) {
                deleteFileIfNeed(entity);
                List<String> ids = new ArrayList<>();
                ids.add(videoId);
                mRepository.deleteByIds(ids);
            }
        });
        notifyActive();
    }

    public void cancelAndDelete(List<String> videoIds) {
        if (videoIds == null || videoIds.isEmpty()) {
            return;
        }
        for (String id : videoIds) {
            VideoDownloadTask task = mRunningTasks.remove(id);
            if (task != null) {
                task.cancel();
            }
        }
        mRepository.getDiskExecutor().execute(() -> {
            for (String id : videoIds) {
                VideoDownloadEntity entity = mRepository.findByIdSync(id);
                if (entity != null) {
                    deleteFileIfNeed(entity);
                }
            }
            mRepository.deleteByIds(videoIds);
        });
        notifyActive();
    }

    private void startTask(VideoDownloadEntity entity) {
        File targetFile = resolveTargetFile(entity);
        entity.setFilePath(targetFile.getAbsolutePath());
        mRepository.updateProgress(entity.getVideoId(), targetFile.exists() ? targetFile.length() : entity.getDownloadedBytes(),
                entity.getTotalBytes(), DownloadStatus.PENDING);

        VideoDownloadTask task = new VideoDownloadTask(entity, mOkHttpClient, targetFile, new VideoDownloadListener() {

            @Override
            public void onStatusChanged(String videoId, DownloadStatus status, long downloadedBytes, long totalBytes) {
                mRepository.updateProgress(videoId, downloadedBytes, totalBytes, status);
                if (status == DownloadStatus.COMPLETED || status == DownloadStatus.CANCELLED
                        || status == DownloadStatus.FAILED || status == DownloadStatus.PAUSED) {
                    mRunningTasks.remove(videoId);
                    notifyActive();
                }
            }

            @Override
            public void onProgress(String videoId, long downloadedBytes, long totalBytes) {
                mRepository.updateProgress(videoId, downloadedBytes, totalBytes, DownloadStatus.DOWNLOADING);
            }
        }, mStorageChecker);
        mRunningTasks.put(entity.getVideoId(), task);
        notifyActive();
        mWorkerExecutor.execute(task);
    }

    private File resolveTargetFile(VideoDownloadEntity entity) {
        File seriesDir = FileUtil.mkdir(new File(mStorageRoot, StrUtil.blankToDefault(entity.getSeriesId(), "default_series")));
        String safeName = buildFileName(entity);
        return new File(seriesDir, safeName + ".mp4");
    }

    private String buildFileName(VideoDownloadEntity entity) {
        String base = StrUtil.blankToDefault(entity.getTitle(), entity.getVideoId());
        base = base.replaceAll("[^a-zA-Z0-9_\\-一-龥]", "_");
        return base + "_" + entity.getVideoId();
    }

    private void deleteFileIfNeed(VideoDownloadEntity entity) {
        if (StrUtil.isBlank(entity.getFilePath())) {
            return;
        }
        FileUtil.del(entity.getFilePath());
    }

    public long getAvailableStorageBytes() {
        StatFs statFs = new StatFs(mStorageRoot.getAbsolutePath());
        return statFs.getAvailableBytes();
    }

    private boolean hasEnoughStorage(long requiredBytes) {
        if (requiredBytes <= 0) {
            return true;
        }
        // 保留 10MB 缓冲区
        long buffer = 10 * 1024 * 1024;
        return getAvailableStorageBytes() > (requiredBytes + buffer);
    }

    private void notifyActive() {
        if (mCallback != null) {
            mCallback.onActiveTaskChanged(mRunningTasks.size());
        }
    }

    File getStorageRoot() {
        return mStorageRoot;
    }

    VideoDownloadTask.StorageChecker getStorageChecker() {
        return mStorageChecker;
    }
}
