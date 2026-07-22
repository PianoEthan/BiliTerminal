package com.RobinNotBad.BiliClient.api;

import com.RobinNotBad.BiliClient.model.VideoCard;
import com.RobinNotBad.BiliClient.util.Logu;
import com.RobinNotBad.BiliClient.util.NetWorkUtil;
import com.RobinNotBad.BiliClient.util.StringUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * 番剧索引 API（带标签筛选）
 * 接口: https://api.bilibili.com/pgc/web/season/index/result
 */
public class BangumiIndexApi {

    public static int result_total = 0;

    /**
     * @param seasonType 1=动画，4=国产
     * @param area       -1=全部，2=日本，1=中国，3=其他
     * @param isFinish   -1=全部，0=连载中，1=已完结
     * @param year       -1=全部，或具体年份
     * @param sort       排序：0=综合，3=按热度，4=按时间
     * @param page       页码，从 1 开始
     * @param list       结果列表
     * @return hasNext
     */
    public static boolean getIndex(int seasonType, int area, int isFinish, int styleId,
                                    int year, int sort, int page, List<VideoCard> list) {
        try {
            String url = "https://api.bilibili.com/pgc/web/season/index/result"
                    + new NetWorkUtil.FormData().setUrlParam(true)
                    .put("season_version", -1)
                    .put("area", area)
                    .put("is_finish", isFinish)
                    .put("copyright", -1)
                    .put("season_status", -1)
                    .put("season_month", -1)
                    .put("year", year)
                    .put("style_id", styleId)
                    .put("order", sort)
                    .put("sort", 0)
                    .put("page", page)
                    .put("season_type", seasonType)
                    .put("pagesize", 20)
                    .put("type", 1)
                    .put("web_location", "333.934");
            JSONObject root = NetWorkUtil.getJson(url);
            int code = root.optInt("code", -1);
            if (code != 0) {
                Logu.w("BangumiIndexApi", "index api code=" + code + " msg=" + root.optString("message"));
                return false;
            }
            JSONObject data = root.optJSONObject("data");
            if (data == null) return false;
            result_total = data.optInt("total", 0);
            JSONArray items = data.optJSONArray("list");
            if (items == null) return false;
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item == null) continue;
                String title = item.optString("title", "");
                String cover = item.optString("cover", "");
                long mediaId = item.optLong("media_id", 0);
                if (mediaId <= 0) mediaId = item.optLong("season_id", 0);
                JSONObject rating = item.optJSONObject("rating");
                String subTitle = "";
                if (rating != null) {
                    float s = (float) rating.optDouble("score", 0);
                    if (s > 0) subTitle = s + "分 ";
                }
                String styles = item.optString("styles", "");
                if (!styles.isEmpty()) subTitle += styles;
                JSONObject order = item.optJSONObject("order");
                long views = order != null ? order.optLong("vv", 0) : 0;
                String viewStr = views > 0 ? StringUtil.toWan(views) + "播放" : "";
                VideoCard card = new VideoCard(title, subTitle, viewStr, cover, mediaId, null);
                card.type = "media_bangumi";
                list.add(card);
            }
            return data.optInt("has_next", 0) == 1 && page * 20 < result_total;
        } catch (Exception e) {
            return false;
        }
    }
}
