package com.RobinNotBad.BiliClient.api;

import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.RobinNotBad.BiliClient.BiliTerminal;
import com.RobinNotBad.BiliClient.model.ApiResponse;
import com.RobinNotBad.BiliClient.model.ArticleCard;
import com.RobinNotBad.BiliClient.model.At;
import com.RobinNotBad.BiliClient.model.Dynamic;
import com.RobinNotBad.BiliClient.model.Emote;
import com.RobinNotBad.BiliClient.model.LiveRoom;
import com.RobinNotBad.BiliClient.model.Stats;
import com.RobinNotBad.BiliClient.model.UserInfo;
import com.RobinNotBad.BiliClient.model.VideoCard;
import com.RobinNotBad.BiliClient.util.DmImgParamUtil;
import com.RobinNotBad.BiliClient.util.EmoteUtil;
import com.RobinNotBad.BiliClient.util.GsonUtil;
import com.RobinNotBad.BiliClient.util.Logu;
import com.RobinNotBad.BiliClient.util.MsgUtil;
import com.RobinNotBad.BiliClient.util.NetWorkUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;
import com.RobinNotBad.BiliClient.util.StringUtil;
import com.google.gson.annotations.SerializedName;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import androidx.core.util.ObjectsCompat;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Response;
import okhttp3.ResponseBody;

public class DynamicApi {

    public static class DynamicListData {
        @SerializedName("has_more")
        public boolean has_more;
        @SerializedName("offset")
        public String offset;
        @SerializedName("update_baseline")
        public long update_baseline;
        @SerializedName("items")
        public List<com.google.gson.JsonElement> items;
    }

    public static class DynamicDetailData {
        @SerializedName("item")
        public com.google.gson.JsonElement item;
    }

    public static class UpdateData {
        @SerializedName("update_num")
        public int update_num;
    }

    public static class PortalData {
        @SerializedName("up_list")
        public List<UpInfo> up_list;
    }

    public static long publishTextContent(String content) throws IOException {
        String url = "https://api.vc.bilibili.com/dynamic_svr/v1/dynamic_svr/create";
        Response resp = ObjectsCompat.requireNonNull(NetWorkUtil.post(url, new NetWorkUtil.FormData()
                .put("dynamic_id", 0).put("type", 4).put("rid", 0).put("content", content)
                .put("csrf", SharedPreferencesUtil.getString("csrf", "")).toString(), NetWorkUtil.webHeaders));
        try {
            ResponseBody body = resp.body();
            if (body == null) return -1;
            ApiResponse<DynamicIdData> r = GsonUtil.fromJson(body.string(), new com.google.gson.reflect.TypeToken<ApiResponse<DynamicIdData>>(){}.getType());
            return (r != null && r.isSuccess() && r.data != null) ? r.data.dynamic_id : -1;
        } catch (Exception ignored) { return -1; }
    }

    public static class DynamicIdData {
        @SerializedName("dynamic_id") public long dynamic_id;
        @SerializedName("dyn_id") public long dyn_id;
    }

    public static long publishComplex(@NonNull JSONArray contents, JSONArray pics, JSONObject option, JSONObject topic, int scene, Map<String, Object> otherArgs) throws IOException, JSONException {
        String url = "https://api.bilibili.com/x/dynamic/feed/create/dyn?csrf=" + SharedPreferencesUtil.getString("csrf", "");
        JSONObject reqBody = new JSONObject()
                .put("content", new JSONObject().put("contents", contents))
                .put("scene", scene)
                .put("meta", new JSONObject().put("app_meta", new JSONObject().put("from", "create.dynamic.web").put("mobi_app", "web")));
        if (pics != null) reqBody.put("pics", pics);
        if (option != null) reqBody.put("option", option);
        if (topic != null) reqBody.put("topic", topic);
        reqBody = new JSONObject().put("dyn_req", reqBody);
        if (otherArgs != null) for (Map.Entry<String, Object> e : otherArgs.entrySet()) reqBody.put(e.getKey(), e.getValue());
        Logu.v("publishComplex reqBody=" + reqBody);
        Response resp = ObjectsCompat.requireNonNull(NetWorkUtil.postJson(url, reqBody.toString()));
        try {
            ResponseBody body = resp.body();
            if (body == null) return -1;
            ApiResponse<DynamicIdData> r = GsonUtil.fromJson(body.string(), new com.google.gson.reflect.TypeToken<ApiResponse<DynamicIdData>>(){}.getType());
            return (r != null && r.isSuccess() && r.data != null) ? r.data.dyn_id : -1;
        } catch (Exception e) { MsgUtil.err("发送动态", e); return -1; }
    }

