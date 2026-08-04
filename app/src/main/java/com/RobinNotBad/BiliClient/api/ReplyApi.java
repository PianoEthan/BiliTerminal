package com.RobinNotBad.BiliClient.api;

import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.RobinNotBad.BiliClient.model.ApiResponse;
import com.RobinNotBad.BiliClient.model.ContentType;
import com.RobinNotBad.BiliClient.model.Reply;
import com.RobinNotBad.BiliClient.util.CookieGenerator;
import com.RobinNotBad.BiliClient.util.GsonUtil;
import com.RobinNotBad.BiliClient.util.NetWorkUtil;
import com.RobinNotBad.BiliClient.util.Result;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;
import com.google.gson.annotations.SerializedName;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class ReplyApi {

    public static final int REPLY_TYPE_VIDEO_CHILD = 0;
    public static final int REPLY_TYPE_VIDEO = 1;
    public static final int REPLY_TYPE_ARTICLE = 12;
    public static final int REPLY_TYPE_DYNAMIC_CHILD = 11;
    public static final int REPLY_TYPE_DYNAMIC = 17;
    public static final String TOP_TIP = "[置顶]";

    public static class ReplyListData {
        @SerializedName("replies")
        public JsonArray replies;
        @SerializedName("top_replies")
        public JsonArray top_replies;
        @SerializedName("page")
        public PageData page;
    }

    public static class PageData {
        @SerializedName("size")
        public int size;
        @SerializedName("num")
        public int num;
    }

    public static class ReplyLazyData {
        @SerializedName("replies")
        public JsonArray replies;
        @SerializedName("top_replies")
        public JsonArray top_replies;
        @SerializedName("cursor")
        public CursorData cursor;
    }

    public static class CursorData {
        @SerializedName("is_begin")
        public boolean is_begin;
        @SerializedName("is_end")
        public boolean is_end;
        @SerializedName("pagination_reply")
        public PaginationReply pagination_reply;
    }

    public static class PaginationReply {
        @SerializedName("next_offset")
        public String next_offset;
    }

    public static class ReplyRootData {
        @SerializedName("root")
        public JsonElement root;
    }

    public static class ReplyCountData {
        @SerializedName("count")
        public long count;
    }

    public static int getReplies(long originId, long rpid, int pageNumber, ContentType type, int sort, List<Reply> replyArrayList) throws IOException, JSONException {
        String url = "https://api.bilibili.com/x/v2/reply" + (rpid == 0 ? "" : "/reply") + "?pn=" + pageNumber
                + "&type=" + type.getTypeCode() + "&oid=" + originId + "&sort=" + sort + (rpid == 0 ? "" : ("&root=" + rpid));
        String json = NetWorkUtil.getJson(url).toString();
        ApiResponse<ReplyListData> resp = GsonUtil.fromJson(json, new com.google.gson.reflect.TypeToken<ApiResponse<ReplyListData>>(){}.getType());
        if (resp == null || resp.code != 0 || resp.data == null) return -1;

        int size = replyArrayList.size();
        ReplyListData data = resp.data;
        if (data.replies == null || data.page == null || data.page.size <= 0) return 1;

        if (rpid == 0 && data.top_replies != null && data.page.num == 1) {
            analyzeReplyArray(true, data.top_replies, replyArrayList);
        }
        analyzeReplyArray(rpid == 0, data.replies, replyArrayList);
        return replyArrayList.size() == size ? 1 : 0;
    }

    public static Result<Reply> getRootReply(ContentType contentType, long originId, long rpid) {
        String url = "https://api.bilibili.com/x/v2/reply/reply?type=" + contentType.getTypeCode() + "&oid=" + originId + "&root=" + rpid;
        try {
            String json = NetWorkUtil.getJson(url).toString();
            ApiResponse<ReplyRootData> resp = GsonUtil.fromJson(json, new com.google.gson.reflect.TypeToken<ApiResponse<ReplyRootData>>(){}.getType());
            if (resp == null || resp.code != 0 || resp.data == null || resp.data.root == null)
                return Result.failure(new Exception("未找到根评论"));
            JSONObject rootJson = new JSONObject(resp.data.root.toString());
            return Result.success(new Reply(true, rootJson));
        } catch (Exception e) { return Result.failure(e); }
    }

    @NonNull
    public static Pair<Integer, String> getRepliesLazy(long oid, long rpid, String pagination, int type, int sort, List<Reply> replyArrayList) throws IOException, JSONException {
        NetWorkUtil.FormData reqData = new NetWorkUtil.FormData().setUrlParam(true)
                .put("type", type).put("oid", oid).put("plat", 1).put("web_location", "1315875").put("mode", sort);
        reqData.put("pagination_str", new JSONObject().put("offset", TextUtils.isEmpty(pagination) ? "" : pagination));
        if (rpid > 0) reqData.put("seek_rpid", rpid);
        String url = "https://api.bilibili.com/x/v2/reply/wbi/main" + reqData;
        String json = NetWorkUtil.getJson(ConfInfoApi.signWBI(url)).toString();
        ApiResponse<ReplyLazyData> resp = GsonUtil.fromJson(json, new com.google.gson.reflect.TypeToken<ApiResponse<ReplyLazyData>>(){}.getType());
        if (resp == null || resp.code != 0 || resp.data == null) return new Pair<>(-1, "");

        ReplyLazyData data = resp.data;
        CursorData cursor = data.cursor;
        if (cursor == null) return new Pair<>(-1, "");

        if (data.replies != null && data.replies.size() > 0) {
            if (rpid <= 0 && data.top_replies != null && cursor.is_begin)
                analyzeReplyArray(true, data.top_replies, replyArrayList);
            analyzeReplyArray(true, data.replies, replyArrayList);
            String nextOffset = cursor.pagination_reply != null ? cursor.pagination_reply.next_offset : null;
            if (cursor.is_end || TextUtils.isEmpty(nextOffset)) return new Pair<>(1, "");
            return new Pair<>(0, nextOffset);
        } else if (rpid <= 0 && data.top_replies != null && cursor.is_begin) {
            analyzeReplyArray(true, data.top_replies, replyArrayList);
            return new Pair<>(1, "");
        }
        return new Pair<>(1, "");
    }

    public static void analyzeReplyArray(boolean isRoot, JSONArray replies, List<Reply> replyArrayList) {
        for (int i = 0; i < replies.length(); i++) {
            try {
                JSONObject reply = replies.optJSONObject(i);
                if (reply != null) replyArrayList.add(new Reply(isRoot, reply));
            } catch (Exception e) {
                Log.w("ReplyApi", "Failed to parse reply at index " + i + ": " + e.getMessage());
            }
        }
    }

    public static void analyzeReplyArray(boolean isRoot, JsonArray replies, List<Reply> replyArrayList) {
        for (int i = 0; i < replies.size(); i++) {
            try {
                JSONObject reply = new JSONObject(replies.get(i).toString());
                replyArrayList.add(new Reply(isRoot, reply));
            } catch (Exception e) {
                Log.w("ReplyApi", "Failed to parse reply at index " + i + ": " + e.getMessage());
            }
        }
    }

    public static Pair<Integer, Reply> sendReply(long oid, long root, long parent, String text, int type) throws IOException, JSONException {
        String url = "https://api.bilibili.com/x/v2/reply/add";
        String arg = "oid=" + oid + "&type=" + type + (root == 0 ? "" : ("&root=" + root + "&parent=" + parent))
                + "&message=" + text + "&jsonp=jsonp&csrf=" + SharedPreferencesUtil.getString("csrf", "");
        JSONObject result = new JSONObject(Objects.requireNonNull(NetWorkUtil.post(url, arg, NetWorkUtil.webHeaders).body()).string());
        int code = result.optInt("code", -1);
        JSONObject data = result.optJSONObject("data");
        JSONObject replyJson = data != null ? data.optJSONObject("reply") : null;
        Reply replyResult = null;
        try { if (replyJson != null) replyResult = new Reply(root != 0, replyJson); } catch (Exception ignored) {}
        return new Pair<>(code, replyResult);
    }

    public static Pair<Integer, Reply> sendReply(long oid, long root, long parent, String text) throws IOException, JSONException {
        return sendReply(oid, root, parent, text, REPLY_TYPE_VIDEO);
    }

    public static Pair<Integer, Reply> sendDynamicReply(long oid, long root, long parent, String text) throws IOException, JSONException {
        return sendReply(oid, root, parent, text, REPLY_TYPE_DYNAMIC);
    }

    public static class UploadImageData {
        @SerializedName("image_url")
        public String image_url;
        @SerializedName("image_width")
        public int image_width;
        @SerializedName("image_height")
        public int image_height;
        @SerializedName("img_size")
        public long img_size;
    }

    public static Result<UploadImageData> uploadReplyImage(byte[] imageData, String fileName) {
        String url = "https://api.bilibili.com/x/dynamic/feed/draw/upload_bfs";
        String csrf = SharedPreferencesUtil.getString("csrf", "");
        try {
            RequestBody fileBody = RequestBody.create(Objects.requireNonNull(MediaType.parse("image/jpeg")), imageData);
            MultipartBody multipartBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file_up", fileName, fileBody)
                    .addFormDataPart("category", "daily")
                    .addFormDataPart("biz", "new_dyn")
                    .addFormDataPart("csrf", csrf)
                    .build();

            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(url)
                    .post(multipartBody)
                    .addHeader("Cookie", CookieGenerator.getCookieString(true))
                    .addHeader("Referer", "https://www.bilibili.com/")
                    .addHeader("Origin", "https://www.bilibili.com")
                    .addHeader("User-Agent", NetWorkUtil.USER_AGENT_WEB)
                    .addHeader("Accept", "application/json, text/plain, */*")
                    .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .build();

            String json = NetWorkUtil.getOkHttpInstance().newCall(request).execute().body().string();
            ApiResponse<UploadImageData> resp = GsonUtil.fromJson(json, new com.google.gson.reflect.TypeToken<ApiResponse<UploadImageData>>(){}.getType());
            if (resp == null || resp.code != 0 || resp.data == null)
                return Result.failure(new Exception("上传图片失败，code=" + (resp == null ? "null" : resp.code)));
            return Result.success(resp.data);
        } catch (Exception e) {
            return Result.failure(e);
        }
    }

    public static Pair<Integer, Reply> sendReply(long oid, long root, long parent, String text, int type, String pictures) throws IOException, JSONException {
        String url = "https://api.bilibili.com/x/v2/reply/add";
        String arg = "oid=" + oid + "&type=" + type + (root == 0 ? "" : ("&root=" + root + "&parent=" + parent))
                + "&message=" + java.net.URLEncoder.encode(text, "UTF-8")
                + (pictures.isEmpty() ? "" : ("&pictures=" + java.net.URLEncoder.encode(pictures, "UTF-8")))
                + "&jsonp=jsonp&csrf=" + SharedPreferencesUtil.getString("csrf", "");
        JSONObject result = new JSONObject(Objects.requireNonNull(NetWorkUtil.post(url, arg, NetWorkUtil.webHeaders).body()).string());
        int code = result.optInt("code", -1);
        JSONObject data = result.optJSONObject("data");
        JSONObject replyJson = data != null ? data.optJSONObject("reply") : null;
        Reply replyResult = null;
        try { if (replyJson != null) replyResult = new Reply(root != 0, replyJson); } catch (Exception ignored) {}
        return new Pair<>(code, replyResult);
    }

    public static int likeReply(long oid, long root, boolean action) throws IOException, JSONException {
        String url = "https://api.bilibili.com/x/v2/reply/action";
        String arg = "oid=" + oid + "&type=1&rpid=" + root + "&action=" + (action ? "1" : "0") + "&jsonp=jsonp&csrf=" + SharedPreferencesUtil.getString("csrf", "");
        return new JSONObject(Objects.requireNonNull(NetWorkUtil.post(url, arg, NetWorkUtil.webHeaders).body()).string()).optInt("code", -1);
    }

    public static int deleteReply(long oid, long rpid, int type) throws IOException, JSONException {
        String url = "https://api.bilibili.com/x/v2/reply/del";
        String reqBody = new NetWorkUtil.FormData().put("type", type).put("oid", oid).put("rpid", rpid).put("csrf", SharedPreferencesUtil.getString("csrf", "")).toString();
        return new JSONObject(Objects.requireNonNull(NetWorkUtil.post(url, reqBody, NetWorkUtil.webHeaders).body()).string()).optInt("code", -1);
    }

    public static long getReplyCount(long oid, int type) throws IOException, JSONException {
        String json = NetWorkUtil.getJson("https://api.bilibili.com/x/v2/reply/count?oid=" + oid + "&type=" + type).toString();
        ApiResponse<ReplyCountData> resp = GsonUtil.fromJson(json, new com.google.gson.reflect.TypeToken<ApiResponse<ReplyCountData>>(){}.getType());
        return (resp != null && resp.data != null) ? resp.data.count : 0;
    }
}
