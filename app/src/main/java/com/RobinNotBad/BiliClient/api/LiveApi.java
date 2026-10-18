package com.RobinNotBad.BiliClient.api;

import com.RobinNotBad.BiliClient.model.ApiResponse;
import com.RobinNotBad.BiliClient.model.LivePlayInfo;
import com.RobinNotBad.BiliClient.model.LiveRoom;
import com.RobinNotBad.BiliClient.util.GsonUtil;
import com.RobinNotBad.BiliClient.util.NetWorkUtil;
import com.google.gson.annotations.SerializedName;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

public class LiveApi {
    public static final LinkedHashMap<String, Integer> QualityMap = new LinkedHashMap<>() {{
        put("流畅", 80); put("高清", 150); put("超清", 250); put("蓝光", 400); put("原画", 10000);
    }};

    public static class LiveRoomListData {
        @SerializedName("list")
        public List<LiveRoom> list;
        @SerializedName("rooms")
        public List<LiveRoom> rooms;
    }

    public static class LivePlayInfoData {
        @SerializedName("room_id")
        public long room_id;
        @SerializedName("short_id")
        public long short_id;
        @SerializedName("uid")
        public long uid;
        @SerializedName("is_hidden")
        public boolean is_hidden;
        @SerializedName("is_locked")
        public boolean is_locked;
        @SerializedName("is_portrait")
        public boolean is_portrait;
        @SerializedName("live_status")
        public int live_status;
        @SerializedName("encrypted")
        public boolean encrypted;
        @SerializedName("pwd_verified")
        public boolean pwd_verified;
        @SerializedName("live_time")
        public long live_time;
        @SerializedName("playurl_info")
        public PlayurlInfo playurl_info;
        @SerializedName("official_type")
        public int official_type;
        @SerializedName("official_room_id")
        public int official_room_id;
        @SerializedName("risk_with_delay")
        public int risk_with_delay;
    }

    public static class PlayurlInfo {
        @SerializedName("conf_json")
        public String conf_json;
        @SerializedName("playurl")
        public Playurl playurl;
    }

    public static class Playurl {
        @SerializedName("cid")
        public long cid;
        @SerializedName("g_qn_desc")
        public List<QnDesc> g_qn_desc;
        @SerializedName("stream")
        public List<ProtocolInfo> stream;
        @SerializedName("p2p_data")
        public P2PData p2p_data;
        @SerializedName("dolby_qn")
        public int dolby_qn;
    }

    public static class QnDesc {
        @SerializedName("qn") public int qn;
        @SerializedName("desc") public String desc;
        @SerializedName("hdr_desc") public String hdr_desc;
        @SerializedName("attr_desc") public String attr_desc;
    }

    public static class ProtocolInfo {
        @SerializedName("protocol_name") public String protocol_name;
        @SerializedName("format") public List<FormatInfo> format;
    }

    public static class FormatInfo {
        @SerializedName("format_name") public String format_name;
        @SerializedName("codec") public List<CodecInfo> codec;
        @SerializedName("master_url") public String master_url;
    }

    public static class CodecInfo {
        @SerializedName("codec_name") public String codec_name;
        @SerializedName("current_qn") public int current_qn;
        @SerializedName("accept_qn") public List<Integer> accept_qn;
        @SerializedName("base_url") public String base_url;
        @SerializedName("url_info") public List<UrlInfo> url_info;
        @SerializedName("hdr_qn") public int hdr_qn;
        @SerializedName("dolby_type") public int dolby_type;
        @SerializedName("attr_name") public String attr_name;
    }

    public static class UrlInfo {
        @SerializedName("host") public String host;
        @SerializedName("extra") public String extra;
        @SerializedName("stream_ttl") public int stream_ttl;
    }

    public static class P2PData {
        @SerializedName("p2p") public boolean p2p;
        @SerializedName("p2p_type") public int p2p_type;
        @SerializedName("m_p2p") public boolean m_p2p;
        @SerializedName("m_servers") public List<String> m_servers;
    }

