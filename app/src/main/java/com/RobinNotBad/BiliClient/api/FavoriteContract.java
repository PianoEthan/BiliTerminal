package com.RobinNotBad.BiliClient.api;

import com.RobinNotBad.BiliClient.model.FavoriteFolder;
import com.RobinNotBad.BiliClient.model.VideoCard;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Locale;

import okhttp3.FormBody;
import okhttp3.HttpUrl;

public final class FavoriteContract {
    private static final Gson GSON = new Gson();
    private static final String BASE = "https://api.bilibili.com";

    private FavoriteContract() {}

    private static final class Envelope {
        Integer code;
        String message;
        JsonElement data;
    }

    public static final class FolderPage {
        public final ArrayList<FavoriteFolder> folders;
        public final boolean hasMore;

        private FolderPage(ArrayList<FavoriteFolder> folders, boolean hasMore) {
            this.folders = folders;
            this.hasMore = hasMore;
        }
    }

    public static final class VideoPage {
        public final ArrayList<VideoCard> videos;
        public final boolean hasMore;

        private VideoPage(ArrayList<VideoCard> videos, boolean hasMore) {
            this.videos = videos;
            this.hasMore = hasMore;
        }
    }

    public static final class StatePage {
        public final ArrayList<String> titles = new ArrayList<>();
        public final ArrayList<Long> mediaIds = new ArrayList<>();
        public final ArrayList<Boolean> states = new ArrayList<>();
    }

    public static class ApiException extends IOException {
        public ApiException(String message) {
            super(message);
        }
    }

    private static ApiException invalid() {
        return new ApiException("收藏接口返回数据异常：响应不是有效 JSON，或缺少必要字段、字段格式错误");
    }

    private static Envelope envelope(String json) throws IOException {
        try (JsonReader reader = new JsonReader(new StringReader(json == null ? "" : json))) {
            reader.setLenient(false);
            JsonElement root = GSON.getAdapter(JsonElement.class).read(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT || root == null || !root.isJsonObject()) throw invalid();
            JsonObject object = root.getAsJsonObject();
            long code = number(object, "code");
            if (code < Integer.MIN_VALUE || code > Integer.MAX_VALUE) throw invalid();
            Envelope result = GSON.fromJson(object, Envelope.class);
            if (result.code == null) throw invalid();
            return result;
        } catch (IOException e) {
            throw invalid();
        } catch (RuntimeException e) {
            throw invalid();
        }
    }

    public static int parseWriteCode(String json) throws IOException {
        return envelope(json).code;
    }

    private static JsonObject data(String json) throws IOException {
        Envelope response = envelope(json);
        if (response.code != 0) {
            String message = response.message == null ? "" : response.message;
            throw new ApiException("Favorite API code=" + response.code + ", message=" + message);
        }
        return object(response.data);
    }

    private static JsonObject object(JsonElement value) throws IOException {
        if (value == null || !value.isJsonObject()) throw invalid();
        return value.getAsJsonObject();
    }

    private static long number(JsonObject object, String key) throws IOException {
        try {
            JsonElement value = object.get(key);
            if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw invalid();
            return new BigDecimal(value.getAsString()).longValueExact();
        } catch (RuntimeException e) {
            throw invalid();
        }
    }

