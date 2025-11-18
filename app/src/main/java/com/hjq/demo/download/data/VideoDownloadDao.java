package com.hjq.demo.download.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface VideoDownloadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(VideoDownloadEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insert(List<VideoDownloadEntity> entities);

    @Update
    int update(VideoDownloadEntity entity);

    @Delete
    int delete(VideoDownloadEntity entity);

    @Query("DELETE FROM video_downloads WHERE video_id IN(:videoIds)")
    void deleteByIds(List<String> videoIds);

    @Query("SELECT * FROM video_downloads WHERE video_id = :videoId LIMIT 1")
    VideoDownloadEntity findById(String videoId);

    @Query("SELECT * FROM video_downloads WHERE series_id = :seriesId ORDER BY update_time DESC")
    LiveData<List<VideoDownloadEntity>> observeBySeries(String seriesId);

    @Query("SELECT * FROM video_downloads ORDER BY update_time DESC")
    LiveData<List<VideoDownloadEntity>> observeAll();

    @Query("UPDATE video_downloads SET downloaded_bytes = :downloadedBytes, total_bytes = :totalBytes, status = :status, update_time = :updateTime WHERE video_id = :videoId")
    void updateProgress(String videoId, long downloadedBytes, long totalBytes, DownloadStatus status, long updateTime);
}
