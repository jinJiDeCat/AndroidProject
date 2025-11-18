package com.hjq.demo.http;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.internal.bind.TypeAdapters;
import com.hjq.demo.http.callback.HttpCallback;
import com.hjq.demo.http.core.RequestApi;
import com.hjq.demo.http.core.ServerHost;
import com.hjq.demo.http.json.BooleanTypeAdapter;
import com.hjq.demo.http.json.DoubleTypeAdapter;
import com.hjq.demo.http.json.FloatTypeAdapter;
import com.hjq.demo.http.json.IntegerTypeAdapter;
import com.hjq.demo.http.json.ListTypeAdapter;
import com.hjq.demo.http.json.LongTypeAdapter;
import com.hjq.demo.http.json.StringTypeAdapter;
import com.hjq.demo.http.listener.OnDownloadListener;
import com.hjq.demo.http.listener.OnHttpListener;
import com.hjq.demo.http.model.DownloadInfo;
import com.hjq.demo.http.model.HttpMethod;
import com.hjq.demo.http.model.RequestHandler;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

/**
 * 自定义网络请求封装
 */
public final class SimpleHttp {

    private static final String TAG = "SimpleHttp";
    private static final MediaType MEDIA_TYPE_JSON = MediaType.get("application/json; charset=utf-8");

    private static SimpleHttp sInstance;

    private final OkHttpClient mClient;
    private final ServerHost mServer;
    private final RequestHandler mHandler;
    private final Gson mGson;

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, Object> mGlobalParams = new ConcurrentHashMap<>();
    private final Map<String, String> mGlobalHeaders = new ConcurrentHashMap<>();
    private final Map<LifecycleOwner, LifecycleCallObserver> mLifecycleObservers = new ConcurrentHashMap<>();

    private SimpleHttp(Application application, OkHttpClient client, ServerHost server) {
        mClient = client;
        mServer = server;
        mGson = createGson();
        mHandler = new RequestHandler(application, mGson);
    }

    public static void init(@NonNull Application application,
                            @NonNull OkHttpClient client,
                            @NonNull ServerHost server) {
        sInstance = new SimpleHttp(application, client, server);
    }

