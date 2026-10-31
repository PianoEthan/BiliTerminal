package com.RobinNotBad.BiliClient.api;

import android.util.Pair;

import com.RobinNotBad.BiliClient.model.ApiResponse;
import com.RobinNotBad.BiliClient.model.Collection;
import com.RobinNotBad.BiliClient.model.FavoriteFolder;
import com.RobinNotBad.BiliClient.model.Opus;
import com.RobinNotBad.BiliClient.util.GsonUtil;
import com.RobinNotBad.BiliClient.util.NetWorkUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;
import com.RobinNotBad.BiliClient.util.StringUtil;
import com.google.gson.annotations.SerializedName;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FavoriteApi {

    public static class CollectedListData {
        @SerializedName("has_more")
        public boolean has_more;
        @SerializedName("list")
        public List<CollectionItem> list;
    }

    public static class CollectionItem {
        @SerializedName("id")
        public int id;
        @SerializedName("mid")
        public long mid;
        @SerializedName("title")
        public String title;
        @SerializedName("cover")
        public String cover;
        @SerializedName("intro")
        public String intro;
        @SerializedName("view_count")
        public int view_count;
    }

    public static class OpusFavData {
        @SerializedName("has_more")
        public boolean has_more;
        @SerializedName("items")
        public List<OpusItem> items;
    }

    public static class OpusItem {
        @SerializedName("content")
        public String content;
        @SerializedName("cover")
        public String cover;
        @SerializedName("title")
        public String title;
        @SerializedName("opus_id")
        public String opus_id;
        @SerializedName("time_text")
        public String time_text;
    }

    public static ArrayList<FavoriteFolder> getFavoriteFolders(long mid) throws IOException, JSONException {
        ArrayList<FavoriteFolder> folders = new ArrayList<>();
        for (int page = 1; ; page++) {
            FavoriteContract.FolderPage result = FavoriteContract.parseFolders(getFavoriteJson(FavoriteContract.foldersUrl(mid, page)));
            folders.addAll(result.folders);
            if (!result.hasMore || result.folders.isEmpty()) return folders;
        }
    }

    public static FavoriteFolder getFolderInfo(long mediaId) throws IOException, JSONException {
        return FavoriteContract.parseFolderInfo(getFavoriteJson(FavoriteContract.folderInfoUrl(mediaId)));
    }

    private static String getFavoriteJson(String url) throws IOException {
        try (okhttp3.Response response = NetWorkUtil.get(url)) {
            if (!response.isSuccessful()) throw new IOException("Favorite HTTP code=" + response.code());
            if (response.body() == null) throw new IOException("Empty favorite response");
            return response.body().string();
        }
    }

    public static Pair<Integer, Boolean> getFavoritedCollections(long mid, int page, List<Collection> collectionList) throws IOException, JSONException {
        String url = "https://api.bilibili.com/x/v3/fav/folder/collected/list" + new NetWorkUtil.FormData()
                .setUrlParam(true).put("platform", "web").put("up_mid", mid).put("pn", page).put("ps", 10);
        String json = NetWorkUtil.getJson(url).toString();
        ApiResponse<CollectedListData> resp = GsonUtil.fromJson(json,
                new com.google.gson.reflect.TypeToken<ApiResponse<CollectedListData>>(){}.getType());
        int code = resp != null ? resp.code : -1;
        boolean hasMore = false;
        if (resp != null && resp.data != null) {
            hasMore = resp.data.has_more;
            if (resp.data.list != null) {
                for (CollectionItem item : resp.data.list) {
                    if (item == null) continue;
                    Collection c = new Collection();
                    c.id = item.id;
                    c.mid = item.mid;
                    c.title = item.title;
                    c.cover = item.cover;
                    c.intro = item.intro;
                    c.view = StringUtil.toWan(item.view_count);
                    collectionList.add(c);
                }
            }
        }
        return new Pair<>(code, hasMore);
    }

    public static FavoriteContract.VideoPage getFolderVideos(long mediaId, int page) throws IOException, JSONException {
        return FavoriteContract.parseVideos(getFavoriteJson(FavoriteContract.videosUrl(mediaId, page)));
    }

    public static boolean getFavouriteOpus(ArrayList<Opus> list, int page) throws IOException, JSONException {
        String url = "https://api.bilibili.com/x/polymer/web-dynamic/v1/opus/favlist?page_size=10&page=" + page;
        String json = NetWorkUtil.getJson(url).toString();
        ApiResponse<OpusFavData> resp = GsonUtil.fromJson(json,
                new com.google.gson.reflect.TypeToken<ApiResponse<OpusFavData>>(){}.getType());
        if (resp == null || !resp.isSuccess() || resp.data == null) return false;
        if (resp.data.items != null) {
            for (OpusItem item : resp.data.items) {
                if (item == null) continue;
                Opus opus = new Opus();
                opus.content = item.content;
                opus.cover = item.cover;
                opus.title = item.title;
                if (opus.title == null || opus.title.isEmpty()) opus.title = opus.content;
                try { opus.id = Long.parseLong(item.opus_id); } catch (Exception ignored) {}
                opus.pubTime = item.time_text;
                list.add(opus);
            }
        }
        return resp.data.has_more;
    }

    public static void getFavoriteState(long aid, ArrayList<String> folderList, ArrayList<Long> mediaIds, ArrayList<Boolean> states) throws IOException, JSONException {
        FavoriteContract.StatePage result = FavoriteContract.parseState(getFavoriteJson(
                FavoriteContract.stateUrl(aid, SharedPreferencesUtil.getLong("mid", 0))));
        folderList.addAll(result.titles);
        mediaIds.addAll(result.mediaIds);
        states.addAll(result.states);
    }

    public static int addFavorite(long aid, long mediaId) throws IOException, JSONException {
        return postFavorite(FavoriteContract.addFavoriteUrl(), FavoriteContract.addFavoriteForm(aid, mediaId, csrf()));
    }

    public static int deleteFavorite(long aid, long mediaId) throws IOException, JSONException {
        return postFavorite(FavoriteContract.deleteFavoriteUrl(), FavoriteContract.deleteFavoriteForm(aid, mediaId, csrf()));
    }

    public static int addFolder(String title, String intro, int privacy) throws IOException, JSONException {
        return postFavorite(FavoriteContract.addFolderUrl(), FavoriteContract.addFolderForm(title, intro, privacy, csrf()));
    }

    public static int editFolder(long mediaId, String title, String intro, int privacy, String cover) throws IOException, JSONException {
        return postFavorite(FavoriteContract.editFolderUrl(), FavoriteContract.editFolderForm(mediaId, title, intro, privacy, cover, csrf()));
    }

    public static int deleteFolder(long mediaId) throws IOException, JSONException {
        return postFavorite(FavoriteContract.deleteFolderUrl(), FavoriteContract.deleteFolderForm(mediaId, csrf()));
    }

    private static String csrf() {
        return SharedPreferencesUtil.getString("csrf", "");
    }

    private static int postFavorite(String url, okhttp3.FormBody form) throws IOException {
        try (okhttp3.Response response = NetWorkUtil.post(url, FavoriteContract.encodeForm(form), NetWorkUtil.webHeaders)) {
            if (!response.isSuccessful()) throw new IOException("Favorite HTTP code=" + response.code());
            if (response.body() == null) throw new IOException("Empty favorite response");
            return FavoriteContract.parseWriteCode(response.body().string());
        }
    }
}
