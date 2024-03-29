package com.cocosw.framework.network;

import android.content.Context;
import android.os.StatFs;

import com.cocosw.framework.exception.CocoException;
import com.cocosw.framework.log.Log;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.squareup.okhttp.HttpResponseCache;
import com.squareup.okhttp.OkHttpClient;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.Map;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.FROYO;

/**
 * This class is for provide convenient interface for sync http access using OKHttp
 * especially get json object from url
 * <p/>
 * Usage:
 * public static App getApp(long id) throws CocoException {
 * return request(HOST + "apps/" + id, App.class);
 * }
 * <p/>
 * User: soarcn
 * Date: 13-11-15
 * Time: 下午6:17
 */
public class Network {

    private static int TIMEOUT = 300 * 1000;

    protected static Gson GSON = new GsonBuilder().setDateFormat(
            "yyyy-MM-dd").create();
    private static Map<String, String> headers;

    /**
     * use for app initialization
     */
    public static void init(Context context) {
        HttpRequest.setConnectionFactory(new OkConnectionFactory(context));
        if (SDK_INT <= FROYO)
            HttpRequest.keepAlive(false);
    }

    public static void setTIMEOUT(int TIMEOUT) {
        Network.TIMEOUT = TIMEOUT;
    }

    public static void setGSON(Gson GSON) {
        Network.GSON = GSON;
    }

    /**
     * Execute request
     *
     * @param target
     * @return request
     * @throws IOException
     * @throws com.cocosw.framework.exception.CocoException
     */
    protected static <T extends Object> T request(String url, Class<T> target, String rawjson)
            throws CocoException {
        return fromRequest(requestHttp(url, rawjson), target);
    }

    /**
     * Set global headers for all http request
     *
     * @param headers
     */
    protected static void setHeader(final Map<String, String> headers) {
        Network.headers = headers;
    }

    /**
     * Execute request
     *
     * @return request
     * @throws IOException
     * @throws com.cocosw.framework.exception.CocoException
     */
    protected static HttpRequest requestHttp(String url, String rawjson)
            throws CocoException {
        Log.d(url);
        HttpRequest request = HttpRequest.get(url);
        request.connectTimeout(TIMEOUT).readTimeout(TIMEOUT);
        request.contentType(HttpRequest.CONTENT_TYPE_JSON);
        //TODO preset header setter
        // request.header("imei",IMEI);
        if (!headers.isEmpty()) {
            request.headers(headers);
        }
        request.header("Connection", "close");
        request.acceptJson();
        request.acceptCharset(HttpRequest.CHARSET_UTF8);
        request.useCaches(true);
        if (rawjson != null)
            request.send(rawjson);
        if (!request.ok()) {
            Log.d(request.message());
            throw new CocoException("当前网络出了一些问题，请稍后重试");
        }
        return request;
    }

    protected static <T extends Object> T fromRequest(HttpRequest request, Class<T> target) {
        Reader reader = request.bufferedReader();
        Log.i(request.body());
        try {
            return GSON.fromJson(reader, target);
        } catch (JsonParseException e) {
            e.printStackTrace();
            throw new CocoException("当前服务器出了一些问题，请稍后重试", e);
        } finally {
            try {
                reader.close();
            } catch (IOException ignored) {
                // Ignored
            }
        }
    }

    /**
     * Execute request
     *
     * @param target
     * @return request
     * @throws IOException
     * @throws CocoException
     */
    protected static <T extends Object> T request(String url, Class<T> target)
            throws CocoException {
        return request(url, target, null);
    }


    /**
     * A {@link HttpRequest.ConnectionFactory connection factory} which uses OkHttp.
     * <p/>
     * Call {@link HttpRequest#setConnectionFactory(HttpRequest.ConnectionFactory)} with an instance of
     * this class to enable.
     */
    private static class OkConnectionFactory implements HttpRequest.ConnectionFactory {
        private static final String COCO_CACHE = "coco-cache";
        private static final int MIN_DISK_CACHE_SIZE = 5 * 1024 * 1024; // 5MB
        private static final int MAX_DISK_CACHE_SIZE = 50 * 1024 * 1024; // 50MB

        private final OkHttpClient client;

        public OkConnectionFactory(Context context) {
            this(new OkHttpClient(), context);
        }

        public OkConnectionFactory(OkHttpClient client, Context context) {
            if (client == null) {
                throw new NullPointerException("Client must not be null.");
            }
            this.client = client;
            try {
                this.client.setResponseCache(new HttpResponseCache(createDefaultCacheDir(context), calculateDiskCacheSize(createDefaultCacheDir(context))));
            } catch (IOException ignored) {
            }
        }

        public HttpURLConnection create(URL url) throws IOException {
            return client.open(url);
        }

        public HttpURLConnection create(URL url, Proxy proxy) throws IOException {
            throw new UnsupportedOperationException(
                    "Per-connection proxy is not supported. Use OkHttpClient's setProxy instead.");
        }

        private File createDefaultCacheDir(Context context) {
            File cache = new File(context.getApplicationContext().getCacheDir(), COCO_CACHE);
            if (!cache.exists()) {
                cache.mkdirs();
            }
            return cache;
        }

        private int calculateDiskCacheSize(File dir) {
            StatFs statFs = new StatFs(dir.getAbsolutePath());
            int available = statFs.getBlockCount() * statFs.getBlockSize();
            int size = available / 50;
            return Math.max(Math.min(size, MAX_DISK_CACHE_SIZE), MIN_DISK_CACHE_SIZE);
        }
    }


}