    public static SimpleHttp getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("SimpleHttp has not been initialized");
        }
        return sInstance;
    }

    public static RequestBuilder post(LifecycleOwner owner) {
        return new RequestBuilder(owner, HttpMethod.POST);
    }

    public static RequestBuilder get(LifecycleOwner owner) {
        return new RequestBuilder(owner, HttpMethod.GET);
    }

    public static DownloadBuilder download(LifecycleOwner owner) {
        return new DownloadBuilder(owner);
    }

    public OkHttpClient getClient() {
        return mClient;
    }

    public SimpleHttp addParam(String key, Object value) {
        if (value == null) {
            mGlobalParams.remove(key);
        } else {
            mGlobalParams.put(key, value);
        }
        return this;
    }

    public SimpleHttp removeParam(String key) {
        mGlobalParams.remove(key);
        return this;
    }

    public SimpleHttp clearParams() {
        mGlobalParams.clear();
        return this;
    }

    public SimpleHttp addHeader(String key, String value) {
        if (value == null) {
            mGlobalHeaders.remove(key);
        } else {
            mGlobalHeaders.put(key, value);
        }
        return this;
    }

    public SimpleHttp removeHeader(String key) {
        mGlobalHeaders.remove(key);
        return this;
    }

    private static Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapterFactory(TypeAdapters.newFactory(String.class, new StringTypeAdapter()))
                .registerTypeAdapterFactory(TypeAdapters.newFactory(boolean.class, Boolean.class, new BooleanTypeAdapter()))
                .registerTypeAdapterFactory(TypeAdapters.newFactory(int.class, Integer.class, new IntegerTypeAdapter()))
                .registerTypeAdapterFactory(TypeAdapters.newFactory(long.class, Long.class, new LongTypeAdapter()))
                .registerTypeAdapterFactory(TypeAdapters.newFactory(float.class, Float.class, new FloatTypeAdapter()))
                .registerTypeAdapterFactory(TypeAdapters.newFactory(double.class, Double.class, new DoubleTypeAdapter()))
                .registerTypeHierarchyAdapter(List.class, new ListTypeAdapter())
                .create();
    }

    private void trackCall(LifecycleOwner owner, Call call) {
        if (owner == null) {
            return;
        }
        LifecycleCallObserver observer = mLifecycleObservers.get(owner);
        if (observer == null) {
            observer = new LifecycleCallObserver(owner);
            mLifecycleObservers.put(owner, observer);
            owner.getLifecycle().addObserver(observer);
        }
        observer.track(call);
    }

    private void releaseCall(LifecycleOwner owner, Call call) {
        if (owner == null) {
            return;
        }
        LifecycleCallObserver observer = mLifecycleObservers.get(owner);
        if (observer != null) {
            observer.release(call);
        }
    }

    private void notifyStart(List<OnHttpListener<?>> listeners, Call call) {
        if (listeners.isEmpty()) {
            return;
        }
        mMainHandler.post(() -> {
            for (OnHttpListener<?> listener : listeners) {
                listener.onStart(call);
            }
        });
    }

    private void notifySuccess(List<OnHttpListener<?>> listeners, Object result) {
        if (listeners.isEmpty()) {
            return;
        }
        mMainHandler.post(() -> {
            for (OnHttpListener<?> listener : listeners) {
                //noinspection unchecked
                ((OnHttpListener<Object>) listener).onSucceed(result);
            }
        });
    }

    private void notifyFail(List<OnHttpListener<?>> listeners, Exception e) {
        if (listeners.isEmpty()) {
            return;
        }
        mMainHandler.post(() -> {
            for (OnHttpListener<?> listener : listeners) {
                listener.onFail(e);
            }
        });
    }

    private void notifyEnd(List<OnHttpListener<?>> listeners, Call call) {
        if (listeners.isEmpty()) {
            return;
        }
        mMainHandler.post(() -> {
            for (OnHttpListener<?> listener : listeners) {
                listener.onEnd(call);
            }
        });
    }

    private List<OnHttpListener<?>> collectListeners(LifecycleOwner owner, OnHttpListener<?> callback) {
        List<OnHttpListener<?>> listeners = new ArrayList<>();
        if (owner instanceof OnHttpListener) {
            listeners.add((OnHttpListener<?>) owner);
        }
        if (callback != null && callback != owner) {
            listeners.add(callback);
        }
        return listeners;
    }

    private Request buildRequest(RequestApi api,
                                 HttpMethod method,
                                 Map<String, JsonElement> params,
                                 Map<String, String> query,
                                 Map<String, File> files,
                                 Map<String, String> headers) {

        HttpUrl baseUrl = HttpUrl.parse(mServer.getBaseUrl());
        if (baseUrl == null) {
            throw new IllegalStateException("Invalid base url: " + mServer.getBaseUrl());
        }
        HttpUrl.Builder urlBuilder = baseUrl.newBuilder();
        String path = api.getApi();
        if (!TextUtils.isEmpty(path)) {
            urlBuilder.addEncodedPathSegments(trimPath(path));
        }

        RequestBody body = null;
        Map<String, String> finalQuery = new LinkedHashMap<>(query);

        if (method == HttpMethod.GET) {
            appendQueryFromJson(finalQuery, params);
        } else {
            if (!files.isEmpty()) {
                MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
                for (Map.Entry<String, JsonElement> entry : params.entrySet()) {
                    builder.addFormDataPart(entry.getKey(), toPlainString(entry.getValue()));
                }
                for (Map.Entry<String, File> entry : files.entrySet()) {
                    File file = entry.getValue();
                    builder.addFormDataPart(entry.getKey(),
                            file.getName(),
                            RequestBody.create(guessMimeType(file), file));
                }
                body = builder.build();
            } else {
                JsonObject jsonObject = new JsonObject();
                for (Map.Entry<String, JsonElement> entry : params.entrySet()) {
                    jsonObject.add(entry.getKey(), entry.getValue());
                }
                body = RequestBody.create(MEDIA_TYPE_JSON, jsonObject.toString());
            }
        }

        for (Map.Entry<String, String> entry : finalQuery.entrySet()) {
            urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
        }

        Request.Builder requestBuilder = new Request.Builder().url(urlBuilder.build());
        Map<String, String> finalHeaders = new LinkedHashMap<>(mGlobalHeaders);
        finalHeaders.putAll(headers);
        for (Map.Entry<String, String> entry : finalHeaders.entrySet()) {
            requestBuilder.addHeader(entry.getKey(), entry.getValue());
        }

        if (method == HttpMethod.GET) {
            requestBuilder.get();
        } else if (method == HttpMethod.POST) {
            requestBuilder.post(body != null ? body : RequestBody.create(MEDIA_TYPE_JSON, new byte[0]));
        } else if (method == HttpMethod.PUT) {
            requestBuilder.put(body != null ? body : RequestBody.create(MEDIA_TYPE_JSON, new byte[0]));
        } else if (method == HttpMethod.DELETE) {
            if (body != null) {
                requestBuilder.delete(body);
            } else {
                requestBuilder.delete();
            }
        }
        return requestBuilder.build();
    }

    private static String trimPath(String path) {
        String result = path;
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        return result;
    }

    private void appendQueryFromJson(Map<String, String> query, Map<String, JsonElement> params) {
        for (Map.Entry<String, JsonElement> entry : params.entrySet()) {
            query.put(entry.getKey(), toPlainString(entry.getValue()));
        }
    }

    private static String toPlainString(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "";
        }
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        return element.toString();
    }

    private static MediaType guessMimeType(File file) {
        String type = URLConnection.guessContentTypeFromName(file.getName());
        if (type == null) {
            type = "application/octet-stream";
        }
        return MediaType.parse(type);
    }

    private static void appendAll(Map<String, JsonElement> target, Map<String, ?> source, Gson gson) {
        for (Map.Entry<String, ?> entry : source.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            target.put(entry.getKey(), gson.toJsonTree(value));
        }
    }

    private static RequestPayload buildPayload(RequestApi api, Gson gson) {
        RequestPayload payload = new RequestPayload();
        if (api == null) {
            return payload;
        }
        Class<?> type = api.getClass();
        while (type != null && type != Object.class) {
            Field[] fields = type.getDeclaredFields();
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                try {
                    Object value = field.get(api);
                    if (value == null) {
                        continue;
                    }
                    if (value instanceof File) {
                        payload.files.put(field.getName(), (File) value);
                    } else {
                        payload.params.put(field.getName(), gson.toJsonTree(value));
                    }
                } catch (IllegalAccessException e) {
                    Log.e(TAG, "Reflect field error", e);
                }
            }
            type = type.getSuperclass();
        }
        return payload;
    }

    private String md5(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        try (BufferedSource source = Okio.buffer(Okio.source(file))) {
            Buffer buffer = new Buffer();
            long read;
            while ((read = source.read(buffer, 8192)) != -1) {
                digest.update(buffer.readByteArray(read));
            }
        }
        byte[] result = digest.digest();
        StringBuilder builder = new StringBuilder();
        for (byte b : result) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                builder.append('0');
            }
            builder.append(hex);
        }
        return builder.toString();
    }

    public static final class RequestBuilder {

        private final LifecycleOwner mOwner;
        private final HttpMethod mDefaultMethod;
        private RequestApi mApi;
        private final Map<String, Object> mExtraParams = new LinkedHashMap<>();
        private final Map<String, String> mHeaders = new LinkedHashMap<>();
        private final Map<String, String> mQueries = new LinkedHashMap<>();

        private RequestBuilder(LifecycleOwner owner, HttpMethod method) {
            mOwner = owner;
            mDefaultMethod = method;
        }

        public RequestBuilder api(RequestApi api) {
            mApi = api;
            return this;
        }

        public RequestBuilder addParam(String key, Object value) {
            mExtraParams.put(key, value);
            return this;
        }

        public RequestBuilder addHeader(String key, String value) {
            mHeaders.put(key, value);
            return this;
        }

        public RequestBuilder addQuery(String key, String value) {
            mQueries.put(key, value);
            return this;
        }

        public <T> void request(@NonNull HttpCallback<T> callback) {
            if (mApi == null) {
                throw new IllegalStateException("Request api can not be null");
            }
            SimpleHttp core = SimpleHttp.getInstance();
            RequestPayload payload = buildPayload(mApi, core.mGson);

            Map<String, JsonElement> params = new LinkedHashMap<>();
            appendAll(params, core.mGlobalParams, core.mGson);
            params.putAll(payload.params);
            appendAll(params, mExtraParams, core.mGson);

            Map<String, File> files = payload.files;

            HttpMethod method = mApi.getMethod() != null ? mApi.getMethod() : mDefaultMethod;

            Request request = core.buildRequest(mApi, method, params, mQueries, files, mHeaders);
            Call call = core.mClient.newCall(request);

            core.trackCall(mOwner, call);
            List<OnHttpListener<?>> listeners = core.collectListeners(mOwner, callback);
            core.notifyStart(listeners, call);
            Type type = callback.getType();

            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    core.releaseCall(mOwner, call);
                    Exception exception = core.mHandler.convertException(e);
                    core.notifyFail(listeners, exception);
                    core.notifyEnd(listeners, call);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    core.releaseCall(mOwner, call);
                    try (ResponseBody body = response.body()) {
                        Object result = core.mHandler.parseResponse(response, type);
                        core.notifySuccess(listeners, result);
                    } catch (Exception e) {
                        Exception exception = core.mHandler.convertException(e);
                        core.notifyFail(listeners, exception);
                    } finally {
                        core.notifyEnd(listeners, call);
                    }
                }
            });
        }
    }

    public final class DownloadBuilder {

        private final LifecycleOwner mOwner;
        private HttpMethod mMethod = HttpMethod.GET;
        private File mFile;
        private String mUrl;
        private String mMd5;
        private OnDownloadListener mListener;
        private final Map<String, String> mHeaders = new LinkedHashMap<>();

        private DownloadBuilder(LifecycleOwner owner) {
            mOwner = owner;
        }

        public DownloadBuilder method(HttpMethod method) {
            mMethod = method;
            return this;
        }

        public DownloadBuilder file(File file) {
            mFile = file;
            return this;
        }

        public DownloadBuilder url(String url) {
            mUrl = url;
            return this;
        }

        public DownloadBuilder md5(String md5) {
            mMd5 = md5;
            return this;
        }

        public DownloadBuilder listener(OnDownloadListener listener) {
            mListener = listener;
            return this;
        }

        public DownloadBuilder addHeader(String key, String value) {
            mHeaders.put(key, value);
            return this;
        }

        public Call start() {
            if (TextUtils.isEmpty(mUrl)) {
                throw new IllegalStateException("Download url can not be empty");
            }
            if (mFile == null) {
                throw new IllegalStateException("Download file can not be null");
            }
            if (mFile.getParentFile() != null && !mFile.getParentFile().exists()) {
                //noinspection ResultOfMethodCallIgnored
                mFile.getParentFile().mkdirs();
            }

            Request.Builder builder = new Request.Builder().url(mUrl);
            if (mMethod == HttpMethod.GET) {
                builder.get();
            } else {
                builder.method(mMethod.name(), RequestBody.create(MEDIA_TYPE_JSON, new byte[0]));
            }
            Map<String, String> finalHeaders = new LinkedHashMap<>(mGlobalHeaders);
            finalHeaders.putAll(mHeaders);
            for (Map.Entry<String, String> entry : finalHeaders.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }

            Call call = mClient.newCall(builder.build());
            trackCall(mOwner, call);
            OnDownloadListener listener = mListener;
            notifyDownloadStart(listener, call);

            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    releaseCall(mOwner, call);
                    notifyDownloadError(listener, new DownloadInfo(mFile, 0, 0), mHandler.convertException(e));
                    notifyDownloadEnd(listener, call);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    releaseCall(mOwner, call);
                    if (!response.isSuccessful()) {
                        notifyDownloadError(listener, new DownloadInfo(mFile, 0, 0),
                                new IOException("response code " + response.code()));
                        notifyDownloadEnd(listener, call);
                        return;
                    }
                    ResponseBody body = response.body();
                    if (body == null) {
                        notifyDownloadError(listener, new DownloadInfo(mFile, 0, 0),
                                new IOException("empty body"));
                        notifyDownloadEnd(listener, call);
                        return;
                    }
                    long total = body.contentLength();
                    long progress = 0;
                    try (BufferedSource source = body.source();
                         BufferedSink sink = Okio.buffer(Okio.sink(mFile))) {
                        Buffer buffer = new Buffer();
                        long read;
                        while ((read = source.read(buffer, 8192)) != -1) {
                            sink.write(buffer, read);
                            progress += read;
                            notifyDownloadProgress(listener, new DownloadInfo(mFile, total, progress));
                        }
                        sink.flush();
                        if (!TextUtils.isEmpty(mMd5)) {
                            String fileMd5 = md5(mFile);
                            if (!mMd5.equalsIgnoreCase(fileMd5)) {
                                throw new IOException("md5 verify fail");
                            }
                        }
                        notifyDownloadComplete(listener, new DownloadInfo(mFile, total, progress));
                    } catch (Exception e) {
                        notifyDownloadError(listener, new DownloadInfo(mFile, total, progress),
                                mHandler.convertException(e));
                    } finally {
                        notifyDownloadEnd(listener, call);
                    }
                }
            });
            return call;
        }

        private void notifyDownloadStart(OnDownloadListener listener, Call call) {
            if (listener == null) {
                return;
            }
            mMainHandler.post(() -> listener.onStart(call));
        }

        private void notifyDownloadProgress(OnDownloadListener listener, DownloadInfo info) {
            if (listener == null) {
                return;
            }
            mMainHandler.post(() -> listener.onProgress(info));
        }

        private void notifyDownloadComplete(OnDownloadListener listener, DownloadInfo info) {
            if (listener == null) {
                return;
            }
            mMainHandler.post(() -> listener.onComplete(info));
        }

        private void notifyDownloadError(OnDownloadListener listener, DownloadInfo info, Exception e) {
            if (listener == null) {
                return;
            }
            mMainHandler.post(() -> listener.onError(info, e));
        }

        private void notifyDownloadEnd(OnDownloadListener listener, Call call) {
            if (listener == null) {
                return;
            }
            mMainHandler.post(() -> listener.onEnd(call));
        }
    }

    private static final class RequestPayload {

        private final Map<String, JsonElement> params = new LinkedHashMap<>();
        private final Map<String, File> files = new LinkedHashMap<>();
    }

    private final class LifecycleCallObserver implements DefaultLifecycleObserver {

        private final LifecycleOwner mOwner;
        private final Set<Call> mCalls = Collections.newSetFromMap(new ConcurrentHashMap<>());

        private LifecycleCallObserver(LifecycleOwner owner) {
            mOwner = owner;
        }

        private void track(Call call) {
            mCalls.add(call);
        }

        private void release(Call call) {
            mCalls.remove(call);
        }

        @Override
        public void onDestroy(@NonNull LifecycleOwner owner) {
            for (Call call : mCalls) {
                if (!call.isCanceled()) {
                    call.cancel();
                }
            }
            mCalls.clear();
            owner.getLifecycle().removeObserver(this);
            mLifecycleObservers.remove(mOwner);
        }
    }
}
