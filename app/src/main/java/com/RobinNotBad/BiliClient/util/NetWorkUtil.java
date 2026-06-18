package com.RobinNotBad.BiliClient.util;

import android.annotation.SuppressLint;
import android.os.Build;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.Inflater;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Dns;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.ByteString;

/**
 * 被 luern0313 创建于 2019/10/13.
 * #以下代码来源于腕上哔哩的开源项目，感谢开源者做出的贡献！
 */

public class NetWorkUtil {
    private static final AtomicReference<OkHttpClient> INSTANCE = new AtomicReference<>();

    public static class Inet4Selector implements Dns {
        @NonNull
        @Override
        public List<InetAddress> lookup(@NonNull String hostname) throws UnknownHostException {
            List<InetAddress> hosts = Dns.SYSTEM.lookup(hostname);
            List<InetAddress> inet4Hosts = new ArrayList<>();
            for (InetAddress host : hosts) {
                if (host.getAddress().length == 4) inet4Hosts.add(host);
            }
            return inet4Hosts;    //筛选IPV4地址，IPV6请求有异常
        }
    }

    public static OkHttpClient getOkHttpInstance() {
        while (INSTANCE.get() == null) {
            INSTANCE.compareAndSet(null, setOkHttpSsl(new OkHttpClient.Builder())
                    .followRedirects(false)
                    .addInterceptor(chain -> {
                        Request request = chain.request();
                        Response response = chain.proceed(request);
                        RedirectHandler handler;
                        String location = response.header("Location");
                        boolean isSslRedirect = false;
                        try {
                            isSslRedirect = location != null && !request.isHttps() && new URI(location).getScheme().equalsIgnoreCase("https") && request.url().host().equalsIgnoreCase(new URI(location).getHost());
                        } catch (URISyntaxException ignored) {
                        }

                        if (response.isRedirect() && location != null) {
                            if (request.url().host().equals("b23.tv") && !isSslRedirect && (handler = request.tag(RedirectHandler.class)) != null) {
                                handler.handleRedirect(location);
                            } else {
                                Request newRequest = request.newBuilder()
                                        .url(location)
                                        .build();
                                return chain.proceed(newRequest);
                            }
                        }
                        return response;
                    })
                    .addInterceptor(new CookieSaveInterceptor())
                    .dns(new Inet4Selector())
                    .pingInterval(8, TimeUnit.SECONDS)
                    .connectTimeout(8, TimeUnit.SECONDS)
                    .readTimeout(16, TimeUnit.SECONDS).build());
        }
        return INSTANCE.get();
    }

