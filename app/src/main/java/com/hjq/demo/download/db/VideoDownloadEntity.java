package com.hjq.demo.download.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.hjq.demo.download.annotation.DownloadStatus;

/**
 * Room 实体，记录每个视频的下载状态。
 */
@Entity(tableName = "video_downloads")
public class VideoDownloadEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "download_id")
    private String downloadId;

    @ColumnInfo(name = "series_id")
    private String seriesId;

    @ColumnInfo(name = "episode_id")
    private String episodeId;

    @ColumnInfo(name = "video_name")
    private String videoName;

    @ColumnInfo(name = "video_url")
    private String videoUrl;

    @ColumnInfo(name = "cover_url")
    private String coverUrl;

    @ColumnInfo(name = "file_path")
    private String filePath;

    @ColumnInfo(name = "support_range")
    private boolean supportRange;

    @ColumnInfo(name = "total_bytes")
    private long totalBytes;

    @ColumnInfo(name = "downloaded_bytes")
    private long downloadedBytes;

    @DownloadStatus
    @ColumnInfo(name = "status")
    private int status = DownloadStatus.IDLE;

    @ColumnInfo(name = "create_time")
    private long createTime;

    @ColumnInfo(name = "update_time")
    private long updateTime;

    @ColumnInfo(name = "extra")
    private String extra;

    public VideoDownloadEntity() {
    }

    @NonNull
    public String getDownloadId() {
        return downloadId;
    }

    public void setDownloadId(@NonNull String downloadId) {
        this.downloadId = downloadId;
    }

    public String getSeriesId() {
        return seriesId;
    }

    public void setSeriesId(String seriesId) {
        this.seriesId = seriesId;
    }

    public String getEpisodeId() {
        return episodeId;
    }

    public void setEpisodeId(String episodeId) {
        this.episodeId = episodeId;
    }

    public String getVideoName() {
        return videoName;
    }

    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public boolean isSupportRange() {
        return supportRange;
    }

    public void setSupportRange(boolean supportRange) {
        this.supportRange = supportRange;
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

    @DownloadStatus
    public int getStatus() {
        return status;
    }

    public void setStatus(@DownloadStatus int status) {
        this.status = status;
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

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public float getProgress() {
        if (totalBytes <= 0) {
            return 0F;
        }
        return (float) downloadedBytes / (float) totalBytes;
    }

    public boolean isCompleted() {
        return status == DownloadStatus.COMPLETED;
    }
}