    public static long publishTextContent(String content, Map<String, Long> atUserUid) throws JSONException, IOException {
        return publishComplex(parseAtContent(content, atUserUid), null, null, null, 1, null);
    }

    public static long relayVideo(String text, Map<String, Long> atUserUid, long aid) throws JSONException, IOException {
        return publishComplex(text == null ? new JSONArray().put(Content.create("", 1, null)) : atUserUid != null ? parseAtContent(text, atUserUid) : new JSONArray().put(Content.create(text, 1, null)),
                null, null, null, 5, Collections.singletonMap("web_repost_src", new JSONObject().put("revs_id", new JSONObject().put("dyn_type", 8).put("rid", aid))));
    }

    public static long relayDynamic(String text, long dyid) throws IOException {
        String url = "https://api.vc.bilibili.com/dynamic_repost/v1/dynamic_repost/repost";
        Response resp = ObjectsCompat.requireNonNull(NetWorkUtil.post(url, new NetWorkUtil.FormData()
                .put("dynamic_id", dyid).put("content", text).put("csrf_token", SharedPreferencesUtil.getString("csrf", "")).toString(), NetWorkUtil.webHeaders));
        try {
            ResponseBody body = resp.body();
            if (body == null) return -1;
            ApiResponse<DynamicIdData> r = GsonUtil.fromJson(body.string(), new com.google.gson.reflect.TypeToken<ApiResponse<DynamicIdData>>(){}.getType());
            return (r != null && r.isSuccess() && r.data != null) ? r.data.dynamic_id : -1;
        } catch (Exception ignored) { return -1; }
    }

    public static long relayDynamic(String text, Map<String, Long> atUserUid, long dyid) throws JSONException, IOException {
        return publishComplex(text == null ? new JSONArray().put(Content.create("", 1, null)) : atUserUid != null ? parseAtContent(text, atUserUid) : new JSONArray().put(Content.create(text, 1, null)),
                null, null, null, 4, Collections.singletonMap("web_repost_src", new JSONObject().put("dyn_id_str", String.valueOf(dyid))));
    }

    public static JSONArray parseAtContent(String content, Map<String, Long> atUserUid) throws JSONException {
        JSONArray contentJSONArray = new JSONArray();
        Set<Pair<Integer, Integer>> indexes = new HashSet<>();
        Map<Pair<Integer, Integer>, Long> uidIndexes = new HashMap<>();
        for (Map.Entry<String, Long> entry : atUserUid.entrySet()) {
            Pattern pattern = Pattern.compile("@" + entry.getKey() + " ");
            Matcher matcher = pattern.matcher(content);
            List<Pair<Integer, Integer>> mIndex = new ArrayList<>();
            while (matcher.find()) {
                Pair<Integer, Integer> pair = new Pair<>(matcher.start(), matcher.end());
                mIndex.add(pair);
                uidIndexes.put(pair, entry.getValue());
            }
            indexes.addAll(mIndex);
        }
        ArrayList<Pair<Integer, Integer>> indexesList = new ArrayList<>(indexes);
        int pos = 0;
        for (Pair<Integer, Integer> index : indexesList) {
            String sub = content.substring(pos, index.first);
            if (!sub.isEmpty()) contentJSONArray.put(Content.create(sub, 1, null));
            contentJSONArray.put(Content.create(content.substring(index.first, index.second), 2, String.valueOf(uidIndexes.get(index))));
            pos = index.second;
        }
        String sub = content.substring(pos);
        if (!sub.isEmpty()) contentJSONArray.put(Content.create(sub, 1, null));
        if (indexesList.isEmpty()) contentJSONArray.put(Content.create(content, 1, null));
        return contentJSONArray;
    }

    public static int likeDynamic(long dyid, boolean up) throws IOException {
        String url = "https://api.vc.bilibili.com/dynamic_like/v1/dynamic_like/thumb";
        Response resp = ObjectsCompat.requireNonNull(NetWorkUtil.post(url, new NetWorkUtil.FormData()
                .put("dynamic_id", dyid).put("up", up ? 1 : 2).put("csrf_token", SharedPreferencesUtil.getString("csrf", "")).toString(), NetWorkUtil.webHeaders));
        try {
            ResponseBody body = resp.body();
            if (body == null) return -1;
            return GsonUtil.fromJson(body.string(), ApiResponse.class).code;
        } catch (Exception e) { return -1; }
    }

