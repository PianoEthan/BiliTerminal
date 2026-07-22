package com.RobinNotBad.BiliClient.api;

import com.RobinNotBad.BiliClient.model.HotSearchCard;
import com.RobinNotBad.BiliClient.util.NetWorkUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * B站热搜榜 API
 * 接口: https://api.bilibili.com/x/web-interface/wbi/search/square?limit=50
 */
public class HotSearchApi {

    /**
     * 获取热搜榜单
     * @param list 结果填充到这个列表
     * @return true=成功, false=失败
     */
    public static boolean getHotSearch(List<HotSearchCard> list) {
        try {
            String url = "https://api.bilibili.com/x/web-interface/wbi/search/square?limit=50";
            String signedUrl = ConfInfoApi.signWBI(url);
            JSONObject root = NetWorkUtil.getJson(signedUrl);

            int code = root.optInt("code", -1);
            if (code != 0) return false;

            JSONObject data = root.optJSONObject("data");
            if (data == null) return false;

            JSONObject trending = data.optJSONObject("trending");
            if (trending == null) return false;

            JSONArray trendList = trending.optJSONArray("list");
            if (trendList == null) return false;

            for (int i = 0; i < trendList.length(); i++) {
                JSONObject item = trendList.optJSONObject(i);
                if (item == null) continue;
                HotSearchCard card = new HotSearchCard();
                card.keyword = item.optString("keyword", "");
                card.showName = item.optString("show_name", card.keyword);
                card.icon = item.optString("icon", "");
                card.position = item.optInt("position", i + 1);
                card.heatScore = item.optLong("heat_score", 0);
                list.add(card);
            }
            return !list.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
