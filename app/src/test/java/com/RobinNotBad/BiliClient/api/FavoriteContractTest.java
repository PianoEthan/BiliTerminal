package com.RobinNotBad.BiliClient.api;

import com.RobinNotBad.BiliClient.model.FavoriteFolder;
import com.RobinNotBad.BiliClient.model.VideoCard;
import com.google.gson.Gson;

import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static org.junit.Assert.*;

public class FavoriteContractTest {
    private static final long MEDIA_ID = 9007199254740993L;
    private static final String FOLDER = "{\"id\":9007199254740993,\"title\":\"默认收藏\",\"media_count\":7,\"attr\":1,\"intro\":\"简介\"}";

    private interface Parse { void run() throws IOException; }

    private static void fails(Parse parse) throws Exception {
        try {
            parse.run();
            fail("Expected IOException");
        } catch (IOException expected) {
            assertTrue(expected instanceof FavoriteContract.ApiException);
            assertNotNull(expected.getMessage());
        }
    }

    private static String response(String data) {
        return "{\"code\":0,\"message\":\"0\",\"data\":" + data + "}";
    }

    private static Map<String, String> decode(String body) throws Exception {
        Map<String, String> fields = new HashMap<>();
        for (String field : body.split("&", -1)) {
            String[] pair = field.split("=", 2);
            fields.put(URLDecoder.decode(pair[0], "UTF-8"), URLDecoder.decode(pair[1], "UTF-8"));
        }
        return fields;
    }

    @Test public void folderV3FieldsAndLongIds() throws Exception {
        FavoriteContract.FolderPage page = FavoriteContract.parseFolders(response("{\"list\":[" + FOLDER + "],\"has_more\":true}"));
        assertTrue(page.hasMore);
        assertEquals(1, page.folders.size());
        FavoriteFolder folder = page.folders.get(0);
        assertEquals(MEDIA_ID, folder.id);
        assertEquals(MEDIA_ID, folder.mediaId);
        assertEquals("默认收藏", folder.name);
        assertEquals("", folder.cover);
        assertEquals(7, folder.videoCount);
        assertEquals(0, folder.maxCount);
        assertTrue(folder.isDefault);
        assertTrue(folder.isPrivate);
        assertEquals("简介", folder.intro);
        assertEquals(MEDIA_ID, FavoriteContract.parseFolderInfo(response(FOLDER)).mediaId);
        FavoriteFolder other = FavoriteContract.parseFolderInfo(response(FOLDER.replace("\"attr\":1", "\"attr\":2")));
        assertFalse(other.isDefault);
        assertFalse(other.isPrivate);
        FavoriteContract.FolderPage empty = FavoriteContract.parseFolders(response("{\"list\":[],\"has_more\":false}"));
        assertTrue(empty.folders.isEmpty());
        assertFalse(empty.hasMore);
    }

    @Test public void videosUseV3NamesAndServerHasMore() throws Exception {
        String media = "{\"id\":9007199254740993,\"title\":\"视频\",\"cover\":\"cover\",\"upper\":{\"name\":\"作者\"},\"cnt_info\":{\"play\":12},\"bv_id\":\"BV1test\"}";
        FavoriteContract.VideoPage page = FavoriteContract.parseVideos(response("{\"medias\":[" + media + "],\"has_more\":true}"));
        assertTrue(page.hasMore);
        assertEquals(1, page.videos.size());
        VideoCard card = page.videos.get(0);
        assertEquals(MEDIA_ID, card.aid);
        assertEquals("BV1test", card.bvid);
        assertEquals("视频", card.title);
        assertEquals("作者", card.upName);
        assertEquals("12观看", card.view);
        assertEquals("cover", card.cover);
        assertFalse(FavoriteContract.parseVideos(response("{\"medias\":[" + media + "],\"has_more\":false}")).hasMore);
    }

    @Test public void emptyAndNullMediasAlwaysEndPagination() throws Exception {
        for (String medias : new String[]{"[]", "null"}) {
            for (boolean more : new boolean[]{true, false}) {
                FavoriteContract.VideoPage page = FavoriteContract.parseVideos(response("{\"medias\":" + medias + ",\"has_more\":" + more + "}"));
                assertTrue(page.videos.isEmpty());
                assertFalse(page.hasMore);
            }
        }
    }

