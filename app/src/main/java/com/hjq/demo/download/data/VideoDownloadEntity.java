package com.hjq.demo.download.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "video_downloads",
        indices = {@Index(value = {"series_id"}), @Index(value = {"status"})})
public class VideoDownloadEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "video_id")
    private String videoId;

    @ColumnInfo(name = "series_id")
    private String seriesId;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "url")
    private String url;

    @ColumnInfo(name = "file_path")
    private String filePath;

    @ColumnInfo(name = "status")
    @NonNull
    private DownloadStatus status = DownloadStatus.PENDING;

    @ColumnInfo(name = "total_bytes")
    private long totalBytes;

    @ColumnInfo(name = "downloaded_bytes")
    private long downloadedBytes;

    @ColumnInfo(name = "create_time")
    private long createTime;

    @ColumnInfo(name = "update_time")
    private long updateTime;

    public VideoDownloadEntity() {
    }

    public VideoDownloadEntity(@NonNull String videoId, String seriesId, String title, String url) {
        this.videoId = videoId;
        this.seriesId = seriesId;
        this.title = title;
        this.url = url;
        long now = System.currentTimeMillis();
        this.createTime = now;
        this.updateTime = now;
    }

    @NonNull
    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(@NonNull String videoId) {
        this.videoId = videoId;
    }

    public String getSeriesId() {
        return seriesId;
    }

    public void setSeriesId(String seriesId) {
        this.seriesId = seriesId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @NonNull
    public DownloadStatus getStatus() {
        return status;
    }

    public void setStatus(@NonNull DownloadStatus status) {
        this.status = status;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public long getDownloadedBytes() {
        return downloadedBytes;
    }

    public void setDownloadedBytes(long downloadedBytes) {
        this.downloadedBytes = downloadedBytes;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
}
