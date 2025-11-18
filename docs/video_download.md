# 视频下载系统

借助 OkHttp、Hutool、Room 以及前台 `Service`，`app` 模块已经内置了一个可断点续传的多任务视频下载系统，核心路径位于 `com.hjq.demo.download`。该系统提供：

- **多/单任务下载**：`VideoDownloadManager` 基于线程池同时下载多个视频。
- **暂停 / 继续**：通过 `DownloadTask` 的原子状态与 Range 请求实现秒级暂停继续。
- **断点续传**：未完成的文件会记录已下载大小，重启或重新绑定服务即可继续。
- **实时数据库进度**：`Room` 实体 `VideoDownloadEntity` 每 400ms 更新一次，界面可直接观察 `LiveData`。
- **多/单条插入与删除**：`VideoDownloadDao` 暴露了单条、批量插入以及单条、批量删除接口。
- **剧集维度查询**：`observeSeries(seriesId)` 可实时返回同一剧集所有视频的状态。

## 依赖入口

`app/build.gradle` 新增：

```groovy
implementation 'cn.hutool:hutool-core:5.8.25'
implementation 'androidx.room:room-runtime:2.5.2'
annotationProcessor 'androidx.room:room-compiler:2.5.2'
```

## 启动下载服务

`AndroidManifest.xml` 已注册 `VideoDownloadService` 并声明前台服务权限。只需在界面层按需绑定：

```java
private VideoDownloadService.DownloadBinder mDownloadBinder;

private final ServiceConnection mConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mDownloadBinder = (VideoDownloadService.DownloadBinder) service;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mDownloadBinder = null;
    }
};

bindService(new Intent(this, VideoDownloadService.class), mConnection, Context.BIND_AUTO_CREATE);
```

## 构建下载任务

```java
DownloadRequest request = new DownloadRequest.Builder("episode-01", "https://cdn.example.com/episode01.mp4")
        .seriesId("season-2025")
        .episodeId("01")
        .videoName("第 1 集")
        .fileName("S2025E01.mp4")        // 可选，默认 downloadId.mp4
        .targetDir(customDirPath)        // 可选，默认 /Android/data/.../files/Movies/videos
        .extra("{\"bitrate\":\"1080p\"}") // 可选扩展字段
        .build();

mDownloadBinder.startDownload(request);      // 单任务
// 或者
mDownloadBinder.startDownloads(Arrays.asList(request1, request2)); // 多任务并发
```

## 暂停 / 继续 / 删除

```java
mDownloadBinder.pause("episode-01");
mDownloadBinder.resume("episode-01");
mDownloadBinder.pauseAll();
mDownloadBinder.resumeAll();
mDownloadBinder.delete("episode-01");
mDownloadBinder.delete(Arrays.asList("episode-01", "episode-02"));
```

## 数据库查询与实时进度

`VideoDownloadDao` 和 `VideoDownloadRepository` 支持单条、多条插入，`VideoDownloadService` 直接暴露了 `LiveData`：

```java
// 观察整个下载队列
mDownloadBinder.observeAll().observe(this, downloads -> {
    // 更新 UI：downloads 包含下载进度、状态、文件路径等
});

// 查询同一剧集的所有视频
mDownloadBinder.observeSeries("season-2025").observe(this, episodes -> {
    // 展示剧集维度的下载情况
});
```

`VideoDownloadEntity` 字段说明：

- `downloadId`：下载唯一 ID，既是主键也是断点续传的索引。
- `seriesId`：剧集 ID，可用作 “查询同一剧集所有视频” 的条件。
- `status`：受 `@DownloadStatus` 管理，包含等待、下载中、暂停、完成、失败、删除等。
- `downloadedBytes / totalBytes`：实时进度，可计算百分比。
- `filePath`：本地保存路径，删除任务时会通过 Hutool `FileUtil.del` 级联删除文件。

## Room 直接操作（可选）

如需绕过 Service 直接批量写入记录，可注入 `VideoDownloadRepository`：

```java
VideoDownloadRepository repository = new VideoDownloadRepository(context);
VideoDownloadEntity entity = new VideoDownloadEntity();
entity.setDownloadId("episode-03");
entity.setSeriesId("season-2025");
entity.setVideoUrl("https://...");
repository.insert(entity);               // 单条插入
repository.insert(Arrays.asList(e1, e2)); // 多条插入
```

随后调用 `VideoDownloadService` 继续下载即可。这样可以在登录成功后一次性写入整季数据，再按需触发实际下载。