    @Test public void readErrorsNeverBecomeEmptySuccess() throws Exception {
        String[] invalid = {"", "null", "[]", "not json", "{code:0}", "{\"code\":0,}",
                "{\"data\":{}}", "{\"code\":null,\"data\":{}}", "{\"code\":\"0\",\"data\":{}}",
                "{\"code\":0.5,\"data\":{}}", "{\"code\":0} trailing", "{\"code\":-101,\"message\":\"请登录\"}",
                response("null"), response("[]"), response("{}"), response("{\"has_more\":false}"),
                response("{\"medias\":{},\"has_more\":false}"), response("{\"medias\":[null],\"has_more\":false}")};
        for (String json : invalid) {
            fails(() -> FavoriteContract.parseFolders(json));
            fails(() -> FavoriteContract.parseFolderInfo(json));
            fails(() -> FavoriteContract.parseVideos(json));
            fails(() -> FavoriteContract.parseState(json));
        }
        try {
            FavoriteContract.parseVideos("{\"code\":-101,\"message\":\"请登录\",\"data\":null}");
            fail();
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("-101"));
            assertTrue(e.getMessage().contains("请登录"));
        }
    }

    @Test public void stateUsesIdNotFidAndRejectsPartialResults() throws Exception {
        String item = "{\"id\":9007199254740993,\"fid\":12,\"title\":\"夹\",\"fav_state\":1}";
        FavoriteContract.StatePage state = FavoriteContract.parseState(response("{\"list\":[" + item + "]}"));
        assertEquals(Long.valueOf(MEDIA_ID), state.mediaIds.get(0));
        assertEquals("夹", state.titles.get(0));
        assertTrue(state.states.get(0));
        fails(() -> FavoriteContract.parseState(response("{\"list\":[{\"fid\":12,\"fav_state\":0}]}")));
        fails(() -> FavoriteContract.parseState(response("{\"list\":[" + item + ",{\"id\":0,\"fav_state\":0}]}")));
        assertTrue(FavoriteContract.parseState(response("{\"list\":[]}")).mediaIds.isEmpty());
    }

    @Test public void writeCodeMustExistAndBeAnInteger() throws Exception {
        assertEquals(0, FavoriteContract.parseWriteCode("{\"code\":0}"));
        assertEquals(-403, FavoriteContract.parseWriteCode("{\"code\":-403,\"message\":\"拒绝\"}"));
        for (String json : new String[]{"{}", "null", "garbage", "{\"code\":null}", "{\"code\":\"0\"}", "{\"code\":0.2}", "{\"code\":2147483648}", "{\"code\":0}{}"}) {
            fails(() -> FavoriteContract.parseWriteCode(json));
        }
    }

    @Test public void formsKeepRealMediaIdAndEncodeCsrf() throws Exception {
        String csrf = "synthetic +&=令牌";
        String encoded = FavoriteContract.encodeForm(FavoriteContract.addFavoriteForm(42, MEDIA_ID, csrf));
        Map<String, String> add = decode(encoded);
        assertEquals("42", add.get("rid"));
        assertEquals("2", add.get("type"));
        assertEquals(String.valueOf(MEDIA_ID), add.get("add_media_ids"));
        assertEquals("", add.get("del_media_ids"));
        assertEquals(csrf, add.get("csrf"));
        assertFalse(encoded.contains(csrf));
        Map<String, String> delete = decode(FavoriteContract.encodeForm(FavoriteContract.deleteFavoriteForm(42, MEDIA_ID, csrf)));
        assertEquals("42:2", delete.get("resources"));
        assertEquals(String.valueOf(MEDIA_ID), delete.get("media_id"));
        assertEquals(csrf, delete.get("csrf"));
    }

    @Test public void editRetainsCoverPrivacyAndIntro() throws Exception {
        String cover = "https://example.invalid/a?x=1&y=2";
        Map<String, String> edit = decode(FavoriteContract.encodeForm(FavoriteContract.editFolderForm(MEDIA_ID, "夹 +&", "简介=保留", 1, cover, "fake")));
        assertEquals(String.valueOf(MEDIA_ID), edit.get("media_id"));
        assertEquals("夹 +&", edit.get("title"));
        assertEquals("简介=保留", edit.get("intro"));
        assertEquals("1", edit.get("privacy"));
        assertEquals(cover, edit.get("cover"));
        assertEquals("fake", edit.get("csrf"));
        Map<String, String> add = decode(FavoriteContract.encodeForm(FavoriteContract.addFolderForm("夹", null, 0, "fake")));
        assertEquals("", add.get("intro"));
        assertEquals("0", add.get("privacy"));
        Map<String, String> delete = decode(FavoriteContract.encodeForm(FavoriteContract.deleteFolderForm(MEDIA_ID, "fake")));
        assertEquals(String.valueOf(MEDIA_ID), delete.get("media_ids"));
        assertEquals("fake", delete.get("csrf"));
    }

    @Test public void urlsUseRealIdsAndV3Pagination() {
        HttpUrl folders = HttpUrl.parse(FavoriteContract.foldersUrl(123, 2));
        assertEquals("/x/v3/fav/folder/created/list", folders.encodedPath());
        assertEquals("123", folders.queryParameter("up_mid"));
        assertEquals("2", folders.queryParameter("pn"));
        assertEquals("50", folders.queryParameter("ps"));
        HttpUrl videos = HttpUrl.parse(FavoriteContract.videosUrl(MEDIA_ID, 3));
        assertEquals("/x/v3/fav/resource/list", videos.encodedPath());
        assertEquals(String.valueOf(MEDIA_ID), videos.queryParameter("media_id"));
        assertEquals("3", videos.queryParameter("pn"));
        assertEquals("30", videos.queryParameter("ps"));
        assertEquals("web", videos.queryParameter("platform"));
        assertEquals(String.valueOf(MEDIA_ID), HttpUrl.parse(FavoriteContract.folderInfoUrl(MEDIA_ID)).queryParameter("media_id"));
        assertEquals("/x/v3/fav/resource/deal", HttpUrl.parse(FavoriteContract.addFavoriteUrl()).encodedPath());
        assertEquals("/medialist/gateway/coll/resource/batch/del", HttpUrl.parse(FavoriteContract.deleteFavoriteUrl()).encodedPath());
        assertEquals("/x/v3/fav/folder/add", HttpUrl.parse(FavoriteContract.addFolderUrl()).encodedPath());
        assertEquals("/x/v3/fav/folder/edit", HttpUrl.parse(FavoriteContract.editFolderUrl()).encodedPath());
        assertEquals("/x/v3/fav/folder/del", HttpUrl.parse(FavoriteContract.deleteFolderUrl()).encodedPath());
        HttpUrl state = HttpUrl.parse(FavoriteContract.stateUrl(42, 123));
        assertEquals("42", state.queryParameter("rid"));
        assertEquals("123", state.queryParameter("up_mid"));
    }

    private interface WireAssertion {
        void check(String requestLine, Map<String, String> headers, String body) throws Exception;
    }

    private static String readLine(InputStream input) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        int value;
        while ((value = input.read()) != -1) {
            if (value == '\n') return new String(line.toByteArray(), StandardCharsets.US_ASCII).replace("\r", "");
            line.write(value);
            if (line.size() > 16384) throw new IOException("Oversized mock request line");
        }
        throw new IOException("Unexpected end of mock request");
    }

    private static String exchange(String contractUrl, FormBody form, String json, WireAssertion assertion) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        OkHttpClient client = new OkHttpClient.Builder().proxy(Proxy.NO_PROXY)
                .followRedirects(false).retryOnConnectionFailure(false)
                .connectTimeout(5, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS).build();
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            server.setSoTimeout(5000);
            Future<Void> served = executor.submit(() -> {
                try (Socket socket = server.accept()) {
                    socket.setSoTimeout(5000);
                    InputStream input = new BufferedInputStream(socket.getInputStream());
                    String requestLine = readLine(input);
                    Map<String, String> headers = new HashMap<>();
                    String header;
                    while (!(header = readLine(input)).isEmpty()) {
                        int colon = header.indexOf(':');
                        if (colon < 0) throw new IOException("Invalid mock request header");
                        headers.put(header.substring(0, colon).toLowerCase(java.util.Locale.ROOT), header.substring(colon + 1).trim());
                    }
                    int length = Integer.parseInt(headers.getOrDefault("content-length", "0"));
                    if (length < 0 || length > 65536) throw new IOException("Invalid mock request length");
                    byte[] body = new byte[length];
                    for (int offset = 0; offset < length;) {
                        int count = input.read(body, offset, length - offset);
                        if (count < 0) throw new IOException("Incomplete mock request body");
                        offset += count;
                    }
                    byte[] response = json.getBytes(StandardCharsets.UTF_8);
                    socket.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: "
                            + response.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                    socket.getOutputStream().write(response);
                    socket.getOutputStream().flush();
                    assertion.check(requestLine, headers, new String(body, StandardCharsets.UTF_8));
                }
                return null;
            });
            HttpUrl localUrl = HttpUrl.parse(contractUrl).newBuilder().scheme("http")
                    .host("127.0.0.1").port(server.getLocalPort()).build();
            Request.Builder request = new Request.Builder().url(localUrl);
            if (form != null) request.post(form);
            String result;
            try (Response response = client.newCall(request.build()).execute()) {
                assertEquals(200, response.code());
                assertNotNull(response.body());
                result = response.body().string();
            }
            served.get(5, TimeUnit.SECONDS);
            return result;
        } finally {
            executor.shutdownNow();
            client.connectionPool().evictAll();
            client.dispatcher().executorService().shutdownNow();
        }
    }

    @Test public void localHttpReadTransmitsV3QueryAndParsesResponse() throws Exception {
        String media = "{\"id\":9007199254740993,\"title\":\"本地视频\",\"cover\":\"cover\",\"upper\":{\"name\":\"作者\"},\"cnt_info\":{\"play\":23},\"bv_id\":\"BVlocal\"}";
        String json = exchange(FavoriteContract.videosUrl(MEDIA_ID, 2), null,
                response("{\"medias\":[" + media + "],\"has_more\":true}"), (line, headers, body) -> {
                    assertEquals("GET /x/v3/fav/resource/list?media_id=" + MEDIA_ID + "&pn=2&ps=30&platform=web HTTP/1.1", line);
                    assertEquals("", body);
                });
        FavoriteContract.VideoPage page = FavoriteContract.parseVideos(json);
        assertTrue(page.hasMore);
        VideoCard video = page.videos.get(0);
        assertEquals(MEDIA_ID, video.aid);
        assertEquals("BVlocal", video.bvid);
        assertEquals("本地视频", video.title);
        assertEquals("作者", video.upName);
        assertEquals("23观看", video.view);
    }

    @Test public void localHttpAddSendsRealIdAndEncodedCsrf() throws Exception {
        String csrf = "synthetic +&=令牌";
        String json = exchange(FavoriteContract.addFavoriteUrl(), FavoriteContract.addFavoriteForm(42, MEDIA_ID, csrf),
                "{\"code\":0}", (line, headers, body) -> {
                    assertEquals("POST /x/v3/fav/resource/deal HTTP/1.1", line);
                    assertTrue(headers.get("content-type").startsWith("application/x-www-form-urlencoded"));
                    Map<String, String> fields = decode(body);
                    assertEquals(String.valueOf(MEDIA_ID), fields.get("add_media_ids"));
                    assertEquals("42", fields.get("rid"));
                    assertEquals("2", fields.get("type"));
                    assertEquals("", fields.get("del_media_ids"));
                    assertEquals(csrf, fields.get("csrf"));
                    assertTrue(body.contains("%2B%26%3D"));
                    assertTrue(body.contains("%E4%BB%A4%E7%89%8C"));
                    assertFalse(body.contains(csrf));
                });
        assertEquals(0, FavoriteContract.parseWriteCode(json));
    }

    @Test public void localHttpDeletePreservesIdAndReturnsBusinessCode() throws Exception {
        String json = exchange(FavoriteContract.deleteFavoriteUrl(), FavoriteContract.deleteFavoriteForm(42, MEDIA_ID, "fake +&"),
                "{\"code\":-403,\"message\":\"拒绝\"}", (line, headers, body) -> {
                    assertEquals("POST /medialist/gateway/coll/resource/batch/del HTTP/1.1", line);
                    Map<String, String> fields = decode(body);
                    assertEquals(String.valueOf(MEDIA_ID), fields.get("media_id"));
                    assertEquals("42:2", fields.get("resources"));
                    assertEquals("fake +&", fields.get("csrf"));
                    assertTrue(body.contains("resources=42%3A2"));
                });
        assertEquals(-403, FavoriteContract.parseWriteCode(json));
    }

    @Test public void localHttpBusinessErrorIsNotAnEmptyPage() throws Exception {
        String json = exchange(FavoriteContract.folderInfoUrl(MEDIA_ID), null,
                "{\"code\":-101,\"message\":\"请登录\",\"data\":null}", (line, headers, body) ->
                        assertEquals("GET /x/v3/fav/folder/info?media_id=" + MEDIA_ID + " HTTP/1.1", line));
        try {
            FavoriteContract.parseFolderInfo(json);
            fail("Expected business error");
        } catch (FavoriteContract.ApiException expected) {
            assertTrue(expected.getMessage().contains("-101"));
            assertTrue(expected.getMessage().contains("请登录"));
        }
    }

    @Test public void invalidResponseMessageClearlyDescribesDataFailure() throws Exception {
        try {
            FavoriteContract.parseWriteCode("{}");
            fail("Expected invalid response");
        } catch (FavoriteContract.ApiException expected) {
            assertTrue(expected.getMessage().contains("收藏接口返回数据异常"));
            assertFalse(expected.getMessage().contains("code=0"));
        }
    }

    @Test public void favoriteFolderGsonHasNoConflictingFields() {
        Gson gson = new Gson();
        FavoriteFolder folder = gson.fromJson(FOLDER, FavoriteFolder.class);
        assertEquals(MEDIA_ID, folder.id);
        assertEquals("默认收藏", folder.name);
        assertEquals(7, folder.videoCount);
        folder.mediaId = MEDIA_ID;
        FavoriteFolder copy = gson.fromJson(gson.toJson(folder), FavoriteFolder.class);
        assertEquals(MEDIA_ID, copy.mediaId);
        assertEquals(MEDIA_ID, copy.id);
    }
}
