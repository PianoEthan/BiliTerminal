package com.RobinNotBad.BiliClient.api;

import android.text.SpannableString;
import android.util.Pair;

import com.RobinNotBad.BiliClient.model.ApiResponse;
import com.RobinNotBad.BiliClient.model.MessageCard;
import com.RobinNotBad.BiliClient.model.Reply;
import com.RobinNotBad.BiliClient.model.UserInfo;
import com.RobinNotBad.BiliClient.model.VideoCard;
import com.RobinNotBad.BiliClient.util.GsonUtil;
import com.RobinNotBad.BiliClient.util.NetWorkUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;
import com.google.gson.annotations.SerializedName;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Response;

public class MessageApi {

    public static class UnreadData {
        @SerializedName("at") public int at;
        @SerializedName("like") public int like;
        @SerializedName("reply") public int reply;
        @SerializedName("sys_msg") public int sys_msg;
        @SerializedName("unfollow_unread") public int unfollow_unread;
        @SerializedName("follow_unread") public int follow_unread;
        @SerializedName("unfollow_push_msg") public int unfollow_push_msg;
        @SerializedName("dustbin_push_msg") public int dustbin_push_msg;
        @SerializedName("dustbin_unread") public int dustbin_unread;
        @SerializedName("biz_msg_unfollow_unread") public int biz_msg_unfollow_unread;
        @SerializedName("biz_msg_follow_unread") public int biz_msg_follow_unread;
        @SerializedName("custom_unread") public int custom_unread;
        @SerializedName("unread_count") public int unread_count;
    }

    public static class LikeMsgData {
        @SerializedName("total") public MsgItems total;
        @SerializedName("cursor") public MsgCursor cursor;
    }
    public static class MsgItems {
        @SerializedName("items") public List<LikeMsgItem> items;
    }
    public static class LikeMsgItem {
        @SerializedName("id") public long id;
        @SerializedName("users") public List<MsgUser> users;
        @SerializedName("like_time") public long like_time;
        @SerializedName("counts") public long counts;
        @SerializedName("item") public LikeItemData item;
    }
    public static class MsgUser {
        @SerializedName("mid") public long mid;
        @SerializedName("nickname") public String nickname;
        @SerializedName("avatar") public String avatar;
        @SerializedName("fans") public int fans;
        @SerializedName("follow") public boolean follow;
        @SerializedName("uname") public String uname;
    }
    public static class LikeItemData {
        @SerializedName("business_id") public int business_id;
        @SerializedName("item_id") public long item_id;
        @SerializedName("subject_id") public long subject_id;
        @SerializedName("source_id") public long source_id;
        @SerializedName("root_id") public long root_id;
        @SerializedName("type") public String type;
        @SerializedName("title") public String title;
        @SerializedName("source_content") public String source_content;
        @SerializedName("image") public String image;
        @SerializedName("uri") public String uri;
        @SerializedName("target_id") public long target_id;
    }
    public static class MsgCursor {
        @SerializedName("is_end") public boolean is_end;
        @SerializedName("id") public long id;
        @SerializedName("time") public long time;
    }

    public static class ReplyMsgData {
        @SerializedName("items") public List<ReplyMsgItem> items;
        @SerializedName("cursor") public MsgCursor cursor;
    }
    public static class ReplyMsgItem {
        @SerializedName("id") public long id;
        @SerializedName("user") public MsgUser user;
        @SerializedName("reply_time") public long reply_time;
        @SerializedName("item") public LikeItemData item;
    }

    public static class AtMsgData {
        @SerializedName("items") public List<ReplyMsgItem> items;
        @SerializedName("cursor") public MsgCursor cursor;
    }

    public static class SystemMsgData {
        @SerializedName("system_notify_list") public List<SystemNotifyItem> system_notify_list;
    }
    public static class SystemNotifyItem {
        @SerializedName("id") public long id;
        @SerializedName("time_at") public String time_at;
        @SerializedName("title") public String title;
        @SerializedName("content") public String content;
    }

    private static UnreadData getUnreadData() {
        try {
            String json = NetWorkUtil.getJson("https://api.bilibili.com/x/msgfeed/unread").toString();
            ApiResponse<UnreadData> resp = GsonUtil.fromJson(json, new com.google.gson.reflect.TypeToken<ApiResponse<UnreadData>>(){}.getType());
            return (resp != null && resp.isSuccess() && resp.data != null) ? resp.data : new UnreadData();
        } catch (Exception e) { return new UnreadData(); }
    }

