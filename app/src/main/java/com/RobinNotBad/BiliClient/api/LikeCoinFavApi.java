package com.RobinNotBad.BiliClient.api;

import com.RobinNotBad.BiliClient.model.ApiResponse;
import com.RobinNotBad.BiliClient.model.VideoInfo;
import com.RobinNotBad.BiliClient.util.GsonUtil;
import com.RobinNotBad.BiliClient.util.MsgUtil;
import com.RobinNotBad.BiliClient.util.NetWorkUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;
import com.google.gson.annotations.SerializedName;

import org.json.JSONException;

import java.io.IOException;
import androidx.core.util.ObjectsCompat;

public class LikeCoinFavApi {

    public static class RelationData {
        @SerializedName("attention") public boolean attention;
        @SerializedName("like") public boolean like;
        @SerializedName("dislik") public boolean dislik;
        @SerializedName("favorite") public boolean favorite;
        @SerializedName("coin") public int coin;
    }

    public static int triple(long aid) throws IOException, JSONException {
        String per = "aid=" + aid + "&csrf=" + SharedPreferencesUtil.getString("csrf", "");
        return GsonUtil.fromJson(ObjectsCompat.requireNonNull(NetWorkUtil.post("https://api.bilibili.com/x/web-interface/archive/like/triple", per, NetWorkUtil.webHeaders).body()).string(), ApiResponse.class).code;
    }

    public static int like(long aid, int likeState) throws IOException, JSONException {
        String per = "aid=" + aid + "&like=" + likeState + "&csrf=" + SharedPreferencesUtil.getString("csrf", "");
        return GsonUtil.fromJson(ObjectsCompat.requireNonNull(NetWorkUtil.post("https://api.bilibili.com/x/web-interface/archive/like", per, NetWorkUtil.webHeaders).body()).string(), ApiResponse.class).code;
    }

    public static int coin(long aid, int multiply) throws IOException, JSONException {
        String per = "aid=" + aid + "&multiply=" + multiply + "&csrf=" + SharedPreferencesUtil.getString("csrf", "");
        return GsonUtil.fromJson(ObjectsCompat.requireNonNull(NetWorkUtil.post("https://api.bilibili.com/x/web-interface/coin/add", per, NetWorkUtil.webHeaders).body()).string(), ApiResponse.class).code;
    }

    public static int favorite(long aid, long fid) throws IOException, JSONException {
        String strMid = String.valueOf(SharedPreferencesUtil.getLong("mid", 0));
        String per = "rid=" + aid + "&type=2&add_media_ids=" + (fid + strMid.substring(strMid.length() - 2)) + "&del_media_ids=&csrf=" + SharedPreferencesUtil.getString("csrf", "");
        return GsonUtil.fromJson(ObjectsCompat.requireNonNull(NetWorkUtil.post("https://api.bilibili.com/medialist/gateway/coll/resource/deal", per, NetWorkUtil.webHeaders).body()).string(), ApiResponse.class).code;
    }

    public static ApiResponse getVideoStats(VideoInfo videoInfo) {
        ApiResponse apiResult = new ApiResponse();
        if (SharedPreferencesUtil.getLong("mid", 0) == 0) return apiResult;
        try {
            String json = NetWorkUtil.getJson("https://api.bilibili.com/x/web-interface/archive/relation?aid=" + videoInfo.aid).toString();
            ApiResponse<RelationData> resp = GsonUtil.fromJson(json, new com.google.gson.reflect.TypeToken<ApiResponse<RelationData>>(){}.getType());
            if (resp != null) { apiResult.code = resp.code; apiResult.message = resp.message; }
            if (resp != null && resp.data != null && videoInfo.stats != null) {
                videoInfo.stats.followed = resp.data.attention;
                videoInfo.stats.liked = resp.data.like;
                videoInfo.stats.disliked = resp.data.dislik;
                videoInfo.stats.favoured = resp.data.favorite;
                videoInfo.stats.coined = resp.data.coin;
            }
        } catch (Exception e) { MsgUtil.err(apiResult.message, e); }
        return apiResult;
    }
}