    public static int deleteDynamic(long dyid) throws IOException {
        String url = "https://api.vc.bilibili.com/dynamic_svr/v1/dynamic_svr/rm_dynamic";
        Response resp = ObjectsCompat.requireNonNull(NetWorkUtil.post(url, new NetWorkUtil.FormData()
                .put("dynamic_id", dyid).put("csrf_token", SharedPreferencesUtil.getString("csrf", "")).toString(), NetWorkUtil.webHeaders));
        try {
            ResponseBody body = resp.body();
            if (body == null) return -1;
            return GsonUtil.fromJson(body.string(), ApiResponse.class).code;
        } catch (Exception ignored) { return -1; }
    }

    public static long mentionAtFindUser(String name) throws IOException, JSONException {
        String url = "https://api.bilibili.com/x/polymer/web-dynamic/v1/mention/search?keyword=" + name;
        String json = NetWorkUtil.getJson(url, NetWorkUtil.webHeaders).toString();
        MentionData data = GsonUtil.fromJson(json, MentionData.class);
        if (data == null || data.data == null || data.data.groups == null) return -1;
        for (MentionGroup group : data.data.groups) {
            if (group == null || group.items == null) continue;
            for (MentionItem item : group.items) {
                if (item != null && name.equals(item.name)) {
                    try { return Long.parseLong(item.uid); } catch (Exception ignored) {}
                }
            }
        }
        return -1;
    }

    public static class MentionData { @SerializedName("data") public MentionDataInner data; }
    public static class MentionDataInner { @SerializedName("groups") public List<MentionGroup> groups; }
    public static class MentionGroup { @SerializedName("items") public List<MentionItem> items; }
    public static class MentionItem { @SerializedName("name") public String name; @SerializedName("uid") public String uid; }

    public static long getDynamicList(List<Dynamic> dynamicList, long offset, long mid, String type) throws IOException, JSONException {
        String url;
        if (mid == 0) {
            url = "https://api.bilibili.com/x/polymer/web-dynamic/v1/feed/all?type=" + type + (offset == 0 ? "" : "&offset=" + offset) + "&features=itemOpusStyle,listOnlyfans,opusBigCover,onlyfansVote,forwardListHidden,decorationCard,commentsNewVersion,onlyfansAssetsV2,ugcDelete,onlyfansQaCard,avatarAutoTheme,sunflowerStyle,eva3CardOpus,eva3CardVideo,eva3CardComment";
        } else {
            url = "https://api.bilibili.com/x/polymer/web-dynamic/v1/feed/space?host_mid=" + mid + "&platform=web&web_location=333.1387&timezone_offset=-480" + (offset == 0 ? "" : "&offset=" + offset) + "&features=itemOpusStyle,listOnlyfans,opusBigCover,onlyfansVote,forwardListHidden,decorationCard,commentsNewVersion,onlyfansAssetsV2,ugcDelete,onlyfansQaCard,avatarAutoTheme,sunflowerStyle,eva3CardOpus,eva3CardVideo,eva3CardComment";
        }
        String json = NetWorkUtil.getJson(ConfInfoApi.signWBI(DmImgParamUtil.getDmImgParamsUrl(url))).toString();
        ApiResponse<DynamicListData> resp = GsonUtil.fromJson(json, new com.google.gson.reflect.TypeToken<ApiResponse<DynamicListData>>(){}.getType());
        if (resp == null || !resp.isSuccess() || resp.data == null) throw new JSONException("获取动态列表失败");

        DynamicListData data = resp.data;
        long offset_new;
        try {
            offset_new = data.has_more && data.offset != null ? Long.parseLong(data.offset) : -1;
        } catch (NumberFormatException e) {
            offset_new = -1;
        }

        if (mid == 0) {
            if (data.update_baseline > -1) SharedPreferencesUtil.putLong("dynamic_update_baseline", data.update_baseline);
            else if (offset_new != -1) SharedPreferencesUtil.putLong("dynamic_update_baseline", offset_new);
        }

        if (data.items != null) {
            for (com.google.gson.JsonElement item : data.items) {
                if (item == null) continue;
                try {
                    dynamicList.add(analyzeDynamic(new JSONObject(item.toString())));
                } catch (Exception ignored) {}
            }
        }
        return offset_new;
    }

