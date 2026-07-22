package com.RobinNotBad.BiliClient.api;

import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.RobinNotBad.BiliClient.model.ApiResponse;
import com.RobinNotBad.BiliClient.model.ContentType;
import com.RobinNotBad.BiliClient.model.Reply;
import com.RobinNotBad.BiliClient.util.GsonUtil;
import com.RobinNotBad.BiliClient.util.Logu;
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

public class ReplyApi {

    /**
     * 下方 REPLY_TYPE_* 常量对应的是【消息通知 API】返回的 businessId 业务类型编码
     * （见 NoticeHolder 的跳转分类），并非评论区接口的 type 参数，请勿混用。
     * 消息通知 businessId：0=视频子评论 1=视频 11=动态子评论 12=专栏 17=动态
     */
    public static final int REPLY_TYPE_VIDEO_CHILD = 0;
    public static final int REPLY_TYPE_VIDEO = 1;
    public static final int REPLY_TYPE_ARTICLE = 12;
    public static final int REPLY_TYPE_DYNAMIC_CHILD = 11;
    public static final int REPLY_TYPE_DYNAMIC = 17;

    /**
     * 评论区接口（/x/v2/reply 与 /x/v2/reply/wbi/main）的 type 参数编码。
     * 用于区分评论挂在哪种内容下，调用评论 API 时统一使用这组常量，避免魔法数字。
     * 参考：1=视频 11=图文专栏 12=音频 17=动态 23=opus(新版专栏)
     */
    public static final int COMMENT_TYPE_VIDEO = 1;
    public static final int COMMENT_TYPE_ARTICLE = 11;
    public static final int COMMENT_TYPE_AUDIO = 12;
    public static final int COMMENT_TYPE_DYNAMIC = 17;

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

    /**
     * 评论加载结果，统一承载状态、下一页游标与 API 错误信息。
     * status 取值：
     *   {@link #STATUS_HAS_MORE} 有更多数据（nextOffset 有效）
     *   {@link #STATUS_END}      已到底，停止加载
     *   {@link #STATUS_API_ERROR} API 返回业务错误码（见 apiCode）
     *   {@link #STATUS_NET_ERROR} 网络异常或解析失败
     */
    public static class ReplyResult {
        public static final int STATUS_HAS_MORE = 0;
        public static final int STATUS_END = 1;
        public static final int STATUS_API_ERROR = -2;
        public static final int STATUS_NET_ERROR = -1;

        public int status;
        /** 下一页游标，仅当 status==STATUS_HAS_MORE 时有效 */
        public String nextOffset = "";
        /** B站 API 返回的业务码（code 字段），0 表示成功 */
        public int apiCode = 0;
        /** 可向用户展示的错误描述 */
        public String message = "";

        public boolean isSuccess() { return status == STATUS_HAS_MORE || status == STATUS_END; }
        public boolean isEnd() { return status == STATUS_END; }

        public static ReplyResult hasMore(String nextOffset) {
            ReplyResult r = new ReplyResult();
            r.status = STATUS_HAS_MORE;
            r.nextOffset = nextOffset == null ? "" : nextOffset;
            return r;
        }
        public static ReplyResult end() {
            ReplyResult r = new ReplyResult();
            r.status = STATUS_END;
            return r;
        }
        public static ReplyResult apiError(int code, String message) {
            ReplyResult r = new ReplyResult();
            r.status = STATUS_API_ERROR;
            r.apiCode = code;
            r.message = message;
            return r;
        }
        public static ReplyResult netError(String message) {
            ReplyResult r = new ReplyResult();
            r.status = STATUS_NET_ERROR;
            r.message = message;
            return r;
        }
    }