    private static int integer(JsonObject object, String key) throws IOException {
        long value = number(object, key);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) throw invalid();
        return (int) value;
    }

    private static String text(JsonObject object, String key) throws IOException {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) return "";
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw invalid();
        return value.getAsString();
    }

    private static boolean hasMore(JsonObject object) throws IOException {
        JsonElement value = object.get("has_more");
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) throw invalid();
        return value.getAsBoolean();
    }

    private static JsonArray list(JsonObject object, String key) throws IOException {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonArray()) throw invalid();
        return value.getAsJsonArray();
    }

    private static FavoriteFolder folder(JsonObject item) throws IOException {
        FavoriteFolder folder = new FavoriteFolder();
        folder.id = number(item, "id");
        if (folder.id <= 0) throw invalid();
        folder.mediaId = folder.id;
        folder.name = text(item, "title");
        folder.cover = text(item, "cover");
        folder.videoCount = integer(item, "media_count");
        int attr = integer(item, "attr");
        folder.intro = text(item, "intro");
        folder.isDefault = (attr & 2) == 0;
        folder.isPrivate = (attr & 1) != 0;
        return folder;
    }

    public static FolderPage parseFolders(String json) throws IOException {
        JsonObject data = data(json);
        ArrayList<FavoriteFolder> folders = new ArrayList<>();
        for (JsonElement item : list(data, "list")) folders.add(folder(object(item)));
        return new FolderPage(folders, hasMore(data));
    }

    public static FavoriteFolder parseFolderInfo(String json) throws IOException {
        return folder(data(json));
    }

    public static VideoPage parseVideos(String json) throws IOException {
        JsonObject data = data(json);
        boolean more = hasMore(data);
        ArrayList<VideoCard> videos = new ArrayList<>();
        JsonElement medias = data.get("medias");
        if (medias == null) throw invalid();
        if (!medias.isJsonNull()) {
            for (JsonElement entry : list(data, "medias")) {
                JsonObject item = object(entry);
                long aid = number(item, "id");
                if (aid <= 0) throw invalid();
                JsonElement upper = item.get("upper");
                JsonElement counts = item.get("cnt_info");
                String name = upper == null || upper.isJsonNull() ? "" : text(object(upper), "name");
                long play = counts == null || counts.isJsonNull() ? 0 : number(object(counts), "play");
                String view;
                if (play >= 100000000) view = String.format(Locale.CHINA, "%.1f亿", (float) play / 100000000);
                else if (play >= 10000) view = String.format(Locale.CHINA, "%.1f万", (float) play / 10000);
                else view = String.valueOf(play);
                videos.add(new VideoCard(text(item, "title"), name, view + "观看", text(item, "cover"), aid, text(item, "bv_id")));
            }
        }
        return new VideoPage(videos, more && !videos.isEmpty());
    }

    public static StatePage parseState(String json) throws IOException {
        StatePage result = new StatePage();
        for (JsonElement entry : list(data(json), "list")) {
            JsonObject item = object(entry);
            long id = number(item, "id");
            if (id <= 0) throw invalid();
            int state = integer(item, "fav_state");
            if (state != 0 && state != 1) throw invalid();
            result.titles.add(text(item, "title"));
            result.mediaIds.add(id);
            result.states.add(state == 1);
        }
        return result;
    }

    private static HttpUrl.Builder url(String path) {
        return HttpUrl.parse(BASE + path).newBuilder();
    }

    public static String foldersUrl(long mid, int page) {
        return url("/x/v3/fav/folder/created/list").addQueryParameter("up_mid", String.valueOf(mid))
                .addQueryParameter("pn", String.valueOf(page)).addQueryParameter("ps", "50").build().toString();
    }

    public static String folderInfoUrl(long mediaId) {
        return url("/x/v3/fav/folder/info").addQueryParameter("media_id", String.valueOf(mediaId)).build().toString();
    }

    public static String videosUrl(long mediaId, int page) {
        return url("/x/v3/fav/resource/list").addQueryParameter("media_id", String.valueOf(mediaId))
                .addQueryParameter("pn", String.valueOf(page)).addQueryParameter("ps", "30")
                .addQueryParameter("platform", "web").build().toString();
    }

    public static String stateUrl(long aid, long mid) {
        return url("/x/v3/fav/folder/created/list-all").addQueryParameter("type", "2")
                .addQueryParameter("rid", String.valueOf(aid)).addQueryParameter("up_mid", String.valueOf(mid)).build().toString();
    }

    public static String addFavoriteUrl() { return BASE + "/x/v3/fav/resource/deal"; }
    public static String deleteFavoriteUrl() { return BASE + "/medialist/gateway/coll/resource/batch/del"; }
    public static String addFolderUrl() { return BASE + "/x/v3/fav/folder/add"; }
    public static String editFolderUrl() { return BASE + "/x/v3/fav/folder/edit"; }
    public static String deleteFolderUrl() { return BASE + "/x/v3/fav/folder/del"; }

    private static String empty(String value) { return value == null ? "" : value; }

    public static FormBody addFavoriteForm(long aid, long mediaId, String csrf) {
        return new FormBody.Builder().add("rid", String.valueOf(aid)).add("type", "2")
                .add("add_media_ids", String.valueOf(mediaId)).add("del_media_ids", "").add("csrf", empty(csrf)).build();
    }

    public static FormBody deleteFavoriteForm(long aid, long mediaId, String csrf) {
        return new FormBody.Builder().add("resources", aid + ":2").add("media_id", String.valueOf(mediaId)).add("csrf", empty(csrf)).build();
    }

    public static FormBody addFolderForm(String title, String intro, int privacy, String csrf) {
        return new FormBody.Builder().add("title", empty(title)).add("intro", empty(intro))
                .add("privacy", String.valueOf(privacy)).add("csrf", empty(csrf)).build();
    }

    public static FormBody editFolderForm(long mediaId, String title, String intro, int privacy, String cover, String csrf) {
        return new FormBody.Builder().add("media_id", String.valueOf(mediaId)).add("title", empty(title))
                .add("intro", empty(intro)).add("privacy", String.valueOf(privacy)).add("cover", empty(cover)).add("csrf", empty(csrf)).build();
    }

    public static FormBody deleteFolderForm(long mediaId, String csrf) {
        return new FormBody.Builder().add("media_ids", String.valueOf(mediaId)).add("csrf", empty(csrf)).build();
    }

    public static String encodeForm(FormBody form) throws IOException {
        okio.Buffer buffer = new okio.Buffer();
        form.writeTo(buffer);
        return buffer.readUtf8();
    }
}
