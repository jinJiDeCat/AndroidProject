package com.hjq.demo.download.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.hjq.demo.R;
import com.hjq.demo.common.MyAdapter;
import com.hjq.demo.download.data.DownloadStatus;
import com.hjq.demo.download.data.VideoDownloadEntity;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class VideoDownloadAdapter extends MyAdapter<VideoDownloadEntity> {

    public interface OnItemActionListener {
        void onStart(VideoDownloadEntity entity);

        void onPause(VideoDownloadEntity entity);

        void onDelete(VideoDownloadEntity entity);

        void onSelectionChanged(VideoDownloadEntity entity, boolean selected);
    }

    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault());
    private final DecimalFormat mDecimalFormat = new DecimalFormat("#.##");
    private final Set<String> mSelectedIds = new HashSet<>();
    private OnItemActionListener mListener;

    public VideoDownloadAdapter(Context context) {
        super(context);
    }

    public void setOnItemActionListener(OnItemActionListener listener) {
        mListener = listener;
    }

    public void setSelectedIds(Set<String> selectedIds) {
        mSelectedIds.clear();
        if (selectedIds != null) {
            mSelectedIds.addAll(selectedIds);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder();
    }

    private final class ViewHolder extends MyAdapter.ViewHolder {

        private final TextView mTitleView;
        private final TextView mSeriesView;
        private final TextView mStatusView;
        private final TextView mProgressView;
        private final ProgressBar mProgressBar;
        private final CheckBox mSelectView;
        private final View mStartButton;
        private final View mPauseButton;
        private final View mDeleteButton;

        private ViewHolder() {
            super(R.layout.item_video_download);
            mTitleView = (TextView) findViewById(R.id.tv_video_title);
            mSeriesView = (TextView) findViewById(R.id.tv_video_series);
            mStatusView = (TextView) findViewById(R.id.tv_video_status);
            mProgressView = (TextView) findViewById(R.id.tv_video_progress);
            mProgressBar = (ProgressBar) findViewById(R.id.pb_video_progress);
            mSelectView = (CheckBox) findViewById(R.id.cb_video_selected);
            mStartButton = findViewById(R.id.btn_video_start);
            mPauseButton = findViewById(R.id.btn_video_pause);
            mDeleteButton = findViewById(R.id.btn_video_delete);
        }

        @Override
        public void onBindView(int position) {
            VideoDownloadEntity entity = getItem(position);
            mTitleView.setText(entity.getTitle());
            mSeriesView.setText(getContext().getString(R.string.video_download_series_display, entity.getSeriesId()));

            DownloadStatus status = entity.getStatus();
            String statusText = mapStatus(status);
            String timeText = mDateFormat.format(entity.getUpdateTime() == 0 ? System.currentTimeMillis() : entity.getUpdateTime());
            mStatusView.setText(getContext().getString(R.string.video_download_status, statusText, timeText));

            long downloaded = entity.getDownloadedBytes();
            long total = entity.getTotalBytes();
            int progress = total <= 0 ? 0 : (int) Math.min(100, (downloaded * 100f / total));
            mProgressBar.setProgress(progress);
            mProgressView.setText(getContext().getString(R.string.video_download_progress_format,
                    formatBytes(downloaded), formatBytes(total), progress));

            boolean isSelected = mSelectedIds.contains(entity.getVideoId());
            mSelectView.setOnCheckedChangeListener(null);
            mSelectView.setChecked(isSelected);
            mSelectView.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (mListener != null) {
                    mListener.onSelectionChanged(entity, isChecked);
                }
            });

            mStartButton.setEnabled(status != DownloadStatus.DOWNLOADING && status != DownloadStatus.COMPLETED);
            mPauseButton.setEnabled(status == DownloadStatus.DOWNLOADING);

            mStartButton.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onStart(entity);
                }
            });

            mPauseButton.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onPause(entity);
                }
            });

            mDeleteButton.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onDelete(entity);
                }
            });
        }
    }

    private String mapStatus(DownloadStatus status) {
        switch (status) {
            case DOWNLOADING:
                return "下载中";
            case PAUSED:
                return "已暂停";
            case COMPLETED:
                return "已完成";
            case FAILED:
                return "失败";
            case CANCELLED:
                return "已取消";
            case PENDING:
            default:
                return "等待中";
        }
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0) {
            return "0B";
        }
        double value = bytes;
        String unit = "B";
        if (bytes >= 1024) {
            value = bytes / 1024d;
            unit = "KB";
        }
        if (bytes >= 1024 * 1024) {
            value = bytes / (1024d * 1024d);
            unit = "MB";
        }
        if (bytes >= 1024 * 1024 * 1024) {
            value = bytes / (1024d * 1024d * 1024d);
            unit = "GB";
        }
        return mDecimalFormat.format(value) + unit;
    }
}
