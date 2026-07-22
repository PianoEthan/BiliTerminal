package com.RobinNotBad.BiliClient.api;

import com.RobinNotBad.BiliClient.model.VideoCard;
import com.RobinNotBad.BiliClient.util.Logu;
import com.RobinNotBad.BiliClient.util.NetWorkUtil;
import com.RobinNotBad.BiliClient.util.StringUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * 番剧排行榜 / 推荐 API
 * 接口: https://api.bilibili.com/pgc/web/rank/list （无需 WBI 签名）
 */
public class BangumiRankingApi {

    /**
     * 获取番剧排行榜
     * @param day        榜单周期：3=三日榜，7=七日榜
     * @param seasonType 番剧类型：1=动画，4=国产剧
     * @param list       结果填充到此列表
     */
    public static void getRanking(int day, int seasonType, List<VideoCard> list) {
        try {
            String url = "https://api.bilibili.com/pgc/web/rank/list";
            url += new NetWorkUtil.FormData().setUrlParam(true)
                    .put("day", day).put("season_type", seasonType).put("web_location", "333.934");
            //PGC rank 接口不强制 WBI 签名，直接用 getJson 避免签名失败导致静默吞异常
            JSONObject root = NetWorkUtil.getJson(url);
            Logu.d("BangumiRankingApi", "rank code=" + root.optInt("code"));

            if (root.optInt("code", -1) != 0) {
                Logu.w("BangumiRankingApi", "rank api error: " + root.optString("message"));
                return;
            }
            JSONObject result = root.optJSONObject("result");
            if (result == null) return;
            JSONArray rankingList = result.optJSONArray("list");
            if (rankingList == null) return;

            for (int i = 0; i < rankingList.length(); i++) {
                JSONObject item = rankingList.optJSONObject(i);
                if (item == null) continue;
                String title = item.optString("title", "");
                String cover = item.optString("cover", "");
                long mediaId = item.optLong("media_id", 0);
                if (mediaId <= 0) mediaId = item.optLong("season_id", 0);
                JSONObject rating = item.optJSONObject("rating");
                String badge = item.optString("badge", "");
                String subTitle = "";
                if (rating != null) {
                    float score = (float) rating.optDouble("score", 0);
                    if (score > 0) subTitle = score + "分 ";
                }
                if (!badge.isEmpty()) subTitle += badge;
                JSONObject order = item.optJSONObject("order");
                long views = order != null ? order.optLong("vv", 0) : 0;
                String viewStr = views > 0 ? StringUtil.toWan(views) + "播放" : "";
                VideoCard card = new VideoCard(title, subTitle, viewStr, cover, mediaId, null);
                card.type = "media_bangumi";
                list.add(card);
            }
        } catch (Exception e) {
            Logu.e("BangumiRankingApi", "getRanking failed: " + e.getMessage());
        }
    }
}