    public static JSONObject getUnread() throws IOException, JSONException {
        UnreadData d = getUnreadData();
        return new JSONObject().put("at", d.at).put("like", d.like).put("reply", d.reply).put("system", d.sys_msg);
    }

    public static int checkMessageUnread() { UnreadData d = getUnreadData(); return d.at + d.reply; }

    public static int checkPrivateMsgUnread() {
        try {
            String json = NetWorkUtil.getJson("https://api.vc.bilibili.com/session_svr/v1/session_svr/single_unread").toString();
            ApiResponse<UnreadData> resp = GsonUtil.fromJson(json, new com.google.gson.reflect.TypeToken<ApiResponse<UnreadData>>(){}.getType());
            if (resp == null || resp.data == null) return 0;
            UnreadData d = resp.data;
            return d.unfollow_unread + d.follow_unread + d.unfollow_push_msg + d.dustbin_push_msg + d.dustbin_unread + d.biz_msg_unfollow_unread + d.biz_msg_follow_unread + d.custom_unread;
        } catch (Exception e) { return 0; }
    }

    public static int checkGroupMsgUnread() {
        try {
            String json = NetWorkUtil.getJson("https://api.vc.bilibili.com/session_svr/v1/session_svr/my_group_unread").toString();
            ApiResponse<UnreadData> resp = GsonUtil.fromJson(json, new com.google.gson.reflect.TypeToken<ApiResponse<UnreadData>>(){}.getType());
            return (resp != null && resp.data != null) ? resp.data.unread_count : 0;
        } catch (Exception e) { return 0; }
    }

    private static UserInfo parseUser(MsgUser u) {
        if (u == null) return new UserInfo(0, "", "", "", 0, 0, 0, false, "", 0, "", 0);
        return new UserInfo(u.mid, u.nickname != null ? u.nickname : u.uname, u.avatar, "", u.fans, 0, 0, u.follow, "", 0, "", 0);
    }

    private static VideoCard parseVideoCard(LikeItemData item) {
        if (item == null) return new VideoCard();
        VideoCard vc = new VideoCard();
        vc.title = item.title; vc.cover = item.image;
        vc.bvid = item.uri != null ? item.uri.replace("https://www.bilibili.com/video/BV", "") : "";
        return vc;
    }

    private static Reply buildReply(LikeItemData item, boolean isDynamic) {
        Reply r = new Reply();
        r.rpid = item != null ? item.target_id : 0;
        r.message = new SpannableString(item != null ? item.title : "");
        r.pictureList = new ArrayList<>(); r.childMsgList = new ArrayList<>();
        r.isDynamic = isDynamic;
        if (item != null && item.uri != null) r.ofBvid = item.uri.replace("https://www.bilibili.com/video/", "");
        return r;
    }

    private static MessageCard.Cursor parseCursor(MsgCursor c) {
        return c == null ? null : new MessageCard.Cursor(c.is_end, c.id, c.time);
    }

    public static Pair<MessageCard.Cursor, List<MessageCard>> getLikeMsg(long id, long time) throws IOException, JSONException {
        String url = "https://api.bilibili.com/x/msgfeed/like?platform=web&build=0&mobi_app=web";
        if (id > 0 && time > 0) url += "&id=" + id + "&reply_time=" + time;
        String json = NetWorkUtil.getJson(url).toString();
        ApiResponse<LikeMsgData> resp = GsonUtil.fromJson(json, new com.google.gson.reflect.TypeToken<ApiResponse<LikeMsgData>>(){}.getType());
        ArrayList<MessageCard> list = new ArrayList<>();
        if (resp == null || !resp.isSuccess() || resp.data == null || resp.data.total == null || resp.data.total.items == null)
            return new Pair<>(null, list);

        for (LikeMsgItem obj : resp.data.total.items) {
            if (obj == null) continue;
            MessageCard mc = new MessageCard();
            mc.id = obj.id; mc.timeStamp = obj.like_time;
            mc.user = new ArrayList<>();
            if (obj.users != null) for (MsgUser u : obj.users) mc.user.add(parseUser(u));
            LikeItemData item = obj.item;
            if (item == null) { mc.getType = MessageCard.GET_TYPE_LIKE; list.add(mc); continue; }
            mc.businessId = item.business_id; mc.subjectId = item.subject_id; mc.sourceId = item.source_id;
            mc.rootId = item.root_id; mc.itemType = item.type; mc.contentUri = item.uri;
            switch (item.type) {
                case "video": mc.content = "等总共 " + obj.counts + " 人点赞了你的视频"; mc.videoCard = parseVideoCard(item); break;
                case "reply": mc.content = "等总共 " + obj.counts + " 人点赞了你的评论"; mc.replyInfo = buildReply(item, false); break;
                case "dynamic": case "album": mc.content = "等总共 " + obj.counts + " 人点赞了你的动态"; mc.dynamicInfo = buildReply(item, true); break;
                case "article": mc.content = "等总共 " + obj.counts + " 人点赞了你的专栏"; Reply r = new Reply(); r.rpid = item.target_id; r.message = new SpannableString(item.title); r.childCount = 0; mc.replyInfo = r; break;
                default: mc.content = "无法识别这个类别：" + item.type;
            }
            mc.getType = MessageCard.GET_TYPE_LIKE; list.add(mc);
        }
        return new Pair<>(parseCursor(resp.data.cursor), list);
    }

