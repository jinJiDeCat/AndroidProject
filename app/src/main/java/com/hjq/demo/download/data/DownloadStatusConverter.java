package com.hjq.demo.download.data;

import androidx.annotation.NonNull;
import androidx.room.TypeConverter;

public final class DownloadStatusConverter {

    private DownloadStatusConverter() {}

    @TypeConverter
    public static String toString(@NonNull DownloadStatus status) {
        return status.name();
    }

    @TypeConverter
    public static DownloadStatus toStatus(String value) {
        if (value == null) {
            return DownloadStatus.PENDING;
        }
        try {
            return DownloadStatus.valueOf(value);
        } catch (IllegalArgumentException ignore) {
            return DownloadStatus.PENDING;
        }
    }
}
