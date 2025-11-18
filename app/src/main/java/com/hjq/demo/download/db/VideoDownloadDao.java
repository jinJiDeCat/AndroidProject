package com.hjq.demo.download.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.hjq.demo.download.annotation.DownloadStatus;

import java.util.List;

@Dao
public interface VideoDownloadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(VideoDownloadEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(List<VideoDownloadEntity> entities);

    @Query("SELECT * FROM video_downloads WHERE download_id = :downloadId LIMIT 1")
    VideoDownloadEntity findById(String downloadId);

    @Query("SELECT * FROM video_downloads WHERE series_id = :seriesId ORDER BY update_time DESC")
    LiveData<List<VideoDownloadEntity>> observeBySeries(String seriesId);

    @Query("SELECT * FROM video_downloads WHERE series_id = :seriesId ORDER BY update_time DESC")
    List<VideoDownloadEntity> findBySeriesSync(String seriesId);

    @Query("SELECT * FROM video_downloads ORDER BY update_time DESC")
    LiveData<List<VideoDownloadEntity>> observeAll();

    @Query("SELECT * FROM video_downloads ORDER BY update_time DESC")
    List<VideoDownloadEntity> findAllSync();

    @Query("UPDATE video_downloads SET downloaded_bytes = :downloadedBytes, total_bytes = :totalBytes, status = :status, update_time = :updateTime WHERE download_id = :downloadId")
    void updateProgress(String downloadId,
                        long downloadedBytes,
                        long totalBytes,
                        @DownloadStatus int status,
                        long updateTime);

    @Query("UPDATE video_downloads SET status = :status, update_time = :updateTime WHERE download_id = :downloadId")
    void updateStatus(String downloadId,
                      @DownloadStatus int status,
                      long updateTime);

    @Query("DELETE FROM video_downloads WHERE download_id = :downloadId")
    void deleteById(String downloadId);

    @Query("DELETE FROM video_downloads WHERE download_id IN (:downloadIds)")
    void deleteByIds(List<String> downloadIds);
}
