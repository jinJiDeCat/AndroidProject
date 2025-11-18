package com.hjq.demo.download.core;

import androidx.annotation.NonNull;

import com.hjq.demo.download.data.DownloadStatus;
import com.hjq.demo.download.data.VideoDownloadEntity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.hutool.core.io.FileUtil;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class VideoDownloadTask implements Runnable {

    private final VideoDownloadEntity mEntity;
    private final OkHttpClient mOkHttpClient;
    private final File mTargetFile;
    private final VideoDownloadListener mListener;
    private final AtomicBoolean mPaused = new AtomicBoolean(false);
    private final AtomicBoolean mCancelled = new AtomicBoolean(false);

    public VideoDownloadTask(@NonNull VideoDownloadEntity entity,
                             @NonNull OkHttpClient okHttpClient,
                             @NonNull File targetFile,
                             @NonNull VideoDownloadListener listener) {
        mEntity = entity;
        mOkHttpClient = okHttpClient;
        mTargetFile = targetFile;
        mListener = listener;
    }

    @Override
    public void run() {
        long downloadedBytes = mTargetFile.exists() ? mTargetFile.length() : 0;
        if (downloadedBytes <= 0) {
            downloadedBytes = mEntity.getDownloadedBytes();
        }

        Response response = null;
        RandomAccessFile accessFile = null;
        InputStream inputStream = null;
        try {
            Request request = buildRequest(downloadedBytes);
            response = mOkHttpClient.newCall(request).execute();

            if (!response.isSuccessful()) {
                throw new IOException("http error code " + response.code());
            }

            if (downloadedBytes > 0 && response.code() == 200) {
                // 服务器不支持 Range，重新走全量下载
                FileUtil.del(mTargetFile);
                downloadedBytes = 0;
                closeQuietly(response);
                response = mOkHttpClient.newCall(buildRequest(0)).execute();
                if (!response.isSuccessful()) {
                    throw new IOException("http error code " + response.code());
                }
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("response body empty");
            }

            long contentLength = body.contentLength();
            long totalBytes = contentLength == -1 ? mEntity.getTotalBytes() : downloadedBytes + contentLength;
            if (totalBytes <= 0) {
                totalBytes = downloadedBytes;
            }

            mListener.onStatusChanged(mEntity.getVideoId(), DownloadStatus.DOWNLOADING, downloadedBytes, totalBytes);

            FileUtil.touch(mTargetFile);
            accessFile = new RandomAccessFile(mTargetFile, "rwd");
            if (downloadedBytes > 0) {
                accessFile.seek(downloadedBytes);
            }

            byte[] buffer = new byte[8 * 1024];
            int len;
            inputStream = body.byteStream();
            while ((len = inputStream.read(buffer)) != -1) {
                if (mCancelled.get()) {
                    mListener.onStatusChanged(mEntity.getVideoId(), DownloadStatus.CANCELLED, downloadedBytes, totalBytes);
                    return;
                }
                if (mPaused.get()) {
                    mListener.onStatusChanged(mEntity.getVideoId(), DownloadStatus.PAUSED, downloadedBytes, totalBytes);
                    return;
                }
                accessFile.write(buffer, 0, len);
                downloadedBytes += len;
                mListener.onProgress(mEntity.getVideoId(), downloadedBytes, totalBytes);
            }
            mListener.onStatusChanged(mEntity.getVideoId(), DownloadStatus.COMPLETED, downloadedBytes, totalBytes);
        } catch (IOException e) {
            if (mPaused.get() || mCancelled.get()) {
                return;
            }
            long failedBytes = mTargetFile.exists() ? mTargetFile.length() : 0;
            mListener.onStatusChanged(mEntity.getVideoId(), DownloadStatus.FAILED, failedBytes,
                    failedBytes == 0 ? mEntity.getTotalBytes() : failedBytes);
        } finally {
            closeQuietly(accessFile);
            closeQuietly(inputStream);
            closeQuietly(response);
        }
    }

    private Request buildRequest(long downloadedBytes) {
        Request.Builder builder = new Request.Builder()
                .url(mEntity.getUrl())
                .addHeader("Connection", "Keep-Alive");
        if (downloadedBytes > 0) {
            builder.addHeader("Range", "bytes=" + downloadedBytes + "-");
        }
        return builder.build();
    }

    public void pause() {
        mPaused.set(true);
    }

    public void cancel() {
        mCancelled.set(true);
    }

    private void closeQuietly(Response response) {
        if (response != null) {
            response.close();
        }
    }

    private void closeQuietly(RandomAccessFile file) {
        if (file != null) {
            try {
                file.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void closeQuietly(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ignored) {
            }
        }
    }
}