    public static int getReplies(long originId, long rpid, int pageNumber, ContentType type, int sort, List<Reply> replyArrayList) throws IOException, JSONException {
        String url = "https://api.bilibili.com/x/v2/reply" + (rpid == 0 ? "" : "/reply") + "?pn=" + pageNumber
                + "&type=" + type.getTypeCode() + "&oid=" + originId + "&sort=" + sort + (rpid == 0 ? "" : ("&root=" + rpid));
        String json = NetWorkUtil.getJson(url).toString();
        ApiResponse<ReplyListData> resp = GsonUtil.fromJson(json, new com.google.gson.reflect.TypeToken<ApiResponse<ReplyListData>>(){}.getType());
        if (resp == null || resp.data == null) return -1;
        //B站楼中楼接口对不存在的页码可能返回 -404 等，视为到底而非错误，否则为真正的 API 错误
        if (resp.code != 0) return (resp.code == -404) ? 1 : -1;

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
    public static ReplyResult getRepliesLazy(long oid, long rpid, String pagination, int type, int sort, List<Reply> replyArrayList, boolean useWbi) throws IOException, JSONException {
        NetWorkUtil.FormData reqData = new NetWorkUtil.FormData().setUrlParam(true)
                .put("type", type).put("oid", oid).put("plat", 1).put("web_location", "1315875").put("mode", sort);
        reqData.put("pagination_str", new JSONObject().put("offset", TextUtils.isEmpty(pagination) ? "" : pagination));
        if (rpid > 0) reqData.put("seek_rpid", rpid);
        //useWbi=true 走需 WBI 签名的 /x/v2/reply/wbi/main；useWbi=false 走无需签名的 /x/v2/reply/main 作为降级
        String path = useWbi ? "/x/v2/reply/wbi/main" : "/x/v2/reply/main";
        String url = "https://api.bilibili.com" + path + reqData;
        String finalUrl = useWbi ? ConfInfoApi.signWBI(url) : url;
        //直接用 JSONObject 解析，绕过 Gson 泛型/类型适配器偶发解析失败的问题（B站返回合法 JSON 但 Gson 可能返回 null）
        JSONObject root = NetWorkUtil.getJson(finalUrl);
        Logu.d("ReplyApi", "getRepliesLazy " + (useWbi ? "wbi" : "nowbi") + " type=" + type + " oid=" + oid + " sort=" + sort + " offset=" + pagination);

        int code = root.optInt("code", -1);
        if (code != 0) {
            String msg = root.optString("message", "");
            Logu.w("ReplyApi", "getRepliesLazy api error code=" + code + " msg=" + msg);
            return ReplyResult.apiError(code, msg);
        }
        JSONObject data = root.optJSONObject("data");
        if (data == null) return ReplyResult.netError("data 字段缺失");
        JSONObject cursor = data.optJSONObject("cursor");
        if (cursor == null) return ReplyResult.netError("cursor 字段缺失");

        boolean isBegin = cursor.optBoolean("is_begin", false);
        boolean isEnd = cursor.optBoolean("is_end", false);
        JSONObject paginationReply = cursor.optJSONObject("pagination_reply");
        String nextOffset = paginationReply != null ? paginationReply.optString("next_offset", null) : null;

        //replies 可能为 null（B站对该页无评论时只返回 cursor），用 optJSONArray 容错
        JSONArray replies = data.optJSONArray("replies");
        if (replies != null && replies.length() > 0) {
            if (rpid <= 0 && isBegin) {
                JSONArray topReplies = data.optJSONArray("top_replies");
                if (topReplies != null) analyzeReplyArray(true, topReplies, replyArrayList);
            }
            analyzeReplyArray(true, replies, replyArrayList);
        } else if (rpid <= 0 && isBegin) {
            JSONArray topReplies = data.optJSONArray("top_replies");
            if (topReplies != null) analyzeReplyArray(true, topReplies, replyArrayList);
        }
        if (isEnd || TextUtils.isEmpty(nextOffset)) return ReplyResult.end();
        return ReplyResult.hasMore(nextOffset);
    }

    /**单条路径的重试包装：网络/解析异常时指数退避重试。*/
    private static ReplyResult tryWithRetry(boolean useWbi, long oid, long rpid, String pagination, int type, int sort, List<Reply> replyArrayList) {
        int maxRetry = useWbi ? 3 : 1;
        for (int attempt = 0; ; attempt++) {
            try {
                return getRepliesLazy(oid, rpid, pagination, type, sort, replyArrayList, useWbi);
            } catch (Exception e) {
                Logu.w("ReplyApi", "getRepliesLazy(" + (useWbi ? "wbi" : "nowbi") + ") attempt " + (attempt + 1) + " failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                if (attempt >= maxRetry) {
                    return ReplyResult.netError(e.getClass().getSimpleName() + ": " + e.getMessage());
                }
                try {
                    Thread.sleep(1000L * (1L << attempt));   //指数退避：1s、2s、4s
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return ReplyResult.netError("请求被中断");
                }
            }
        }
    }

    /**
     * 带重试与降级的评论加载入口：
     * 1. 先走需 WBI 签名的 /x/v2/reply/wbi/main（主路径），网络异常时指数退避重试 3 次；
     * 2. 若 WBI 路径失败（签名校验失败/网络异常/解析失败），降级到无需签名的 /x/v2/reply/main 再试 1 次；
     * 3. 两条路径都失败时返回带详情的错误信息，便于上层提示与排查。
     * API 业务错误（如 -404/-101/12002）不重试不降级，直接返回。
     */
    @NonNull
    public static ReplyResult getRepliesLazyWithRetry(long oid, long rpid, String pagination, int type, int sort, List<Reply> replyArrayList) {
        ReplyResult result = tryWithRetry(true, oid, rpid, pagination, type, sort, replyArrayList);
        if (result.isSuccess()) return result;
        //WBI 路径失败：降级到无签名接口。先清空可能被部分填充的临时列表，避免降级返回时混入脏数据
        Logu.w("ReplyApi", "wbi 路径失败(status=" + result.status + ")，降级到无签名接口");
        replyArrayList.clear();
        ReplyResult fallback = tryWithRetry(false, oid, rpid, pagination, type, sort, replyArrayList);
        if (fallback.isSuccess()) return fallback;
        //两条路径都失败，合并错误详情（优先 WBI 路径信息）
        return ReplyResult.netError(result.message + " | 降级也失败：" + fallback.message);
    }

    /**
     * 基于 rpid 去重：过滤掉 alreadyLoaded 中已存在的评论，并把新增的 rpid 加入集合。
     * 作为游标分页的兜底，避免 B站 API 在游标边界返回重复评论。
     */
    public static void filterDuplicateReplies(List<Reply> source, java.util.Set<Long> alreadyLoaded) {
        if (source == null || alreadyLoaded == null) return;
        java.util.Iterator<Reply> it = source.iterator();
        while (it.hasNext()) {
            Reply r = it.next();
            if (r == null || r.rpid == 0 || alreadyLoaded.contains(r.rpid)) {
                it.remove();
            } else {
                alreadyLoaded.add(r.rpid);
            }
        }
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

    public static int likeReply(long oid, long root, boolean action) throws IOException, JSONException {
        //FIXME: 这里 type 硬编码为 1（视频），动态/专栏评论点赞时应传对应 type，否则会被服务端拒绝
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
