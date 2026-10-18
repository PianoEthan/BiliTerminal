package com.RobinNotBad.BiliClient.api;

import com.RobinNotBad.BiliClient.model.ApiResponse;
import com.RobinNotBad.BiliClient.model.Bangumi;
import com.RobinNotBad.BiliClient.model.VideoCard;
import com.RobinNotBad.BiliClient.util.GsonUtil;
import com.RobinNotBad.BiliClient.util.NetWorkUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;
import com.RobinNotBad.BiliClient.util.StringUtil;
import com.google.gson.annotations.SerializedName;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import androidx.core.util.ObjectsCompat;

public class BangumiApi {

    public static class BangumiFollowData {
        @SerializedName("list")
        public List<BangumiFollowItem> list;
    }

    public static class BangumiFollowItem {
        @SerializedName("media_id")
        public long media_id;
        @SerializedName("title")
        public String title;
        @SerializedName("cover")
        public String cover;
        @SerializedName("stat")
        public FollowStat stat;
    }

    public static class FollowStat {
        @SerializedName("view")
        public int view;
    }

    public static class ReviewData {
        @SerializedName("result")
        public ReviewResult result;
    }

    public static class ReviewResult {
        @SerializedName("media")
        public MediaData media;
    }

    public static class MediaData {
        @SerializedName("media_id")
        public long media_id;
        @SerializedName("season_id")
        public long season_id;
        @SerializedName("title")
        public String title;
        @SerializedName("cover")
        public String cover;
        @SerializedName("horizontal_picture")
        public String horizontal_picture;
        @SerializedName("type")
        public int type;
        @SerializedName("type_name")
        public String type_name;
        @SerializedName("new_ep")
        public NewEpData new_ep;
        @SerializedName("rating")
        public RatingData rating;
        @SerializedName("areas")
        public List<AreaData> areas;
    }

    public static class NewEpData {
        @SerializedName("index_show")
        public String index_show;
    }

    public static class RatingData {
        @SerializedName("count")
        public int count;
        @SerializedName("score")
        public float score;
    }

    public static class AreaData {
        @SerializedName("name")
        public String name;
    }

    public static class SeasonDetailData {
        @SerializedName("result")
        public SeasonResult result;
    }

    public static class SeasonResult {
        @SerializedName("evaluate")
        public String evaluate;
        @SerializedName("staff")
        public String staff;
        @SerializedName("record")
        public String record;
        @SerializedName("subtitle")
        public String subtitle;
        @SerializedName("publish")
        public PublishData publish;
        @SerializedName("styles")
        public List<String> styles;
        @SerializedName("stat")
        public SeasonStat stat;
        @SerializedName("up_info")
        public UpInfoData up_info;
        @SerializedName("series")
        public SeriesData series;
        @SerializedName("seasons")
        public List<SeasonItem> seasons;
    }

    public static class PublishData {
        @SerializedName("is_finish")
        public int is_finish;
        @SerializedName("is_started")
        public int is_started;
        @SerializedName("pub_time")
        public String pub_time;
        @SerializedName("pub_time_show")
        public String pub_time_show;
    }

    public static class SeasonStat {
        @SerializedName("favorites")
        public int favorites;
        @SerializedName("series_follow")
        public int series_follow;
        @SerializedName("views")
        public int views;
        @SerializedName("vt")
        public int vt;
    }

    public static class UpInfoData {
        @SerializedName("mid")
        public long mid;
        @SerializedName("name")
        public String name;
        @SerializedName("avatar")
        public String avatar;
    }

    public static class SeriesData {
        @SerializedName("series_id")
        public long series_id;
        @SerializedName("series_title")
        public String series_title;
    }

    public static class SeasonItem {
        @SerializedName("media_id")
        public long media_id;
        @SerializedName("season_id")
        public long season_id;
        @SerializedName("season_title")
        public String season_title;
        @SerializedName("cover")
        public String cover;
        @SerializedName("badge")
        public String badge;
    }

    public static class SectionData {
        @SerializedName("result")
        public SectionResult result;
    }

    public static class SectionResult {
        @SerializedName("main_section")
        public SectionItem main_section;
        @SerializedName("section")
        public List<SectionItem> section;
    }

    public static class SectionItem {
        @SerializedName("id")
        public long id;
        @SerializedName("title")
        public String title;
        @SerializedName("type")
        public int type;
        @SerializedName("episodes")
        public List<EpisodeData> episodes;
    }

    public static class EpisodeData {
        @SerializedName("id")
        public long id;
        @SerializedName("aid")
        public long aid;
        @SerializedName("cid")
        public long cid;
        @SerializedName("cover")
        public String cover;
        @SerializedName("badge")
        public String badge;
        @SerializedName("title")
        public String title;
        @SerializedName("long_title")
        public String long_title;
    }

    public static class SeasonIdData {
        @SerializedName("result")
        public SeasonIdResult result;
    }

    public static class SeasonIdResult {
        @SerializedName("media_id")
        public long media_id;
    }

    public static int getFollowingList(int page, List<VideoCard> cardList) throws IOException, JSONException {
        String url = "https://api.bilibili.com/x/space/bangumi/follow/list?type=1&follow_status=0&pn=" + page + "&ps=15&vmid=" + SharedPreferencesUtil.getLong("mid", 0);
        String json = NetWorkUtil.getJson(url).toString();
        ApiResponse<BangumiFollowData> resp = GsonUtil.fromJson(json, new com.google.gson.reflect.TypeToken<ApiResponse<BangumiFollowData>>(){}.getType());
        if (resp == null || !resp.isSuccess() || resp.data == null || resp.data.list == null || resp.data.list.isEmpty()) return 1;
        for (BangumiFollowItem item : resp.data.list) {
            if (item == null) continue;
            VideoCard card = new VideoCard();
            card.type = "media_bangumi";
            card.aid = item.media_id;
            card.title = item.title;
            card.cover = item.cover;
            card.view = StringUtil.toWan(item.stat != null ? item.stat.view : 0);
            cardList.add(card);
        }
        return 0;
    }

