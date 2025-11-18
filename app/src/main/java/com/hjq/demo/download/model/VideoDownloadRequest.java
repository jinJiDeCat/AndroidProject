package com.hjq.demo.download.model;

import androidx.annotation.NonNull;

import java.io.Serializable;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

public final class VideoDownloadRequest implements Serializable {

    private final String videoId;
    private final String seriesId;
    private final String title;
    private final String url;

    private VideoDownloadRequest(Builder builder) {
        this.videoId = StrUtil.blankToDefault(builder.videoId, IdUtil.fastUUID());
        this.seriesId = builder.seriesId;
        this.title = builder.title;
        this.url = builder.url;
    }

    @NonNull
    public String getVideoId() {
        return videoId;
    }

    public String getSeriesId() {
        return seriesId;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String videoId;
        private String seriesId;
        private String title;
        private String url;

        public Builder setVideoId(String videoId) {
            this.videoId = videoId;
            return this;
        }

        public Builder setSeriesId(String seriesId) {
            this.seriesId = seriesId;
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public VideoDownloadRequest build() {
            if (StrUtil.isBlank(url)) {
                throw new IllegalArgumentException("video url can not be empty");
            }
            if (StrUtil.isBlank(title)) {
                title = "未命名";
            }
            if (StrUtil.isBlank(seriesId)) {
                seriesId = "default_series";
            }
            return new VideoDownloadRequest(this);
        }
    }
}
