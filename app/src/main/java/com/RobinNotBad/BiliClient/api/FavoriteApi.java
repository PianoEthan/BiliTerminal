package com.RobinNotBad.BiliClient.api;

import android.util.Pair;

import com.RobinNotBad.BiliClient.model.ApiResponse;
import com.RobinNotBad.BiliClient.model.Collection;
import com.RobinNotBad.BiliClient.model.FavoriteFolder;
import com.RobinNotBad.BiliClient.model.Opus;
import com.RobinNotBad.BiliClient.model.VideoCard;
import com.RobinNotBad.BiliClient.util.GsonUtil;
import com.RobinNotBad.BiliClient.util.NetWorkUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;
import com.RobinNotBad.BiliClient.util.StringUtil;
import com.google.gson.annotations.SerializedName;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import androidx.core.util.ObjectsCompat;

public class FavoriteApi {

    public static class FavFolderListData {
        @SerializedName("list")
        public List<FavFolderItem> list;
    }

    public static class FavFolderItem {
        @SerializedName("fav_box")
        public long fav_box;
        @SerializedName("name")
        public String name;
        @SerializedName("videos")
        public List<FavVideoRef> videos;
        @SerializedName("count")
        public int count;
        @SerializedName("max_count")
        public int max_count;
    }

    public static class FavVideoRef {
        @SerializedName("pic")
        public String pic;
    }

    public static class V3FavFolderData {
        @SerializedName("list")
        public List<V3FavFolderItem> list;
    }

    public static class V3FavFolderItem {
        @SerializedName("fid")
        public long fid;
        @SerializedName("id")
        public long id;
    }

    public static class FavFolderVideosData {
        @SerializedName("archives")
        public List<FavArchiveItem> archives;
    }

    public static class FavArchiveItem {
        @SerializedName("title")
        public String title;
        @SerializedName("pic")
        public String pic;
        @SerializedName("aid")
        public long aid;
        @SerializedName("owner")
        public RecommendApi.Owner owner;
        @SerializedName("stat")
        public RecommendApi.Stat stat;
    }

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

    public static class FavStateData {
        @SerializedName("list")
        public List<FavStateItem> list;
    }

    public static class FavStateItem {
        @SerializedName("title")
        public String title;
        @SerializedName("fid")
        public long fid;
        @SerializedName("fav_state")
        public int fav_state;
    }