    public synchronized static OkHttpClient.Builder setOkHttpSsl(OkHttpClient.Builder okhttpBuilder) {
        if (Build.VERSION.SDK_INT > 22) return okhttpBuilder;
        try {
            @SuppressLint("CustomX509TrustManager") final X509TrustManager trustAllCert =
                    new X509TrustManager() {
                        @SuppressLint("TrustAllX509TrustManager")
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @SuppressLint("TrustAllX509TrustManager")
                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    };
            final SSLSocketFactory sslSocketFactory = new SSLSocketFactoryCompat(trustAllCert);
            okhttpBuilder.sslSocketFactory(sslSocketFactory, trustAllCert);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return okhttpBuilder;
    }

    public static JSONObject getJson(String url) throws IOException, JSONException {
        String bodyString = getBodyStringWithDoctypeRetry(url, webHeaders);
        if (bodyString != null) return new JSONObject(bodyString);
        throw new JSONException("在访问" + url + "时返回数据为空");
    }

    public static JSONObject getJson(String url, ArrayList<String> headers) throws IOException, JSONException {
        String bodyString = getBodyStringWithDoctypeRetry(url, headers);
        if (bodyString != null) return new JSONObject(bodyString);
        throw new JSONException("在访问" + url + "时返回数据为空");
    }

    public static Response get(String url) throws IOException {
        return get(url, webHeaders);
    }

    public static Response get(String url, ArrayList<String> headers) throws IOException {
        return get(url, headers, null);
    }

    public static Response get(String url, ArrayList<String> headers, RedirectHandler redirectHandler) throws IOException {
        Logu.d("get-url", url);
        OkHttpClient client = getOkHttpInstance();
        Request.Builder requestBuilder = new Request.Builder().url(url).get();
        for (int i = 0; i < headers.size(); i += 2)
            requestBuilder.addHeader(headers.get(i), headers.get(i + 1));
        if (redirectHandler != null) requestBuilder.tag(RedirectHandler.class, redirectHandler);
        Request request = requestBuilder.build();
        return executeWithDoctypeRetry(client, request);
    }

    public static Response post(String url, String data, List<String> headers, String contentType) throws IOException {
        Logu.d("post-url", url);
        Logu.d("post-data", data);
        OkHttpClient client = getOkHttpInstance();
        RequestBody body = RequestBody.create(MediaType.parse(contentType + "; charset=utf-8"), data);
        Request.Builder requestBuilder = new Request.Builder().url(url).post(body);
        for (int i = 0; i < headers.size(); i += 2) {
            String key = headers.get(i);
            String val = headers.get(i + 1);
            if (key.equalsIgnoreCase("Content-Type")) val = contentType;
            requestBuilder.addHeader(key, val);
        }
        Request request = requestBuilder.build();
        return executeWithDoctypeRetry(client, request);
    }

    public static Response post(String url, String data, List<String> headers) throws IOException {
        return post(url, data, headers, "application/x-www-form-urlencoded");
    }

    public static Response postJson(String url, String data, List<String> headers) throws IOException {
        return post(url, data, headers, "application/json");
    }

    public static Response postJson(String url, String data) throws IOException {
        return post(url, data, webHeaders, "application/json");
    }

    public static Response post(String url, String data) throws IOException {
        return post(url, data, webHeaders);
    }

    /**
     * 上传文件（multipart/form-data）
     *
     * @param url      上传地址
     * @param fileName 文件名
     * @param mimeType MIME类型
     * @param fileData 文件二进制数据
     * @param params   额外表单参数
     * @return Response
     */
    public static Response uploadFile(String url, String fileName, String mimeType, byte[] fileData, Map<String, String> params) throws IOException {
        Logu.d("upload-url", url);
        OkHttpClient client = getOkHttpInstance();

        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        MediaType mediaType = MediaType.parse(mimeType);

        RequestBody requestBody = new RequestBody() {
            @Override
            public MediaType contentType() {
                return MediaType.parse("multipart/form-data; boundary=" + boundary);
            }

            @Override
            public long contentLength() throws IOException {
                return -1;
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                // 文件部分
                sink.writeUtf8("--").writeUtf8(boundary).writeUtf8("\r\n");
                sink.writeUtf8("Content-Disposition: form-data; name=\"file_up\"; filename=\"").writeUtf8(fileName).writeUtf8("\"\r\n");
                sink.writeUtf8("Content-Type: ").writeUtf8(mimeType).writeUtf8("\r\n\r\n");
                sink.write(ByteString.of(fileData));
                sink.writeUtf8("\r\n");

                // 其他参数
                if (params != null) {
                    for (Map.Entry<String, String> entry : params.entrySet()) {
                        sink.writeUtf8("--").writeUtf8(boundary).writeUtf8("\r\n");
                        sink.writeUtf8("Content-Disposition: form-data; name=\"").writeUtf8(entry.getKey()).writeUtf8("\"\r\n\r\n");
                        sink.writeUtf8(entry.getValue()).writeUtf8("\r\n");
                    }
                }

                // 结束标记
                sink.writeUtf8("--").writeUtf8(boundary).writeUtf8("--\r\n");
                sink.flush();
            }
        };

        Request.Builder requestBuilder = new Request.Builder().url(url).post(requestBody);
        for (int i = 0; i < webHeaders.size(); i += 2) {
            String key = webHeaders.get(i);
            String val = webHeaders.get(i + 1);
            if (!key.equalsIgnoreCase("Content-Type")) {
                requestBuilder.addHeader(key, val);
            }
        }
        Request request = requestBuilder.build();
        return executeWithDoctypeRetry(client, request);
    }

    private static Response executeWithDoctypeRetry(OkHttpClient client, Request request) throws IOException {
        int maxTimes = Math.max(1, SharedPreferencesUtil.getInt(SharedPreferencesUtil.API_RETRY_MAX_TIMES, 5));
        float intervalSeconds = SharedPreferencesUtil.getFloat(SharedPreferencesUtil.API_RETRY_INTERVAL_SECONDS, 0.1f);
        long intervalMillis = Math.max(0L, (long) (intervalSeconds * 1000));

        IOException latestException = null;
        Response latestResponse = null;

        for (int attempt = 1; attempt <= maxTimes; attempt++) {
            if (latestResponse != null) {
                latestResponse.close();
                latestResponse = null;
            }

            try {
                latestResponse = client.newCall(request).execute();
                if (!isDoctypeResponse(latestResponse)) {
                    return latestResponse;
                }
            } catch (IOException e) {
                latestException = e;
            }

            if (attempt < maxTimes && intervalMillis > 0) {
                try {
                    Thread.sleep(intervalMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (latestResponse != null) return latestResponse;
        if (latestException != null) throw latestException;
        throw new IOException("请求失败");
    }

    private static String getBodyStringWithDoctypeRetry(String url, ArrayList<String> headers) throws IOException {
        int maxTimes = Math.max(1, SharedPreferencesUtil.getInt(SharedPreferencesUtil.API_RETRY_MAX_TIMES, 5));
        float intervalSeconds = SharedPreferencesUtil.getFloat(SharedPreferencesUtil.API_RETRY_INTERVAL_SECONDS, 0.1f);
        long intervalMillis = Math.max(0L, (long) (intervalSeconds * 1000));

        String latestBodyString = null;
        for (int attempt = 1; attempt <= maxTimes; attempt++) {
            try (ResponseBody body = get(url, headers).body()) {
                if (body == null) {
                    latestBodyString = null;
                    continue;
                }
                latestBodyString = body.string();
            }

            if (!isDoctypeResponse(latestBodyString)) {
                return latestBodyString;
            }

            if (attempt < maxTimes && intervalMillis > 0) {
                try {
                    Thread.sleep(intervalMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return latestBodyString;
    }

    private static boolean isDoctypeResponse(String responseBody) {
        if (responseBody == null) return false;
        return responseBody.trim().toLowerCase(Locale.ROOT).startsWith("<!doctype");
    }

    private static boolean isDoctypeResponse(Response response) {
        if (response == null || response.body() == null) return false;
        try {
            ResponseBody peekBody = response.peekBody(128);
            return isDoctypeResponse(peekBody.string());
        } catch (Exception ignored) {
            return false;
        }
    }


    public static byte[] readStream(InputStream inStream) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        outStream.close();
        inStream.close();
        return outStream.toByteArray();
    }

    public static byte[] uncompress(byte[] inputByte) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(inputByte.length);
        try {
            Inflater inflater = new Inflater(true);
            inflater.setInput(inputByte);
            byte[] buffer = new byte[4 * 1024];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] output = outputStream.toByteArray();
        outputStream.close();
        return output;
    }

    public static String getInfoFromCookie(String name, String cookie) {
        String[] cookies = cookie.split("; ");
        for (String i : cookies) {
            if (i.contains(name + "="))
                return i.substring(name.length() + 1);
        }
        return "";
    }

    private static void saveCookiesFromResponse(Response response) {
        List<String> newCookies = response.headers("Set-Cookie");

        //如果没有新cookies，直接返回
        if (newCookies.isEmpty()) return;
        String cookiesStr = SharedPreferencesUtil.getString(SharedPreferencesUtil.cookies, "");
        ArrayList<String> oldCookies = (cookiesStr.equals("") ? new ArrayList<>() : new ArrayList<>(Arrays.asList(cookiesStr.split("; "))));  //转list

        for (String newCookie : newCookies) {  //对每一条新cookie遍历

            Cookies cookies = new Cookies(newCookie);
            if (cookies.containsKey("Domain") && !cookies.get("Domain").endsWith("bilibili.com"))
                continue;

            int index = newCookie.indexOf("; ");
            if (index != -1) newCookie = newCookie.substring(0, index);  //如果没有分号不做处理

            index = newCookie.indexOf("=") + 1;
            if (index == 0) continue;   //如果没有等号，跳过

            String key = newCookie.substring(0, index);    //key=
            Logu.d("newCookie", newCookie);

            boolean added = false;
            for (int i = 0; i < oldCookies.size(); i++) {  //查找旧cookie表有没有
                String oldCookie = oldCookies.get(i);
                if (oldCookie.contains(key)) {
                    oldCookies.set(i, newCookie);    //有的话直接换掉
                    added = true;
                    break;
                }
            }
            if (!added) {
                oldCookies.add(newCookie);  //没有就加项
            }
        }

        StringBuilder setCookies = new StringBuilder();
        for (String setCookie : oldCookies) {
            setCookies.append(setCookie).append("; ");
        }
        //如果一次setCookies都没有，就不要存了， 因为是个空字符串
        if (setCookies.length() >= 2) {
            Logu.d("save-result", setCookies.substring(0, setCookies.length() - 2));
            SharedPreferencesUtil.putString(SharedPreferencesUtil.cookies, setCookies.substring(0, setCookies.length() - 2));
            refreshHeaders();
        }
    }

    /**
     * 存储单个Cookie
     *
     * @param key 键
     * @param val 值
     */
    public static void putCookie(String key, String val) {
        synchronized (NetWorkUtil.class) {
            Cookies cookies = new Cookies(SharedPreferencesUtil.getString(SharedPreferencesUtil.cookies, ""));
            cookies.set(key, val);
            SharedPreferencesUtil.putString(SharedPreferencesUtil.cookies, cookies.toString());
            refreshHeaders();
        }
    }

    /**
     * 存储Cookies（覆盖写入）
     *
     * @param cookies cookies
     */
    public static void setCookies(Cookies cookies) {
        synchronized (NetWorkUtil.class) {
            SharedPreferencesUtil.putString(SharedPreferencesUtil.cookies, cookies.toString());
            refreshHeaders();
        }
    }

    /**
     * 获取存储的Cookies
     *
     * @return 存储的Cookies
     */
    public static Cookies getCookies() {
        synchronized (NetWorkUtil.class) {
            return new Cookies(SharedPreferencesUtil.getString(SharedPreferencesUtil.cookies, ""));
        }
    }

    public static final String USER_AGENT_WEB = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.95 Safari/537.36";
    public static final ArrayList<String> webHeaders = new ArrayList<>() {{
        add("Cookie");
        add(SharedPreferencesUtil.getString(SharedPreferencesUtil.cookies, ""));

        add("Origin");
        add("https://www.bilibili.com");

        add("Referer");
        add("https://www.bilibili.com/");

        add("User-Agent");
        add(USER_AGENT_WEB);

        add("Sec-Ch-Ua");
        add("\"Chromium\";v=\"122\", \"Not(A:Brand\";v=\"24\", \"Google Chrome\";v=\"122\"");

        add("Sec-Ch-Ua-Platform");
        add("\"Windows\"");

        add("Sec-Ch-Ua-Mobile");
        add("?0");
    }};

    public static void refreshHeaders() {
        webHeaders.set(1, SharedPreferencesUtil.getString(SharedPreferencesUtil.cookies, ""));
    }

    public static class FormData {
        private final Map<String, String> data;
        private boolean isUrlParam;

        public FormData() {
            data = new HashMap<>();
        }

        public FormData remove(String key) {
            data.remove(key);
            return this;
        }

        public FormData put(String key, Object value) {
            data.put(key, String.valueOf(value));
            return this;
        }

        public FormData setUrlParam(boolean isUrlParam) {
            this.isUrlParam = isUrlParam;
            return this;
        }

        @NonNull
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            if (isUrlParam) sb.append("?");

            try {
                for (String key : data.keySet()) {
                    if (sb.length() > (isUrlParam ? 1 : 0)) {
                        sb.append("&");
                    }
                    sb.append(URLEncoder.encode(key, "UTF-8"));
                    sb.append("=");
                    sb.append(URLEncoder.encode(data.get(key), "UTF-8"));
                }
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }

            return sb.toString();
        }
    }

    public interface RedirectHandler {
        void handleRedirect(String location);
    }

    private static class CookieSaveInterceptor implements Interceptor {
        @NonNull
        @Override
        public Response intercept(Chain chain) throws IOException {
            Response response = chain.proceed(chain.request());
            saveCookiesFromResponse(response);
            return response;
        }
    }

}
