package com.cocosw.framework.network;

import com.cocosw.accessory.utils.FakeX509TrustManager;
import com.cocosw.framework.exception.CocoException;
import com.cocosw.framework.log.Log;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.squareup.okhttp.OkHttpClient;

import java.io.IOException;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.FROYO;

/**
 * This class is for provide convenient interface for sync http access using OKHttp
 * especially get json object from url
 *
 * Usage:
 *     public static App getApp(long id) throws CocoException {
            return request(HOST + "apps/" + id, App.class);
       }
 *
 * User: soarcn
 * Date: 13-11-15
 * Time: 下午6:17
 */
public class Network {

    private static int TIMEOUT = 300 * 1000;

    protected static Gson GSON = new GsonBuilder().setDateFormat(
            "yyyy-MM-dd").create();

    /**
     * use for app initialization
     */
    public static void init() {
        FakeX509TrustManager.allowAllSSL();
        HttpRequest.setConnectionFactory(new OkConnectionFactory());
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
     * @return
     * @return request
     * @throws IOException
     * @throws com.cocosw.framework.exception.CocoException
     */
    protected static <T extends Object> T request(String url, Class<T> target,String rawjson)
            throws CocoException {
        Log.d(url);
        HttpRequest request = HttpRequest.get(url);
        request.connectTimeout(TIMEOUT).readTimeout(TIMEOUT);
        request.contentType(HttpRequest.CONTENT_TYPE_JSON);
        //TODO preset header setter
       // request.header("imei",IMEI);
        request.header("Connection", "close");
        request.acceptJson();
        request.acceptCharset(HttpRequest.CHARSET_UTF8);
        request.useCaches(true);
        if (rawjson!=null)
            request.send(rawjson);
        if (!request.ok()) {
            Log.d(request.message());
            throw new CocoException("当前网络出了一些问题，请稍后重试");
        }
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
     * @return
     * @return request
     * @throws IOException
     * @throws CocoException
     */
    protected static <T extends Object> T request(String url, Class<T> target)
            throws CocoException {
        return request(url,target,null);
    }



    /**
     * A {@link HttpRequest.ConnectionFactory connection factory} which uses OkHttp.
     * <p/>
     * Call {@link HttpRequest#setConnectionFactory(HttpRequest.ConnectionFactory)} with an instance of
     * this class to enable.
     */
    private static class OkConnectionFactory implements HttpRequest.ConnectionFactory {
        private final OkHttpClient client;

        public OkConnectionFactory() {
            this(new OkHttpClient());
        }

        public OkConnectionFactory(OkHttpClient client) {
            if (client == null) {
                throw new NullPointerException("Client must not be null.");
            }
            this.client = client;
        }

        public HttpURLConnection create(URL url) throws IOException {
            return client.open(url);
        }

        public HttpURLConnection create(URL url, Proxy proxy) throws IOException {
            throw new UnsupportedOperationException(
                    "Per-connection proxy is not supported. Use OkHttpClient's setProxy instead.");
        }
    }



}