    public static Dynamic getDynamic(long id) throws JSONException, IOException {
        String url = "https://api.bilibili.com/x/polymer/web-dynamic/v1/detail?id=" + id;
        String json = NetWorkUtil.getJson(url).toString();
        ApiResponse<DynamicDetailData> resp = GsonUtil.fromJson(json, new com.google.gson.reflect.TypeToken<ApiResponse<DynamicDetailData>>(){}.getType());
        if (resp == null || !resp.isSuccess()) {
            String fallbackUrl = "https://api.bilibili.com/x/polymer/web-dynamic/v1/detail?rid=" + id + "&type=2";
            String fallbackJson = NetWorkUtil.getJson(fallbackUrl).toString();
            ApiResponse<DynamicDetailData> fallback = GsonUtil.fromJson(fallbackJson, new com.google.gson.reflect.TypeToken<ApiResponse<DynamicDetailData>>(){}.getType());
            if (fallback == null || !fallback.isSuccess() || fallback.data == null || fallback.data.item == null)
                throw new JSONException("获取动态详情失败");
            return analyzeDynamic(new JSONObject(fallback.data.item.toString()));
        }
        if (resp.data == null || resp.data.item == null) throw new JSONException("data is null");
        return analyzeDynamic(new JSONObject(resp.data.item.toString()));
    }

    public static int checkDynamicUpdate(String type, long updateBaseline) throws IOException, JSONException {
        String url = "https://api.bilibili.com/x/polymer/web-dynamic/v1/feed/all/update?type=" + type + "&update_baseline=" + updateBaseline + "&web_location=333.1365";
        String json = NetWorkUtil.getJson(url, NetWorkUtil.webHeaders).toString();
        ApiResponse<UpdateData> resp = GsonUtil.fromJson(json, new com.google.gson.reflect.TypeToken<ApiResponse<UpdateData>>(){}.getType());
        if (resp == null || !resp.isSuccess()) throw new JSONException("检查动态更新失败");
        return resp.data != null ? resp.data.update_num : 0;
    }

