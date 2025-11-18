package com.hjq.demo.download.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Room 数据库实例。
 */
@Database(
        entities = {VideoDownloadEntity.class},
        version = 1,
        exportSchema = false
)
public abstract class DownloadDatabase extends RoomDatabase {

    private static volatile DownloadDatabase sInstance;

    public static DownloadDatabase getInstance(Context context) {
        if (sInstance == null) {
            synchronized (DownloadDatabase.class) {
                if (sInstance == null) {
                    sInstance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    DownloadDatabase.class,
                                    "video_downloads.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return sInstance;
    }

    public abstract VideoDownloadDao videoDownloadDao();
}