    public static Bangumi getBangumi(long mediaId) throws IOException, JSONException {
        Bangumi bangumi = new Bangumi();
        bangumi.info = getInfo(mediaId);
        bangumi.sectionList = getSections(bangumi.info.season_id);
        return bangumi;
    }

    public static Long getMdidFromEpid(long epid) {
        try {
            String json = NetWorkUtil.getJson("https://api.bilibili.com/pgc/view/web/season?ep_id=" + epid).toString();
            SeasonIdData data = GsonUtil.fromJson(json, SeasonIdData.class);
            return (data != null && data.result != null) ? data.result.media_id : 0L;
        } catch (Exception e) { return 0L; }
    }

    public static Bangumi.Info getInfo(long mediaId) throws IOException, JSONException {
        String json1 = NetWorkUtil.getJson("https://api.bilibili.com/pgc/review/user?media_id=" + mediaId).toString();
        ReviewData review = GsonUtil.fromJson(json1, ReviewData.class);
        if (review == null || review.result == null || review.result.media == null) throw new JSONException("获取番剧信息失败");
        MediaData media = review.result.media;

        Bangumi.Info info = new Bangumi.Info();
        info.media_id = media.media_id;
        info.season_id = media.season_id;
        info.title = media.title;
        info.cover = media.cover;
        info.cover_horizontal = media.horizontal_picture;
        info.type = media.type;
        info.type_name = media.type_name;
        info.indexShow = media.new_ep != null ? media.new_ep.index_show : "敬请期待";
        if (media.rating != null) { info.count = media.rating.count; info.score = media.rating.score; }
        if (media.areas != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < media.areas.size(); i++) {
                if (i > 0) sb.append(" | ");
                AreaData area = media.areas.get(i);
                if (area != null) sb.append(area.name);
            }
            info.area_name = sb.toString();
        }

        String json2 = NetWorkUtil.getJson("https://api.bilibili.com/pgc/view/web/season?season_id=" + info.season_id).toString();
        SeasonDetailData detail = GsonUtil.fromJson(json2, SeasonDetailData.class);
        if (detail != null && detail.result != null) {
            SeasonResult r = detail.result;
            info.evaluate = r.evaluate;
            info.staff = r.staff;
            info.record = r.record;
            info.subtitle = r.subtitle;
            if (r.publish != null) {
                info.publish = new Bangumi.Publish();
                info.publish.is_finish = r.publish.is_finish;
                info.publish.is_started = r.publish.is_started;
                info.publish.pub_time = r.publish.pub_time;
                info.publish.pub_time_show = r.publish.pub_time_show;
            }
            info.styles = r.styles != null ? new ArrayList<>(r.styles) : new ArrayList<>();
            if (r.stat != null) {
                info.stat = new Bangumi.Stat();
                info.stat.favorites = r.stat.favorites;
                info.stat.series_follow = r.stat.series_follow;
                info.stat.views = r.stat.views;
                info.stat.vt = r.stat.vt;
            }
            if (r.up_info != null) {
                info.up_info = new Bangumi.UpInfo();
                info.up_info.mid = r.up_info.mid;
                info.up_info.name = r.up_info.name;
                info.up_info.avatar = r.up_info.avatar;
            }
            if (r.series != null) {
                info.series = new Bangumi.Series();
                info.series.series_id = r.series.series_id;
                info.series.series_title = r.series.series_title;
            }
            if (r.seasons != null) {
                info.seasons = new ArrayList<>();
                for (SeasonItem s : r.seasons) {
                    if (s == null) continue;
                    Bangumi.Season season = new Bangumi.Season();
                    season.media_id = s.media_id;
                    season.season_id = s.season_id;
                    season.season_title = s.season_title;
                    season.cover = s.cover;
                    season.badge = s.badge;
                    info.seasons.add(season);
                }
            }
        }
        return info;
    }

    public static ArrayList<Bangumi.Section> getSections(long season_id) throws IOException, JSONException {
        String json = new String(ObjectsCompat.requireNonNull(ObjectsCompat.requireNonNull(NetWorkUtil.get("https://api.bilibili.com/pgc/web/season/section?season_id=" + season_id)).body()).bytes());
        SectionData data = GsonUtil.fromJson(json, SectionData.class);
        ArrayList<Bangumi.Section> sectionList = new ArrayList<>();
        if (data == null || data.result == null) return sectionList;
        if (data.result.main_section != null) sectionList.add(buildSection(data.result.main_section));
        if (data.result.section != null) {
            for (SectionItem item : data.result.section) {
                if (item != null) sectionList.add(buildSection(item));
            }
        }
        return sectionList;
    }

    private static Bangumi.Section buildSection(SectionItem item) {
        Bangumi.Section section = new Bangumi.Section();
        section.id = item.id;
        section.title = item.title;
        section.type = item.type;
        section.episodeList = new ArrayList<>();
        if (item.episodes != null) {
            for (EpisodeData ep : item.episodes) {
                if (ep == null) continue;
                Bangumi.Episode episode = new Bangumi.Episode();
                episode.id = ep.id;
                episode.aid = ep.aid;
                episode.cid = ep.cid;
                episode.cover = ep.cover;
                episode.badge = ep.badge;
                episode.title = ep.title;
                episode.title_long = ep.long_title;
                section.episodeList.add(episode);
            }
        }
        return section;
    }
}