    public static Dynamic analyzeDynamic(JSONObject dynamic_json) {
        Dynamic dynamic = new Dynamic();
        try { dynamic.dynamicId = Long.parseLong(dynamic_json.optString("id_str", "0")); } catch (Exception ignored) {}
        dynamic.type = dynamic_json.optString("type", "");

        JSONObject basic = dynamic_json.optJSONObject("basic");
        if (basic != null) {
            try { dynamic.comment_id = Long.parseLong(basic.optString("comment_id_str", "0")); } catch (Exception ignored) {}
            dynamic.comment_type = basic.optInt("comment_type", 0);
        }
        if (dynamic.comment_id == 0) dynamic.comment_id = dynamic.dynamicId;
        if (dynamic.comment_type == 0) dynamic.comment_type = 17;

        JSONObject modules = dynamic_json.optJSONObject("modules");
        if (modules == null) return dynamic;

        UserInfo userInfo = new UserInfo();
        JSONObject module_author = modules.optJSONObject("module_author");
        if (module_author != null) {
            userInfo.mid = module_author.optLong("mid", 0);
            userInfo.name = module_author.optString("name", "");
            userInfo.followed = module_author.optBoolean("following", false) || module_author.optInt("following", 0) == 1;
            userInfo.avatar = module_author.optString("face", "");
            JSONObject vipJson = module_author.optJSONObject("vip");
            if (vipJson != null) userInfo.vip_nickname_color = vipJson.optString("nickname_color", "");
            dynamic.pubTime = module_author.optString("pub_time", "");
        }
        dynamic.userInfo = userInfo;

        if ("DYNAMIC_TYPE_NONE".equals(dynamic.type)) { dynamic.content = "[动态不存在]"; return dynamic; }

        JSONObject module_dynamic = modules.optJSONObject("module_dynamic");
        if (module_dynamic != null) {
            JSONObject desc = module_dynamic.optJSONObject("desc");
            dynamic.content = desc != null ? analyzeTextContent(desc.optJSONArray("rich_text_nodes")) : "";

            JSONObject major = module_dynamic.optJSONObject("major");
            if (major != null) {
                String major_type = major.optString("type", "");
                dynamic.major_type = major_type;
                switch (major_type) {
                    case "MAJOR_TYPE_ARCHIVE": { JSONObject a = major.optJSONObject("archive"); if (a != null) dynamic.major_object = analyzeVideoCard(a); break; }
                    case "MAJOR_TYPE_UGC_SEASON": { JSONObject u = major.optJSONObject("ugc_season"); if (u != null) dynamic.major_object = analyzeVideoCard(u); break; }
                    case "MAJOR_TYPE_PGC": {
                        JSONObject pgc = major.optJSONObject("pgc");
                        if (pgc != null) {
                            VideoCard card = new VideoCard();
                            card.type = "media_bangumi";
                            card.aid = BangumiApi.getMdidFromEpid(pgc.optLong("epid", 0));
                            card.title = pgc.optString("title", "");
                            card.cover = pgc.optString("cover", "");
                            JSONObject stat = pgc.optJSONObject("stat");
                            card.view = stat != null ? stat.optString("play", "0") : "0";
                            dynamic.major_object = card;
                        }
                        break;
                    }
                    case "MAJOR_TYPE_ARTICLE": {
                        JSONObject art = major.optJSONObject("article");
                        if (art != null) {
                            JSONArray covers = art.optJSONArray("covers");
                            dynamic.major_object = new ArticleCard(art.optString("title", ""), art.optLong("id", 0),
                                    (covers != null && covers.length() > 0) ? covers.optString(0, "") : "", "投稿文章", art.optString("label", ""));
                        }
                        break;
                    }
                    case "MAJOR_TYPE_DRAW": {
                        JSONObject draw = major.optJSONObject("draw");
                        if (draw != null) {
                            JSONArray items = draw.optJSONArray("items");
                            ArrayList<String> pics = new ArrayList<>();
                            if (items != null) for (int i = 0; i < items.length(); i++) { JSONObject it = items.optJSONObject(i); if (it != null) pics.add(it.optString("src", "")); }
                            dynamic.major_object = pics;
                        }
                        break;
                    }
                    case "MAJOR_TYPE_COMMON": dynamic.content += "\n[无法显示活动类动态的附加内容]"; break;
                    case "MAJOR_TYPE_LIVE_RCMD": {
                        JSONObject lrc = major.optJSONObject("live_rcmd");
                        if (lrc != null) try {
                            JSONObject lpi = new JSONObject(lrc.optString("content", "{}")).optJSONObject("live_play_info");
                            if (lpi != null) { LiveRoom room = new LiveRoom(); room.roomid = lpi.optLong("room_id", 0); room.title = lpi.optString("title", ""); room.cover = lpi.optString("cover", ""); room.online = lpi.optInt("online", 0); dynamic.major_object = room; }
                        } catch (Exception ignored) {}
                        dynamic.content = (TextUtils.isEmpty(dynamic.content) ? "" : dynamic.content + "\n"); break;
                    }
                    case "MAJOR_TYPE_LIVE": {
                        JSONObject live = major.optJSONObject("live");
                        if (live != null) { LiveRoom room = new LiveRoom(); room.roomid = live.optLong("id", 0); room.title = live.optString("title", ""); room.cover = live.optString("cover", ""); dynamic.major_object = room; }
                        dynamic.content = (TextUtils.isEmpty(dynamic.content) ? "" : dynamic.content + "\n"); break;
                    }
                    case "MAJOR_TYPE_OPUS": {
                        JSONObject opus = major.optJSONObject("opus");
                        if (opus != null) {
                            String title = opus.optString("title", "");
                            if (!TextUtils.isEmpty(title) && !"null".equals(title)) dynamic.title = title;
                            JSONArray pics = opus.optJSONArray("pics");
                            if (pics != null) { ArrayList<String> pl = new ArrayList<>(); for (int i = 0; i < pics.length(); i++) { JSONObject p = pics.optJSONObject(i); if (p != null) pl.add(p.optString("url", "")); } dynamic.major_object = pl; }
                            JSONObject summary = opus.optJSONObject("summary");
                            dynamic.content = summary != null ? analyzeTextContent(summary.optJSONArray("rich_text_nodes")) : "";
                        }
                        break;
                    }
                    default: dynamic.content += "\n[*哔哩终端暂时无法查看此动态的附加内容QwQ|类型：" + major_type + "]"; break;
                }
            }

            JSONObject module_additional = modules.optJSONObject("module_additional");
            if (module_additional != null) {
                if ("ADDITIONAL_TYPE_UGC".equals(module_additional.optString("type", ""))) {
                    dynamic.major_type = "MAJOR_TYPE_ARCHIVE";
                    JSONObject ugc = module_additional.optJSONObject("ugc");
                    if (ugc != null) dynamic.major_object = analyzeVideoCard(ugc);
                }
            }
        }

        JSONObject module_stat = modules.optJSONObject("module_stat");
        if (module_stat != null) {
            JSONObject like = module_stat.optJSONObject("like");
            if (like != null) {
                Stats stats = new Stats();
                stats.like = like.optInt("count", 0);
                stats.liked = like.optBoolean("status", false) || like.optInt("status", 0) == 1;
                stats.like_disabled = like.optBoolean("forbidden", false) || like.optInt("forbidden", 0) == 1;
                dynamic.stats = stats;
            }
        }

        JSONObject module_more = modules.optJSONObject("module_more");
        if (module_more != null) {
            JSONArray tpi = module_more.optJSONArray("three_point_items");
            if (tpi != null) {
                List<String> types = new ArrayList<>();
                for (int i = 0; i < tpi.length(); i++) { JSONObject it = tpi.optJSONObject(i); if (it != null) types.add(it.optString("type", "")); }
                dynamic.canDelete = types.contains("THREE_POINT_DELETE");
            }
        }

        JSONObject orig = dynamic_json.optJSONObject("orig");
        if (orig != null) dynamic.dynamic_forward = analyzeDynamic(orig);

        return dynamic;
    }

