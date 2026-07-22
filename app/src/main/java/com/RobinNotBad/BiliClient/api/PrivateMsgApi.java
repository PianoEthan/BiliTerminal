package com.RobinNotBad.BiliClient.api;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.util.Log;

import com.RobinNotBad.BiliClient.model.PrivateMessage;
import com.RobinNotBad.BiliClient.model.PrivateMsgSession;
import com.RobinNotBad.BiliClient.model.UserInfo;
import com.RobinNotBad.BiliClient.util.EmoteUtil;
import com.RobinNotBad.BiliClient.util.MsgUtil;
import com.RobinNotBad.BiliClient.util.NetWorkUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import okhttp3.Response;

public class PrivateMsgApi {

    public static final int MSG_TYPE_TEXT = 1;
    public static final int MSG_TYPE_PIC = 2;
    public static final int MSG_TYPE_RETRACT = 5;

    // 返回的是倒序的消息列表，使用时记得列表倒置
    //seqno传0时只看size，不进行seqno的筛选
    public static JSONObject getPrivateMsg(long talkerId, int size, long beginSeqno, long endSeqno)
            throws IOException, JSONException {
        String url =
                "https://api.vc.bilibili.com/svr_sync/v1/svr_sync/fetch_session_msgs?session_type=1&talker_id=" + talkerId
                        + "&size=" + size
                        + "&begin_seqno=" + beginSeqno
                        + "&end_seqno=" + endSeqno;
        JSONObject allMsgJson = NetWorkUtil.getJson(url);
        if (allMsgJson.has("data") && !allMsgJson.isNull("data"))
            return allMsgJson.getJSONObject("data");
        else return new JSONObject();
    }

    public static ArrayList<PrivateMessage> getPrivateMsgList(JSONObject allMsgJson)
            throws IOException, JSONException {
        ArrayList<PrivateMessage> list = new ArrayList<>();
        if (!allMsgJson.isNull("messages")) {
            JSONArray messages = allMsgJson.getJSONArray("messages");
            UserInfo myInfo = UserInfoApi.getCurrentUserInfo();
            UserInfo targetInfo = new UserInfo();

            // list.add(new PrivateMessage(114514,1,new JSONObject("{\"content\":\"
            // 。\"}"),11111111,"aaaaa",111));

            boolean isReqTargetInfo = false;
            for (int i = 0; i < messages.length(); i++) {
                PrivateMessage msgObject = new PrivateMessage();
                JSONObject msgJson = messages.getJSONObject(i);
                msgObject.uid = msgJson.getLong("sender_uid");
                msgObject.type = msgJson.getInt("msg_type");
                if (msgObject.uid == myInfo.mid) {
                    msgObject.name = myInfo.name;
                } else {
                    if (!isReqTargetInfo) {
                        targetInfo = UserInfoApi.getUserInfo(msgObject.uid);
                        isReqTargetInfo = true;
                    }
                    if (targetInfo != null) msgObject.name = targetInfo.name;
                }
                msgObject.content = new JSONObject("{\"content\":\" .\"}"); // 防止内容不为json时解析错误
                if (msgJson.getString("content").endsWith("}")
                        && msgJson.getString("content").startsWith("{")) {
                    try{
                        msgObject.content =
                                new JSONObject(msgJson.getString("content") /*.replace("\\","")*/);
                    }catch (Exception ignore){}
                    try{
                    msgObject.content_array =
                            new JSONArray(msgJson.getString("content") /*.replace("\\","")*/);
                    }catch (Exception ignore){}
                }
                msgObject.timestamp = msgJson.getLong("timestamp");
                msgObject.msg_source = msgJson.getInt("msg_source");
                msgObject.msgId = msgJson.getLong("msg_key");
                msgObject.msgSeqno = msgJson.getLong("msg_seqno");
                list.add(msgObject);

            }
            Log.e("", "返回msgList");
            for (PrivateMessage i : list) {
                Log.e("msg",
                        i.name + "." + i.uid + "." + i.msgId + "." + i.timestamp + "." + i.content + "." + i.type);
            }
        }
        return list;
    }

