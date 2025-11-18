package com.hjq.demo.download.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {VideoDownloadEntity.class}, version = 1, exportSchema = false)
@TypeConverters({DownloadStatusConverter.class})
public abstract class VideoDownloadDatabase extends RoomDatabase {

    private static volatile VideoDownloadDatabase sInstance;

    public abstract VideoDownloadDao videoDownloadDao();

    public static VideoDownloadDatabase getInstance(@NonNull Context context) {
        if (sInstance == null) {
            synchronized (VideoDownloadDatabase.class) {
                if (sInstance == null) {
                    sInstance = Room.databaseBuilder(context.getApplicationContext(),
                            VideoDownloadDatabase.class, "video_downloads.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return sInstance;
    }
}