    private static VideoCard analyzeVideoCard(JSONObject json) {
        JSONObject stat = json.optJSONObject("stat");
        return new VideoCard(json.optString("title", ""), "投稿视频", stat != null ? stat.optString("play", "0") : "0",
                json.optString("cover", ""), json.optLong("aid", 0), json.optString("bvid", ""));
    }

    private static SpannableStringBuilder analyzeTextContent(JSONArray rich_text_nodes) {
        if (rich_text_nodes == null) return new SpannableStringBuilder("[动态内容解析异常]");
        ArrayList<Emote> emoteList = new ArrayList<>();
        ArrayList<At> atList = new ArrayList<>();
        SpannableStringBuilder content = new SpannableStringBuilder();
        for (int i = 0; i < rich_text_nodes.length(); i++) {
            JSONObject node = rich_text_nodes.optJSONObject(i);
            if (node == null) continue;
            String type = node.optString("type", "");
            switch (type) {
                case "RICH_TEXT_NODE_TYPE_EMOJI":
                    content.append(node.optString("text", ""));
                    JSONObject emoji = node.optJSONObject("emoji");
                    if (emoji != null) emoteList.add(new Emote(emoji.optString("text", ""), emoji.optString("icon_url", ""), emoji.optInt("size", 0)));
                    break;
                case "RICH_TEXT_NODE_TYPE_AT":
                    Pair<Integer, Integer> idx = StringUtil.appendString(content, node.optString("text", ""));
                    atList.add(new At(node.optLong("rid", 0), idx.first, idx.second));
                    break;
                case "RICH_TEXT_NODE_TYPE_WEB": content.append(node.optString("orig_text", "")); break;
                default: content.append(node.optString("text", "")); break;
            }
        }
        EmoteUtil.textReplaceEmote(content.toString(), emoteList, 1.0f, BiliTerminal.context, content);
        for (At at : atList) StringUtil.setSingleAt(content, at);
        return content;
    }

    public static class Content {
        public static JSONObject create(@NonNull String raw_text, int type, String biz_id) throws JSONException {
            return new JSONObject().put("raw_text", raw_text).put("type", type).put("biz_id", biz_id == null ? "" : biz_id);
        }
    }

    public static List<UpInfo> getRecentUpList() throws IOException, JSONException {
        String json = NetWorkUtil.getJson("https://api.bilibili.com/x/polymer/web-dynamic/v1/portal", NetWorkUtil.webHeaders).toString();
        ApiResponse<PortalData> resp = GsonUtil.fromJson(json, new com.google.gson.reflect.TypeToken<ApiResponse<PortalData>>(){}.getType());
        if (resp == null || !resp.isSuccess()) throw new JSONException("获取最近UP主列表失败");
        List<UpInfo> upList = new ArrayList<>();
        if (resp.data != null && resp.data.up_list != null) upList.addAll(resp.data.up_list);
        return upList;
    }

    public static class UpInfo {
        @SerializedName("mid") public long mid;
        @SerializedName("uname") public String uname;
        @SerializedName("face") public String face;
        @SerializedName("has_update") public boolean has_update;
    }
}
