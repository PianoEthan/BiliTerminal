package com.RobinNotBad.BiliClient.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import okhttp3.HttpUrl;

public final class ReplyPageContract {
    private static final Gson GSON = new Gson();

    private ReplyPageContract() {}

    public static final class ApiException extends IOException {
        public ApiException(String message) {
            super(message);
        }
    }

    public static final class Page {
        public final ArrayList<JsonObject> replies;
        public final boolean hasMore;
        public final long count;
        public final long upMid;

        private Page(ArrayList<JsonObject> replies, boolean hasMore, long count, long upMid) {
            this.replies = replies;
            this.hasMore = hasMore;
            this.count = count;
            this.upMid = upMid;
        }
    }

    public static String url(long oid, long root, int page, int type, int sort) {
        if (oid <= 0 || root < 0 || page < 1) throw new IllegalArgumentException("评论请求参数无效");
        HttpUrl.Builder builder = HttpUrl.parse("https://api.bilibili.com/x/v2/reply" + (root > 0 ? "/reply" : "")).newBuilder()
                .addQueryParameter("oid", String.valueOf(oid))
                .addQueryParameter("type", String.valueOf(type))
                .addQueryParameter("pn", String.valueOf(page))
                .addQueryParameter("root", String.valueOf(root));
        if (root == 0) builder.addQueryParameter("sort", String.valueOf(sort));
        return builder.build().toString();
    }

    public static String detailUrl(long oid, long root, int type) {
        return HttpUrl.parse("https://api.bilibili.com/x/v2/reply/detail").newBuilder()
                .addQueryParameter("oid", String.valueOf(oid))
                .addQueryParameter("type", String.valueOf(type))
                .addQueryParameter("root", String.valueOf(root))
                .addQueryParameter("ps", "20").addQueryParameter("next", "0").build().toString();
    }

    static JsonObject data(String json) throws IOException {
        try (JsonReader reader = new JsonReader(new StringReader(json == null ? "" : json))) {
            reader.setLenient(false);
            JsonElement element = GSON.getAdapter(JsonElement.class).read(reader);
            if (element == null || !element.isJsonObject() || reader.peek() != JsonToken.END_DOCUMENT) throw invalid();
            JsonObject envelope = element.getAsJsonObject();
            JsonElement code = envelope.get("code");
            if (code == null || !code.isJsonPrimitive() || !code.getAsJsonPrimitive().isNumber()) throw invalid();
            int result = new java.math.BigDecimal(code.getAsString()).intValueExact();
            if (result != 0) {
                JsonElement message = envelope.get("message");
                throw new ApiException("加载评论失败（" + result + "）："
                        + (message != null && message.isJsonPrimitive() ? message.getAsString() : "接口请求失败"));
            }
            JsonElement value = envelope.get("data");
            if (value == null || !value.isJsonObject()) throw invalid();
            return value.getAsJsonObject();
        } catch (ApiException e) {
            throw e;
        } catch (RuntimeException | IOException e) {
            throw invalid();
        }
    }

    private static ApiException invalid() {
        return new ApiException("评论接口返回数据异常，请稍后重试");
    }

    private static JsonArray array(JsonObject data, String key) throws IOException {
        JsonElement value = data.get(key);
        if (value == null || value.isJsonNull()) return new JsonArray();
        if (!value.isJsonArray()) throw invalid();
        return value.getAsJsonArray();
    }

    private static long number(JsonObject data, String key, long fallback) throws IOException {
        JsonElement value = data.get(key);
        if (value == null || value.isJsonNull()) return fallback;
        try {
            if (!value.isJsonPrimitive()) throw invalid();
            return new java.math.BigDecimal(value.getAsString()).longValueExact();
        } catch (RuntimeException e) {
            throw invalid();
        }
    }

    public static boolean hasMore(int page, int received, int pageSize, long count) {
        if (received == 0) return false;
        return pageSize <= 0 || count < 0 || (long) page * pageSize < count;
    }

    public static Page parse(String json, int requestedPage, long rootId) throws IOException {
        JsonObject data = data(json);
        JsonArray normal = array(data, "replies");
        LinkedHashMap<Long, JsonObject> unique = new LinkedHashMap<>();
        if (rootId == 0 && requestedPage == 1) {
            for (JsonElement item : array(data, "top_replies")) add(unique, item, true, rootId);
        }
        for (JsonElement item : normal) add(unique, item, false, rootId);
        int size = 0;
        long count = -1;
        JsonElement pageValue = data.get("page");
        if (pageValue != null && !pageValue.isJsonNull()) {
            if (!pageValue.isJsonObject()) throw invalid();
            JsonObject page = pageValue.getAsJsonObject();
            long returned = number(page, "num", requestedPage);
            if (normal.size() > 0 && returned != requestedPage) throw new ApiException("评论分页未前进，请稍后重试");
            size = (int) number(page, "size", 0);
            count = number(page, "count", -1);
        }
        long upMid = 0;
        JsonElement upper = data.get("upper");
        if (upper != null && upper.isJsonObject()) upMid = number(upper.getAsJsonObject(), "mid", 0);
        return new Page(new ArrayList<>(unique.values()), hasMore(requestedPage, normal.size(), size, count), count, upMid);
    }

    private static void add(LinkedHashMap<Long, JsonObject> unique, JsonElement value, boolean pinned, long rootId) throws IOException {
        if (value == null || value.isJsonNull()) return;
        if (!value.isJsonObject()) throw invalid();
        JsonObject item = value.getAsJsonObject();
        long id = number(item, "rpid", 0);
        if (id <= 0) throw invalid();
        if (id == rootId || unique.containsKey(id)) return;
        JsonObject copy = item.deepCopy();
        if (pinned) {
            JsonElement control = copy.get("reply_control");
            JsonObject flags = control != null && control.isJsonObject() ? control.getAsJsonObject() : new JsonObject();
            flags.addProperty("is_up_top", true);
            copy.add("reply_control", flags);
        }
        unique.put(id, copy);
    }
}
