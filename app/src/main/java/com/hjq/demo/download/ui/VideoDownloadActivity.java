package com.hjq.demo.download.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hjq.demo.R;
import com.hjq.demo.common.MyActivity;
import com.hjq.demo.download.data.DownloadStatus;
import com.hjq.demo.download.data.VideoDownloadEntity;
import com.hjq.demo.download.model.VideoDownloadRequest;
import com.hjq.demo.download.service.VideoDownloadService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class VideoDownloadActivity extends MyActivity implements View.OnClickListener,
        VideoDownloadAdapter.OnItemActionListener {

    public static void start(Context context) {
        Intent intent = new Intent(context, VideoDownloadActivity.class);
        context.startActivity(intent);
    }

    private RecyclerView mRecyclerView;
    private EditText mSeriesInput;
    private TextView mSelectionView;
    private VideoDownloadAdapter mAdapter;
    private VideoDownloadService.DownloadBinder mBinder;
    private final Set<String> mSelectedIds = new HashSet<>();
    private final List<VideoDownloadEntity> mCurrentEntities = new ArrayList<>();
    private boolean mBound;
    private LiveData<List<VideoDownloadEntity>> mActiveLiveData;
    private final Observer<List<VideoDownloadEntity>> mDownloadObserver = this::onDownloadsChanged;

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBinder = (VideoDownloadService.DownloadBinder) service;
            observeAll();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBinder = null;
        }
    };

    @Override
    protected int getLayoutId() {
        return R.layout.video_download_activity;
    }

    @Override
    protected void initView() {
        mRecyclerView = findViewById(R.id.rv_video_download);
        mSeriesInput = findViewById(R.id.et_series_filter);
        mSelectionView = findViewById(R.id.tv_selection_count);

        setOnClickListener(R.id.btn_add_single_download, R.id.btn_add_batch_download,
                R.id.btn_pause_all, R.id.btn_resume_all, R.id.btn_delete_selected,
                R.id.btn_filter_series, R.id.btn_filter_reset);
    }

    @Override
    protected void initData() {
        mAdapter = new VideoDownloadAdapter(this);
        mAdapter.setOnItemActionListener(this);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);

        VideoDownloadService.start(this);
        Intent intent = new Intent(this, VideoDownloadService.class);
        mBound = bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_add_single_download) {
            enqueueSingle();
        } else if (id == R.id.btn_add_batch_download) {
            enqueueBatch();
        } else if (id == R.id.btn_pause_all) {
            if (ensureBinder()) {
                mBinder.pauseAll();
            }
        } else if (id == R.id.btn_resume_all) {
            resumeAllFromList();
        } else if (id == R.id.btn_delete_selected) {
            deleteSelected();
        } else if (id == R.id.btn_filter_series) {
            applySeriesFilter();
        } else if (id == R.id.btn_filter_reset) {
            resetFilter();
        }
    }

    private void enqueueSingle() {
        if (!ensureBinder()) {
            return;
        }
        List<VideoDownloadRequest> samples = buildSampleRequests();
        VideoDownloadRequest request = samples.get((int) (System.currentTimeMillis() % samples.size()));
        mBinder.enqueue(request);
        toast("已添加单个下载任务");
    }

    private void enqueueBatch() {
        if (!ensureBinder()) {
            return;
        }
        mBinder.enqueue(buildSampleRequests());
        toast("已批量添加任务");
    }

    private void resumeAllFromList() {
        if (!ensureBinder()) {
            return;
        }
        List<String> pending = new ArrayList<>();
        for (VideoDownloadEntity entity : mCurrentEntities) {
            if (entity.getStatus() == DownloadStatus.PAUSED
                    || entity.getStatus() == DownloadStatus.FAILED
                    || entity.getStatus() == DownloadStatus.PENDING) {
                pending.add(entity.getVideoId());
            }
        }
        if (pending.isEmpty()) {
            toast("没有可继续的任务");
            return;
        }
        mBinder.resumeAll(pending);
    }

    private void deleteSelected() {
        if (!ensureBinder()) {
            return;
        }
        if (mSelectedIds.isEmpty()) {
            toast("请先选择要删除的视频");
            return;
        }
        mBinder.delete(new ArrayList<>(mSelectedIds));
        mSelectedIds.clear();
        updateSelectionHint();
        mAdapter.setSelectedIds(mSelectedIds);
    }

    private void applySeriesFilter() {
        if (!ensureBinder()) {
            return;
        }
        String seriesId = mSeriesInput.getText().toString().trim();
        if (TextUtils.isEmpty(seriesId)) {
            observeAll();
            return;
        }
        subscribeTo(mBinder.observeSeries(seriesId));
    }

    private void resetFilter() {
        mSeriesInput.setText("");
        observeAll();
    }

    private void observeAll() {
        if (!ensureBinder()) {
            return;
        }
        subscribeTo(mBinder.observeAll());
    }

    private void subscribeTo(LiveData<List<VideoDownloadEntity>> liveData) {
        if (mActiveLiveData != null) {
            mActiveLiveData.removeObservers(this);
        }
        mActiveLiveData = liveData;
        if (mActiveLiveData != null) {
            mActiveLiveData.observe(this, mDownloadObserver);
        }
    }

    private void onDownloadsChanged(List<VideoDownloadEntity> list) {
        mCurrentEntities.clear();
        if (list != null) {
            mCurrentEntities.addAll(list);
        }
        mSelectedIds.retainAll(extractIds(mCurrentEntities));
        mAdapter.setSelectedIds(mSelectedIds);
        updateSelectionHint();
        mAdapter.setData(list == null ? new ArrayList<>() : list);
    }

    private Set<String> extractIds(List<VideoDownloadEntity> entities) {
        Set<String> ids = new HashSet<>();
        for (VideoDownloadEntity entity : entities) {
            ids.add(entity.getVideoId());
        }
        return ids;
    }

    private List<VideoDownloadRequest> buildSampleRequests() {
        List<VideoDownloadRequest> requests = new ArrayList<>();
        requests.add(VideoDownloadRequest.builder()
                .setSeriesId("series_a")
                .setTitle("示例剧集A · 第一集")
                .setUrl("https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
                .build());
        requests.add(VideoDownloadRequest.builder()
                .setSeriesId("series_a")
                .setTitle("示例剧集A · 第二集")
                .setUrl("https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4")
                .build());
        requests.add(VideoDownloadRequest.builder()
                .setSeriesId("series_b")
                .setTitle("示例剧集B · 第一集")
                .setUrl("https://storage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4")
                .build());
        requests.add(VideoDownloadRequest.builder()
                .setSeriesId("series_c")
                .setTitle("示例剧集C · 特别篇")
                .setUrl("https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4")
                .build());
        return requests;
    }

    private boolean ensureBinder() {
        if (mBinder == null) {
            toast("服务未连接，请稍候");
            return false;
        }
        return true;
    }

    private void updateSelectionHint() {
        if (mSelectedIds.isEmpty()) {
            mSelectionView.setText(R.string.video_download_selection_empty);
        } else {
            mSelectionView.setText("已选中 " + mSelectedIds.size() + " 个视频");
        }
    }

    @Override
    public void onStart(VideoDownloadEntity entity) {
        if (!ensureBinder()) {
            return;
        }
        mBinder.resume(entity.getVideoId());
    }

    @Override
    public void onPause(VideoDownloadEntity entity) {
        if (!ensureBinder()) {
            return;
        }
        mBinder.pause(entity.getVideoId());
    }

    @Override
    public void onDelete(VideoDownloadEntity entity) {
        if (!ensureBinder()) {
            return;
        }
        mBinder.delete(entity.getVideoId());
    }

    @Override
    public void onSelectionChanged(VideoDownloadEntity entity, boolean selected) {
        if (selected) {
            mSelectedIds.add(entity.getVideoId());
        } else {
            mSelectedIds.remove(entity.getVideoId());
        }
        updateSelectionHint();
        mAdapter.setSelectedIds(mSelectedIds);
    }
}