    public static List<LiveRoom> getRecommend(int page) throws IOException, JSONException {
        String url = "https://api.live.bilibili.com/xlive/web-interface/v1/second/getUserRecommend" + new NetWorkUtil.FormData().setUrlParam(true).put("page", page).put("page_size", 10).put("platform", "web");
        String json = NetWorkUtil.getJson(ConfInfoApi.signWBI(url)).toString();
        ApiResponse<LiveRoomListData> resp = GsonUtil.fromJson(json, new com.google.gson.reflect.TypeToken<ApiResponse<LiveRoomListData>>(){}.getType());
        if (resp == null || !resp.isSuccess() || resp.data == null || resp.data.list == null) return null;
        return postProcessRooms(resp.data.list);
    }

    public static List<LiveRoom> getFollowed(int page) throws IOException, JSONException {
        String url = "https://api.live.bilibili.com/xlive/web-ucenter/v1/xfetter/GetWebList" + new NetWorkUtil.FormData().setUrlParam(true).put("page", page).put("page_size", 10);
        String json = NetWorkUtil.getJson(url).toString();
        ApiResponse<LiveRoomListData> resp = GsonUtil.fromJson(json, new com.google.gson.reflect.TypeToken<ApiResponse<LiveRoomListData>>(){}.getType());
        if (resp == null || !resp.isSuccess() || resp.data == null || resp.data.rooms == null) return null;
        return postProcessRooms(resp.data.rooms);
    }

    public static LiveRoom getRoomInfo(long room_id) throws IOException, JSONException {
        String url = "https://api.live.bilibili.com/room/v1/Room/get_info" + new NetWorkUtil.FormData().setUrlParam(true).put("room_id", room_id);
        String json = NetWorkUtil.getJson(url).toString();
        ApiResponse<LiveRoom> resp = GsonUtil.fromJson(json, new com.google.gson.reflect.TypeToken<ApiResponse<LiveRoom>>(){}.getType());
        if (resp == null || !resp.isSuccess() || resp.data == null) return null;
        List<LiveRoom> rooms = postProcessRooms(Collections.singletonList(resp.data));
        return !rooms.isEmpty() ? rooms.get(0) : null;
    }

    public static LivePlayInfo getRoomPlayInfo(long roomId, int qn) throws IOException, JSONException {
        String url = "https://api.live.bilibili.com/xlive/web-room/v2/index/getRoomPlayInfo" + new NetWorkUtil.FormData().setUrlParam(true)
                .put("room_id", roomId).put("qn", qn).put("protocol", "0,1").put("format", "0,1,2").put("codec", "0,1,2")
                .put("platform", "web").put("ptype", 8).put("dolby", 5).put("panorama", 1);
        String json = NetWorkUtil.getJson(url).toString();
        ApiResponse<LivePlayInfoData> resp = GsonUtil.fromJson(json, new com.google.gson.reflect.TypeToken<ApiResponse<LivePlayInfoData>>(){}.getType());
        if (resp == null || !resp.isSuccess() || resp.data == null) return null;
        return buildLivePlayInfo(resp.data);
    }

    private static List<LiveRoom> postProcessRooms(List<LiveRoom> rooms) {
        for (LiveRoom room : rooms) {
            if (room == null) continue;
            if (room.roomid <= 0) room.roomid = room.short_id;
        }
        return rooms;
    }

    public static List<LiveRoom> analyzeLiveRooms(JSONArray list) {
        List<LiveRoom> rooms = new ArrayList<>();
        for (int i = 0; i < list.length(); i++) {
            try {
                String json = list.optJSONObject(i).toString();
                LiveRoom room = GsonUtil.fromJson(json, LiveRoom.class);
                if (room != null) rooms.add(room);
            } catch (Exception ignored) {}
        }
        return postProcessRooms(rooms);
    }