    public static JSONArray getEmoteJsonArray(JSONObject allMsgJson) throws JSONException {
        if (allMsgJson.has("e_infos") && !allMsgJson.isNull("e_infos")) {
            return allMsgJson.getJSONArray("e_infos");
        } else {
            return new JSONArray();
        }
    }

    public static HashMap<Long, UserInfo> getUsersInfo(ArrayList<Long> uidList)
            throws IOException, JSONException {
        StringBuilder userString = new StringBuilder();
        for (Long uid : uidList) {
            userString.append(uid).append(",");
        }
        String url = "https://api.vc.bilibili.com/account/v1/user/cards?uids="
                + userString.substring(0, userString.length() - 1);
        HashMap<Long, UserInfo> userMap = new HashMap<>();
        JSONObject root = NetWorkUtil.getJson(url);
        if (root.has("data") && !root.isNull("data")) {
            JSONArray data = root.getJSONArray("data");
            for (int i = 0; i < data.length(); ++i) {
                JSONObject userJson = data.getJSONObject(i);
                UserInfo user = new UserInfo();
                user.mid = userJson.getLong("mid");
                user.name = userJson.getString("name");
                user.avatar = userJson.getString("face");
                Log.e("", user.mid + user.name + user.avatar);
                userMap.put(user.mid, user);
            }
        }
        return userMap;
    }

    /**
     * 获取私信会话列表（参照 PiliPlus getSessionSs 的解析逻辑）
     * 原 Bug：account_info 条件取反，导致有用户信息的会话全部被过滤
     */
    public static ArrayList<PrivateMsgSession> getSessionsList(int size)
            throws IOException, JSONException {
        String url =
                "https://api.vc.bilibili.com/session_svr/v1/session_svr/get_sessions?session_type=1&size=" + size;
        JSONObject root = NetWorkUtil.getJson(url);
        ArrayList<PrivateMsgSession> sessionList = new ArrayList<>();
        if (root.has("data") && !root.isNull("data")) {
            JSONObject data = root.getJSONObject("data");
            if (data.has("session_list") && !data.isNull("session_list")) {
                JSONArray sessions = data.getJSONArray("session_list");
                for (int i = 0; i < sessions.length(); ++i) {
                    PrivateMsgSession session = parseSession(sessions.getJSONObject(i));
                    sessionList.add(session);
                }
            }
        }
        return sessionList;
    }

    /**
     * 统一解析单个会话 JSON（参照 PiliPlus session_ss/data.dart）
     */
    private static PrivateMsgSession parseSession(JSONObject sessionJson) throws JSONException {
        PrivateMsgSession session = new PrivateMsgSession();
        session.talkerUid = sessionJson.getLong("talker_id");
        session.unread = sessionJson.optInt("unread_count", 0);
        session.maxSeqno = sessionJson.optLong("max_seqno", 0);
        session.ackSeqno = sessionJson.optLong("ack_seqno", 0);

        // 解析 account_info 获取用户信息（PiliPlus 风格：直接从 API 取）
        if (sessionJson.has("account_info") && !sessionJson.isNull("account_info")) {
            JSONObject accInfo = sessionJson.getJSONObject("account_info");
            session.talkerName = accInfo.optString("name", "");
            session.talkerFace = accInfo.optString("face", "");
        }

        // 解析最后一条消息
        if (!sessionJson.isNull("last_msg")) {
            JSONObject lastMsg = sessionJson.getJSONObject("last_msg");
            session.contentType = lastMsg.optInt("msg_type", 1);
            session.lastMsgTimestamp = lastMsg.optLong("timestamp", 0);
            String content = lastMsg.optString("content", "");
            if (!content.isEmpty() && content.endsWith("}") && content.startsWith("{")) {
                try {
                    session.content = new JSONObject(content);
                } catch (JSONException ignored) {}
            }
        }

        return session;
    }

