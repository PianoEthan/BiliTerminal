package com.RobinNotBad.BiliClient.api;

import android.text.TextUtils;
import android.util.Log;

import com.RobinNotBad.BiliClient.model.ApiResponse;
import com.RobinNotBad.BiliClient.model.VideoCard;
import com.RobinNotBad.BiliClient.util.GsonUtil;
import com.RobinNotBad.BiliClient.util.NetWorkUtil;
import com.RobinNotBad.BiliClient.util.StringUtil;
import com.google.gson.annotations.SerializedName;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RecommendApi {
    private static final long UNIQ_ID = (long) (new Random().nextDouble() * (1500000000000L - 1300000000000L));

    public static class RecommendResponse {
        @SerializedName("item")
        public List<Item> item;
    }

    public static class PopularResponse {
        @SerializedName("list")
        public List<Item> list;
    }

    public static class Item {
        @SerializedName("bvid")
        public String bvid;
        @SerializedName("pic")
        public String pic;
        @SerializedName("title")
        public String title;
        @SerializedName("owner")
        public Owner owner;
        @SerializedName("stat")
        public Stat stat;
    }

    public static class Owner {
        @SerializedName("name")
        public String name;
    }

    public static class Stat {
        @SerializedName("view")
        public int view;
    }

    public static void getRecommend(List<VideoCard> videoCardList) throws IOException, JSONException {
        String url = "https://api.bilibili.com/x/web-interface/wbi/index/top/feed/rcmd";
        url += new NetWorkUtil.FormData().setUrlParam(true)
                .put("web_location", 1430650)
                .put("feed_version", "V8")
                .put("homepage_ver", 1)
                .put("uniq_id", UNIQ_ID)
                .put("screen", "1100-2056");

        String json = NetWorkUtil.getJson(ConfInfoApi.signWBI(url)).toString();
        ApiResponse<RecommendResponse> resp = GsonUtil.fromJson(json,
                new com.google.gson.reflect.TypeToken<ApiResponse<RecommendResponse>>(){}.getType());
        if (resp == null || !resp.isSuccess() || resp.data == null || resp.data.item == null) return;

        for (Item card : resp.data.item) {
            if (card == null || TextUtils.isEmpty(card.bvid)) {
                Log.d("BiliClient", "RecommendApi getRecommend: isAd");
                continue;
            }
            String upName = card.owner != null ? card.owner.name : "";
            int viewCount = card.stat != null ? card.stat.view : 0;
            videoCardList.add(new VideoCard(card.title, upName, StringUtil.toWan(viewCount) + "观看", card.pic, 0, card.bvid));
        }
    }

    public static ArrayList<VideoCard> getRelated(long aid) throws IOException, JSONException {
        String url = "https://api.bilibili.com/x/web-interface/archive/related?aid=" + aid;
        String json = NetWorkUtil.getJson(url).toString();
        ApiResponse<List<Item>> resp = GsonUtil.fromJson(json,
                new com.google.gson.reflect.TypeToken<ApiResponse<List<Item>>>(){}.getType());
        ArrayList<VideoCard> videoList = new ArrayList<>();
        if (resp == null || !resp.isSuccess() || resp.data == null) return videoList;

        for (Item card : resp.data) {
            if (card == null) continue;
            String upName = card.owner != null ? card.owner.name : "";
            int viewCount = card.stat != null ? card.stat.view : 0;
            videoList.add(new VideoCard(card.title, upName, StringUtil.toWan(viewCount) + "观看", card.pic, 0, card.bvid));
        }
        return videoList;
    }

    public static boolean getPopular(List<VideoCard> videoCardList, int page) throws IOException, JSONException {
        String url = "https://api.bilibili.com/x/web-interface/popular?pn=" + page + "&ps=10";
        String json = NetWorkUtil.getJson(url).toString();
        ApiResponse<PopularResponse> resp = GsonUtil.fromJson(json,
                new com.google.gson.reflect.TypeToken<ApiResponse<PopularResponse>>(){}.getType());
        if (resp == null || !resp.isSuccess() || resp.data == null || resp.data.list == null) return false;

        for (Item card : resp.data.list) {
            if (card == null) continue;
            String upName = card.owner != null ? card.owner.name : "";
            int viewCount = card.stat != null ? card.stat.view : 0;
            videoCardList.add(new VideoCard(card.title, upName, StringUtil.toWan(viewCount) + "观看", card.pic, 0, card.bvid));
        }
        return true;
    }

    public static boolean getPrecious(List<VideoCard> videoCardList, int page) throws IOException, JSONException {
        String url = "https://api.bilibili.com/x/web-interface/popular/precious?page=" + page + "&page_size=10";
        String json = NetWorkUtil.getJson(url).toString();
        ApiResponse<PopularResponse> resp = GsonUtil.fromJson(json,
                new com.google.gson.reflect.TypeToken<ApiResponse<PopularResponse>>(){}.getType());
        if (resp == null || !resp.isSuccess() || resp.data == null || resp.data.list == null) return false;

        for (Item card : resp.data.list) {
            if (card == null) continue;
            String upName = card.owner != null ? card.owner.name : "";
            int viewCount = card.stat != null ? card.stat.view : 0;
            videoCardList.add(new VideoCard(card.title, upName, StringUtil.toWan(viewCount) + "观看", card.pic, 0, card.bvid));
        }
        return true;
    }
}
