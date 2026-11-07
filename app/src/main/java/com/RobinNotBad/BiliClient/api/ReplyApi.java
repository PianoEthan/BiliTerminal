package com.RobinNotBad.BiliClient.api;

import android.util.Pair;

import com.RobinNotBad.BiliClient.model.ApiResponse;
import com.RobinNotBad.BiliClient.model.ContentType;
import com.RobinNotBad.BiliClient.model.Reply;
import com.RobinNotBad.BiliClient.util.CookieGenerator;
import com.RobinNotBad.BiliClient.util.GsonUtil;
import com.RobinNotBad.BiliClient.util.NetWorkUtil;
import com.RobinNotBad.BiliClient.util.Result;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;
import com.google.gson.annotations.SerializedName;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import androidx.core.util.ObjectsCompat;

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

    public static class ReplyCountData {
        @SerializedName("count")
        public long count;
    }

    public static final class ReplyPage {
        public final java.util.ArrayList<Reply> replies;
        public final boolean hasMore;
        public final long count;
        public final long upMid;

        private ReplyPage(java.util.ArrayList<Reply> replies, boolean hasMore, long count, long upMid) {
            this.replies = replies;
            this.hasMore = hasMore;
            this.count = count;
            this.upMid = upMid;
        }
    }

    private static String read(String url) throws IOException {
        try (okhttp3.Response response = NetWorkUtil.get(url)) {
            if (!response.isSuccessful()) throw new IOException("评论请求 HTTP " + response.code());
            if (response.body() == null) throw new IOException("评论响应为空");
            return response.body().string();
        }
    }

    public static ReplyPage getRepliesPage(long oid, long root, int page, int type, int sort) throws IOException, JSONException {
        ReplyPageContract.Page parsed = ReplyPageContract.parse(read(ReplyPageContract.url(oid, root, page, type, sort)), page, root);
        java.util.ArrayList<Reply> replies = new java.util.ArrayList<>();
        for (com.google.gson.JsonObject value : parsed.replies) {
            Reply reply = new Reply(root == 0, new JSONObject(value.toString()));
            if (reply.oid == 0) reply.oid = oid;
            reply.isDynamic = type == REPLY_TYPE_DYNAMIC || type == REPLY_TYPE_DYNAMIC_CHILD;
            replies.add(reply);
        }
        return new ReplyPage(replies, parsed.hasMore, parsed.count, parsed.upMid);
    }

    public static int getReplies(long oid, long root, int page, ContentType type, int sort, List<Reply> replies) throws IOException, JSONException {
        ReplyPage result = getRepliesPage(oid, root, page, type.getTypeCode(), sort);
        appendUnique(replies, result.replies);
        return result.hasMore ? 0 : 1;
    }

    public static int appendUnique(List<Reply> target, List<Reply> incoming) {
        java.util.HashSet<Long> ids = new java.util.HashSet<>();
        for (Reply reply : target) ids.add(reply.rpid);
        int start = target.size();
        for (Reply reply : incoming) {
            if (ids.add(reply.rpid)) target.add(reply);
        }
        return target.size() - start;
    }

    public static Result<Reply> getRootReply(ContentType type, long oid, long rpid) {
        try {
            com.google.gson.JsonElement root = ReplyPageContract.data(read(ReplyPageContract.detailUrl(oid, rpid, type.getTypeCode()))).get("root");
            if (root == null || !root.isJsonObject()) return Result.failure(new ReplyPageContract.ApiException("未找到根评论"));
            Reply reply = new Reply(true, new JSONObject(root.toString()));
            if (reply.oid == 0) reply.oid = oid;
            return Result.success(reply);
        } catch (Exception e) {
            return Result.failure(e);
        }
    }

    public static Pair<Integer, Reply> sendReply(long oid, long root, long parent, String text, int type) throws IOException, JSONException {
        String url = "https://api.bilibili.com/x/v2/reply/add";
        String arg = "oid=" + oid + "&type=" + type + (root == 0 ? "" : ("&root=" + root + "&parent=" + parent))
                + "&message=" + text + "&jsonp=jsonp&csrf=" + SharedPreferencesUtil.getString("csrf", "");
        JSONObject result = new JSONObject(ObjectsCompat.requireNonNull(NetWorkUtil.post(url, arg, NetWorkUtil.webHeaders).body()).string());
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
            RequestBody fileBody = RequestBody.create(ObjectsCompat.requireNonNull(MediaType.parse("image/jpeg")), imageData);
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
        JSONObject result = new JSONObject(ObjectsCompat.requireNonNull(NetWorkUtil.post(url, arg, NetWorkUtil.webHeaders).body()).string());
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
        return new JSONObject(ObjectsCompat.requireNonNull(NetWorkUtil.post(url, arg, NetWorkUtil.webHeaders).body()).string()).optInt("code", -1);
    }

    public static int deleteReply(long oid, long rpid, int type) throws IOException, JSONException {
        String url = "https://api.bilibili.com/x/v2/reply/del";
        String reqBody = new NetWorkUtil.FormData().put("type", type).put("oid", oid).put("rpid", rpid).put("csrf", SharedPreferencesUtil.getString("csrf", "")).toString();
        return new JSONObject(ObjectsCompat.requireNonNull(NetWorkUtil.post(url, reqBody, NetWorkUtil.webHeaders).body()).string()).optInt("code", -1);
    }

    public static long getReplyCount(long oid, int type) throws IOException, JSONException {
        String json = NetWorkUtil.getJson("https://api.bilibili.com/x/v2/reply/count?oid=" + oid + "&type=" + type).toString();
        ApiResponse<ReplyCountData> resp = GsonUtil.fromJson(json, new com.google.gson.reflect.TypeToken<ApiResponse<ReplyCountData>>(){}.getType());
        return (resp != null && resp.data != null) ? resp.data.count : 0;
    }
}