    public static ArrayList<PrivateMsgSession> getNewSessionsList(long beginTs, int size, int build, String mobiApp)
            throws IOException, JSONException {
        StringBuilder url = new StringBuilder("https://api.vc.bilibili.com/session_svr/v1/session_svr/new_sessions?");
        if (beginTs > 0) {
            url.append("begin_ts=").append(beginTs).append("&");
        }
        if (size > 0) {
            url.append("size=").append(size).append("&");
        }
        if (build > 0) {
            url.append("build=").append(build).append("&");
        }
        if (mobiApp != null && !mobiApp.isEmpty()) {
            url.append("mobi_app=").append(mobiApp).append("&");
        }
        if (url.charAt(url.length() - 1) == '&') {
            url.setLength(url.length() - 1);
        }
        JSONObject root = NetWorkUtil.getJson(url.toString());
        ArrayList<PrivateMsgSession> sessionList = new ArrayList<>();
        if (root.has("data") && !root.isNull("data")) {
            JSONObject data = root.getJSONObject("data");
            if (data.has("session_list") && !data.isNull("session_list")) {
                JSONArray sessions = data.getJSONArray("session_list");
                for (int i = 0; i < sessions.length(); ++i) {
                    PrivateMsgSession session = parseSession(sessions.getJSONObject(i));
                    sessionList.add(session);
                }
            }
        }
        return sessionList;
    }

    public static JSONObject sendMsg(
            long senderUid, long receiverUid, int msgType, long timestamp, String content)
            throws IOException, JSONException {
        String url = "https://api.vc.bilibili.com/web_im/v1/web_im/send_msg?";
        String per =
                "msg[dev_id]=" + getDevId()
                        + "&msg[msg_type]=" + msgType
                        + "&msg[content]=" + content
                        + "&msg[receiver_type]=1&csrf=" + SharedPreferencesUtil.getString("csrf", "")
                        + "&msg[sender_uid]=" + senderUid
                        + "&msg[receiver_id]=" + receiverUid
                        + "&msg[timestamp]=" + timestamp;

        JSONObject result = new JSONObject(Objects.requireNonNull(NetWorkUtil.post(url, per, NetWorkUtil.webHeaders).body()).string());

        Log.e("debug-发送私信", result.toString());
        Log.e("debug-发送私信", NetWorkUtil.webHeaders.toString());
        return result;
    }

    public static JSONObject updateAck(long talkerId, int sessionType, long ackSeqno)
            throws IOException, JSONException {
        String url = "https://api.vc.bilibili.com/session_svr/v1/session_svr/update_ack";
        String csrf = SharedPreferencesUtil.getString("csrf", "");
        StringBuilder per = new StringBuilder();
        per.append("talker_id=").append(talkerId);
        per.append("&session_type=").append(sessionType);
        if (ackSeqno > 0) {
            per.append("&ack_seqno=").append(ackSeqno);
        }
        per.append("&csrf_token=").append(csrf);
        per.append("&csrf=").append(csrf);
        per.append("&build=0");
        per.append("&mobi_app=web");

        Response response = NetWorkUtil.post(url, per.toString(), NetWorkUtil.webHeaders);
        if (response.body() != null) {
            return new JSONObject(response.body().string());
        }
        return new JSONObject();
    }

    private static String getDevId() {
        char[] b = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] s = "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".toCharArray();
        for (int i = 0; i < s.length; i++) {
            if ('-' == s[i] || '4' == s[i]) {
                continue;
            }
            int randomInt = (int) (16 * Math.random());
            if ('x' == s[i]) {
                s[i] = b[randomInt];
            } else {
                s[i] = b[3 & randomInt | 8];
            }
        }
        return new String(s);
    }

    // 由于接口特殊性定制的textReplaceEmote
    public static SpannableStringBuilder textReplaceEmote(String text, JSONArray emote, float scale, Context context) throws JSONException, ExecutionException, InterruptedException {
        SpannableStringBuilder result = new SpannableStringBuilder(text);
        if (emote != null && emote.length() > 0) {
            for (int i = 0; i < emote.length(); i++) {    //遍历每一个表情包
                JSONObject key = emote.getJSONObject(i);

                String name = key.getString("text");
                String emoteUrl = key.getString("url");
                int size = key.getInt("size");  //B站十分贴心的帮你把表情包大小都写好了，快说谢谢蜀黍

                EmoteUtil.replaceSingle(result, name, emoteUrl, size, scale, context);
            }
        }
        return result;
    }
}
