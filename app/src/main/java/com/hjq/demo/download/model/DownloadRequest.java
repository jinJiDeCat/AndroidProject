package com.hjq.demo.download.model;

import android.text.TextUtils;

import cn.hutool.core.util.StrUtil;

/**
 * 下载请求参数。
 */
public class DownloadRequest {

    private final String downloadId;
    private final String seriesId;
    private final String episodeId;
    private final String videoName;
    private final String videoUrl;
    private final String coverUrl;
    private final String targetDir;
    private final String fileName;
    private final String extra;

    private DownloadRequest(Builder builder) {
        this.downloadId = builder.downloadId;
        this.seriesId = builder.seriesId;
        this.episodeId = builder.episodeId;
        this.videoName = builder.videoName;
        this.videoUrl = builder.videoUrl;
        this.coverUrl = builder.coverUrl;
        this.targetDir = builder.targetDir;
        this.fileName = builder.fileName;
        this.extra = builder.extra;
    }

    public String getDownloadId() {
        return downloadId;
    }

    public String getSeriesId() {
        return seriesId;
    }

    public String getEpisodeId() {
        return episodeId;
    }

    public String getVideoName() {
        return videoName;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public String getTargetDir() {
        return targetDir;
    }

    public String getFileName() {
        return fileName;
    }

    public String getExtra() {
        return extra;
    }

    public static final class Builder {

        private final String downloadId;
        private final String videoUrl;
        private String seriesId;
        private String episodeId;
        private String videoName;
        private String coverUrl;
        private String targetDir;
        private String fileName;
        private String extra;

        public Builder(String downloadId, String videoUrl) {
            if (StrUtil.isBlank(downloadId)) {
                throw new IllegalArgumentException("downloadId 不能为空");
            }
            if (StrUtil.isBlank(videoUrl)) {
                throw new IllegalArgumentException("videoUrl 不能为空");
            }
            this.downloadId = downloadId;
            this.videoUrl = videoUrl;
        }

        public Builder seriesId(String seriesId) {
            this.seriesId = seriesId;
            return this;
        }

        public Builder episodeId(String episodeId) {
            this.episodeId = episodeId;
            return this;
        }

        public Builder videoName(String videoName) {
            this.videoName = videoName;
            return this;
        }

        public Builder coverUrl(String coverUrl) {
            this.coverUrl = coverUrl;
            return this;
        }

        public Builder targetDir(String targetDir) {
            if (!TextUtils.isEmpty(targetDir)) {
                this.targetDir = targetDir;
            }
            return this;
        }

        public Builder fileName(String fileName) {
            if (!TextUtils.isEmpty(fileName)) {
                this.fileName = fileName;
            }
            return this;
        }

        public Builder extra(String extra) {
            this.extra = extra;
            return this;
        }

        public DownloadRequest build() {
            return new DownloadRequest(this);
        }
    }
}