    public static Pair<MessageCard.Cursor, List<MessageCard>> getReplyMsg(long id, long time) throws IOException, JSONException {
        String url = "https://api.bilibili.com/x/msgfeed/reply?platform=web&build=0&mobi_app=web";
        if (id > 0 && time > 0) url += "&id=" + id + "&reply_time=" + time;
        String json = NetWorkUtil.getJson(url).toString();
        ApiResponse<ReplyMsgData> resp = GsonUtil.fromJson(json, new com.google.gson.reflect.TypeToken<ApiResponse<ReplyMsgData>>(){}.getType());
        ArrayList<MessageCard> list = new ArrayList<>();
        if (resp == null || !resp.isSuccess() || resp.data == null || resp.data.items == null)
            return new Pair<>(null, list);

        for (ReplyMsgItem obj : resp.data.items) {
            if (obj == null) continue;
            MessageCard mc = new MessageCard();
            mc.id = obj.id; mc.timeStamp = obj.reply_time;
            mc.user = new ArrayList<>(); if (obj.user != null) mc.user.add(parseUser(obj.user));
            LikeItemData item = obj.item;
            if (item == null) { mc.getType = MessageCard.GET_TYPE_REPLY; list.add(mc); continue; }
            mc.businessId = item.business_id; mc.subjectId = item.subject_id; mc.sourceId = item.source_id;
            mc.rootId = item.root_id; mc.itemType = item.type; mc.targetId = item.target_id;
            mc.contentUri = item.uri;
            // 原版逻辑：action 文本显示 source_content（直接父级 C）
            mc.content = (item.source_content != null) ? item.source_content : item.title;
            mc.getType = MessageCard.GET_TYPE_REPLY;
            switch (item.type) {
                case "video": mc.videoCard = parseVideoCard(item); break;
                case "reply": Reply r = buildReply(item, false);
                    r.message = new SpannableString("[评论] " + item.title);
                    mc.replyInfo = r; break;
                case "dynamic": case "album": Reply rd = buildReply(item, true); rd.message = new SpannableString("[动态] " + item.title); mc.dynamicInfo = rd; break;
                case "article": Reply ra = new Reply(); ra.rpid = item.target_id; ra.message = new SpannableString("[专栏] " + item.title); ra.childCount = 0; mc.replyInfo = ra; break;
                default: mc.content = "无法识别这个类别：" + item.type;
            }
            list.add(mc);
        }
        return new Pair<>(parseCursor(resp.data.cursor), list);
    }