    private static LivePlayInfo buildLivePlayInfo(LivePlayInfoData data) {
        LivePlayInfo info = new LivePlayInfo();
        info.roomid = data.room_id;
        info.short_id = data.short_id;
        info.uid = data.uid;
        info.isHidden = data.is_hidden;
        info.isLocked = data.is_locked;
        info.isPortrait = data.is_portrait;
        info.live_status = data.live_status;
        info.encrypted = data.encrypted;
        info.pwd_verified = data.pwd_verified;
        info.live_time = data.live_time;
        info.official_type = data.official_type;
        info.official_room_id = data.official_room_id;
        info.risk_with_delay = data.risk_with_delay;

        if (data.playurl_info != null) {
            info.conf_json = data.playurl_info.conf_json;
            if (data.playurl_info.playurl != null) {
                Playurl pu = data.playurl_info.playurl;
                LivePlayInfo.PlayUrl playUrl = new LivePlayInfo.PlayUrl();
                playUrl.cid = pu.cid;
                playUrl.dolby_qn = pu.dolby_qn;

                if (pu.g_qn_desc != null) {
                    playUrl.g_qn_desc = new ArrayList<>();
                    for (QnDesc q : pu.g_qn_desc) {
                        if (q == null) continue;
                        LivePlayInfo.QnDesc d = new LivePlayInfo.QnDesc();
                        d.qn = q.qn; d.desc = q.desc; d.hdr_desc = q.hdr_desc; d.attr_desc = q.attr_desc;
                        playUrl.g_qn_desc.add(d);
                    }
                }

                if (pu.stream != null) {
                    playUrl.stream = new ArrayList<>();
                    for (ProtocolInfo p : pu.stream) {
                        if (p == null) continue;
                        LivePlayInfo.ProtocolInfo pi = new LivePlayInfo.ProtocolInfo();
                        pi.protocol_name = p.protocol_name;
                        if (p.format != null) {
                            pi.format = new ArrayList<>();
                            for (FormatInfo f : p.format) {
                                if (f == null) continue;
                                LivePlayInfo.Format fi = new LivePlayInfo.Format();
                                fi.format_name = f.format_name;
                                fi.master_url = f.master_url;
                                if (f.codec != null) {
                                    fi.codec = new ArrayList<>();
                                    for (CodecInfo c : f.codec) {
                                        if (c == null) continue;
                                        LivePlayInfo.Codec ci = new LivePlayInfo.Codec();
                                        ci.codec_name = c.codec_name; ci.current_qn = c.current_qn;
                                        ci.accept_qn = c.accept_qn; ci.base_url = c.base_url;
                                        ci.hdr_qn = c.hdr_qn; ci.dolby_type = c.dolby_type; ci.attr_name = c.attr_name;
                                        if (c.url_info != null) {
                                            ci.url_info = new ArrayList<>();
                                            for (UrlInfo u : c.url_info) {
                                                if (u == null) continue;
                                                LivePlayInfo.UrlInfo ui = new LivePlayInfo.UrlInfo();
                                                ui.host = u.host; ui.extra = u.extra; ui.stream_ttl = u.stream_ttl;
                                                ci.url_info.add(ui);
                                            }
                                        }
                                        fi.codec.add(ci);
                                    }
                                }
                                pi.format.add(fi);
                            }
                        }
                        playUrl.stream.add(pi);
                    }
                }

                if (pu.p2p_data != null) {
                    LivePlayInfo.P2PData p2p = new LivePlayInfo.P2PData();
                    p2p.p2p = pu.p2p_data.p2p; p2p.p2p_type = pu.p2p_data.p2p_type; p2p.m_p2p = pu.p2p_data.m_p2p;
                    p2p.m_servers = pu.p2p_data.m_servers;
                    playUrl.p2p_data = p2p;
                }

                info.playUrl = playUrl;
            }
        }
        return info;
    }
}
