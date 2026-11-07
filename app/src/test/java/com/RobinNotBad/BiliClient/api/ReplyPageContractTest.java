package com.RobinNotBad.BiliClient.api;

import com.google.gson.JsonObject;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static org.junit.Assert.*;

public class ReplyPageContractTest {
    private static String reply(long id) {
        return "{\"rpid\":" + id + ",\"content\":{\"message\":\"评论\"}}";
    }

    private static String page(int num, int size, int count, String normal, String top) {
        return "{\"code\":0,\"data\":{\"page\":{\"num\":" + num + ",\"size\":" + size + ",\"count\":" + count
                + "},\"replies\":" + normal + ",\"top_replies\":" + top + "}}";
    }

    @Test public void urlsMatchOrbitPageAndChildContracts() {
        HttpUrl root = HttpUrl.parse(ReplyPageContract.url(170001, 0, 2, 1, 1));
        assertEquals("/x/v2/reply", root.encodedPath());
        assertEquals("2", root.queryParameter("pn"));
        assertEquals("1", root.queryParameter("sort"));
        assertEquals("0", root.queryParameter("root"));
        assertNull(root.queryParameter("pagination_str"));
        assertNull(root.queryParameter("w_rid"));
        HttpUrl child = HttpUrl.parse(ReplyPageContract.url(170001, 9007199254740993L, 3, 17, 1));
        assertEquals("/x/v2/reply/reply", child.encodedPath());
        assertEquals("9007199254740993", child.queryParameter("root"));
        assertEquals("17", child.queryParameter("type"));
        assertNull(child.queryParameter("sort"));
        assertEquals("/x/v2/reply/detail", HttpUrl.parse(ReplyPageContract.detailUrl(170001, 42, 1)).encodedPath());
    }

    @Test public void shortPagesStillUseServerTotalRatherThanThreeItemHeuristic() throws Exception {
        ReplyPageContract.Page first = ReplyPageContract.parse(page(1, 20, 100, "[" + reply(1) + "," + reply(2) + "," + reply(3) + "]", "null"), 1, 0);
        assertEquals(3, first.replies.size());
        assertTrue(first.hasMore);
        assertEquals(100, first.count);
        assertFalse(ReplyPageContract.parse(page(5, 20, 100, "[" + reply(4) + "]", "[]"), 5, 0).hasMore);
    }

    @Test public void pinnedRepliesOnlyAppearOnFirstPageAndAreDeduplicated() throws Exception {
        String tops = "[" + reply(9007199254740993L) + "]";
        String normals = "[" + reply(9007199254740993L) + "," + reply(2) + "," + reply(2) + "]";
        ReplyPageContract.Page first = ReplyPageContract.parse(page(1, 20, 100, normals, tops), 1, 0);
        assertEquals(2, first.replies.size());
        assertEquals(9007199254740993L, first.replies.get(0).get("rpid").getAsLong());
        assertTrue(first.replies.get(0).getAsJsonObject("reply_control").get("is_up_top").getAsBoolean());
        ReplyPageContract.Page second = ReplyPageContract.parse(page(2, 20, 100, "[" + reply(3) + "]", tops), 2, 0);
        assertEquals(1, second.replies.size());
        assertEquals(3, second.replies.get(0).get("rpid").getAsLong());
    }

    @Test public void nullEmptyAndZeroNumberedEmptyPagesTerminateNormally() throws Exception {
        for (String empty : new String[]{"null", "[]"}) {
            assertFalse(ReplyPageContract.parse(page(0, 0, 0, empty, "null"), 2, 0).hasMore);
        }
        ReplyPageContract.Page onlyTop = ReplyPageContract.parse(page(1, 20, 1, "null", "[" + reply(2) + "]"), 1, 0);
        assertEquals(1, onlyTop.replies.size());
        assertFalse(onlyTop.hasMore);
    }

    @Test public void childPageExcludesRootAndTopReplies() throws Exception {
        ReplyPageContract.Page result = ReplyPageContract.parse(page(1, 20, 2, "[" + reply(42) + "," + reply(43) + "]", "[" + reply(44) + "]"), 1, 42);
        assertEquals(1, result.replies.size());
        assertEquals(43, result.replies.get(0).get("rpid").getAsLong());
        assertFalse(result.hasMore);
    }

    @Test public void missingMetadataUsesNonEmptyFallback() throws Exception {
        assertTrue(ReplyPageContract.parse("{\"code\":0,\"data\":{\"replies\":[" + reply(1) + "]}}", 1, 0).hasMore);
        assertFalse(ReplyPageContract.hasMore(1, 0, 0, -1));
        assertTrue(ReplyPageContract.hasMore(1000000000, 1, 20, Long.MAX_VALUE));
    }

    @Test public void errorsAndReplayedNonemptyPageCannotBecomeSuccess() throws Exception {
        for (String json : new String[]{"{}", "null", "bad json", "{code:0}", "{\"code\":\"0\",\"data\":{}}",
                "{\"code\":0,\"data\":null}", "{\"code\":0.5,\"data\":{}}", "{\"code\":0}{}",
                "{\"code\":-101,\"message\":\"请先登录\"}", page(1, 20, 50, "[" + reply(1) + "]", "null")}) {
            try {
                ReplyPageContract.parse(json, 2, 0);
                fail(json);
            } catch (ReplyPageContract.ApiException expected) {
                assertNotNull(expected.getMessage());
                if (json.contains("-101")) assertTrue(expected.getMessage().contains("-101"));
            }
        }
    }

    @Test public void actualHttpRequestCarriesIncreasingPageNumbers() throws Exception {
        try (ServerSocket server = new ServerSocket(0, 2, InetAddress.getByName("127.0.0.1"))) {
            server.setSoTimeout(5000);
            FutureTask<Void> responder = new FutureTask<>(() -> {
                for (int num = 1; num <= 2; num++) {
                    try (Socket socket = server.accept()) {
                        socket.setSoTimeout(5000);
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                        String line = in.readLine();
                        assertTrue(line.contains("pn=" + num));
                        assertTrue(line.startsWith("GET /x/v2/reply?"));
                        while (!(line = in.readLine()).isEmpty()) {}
                        byte[] body = page(num, 1, 2, "[" + reply(num) + "]", "null").getBytes("UTF-8");
                        OutputStream out = socket.getOutputStream();
                        out.write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: " + body.length + "\r\nConnection: close\r\n\r\n").getBytes("UTF-8"));
                        out.write(body);
                        out.flush();
                    }
                }
                return null;
            });
            Thread thread = new Thread(responder);
            thread.setDaemon(true);
            thread.start();
            OkHttpClient client = new OkHttpClient.Builder().proxy(java.net.Proxy.NO_PROXY)
                    .readTimeout(5, TimeUnit.SECONDS).retryOnConnectionFailure(false).build();
            try {
                for (int num = 1; num <= 2; num++) {
                    HttpUrl requestUrl = HttpUrl.parse(ReplyPageContract.url(170001, 0, num, 1, 1)).newBuilder()
                            .scheme("http").host("127.0.0.1").port(server.getLocalPort()).build();
                    try (Response response = client.newCall(new Request.Builder().url(requestUrl).build()).execute()) {
                        ReplyPageContract.Page parsed = ReplyPageContract.parse(response.body().string(), num, 0);
                        assertEquals(num, parsed.replies.get(0).get("rpid").getAsLong());
                        assertEquals(num == 1, parsed.hasMore);
                    }
                }
                responder.get(5, TimeUnit.SECONDS);
            } finally {
                client.connectionPool().evictAll();
                client.dispatcher().executorService().shutdownNow();
            }
        }
    }
}