    public static Pair<MessageCard.Cursor, List<MessageCard>> getAtMsg(long id, long time) throws IOException, JSONException {
        String url = "https://api.bilibili.com/x/msgfeed/at?platform=web&build=0&mobi_app=web";
        if (id > 0 && time > 0) url += "&id=" + id + "&at_time=" + time;
        String json = NetWorkUtil.getJson(url).toString();
        ApiResponse<AtMsgData> resp = GsonUtil.fromJson(json, new com.google.gson.reflect.TypeToken<ApiResponse<AtMsgData>>(){}.getType());
        ArrayList<MessageCard> list = new ArrayList<>();
        if (resp == null || !resp.isSuccess() || resp.data == null || resp.data.items == null)
            return new Pair<>(null, list);

        for (ReplyMsgItem obj : resp.data.items) {
            if (obj == null) continue;
            MessageCard mc = new MessageCard();
            mc.id = obj.id; mc.timeStamp = obj.reply_time; mc.content = "提到了我";
            mc.user = new ArrayList<>(); if (obj.user != null) mc.user.add(parseUser(obj.user));
            LikeItemData item = obj.item;
            if (item == null) { mc.getType = MessageCard.GET_TYPE_AT; list.add(mc); continue; }
            mc.businessId = item.business_id; mc.subjectId = item.subject_id; mc.sourceId = item.source_id;
            mc.rootId = item.root_id; mc.itemType = item.type; mc.getType = MessageCard.GET_TYPE_AT;
            mc.contentUri = item.uri;
            switch (item.type) {
                case "video": mc.videoCard = parseVideoCard(item); break;
                case "reply": Reply r = buildReply(item, false);
                    r.message = new SpannableString("[评论] " + item.title);
                    mc.replyInfo = r; break;
                case "dynamic": Reply rd = buildReply(item, true); rd.message = new SpannableString("[动态] " + item.title); mc.dynamicInfo = rd; break;
                case "article": Reply ra = new Reply(); ra.rpid = item.target_id; ra.message = new SpannableString("[专栏] " + item.title); ra.childCount = 0; mc.replyInfo = ra; break;
            }
            list.add(mc);
        }
        return new Pair<>(parseCursor(resp.data.cursor), list);
    }

    public static ArrayList<MessageCard> getSystemMsg() throws IOException, JSONException {
        String url = "https://message.bilibili.com/x/sys-msg/query_user_notify?csrf=" + NetWorkUtil.getInfoFromCookie("bili_jct", SharedPreferencesUtil.getString(SharedPreferencesUtil.cookies, "")) + "&page_size=35&build=0&mobi_app=web";
        String json = NetWorkUtil.getJson(url).toString();
        ApiResponse<SystemMsgData> resp = GsonUtil.fromJson(json, new com.google.gson.reflect.TypeToken<ApiResponse<SystemMsgData>>(){}.getType());
        ArrayList<MessageCard> list = new ArrayList<>();
        if (resp == null || !resp.isSuccess() || resp.data == null || resp.data.system_notify_list == null) return list;
        for (SystemNotifyItem item : resp.data.system_notify_list) {
            if (item == null) continue;
            MessageCard mc = new MessageCard();
            mc.user = new ArrayList<>();
            mc.id = item.id; mc.timeDesc = item.time_at;
            mc.content = item.title + "\n" + item.content;
            list.add(mc);
        }
        return list;
    }

    public static JSONObject getMsgSettings() throws IOException, JSONException {
        String csrf = NetWorkUtil.getInfoFromCookie("bili_jct", SharedPreferencesUtil.getString(SharedPreferencesUtil.cookies, ""));
        Response response = NetWorkUtil.post("https://api.vc.bilibili.com/link_setting/v1/link_setting/get",
                new NetWorkUtil.FormData().put("msg_notify", 1).put("show_unfollowed_msg", 1).put("build", 0).put("mobi_app", "web").put("csrf_token", csrf).put("csrf", csrf).toString(), NetWorkUtil.webHeaders);
        return new JSONObject(response.body().string());
    }

    public static JSONObject setMsgSettings(JSONObject settings) throws IOException, JSONException {
        String csrf = NetWorkUtil.getInfoFromCookie("bili_jct", SharedPreferencesUtil.getString(SharedPreferencesUtil.cookies, ""));
        NetWorkUtil.FormData formData = new NetWorkUtil.FormData().put("csrf_token", csrf).put("csrf", csrf).put("build", 0).put("mobi_app", "web");
        String[] keys = {"msg_notify", "show_unfollowed_msg", "is_group_fold", "should_receive_group", "receive_unfollow_msg", "ai_intercept"};
        for (String key : keys) if (settings.has(key)) formData.put(key, settings.optInt(key, 0));
        Response response = NetWorkUtil.post("https://api.vc.bilibili.com/link_setting/v1/link_setting/set", formData.toString(), NetWorkUtil.webHeaders);
        return new JSONObject(response.body().string());
    }
}