    public static ArrayList<FavoriteFolder> getFavoriteFolders(long mid) throws IOException, JSONException {
        String url = "https://space.bilibili.com/ajax/fav/getBoxList?mid=" + mid;
        String json = NetWorkUtil.getJson(url).toString();
        ApiResponse<FavFolderListData> resp = GsonUtil.fromJson(json,
                new com.google.gson.reflect.TypeToken<ApiResponse<FavFolderListData>>(){}.getType());
        ArrayList<FavoriteFolder> folderList = new ArrayList<>();
        if (resp == null || resp.data == null || resp.data.list == null) return folderList;

        Map<Long, Long> fidToMediaIdMap = new HashMap<>();
        try {
            String v3Url = "https://api.bilibili.com/x/v3/fav/folder/created/list-all?type=2&up_mid=" + mid;
            String v3Json = NetWorkUtil.getJson(v3Url).toString();
            ApiResponse<V3FavFolderData> v3Resp = GsonUtil.fromJson(v3Json,
                    new com.google.gson.reflect.TypeToken<ApiResponse<V3FavFolderData>>(){}.getType());
            if (v3Resp != null && v3Resp.data != null && v3Resp.data.list != null) {
                for (V3FavFolderItem item : v3Resp.data.list) {
                    if (item != null && item.fid > 0 && item.id > 0) {
                        fidToMediaIdMap.put(item.fid, item.id);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        for (int i = 0; i < resp.data.list.size(); i++) {
            FavFolderItem folder = resp.data.list.get(i);
            if (folder == null) continue;
            FavoriteFolder fav = new FavoriteFolder();
            fav.id = folder.fav_box;
            fav.name = folder.name;
            if (folder.videos != null && !folder.videos.isEmpty() && folder.videos.get(0) != null) {
                fav.cover = folder.videos.get(0).pic;
            } else {
                fav.cover = "";
            }
            fav.videoCount = folder.count;
            fav.maxCount = folder.max_count;
            fav.isDefault = (i == 0 || fav.id == 0);
            Long mediaId = fidToMediaIdMap.get(fav.id);
            if (mediaId != null) fav.mediaId = mediaId;
            folderList.add(fav);
        }
        return folderList;
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

    public static int getFolderVideos(long mid, long fid, int page, ArrayList<VideoCard> videoList) throws IOException, JSONException {
        String url = "https://api.bilibili.com/x/space/fav/arc?vmid=" + mid + "&ps=30&fid=" + fid + "&tid=0&keyword=&pn=" + page + "&order=fav_time";
        String json = NetWorkUtil.getJson(url).toString();
        ApiResponse<FavFolderVideosData> resp = GsonUtil.fromJson(json,
                new com.google.gson.reflect.TypeToken<ApiResponse<FavFolderVideosData>>(){}.getType());
        if (resp == null || resp.data == null || resp.data.archives == null || resp.data.archives.isEmpty()) return 1;
        for (FavArchiveItem item : resp.data.archives) {
            if (item == null) continue;
            String upName = item.owner != null ? item.owner.name : "";
            int viewCount = item.stat != null ? item.stat.view : 0;
            videoList.add(new VideoCard(item.title, upName, StringUtil.toWan(viewCount) + "观看", item.pic, item.aid, ""));
        }
        return 0;
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

    public static void getFavoriteState(long aid, ArrayList<String> folderList, ArrayList<Long> fidList, ArrayList<Boolean> stateList) throws IOException, JSONException {
        String url = "https://api.bilibili.com/x/v3/fav/folder/created/list-all?type=2&jsonp=jsonp&rid=" + aid + "&up_mid=" + SharedPreferencesUtil.getLong("mid", 0);
        String json = NetWorkUtil.getJson(url).toString();
        ApiResponse<FavStateData> resp = GsonUtil.fromJson(json,
                new com.google.gson.reflect.TypeToken<ApiResponse<FavStateData>>(){}.getType());
        if (resp == null || resp.data == null || resp.data.list == null) return;
        for (FavStateItem item : resp.data.list) {
            if (item == null) continue;
            folderList.add(item.title);
            fidList.add(item.fid);
            stateList.add(item.fav_state == 1);
        }
    }

    public static int addFavorite(long aid, long fid) throws IOException, JSONException {
        String strMid = String.valueOf(SharedPreferencesUtil.getLong("mid", 0));
        String addFid = fid + strMid.substring(strMid.length() - 2);
        String url = "https://api.bilibili.com/medialist/gateway/coll/resource/deal";
        String per = "rid=" + aid + "&type=2&add_media_ids=" + addFid + "&del_media_ids=&csrf=" + SharedPreferencesUtil.getString("csrf", "");
        return GsonUtil.fromJson(ObjectsCompat.requireNonNull(NetWorkUtil.post(url, per, NetWorkUtil.webHeaders).body()).string(), ApiResponse.class).code;
    }

    public static int deleteFavorite(long aid, long fid) throws IOException, JSONException {
        String strMid = String.valueOf(SharedPreferencesUtil.getLong("mid", 0));
        String delFid = fid + strMid.substring(strMid.length() - 2);
        String url = "https://api.bilibili.com/medialist/gateway/coll/resource/batch/del";
        String per = "resources=" + aid + ":2&media_id=" + delFid + "&csrf=" + SharedPreferencesUtil.getString("csrf", "");
        return GsonUtil.fromJson(ObjectsCompat.requireNonNull(NetWorkUtil.post(url, per, NetWorkUtil.webHeaders).body()).string(), ApiResponse.class).code;
    }

    public static int addFolder(String title, String intro, int privacy) throws IOException, JSONException {
        String url = "https://api.bilibili.com/x/v3/fav/folder/add";
        String data = new NetWorkUtil.FormData().put("title", title).put("intro", intro != null ? intro : "").put("privacy", privacy).put("csrf", SharedPreferencesUtil.getString("csrf", "")).toString();
        return GsonUtil.fromJson(ObjectsCompat.requireNonNull(NetWorkUtil.post(url, data, NetWorkUtil.webHeaders).body()).string(), ApiResponse.class).code;
    }

    public static int editFolder(long mediaId, String title, String intro, int privacy) throws IOException, JSONException {
        String url = "https://api.bilibili.com/x/v3/fav/folder/edit";
        String data = new NetWorkUtil.FormData().put("media_id", mediaId).put("title", title).put("intro", intro != null ? intro : "").put("privacy", privacy).put("csrf", SharedPreferencesUtil.getString("csrf", "")).toString();
        return GsonUtil.fromJson(ObjectsCompat.requireNonNull(NetWorkUtil.post(url, data, NetWorkUtil.webHeaders).body()).string(), ApiResponse.class).code;
    }

    public static int deleteFolder(long mediaId) throws IOException, JSONException {
        String url = "https://api.bilibili.com/x/v3/fav/folder/del";
        String data = new NetWorkUtil.FormData().put("media_ids", String.valueOf(mediaId)).put("csrf", SharedPreferencesUtil.getString("csrf", "")).toString();
        return GsonUtil.fromJson(ObjectsCompat.requireNonNull(NetWorkUtil.post(url, data, NetWorkUtil.webHeaders).body()).string(), ApiResponse.class).code;
    }
}
