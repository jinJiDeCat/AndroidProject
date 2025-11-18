package com.hjq.demo.download.core;

import android.os.SystemClock;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hjq.demo.download.annotation.DownloadStatus;
import com.hjq.demo.download.db.VideoDownloadDao;
import com.hjq.demo.download.db.VideoDownloadEntity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.hutool.core.io.FileUtil;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 单个视频的下载任务，负责断点续传、进度回调与状态更新。
 */
public class DownloadTask implements Runnable {

    public interface Listener {
        void onTaskCompleted(@NonNull VideoDownloadEntity entity);

        void onTaskFailed(@NonNull VideoDownloadEntity entity, @NonNull Throwable throwable);

        void onTaskPaused(@NonNull VideoDownloadEntity entity);

        void onTaskCanceled(@NonNull VideoDownloadEntity entity);
    }

    private final VideoDownloadEntity mEntity;
    private final VideoDownloadDao mDao;
    private final OkHttpClient mClient;
    private final Listener mListener;

    private final AtomicBoolean mPaused = new AtomicBoolean(false);
    private final AtomicBoolean mCanceled = new AtomicBoolean(false);

    private volatile Call mCurrentCall;

    public DownloadTask(@NonNull VideoDownloadEntity entity,
                        @NonNull VideoDownloadDao dao,
                        @NonNull OkHttpClient client,
                        @NonNull Listener listener) {
        this.mEntity = entity;
        this.mDao = dao;
        this.mClient = client;
        this.mListener = listener;
    }

    @Override
    public void run() {
        try {
            doDownload();
        } catch (IOException exception) {
            if (mCanceled.get()) {
                updateStatus(DownloadStatus.DELETED, mEntity.getDownloadedBytes(), mEntity.getTotalBytes());
                mListener.onTaskCanceled(mEntity);
            } else if (mPaused.get()) {
                updateStatus(DownloadStatus.PAUSED, mEntity.getDownloadedBytes(), mEntity.getTotalBytes());
                mListener.onTaskPaused(mEntity);
            } else {
                updateStatus(DownloadStatus.FAILED, mEntity.getDownloadedBytes(), mEntity.getTotalBytes());
                mListener.onTaskFailed(mEntity, exception);
            }
        }
    }

    private void doDownload() throws IOException {
        File targetFile = ensureTargetFile(mEntity.getFilePath());
        long downloadedBytes = targetFile.length();
        long totalBytes = mEntity.getTotalBytes() > 0 ? mEntity.getTotalBytes() : -1;

        updateStatus(DownloadStatus.WAITING, downloadedBytes, totalBytes);

        Request.Builder requestBuilder = new Request.Builder()
                .url(mEntity.getVideoUrl());

        if (downloadedBytes > 0) {
            requestBuilder.addHeader("Range", "bytes=" + downloadedBytes + "-");
        }

        Request request = requestBuilder.build();
        mCurrentCall = mClient.newCall(request);

        try (Response response = mCurrentCall.execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("下载失败 code=" + response.code());
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("response body empty");
            }

            boolean supportRange = response.code() == 206
                    || "bytes".equalsIgnoreCase(response.header("Accept-Ranges"));

            if (!supportRange && downloadedBytes > 0) {
                FileUtil.del(targetFile);
                targetFile = ensureTargetFile(mEntity.getFilePath());
                downloadedBytes = 0;
            }

            long contentLength = body.contentLength();
            if (contentLength > 0) {
                totalBytes = supportRange && downloadedBytes > 0 ? downloadedBytes + contentLength : contentLength;
                mEntity.setTotalBytes(totalBytes);
            }
            mEntity.setSupportRange(supportRange);

            updateStatus(DownloadStatus.DOWNLOADING, downloadedBytes, totalBytes);

            try (InputStream inputStream = body.byteStream();
                 RandomAccessFile raf = new RandomAccessFile(targetFile, "rw")) {
                if (downloadedBytes > 0) {
                    raf.seek(downloadedBytes);
                }

                byte[] buffer = new byte[8192];
                long lastDispatchTime = 0;

                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    if (mCanceled.get()) {
                        updateStatus(DownloadStatus.DELETED, downloadedBytes, totalBytes);
                        mListener.onTaskCanceled(mEntity);
                        return;
                    }
                    if (mPaused.get()) {
                        updateStatus(DownloadStatus.PAUSED, downloadedBytes, totalBytes);
                        mListener.onTaskPaused(mEntity);
                        return;
                    }
                    raf.write(buffer, 0, len);
                    downloadedBytes += len;

                    long now = SystemClock.elapsedRealtime();
                    if (now - lastDispatchTime > 400) {
                        updateStatus(DownloadStatus.DOWNLOADING, downloadedBytes, totalBytes);
                        lastDispatchTime = now;
                    }
                }
            }
        }

        updateStatus(DownloadStatus.COMPLETED, mEntity.getTotalBytes(), mEntity.getTotalBytes());
        mListener.onTaskCompleted(mEntity);
    }

    public void pause() {
        mPaused.set(true);
        cancelCurrentCall();
    }

    public void cancel() {
        mCanceled.set(true);
        cancelCurrentCall();
    }

    private void cancelCurrentCall() {
        Call call = mCurrentCall;
        if (call != null && !call.isCanceled()) {
            call.cancel();
        }
    }

    private void updateStatus(@DownloadStatus int status, long downloadedBytes, long totalBytes) {
        mEntity.setStatus(status);
        if (downloadedBytes >= 0) {
            mEntity.setDownloadedBytes(downloadedBytes);
        }
        if (totalBytes >= 0) {
            mEntity.setTotalBytes(totalBytes);
        }
        long now = System.currentTimeMillis();
        mEntity.setUpdateTime(now);
        mDao.updateProgress(mEntity.getDownloadId(),
                mEntity.getDownloadedBytes(),
                mEntity.getTotalBytes(),
                status,
                now);
    }

    private File ensureTargetFile(@Nullable String path) throws IOException {
        if (TextUtils.isEmpty(path)) {
            throw new IOException("下载路径为空");
        }
        File target = FileUtil.file(path);
        FileUtil.touch(target);
        return target;
    }
}
