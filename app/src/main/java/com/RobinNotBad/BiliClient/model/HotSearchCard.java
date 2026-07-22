package com.RobinNotBad.BiliClient.model;

/**
 * B站热搜榜条目模型
 * API: /x/web-interface/wbi/search/square
 */
public class HotSearchCard {
    public String keyword;      // 搜索关键词
    public String showName;     // 展示名称
    public String icon;         // 图标URL
    public int position;        // 排名
    public long heatScore;      // 热度值
}
