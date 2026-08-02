package com.RobinNotBad.BiliClient.api;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;

import androidx.core.content.FileProvider;

import com.RobinNotBad.BiliClient.BiliTerminal;
import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.player.PlayerActivity;
import com.RobinNotBad.BiliClient.activity.settings.SettingPlayerChooseActivity;
import com.RobinNotBad.BiliClient.activity.video.JumpToPlayerActivity;
import com.RobinNotBad.BiliClient.model.ApiResponse;
import com.RobinNotBad.BiliClient.model.DashAudioStream;
import com.RobinNotBad.BiliClient.model.DashData;
import com.RobinNotBad.BiliClient.model.DashVideoStream;
import com.RobinNotBad.BiliClient.model.HighEnergyData;
import com.RobinNotBad.BiliClient.model.PlayerData;
import com.RobinNotBad.BiliClient.model.Subtitle;
import com.RobinNotBad.BiliClient.model.SubtitleLink;
import com.RobinNotBad.BiliClient.model.VideoInfo;
import com.RobinNotBad.BiliClient.service.DownloadService;
import com.RobinNotBad.BiliClient.util.FileUtil;
import com.RobinNotBad.BiliClient.util.GsonUtil;
import com.RobinNotBad.BiliClient.util.Logu;
import com.RobinNotBad.BiliClient.util.NetWorkUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;
import com.RobinNotBad.BiliClient.util.ToolsUtil;
import com.google.gson.annotations.SerializedName;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerApi {

    public static class PlayUrlData {
        @SerializedName("durl") public List<DurlItem> durl;
        @SerializedName("dash") public DashData dash;
        @SerializedName("last_play_cid") public long last_play_cid;
        @SerializedName("last_play_time") public int last_play_time;
        @SerializedName("accept_description") public List<String> accept_description;
        @SerializedName("accept_quality") public List<Integer> accept_quality;
    }

    public static class DurlItem {
        @SerializedName("url") public String url;
    }

    public static class PlayUrlResult {
        @SerializedName("result") public PlayUrlData result;
    }

    public static class SubtitleLinkData {
        @SerializedName("data") public SubtitleDataInner data;
    }
    public static class SubtitleDataInner {
        @SerializedName("subtitle") public SubtitleInner subtitle;
        @SerializedName("interaction") public InteractionData interaction;
        @SerializedName("view_points") public List<ViewPointData> view_points;
    }
    public static class SubtitleInner {
        @SerializedName("subtitles") public List<SubtitleItem> subtitles;
    }
    public static class SubtitleItem {
        @SerializedName("id") public long id;
        @SerializedName("type") public int type;
        @SerializedName("lan_doc") public String lan_doc;
        @SerializedName("subtitle_url") public String subtitle_url;
    }
    public static class InteractionData {
        @SerializedName("graph_version") public long graph_version;
    }
    public static class ViewPointData {
        @SerializedName("content") public String content;
        @SerializedName("from") public int from;
        @SerializedName("to") public int to;
        @SerializedName("type") public int type;
        @SerializedName("imgUrl") public String imgUrl;
        @SerializedName("logoUrl") public String logoUrl;
    }

    public static class SubtitleBody {
        @SerializedName("body") public List<SubtitleEntry> body;
    }
    public static class SubtitleEntry {
        @SerializedName("content") public String content;
        @SerializedName("from") public double from;
        @SerializedName("to") public double to;
    }

    public static void startGettingUrl(PlayerData playerData) {
        startGettingUrl(playerData, null);
    }

    public static void startGettingUrl(PlayerData playerData, String cover) {
        Context context = BiliTerminal.context;
        Intent intent = new Intent().setClass(context, JumpToPlayerActivity.class)
                .putExtra("data", playerData)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (cover != null) intent.putExtra("cover", cover);
        context.startActivity(intent);
    }

    public static void startDownloading(VideoInfo videoInfo, int page, int qn) {
        if (SharedPreferencesUtil.getBoolean("dev_download_old", false)) {
            Context context = BiliTerminal.context;
            context.startActivity(new Intent(context, JumpToPlayerActivity.class).putExtra("data", videoInfo.toPlayerData(page)).putExtra("download", videoInfo.pagenames.size() == 1 ? 1 : 2).putExtra("cover", videoInfo.cover).putExtra("parent_title", videoInfo.title).putExtra("qn", qn).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        }
        if (videoInfo.cids.size() == 1) DownloadService.startDownload(videoInfo.title, videoInfo.aid, videoInfo.cids.get(0), videoInfo.cover, qn, "video", "");
        else DownloadService.startDownload(videoInfo.title, videoInfo.pagenames.get(page), videoInfo.aid, videoInfo.cids.get(page), videoInfo.cover, qn, "video", "");
    }

    public static void startDownloadingAudioOnly(VideoInfo videoInfo, int page, int qn, String audioUrl) {
        if (videoInfo.cids.size() == 1) DownloadService.startDownload(videoInfo.title, videoInfo.aid, videoInfo.cids.get(0), videoInfo.cover, qn, "audio_only", audioUrl);
        else DownloadService.startDownload(videoInfo.title, videoInfo.pagenames.get(page), videoInfo.aid, videoInfo.cids.get(page), videoInfo.cover, qn, "audio_only", audioUrl);
    }

    public static void getVideoDash(PlayerData playerData) throws IOException, JSONException {
        playerData.danmakuUrl = "https://comment.bilibili.com/" + playerData.cid + ".xml";
        String url = "https://api.bilibili.com/x/player/wbi/playurl?avid=" + playerData.aid + "&cid=" + playerData.cid + "&qn=" + playerData.qn + "&fnval=16&fnver=0&platform=pc&voice_balance=1&gaia_source=pre-load&isGaiaAvoided=true";
        String json = NetWorkUtil.getJson(ConfInfoApi.signWBI(url), NetWorkUtil.webHeaders).toString();
        ApiResponse<PlayUrlData> resp = GsonUtil.fromJson(json, new com.google.gson.reflect.TypeToken<ApiResponse<PlayUrlData>>(){}.getType());
        if (resp == null || !resp.isSuccess() || resp.data == null) throw new JSONException("获取播放地址失败");
        PlayUrlData data = resp.data;

        if (data.dash != null) {
            playerData.dashData = data.dash;
            DashVideoStream vs = playerData.dashData.getVideoStream(playerData.qn);
            if (vs != null) playerData.videoUrl = vs.baseUrl;
            DashAudioStream as = playerData.dashData.getBestAudioStream();
            if (as != null) playerData.audioUrl = as.baseUrl;
        } else {
            getVideo(playerData, true);
            return;
        }

        playerData.cidHistory = data.last_play_cid;
        playerData.progress = data.last_play_time;
        if (playerData.cidHistory == 0) { playerData.cidHistory = playerData.cid; playerData.progress = 0; }

        if (data.accept_description != null && data.accept_quality != null) {
            String[] qnStrList = data.accept_description.toArray(new String[0]);
            int[] qnValueList = new int[data.accept_quality.size()];
            for (int i = 0; i < qnValueList.length; i++) qnValueList[i] = data.accept_quality.get(i);
            playerData.qnStrList = qnStrList; playerData.qnValueList = qnValueList;
        }
    }

    public static void getVideo(PlayerData playerData, boolean download) throws IOException, JSONException {
        if (System.currentTimeMillis() - playerData.timeStamp < 600000) return;
        playerData.timeStamp = System.currentTimeMillis();
        playerData.danmakuUrl = "https://comment.bilibili.com/" + playerData.cid + ".xml";
        boolean html5 = !download && "mtvPlayer".equals(SharedPreferencesUtil.getString("player", ""));
        String url = "https://api.bilibili.com/x/player/wbi/playurl?avid=" + playerData.aid + "&cid=" + playerData.cid + (html5 ? "&high_quality=1" : "") + "&qn=" + playerData.qn + "&fnval=1&fnver=0&platform=" + (html5 ? "html5" : "pc") + "&voice_balance=1&gaia_source=pre-load&isGaiaAvoided=true";
        String json = NetWorkUtil.getJson(ConfInfoApi.signWBI(url), NetWorkUtil.webHeaders).toString();
        ApiResponse<PlayUrlData> resp = GsonUtil.fromJson(json, new com.google.gson.reflect.TypeToken<ApiResponse<PlayUrlData>>(){}.getType());
        if (resp == null || !resp.isSuccess() || resp.data == null) throw new JSONException("获取播放地址失败");
        PlayUrlData data = resp.data;
        if (data.durl == null || data.durl.isEmpty()) throw new JSONException("durl is empty");
        playerData.videoUrl = data.durl.get(0).url;
        playerData.cidHistory = data.last_play_cid; playerData.progress = data.last_play_time;
        if (playerData.cidHistory == 0) { playerData.cidHistory = playerData.cid; playerData.progress = 0; }
        if (data.accept_description != null && data.accept_quality != null) {
            playerData.qnStrList = data.accept_description.toArray(new String[0]);
            int[] qnValueList = new int[data.accept_quality.size()];
            for (int i = 0; i < qnValueList.length; i++) qnValueList[i] = data.accept_quality.get(i);
            playerData.qnValueList = qnValueList;
        }
    }

    public static void getBangumi(PlayerData playerData) throws IOException, JSONException {
        String session = ToolsUtil.md5(String.valueOf(System.currentTimeMillis() - SystemClock.currentThreadTimeMillis()));
        String url = "https://api.bilibili.com/pgc/player/web/playurl" + new NetWorkUtil.FormData().setUrlParam(true)
                .put("aid", playerData.aid).put("cid", playerData.cid).put("fnval", 1).put("fnvar", 0).put("qn", playerData.qn).put("season_type", 1).put("session", session).put("platform", "pc");
        String json = NetWorkUtil.getJson(url).toString();
        PlayUrlResult result = GsonUtil.fromJson(json, PlayUrlResult.class);
        if (result == null || result.result == null || result.result.durl == null || result.result.durl.isEmpty())
            throw new JSONException("获取番剧播放地址失败");
        playerData.videoUrl = result.result.durl.get(0).url;
        playerData.danmakuUrl = "https://comment.bilibili.com/" + playerData.cid + ".xml";
        if (result.result.accept_description != null && result.result.accept_quality != null) {
            playerData.qnStrList = result.result.accept_description.toArray(new String[0]);
            int[] qnValueList = new int[result.result.accept_quality.size()];
            for (int i = 0; i < qnValueList.length; i++) qnValueList[i] = result.result.accept_quality.get(i);
            playerData.qnValueList = qnValueList;
        }
    }

    public static Intent jumpToPlayer(PlayerData playerData) {
        Context context = BiliTerminal.context;
        Intent intent = new Intent();
        switch (SharedPreferencesUtil.getString("player", "null")) {
            case "terminalPlayer":
                intent.setClass(context, PlayerActivity.class);
                intent.putExtra("url", playerData.videoUrl).putExtra("danmaku", playerData.danmakuUrl).putExtra("title", playerData.title).putExtra("aid", playerData.aid).putExtra("cid", playerData.cid).putExtra("mid", playerData.mid).putExtra("progress", playerData.progress).putExtra("live_mode", playerData.isLive());
                if (playerData.qnStrList != null && playerData.qnValueList != null) { intent.putExtra("qnStrList", playerData.qnStrList).putExtra("qnValueList", playerData.qnValueList).putExtra("currentQuality", playerData.qn); }
                if (playerData.pagenames != null && playerData.cids != null && playerData.pagenames.size() > 1) {
                    intent.putStringArrayListExtra("pagenames", playerData.pagenames);
                    long[] cidArray = new long[playerData.cids.size()]; for (int i = 0; i < playerData.cids.size(); i++) cidArray[i] = playerData.cids.get(i);
                    intent.putExtra("cids", cidArray).putExtra("currentPageIndex", playerData.currentPageIndex);
                }
                break;
            case "mtvPlayer":
                intent.setClassName(context.getString(R.string.player_package_mtv), "com.xinxiangshicheng.wearbiliplayer.cn.player.PlayerActivity");
                intent.setAction(Intent.ACTION_VIEW).putExtra("cookie", SharedPreferencesUtil.getString("cookies", "")).putExtra("mode", playerData.isLocal() ? "2" : "0").putExtra("url", playerData.videoUrl).putExtra("danmaku", playerData.danmakuUrl).putExtra("title", playerData.title).putExtra("live_mode", playerData.isLive());
                break;
            case "aliangPlayer":
                intent.setClassName(context.getString(R.string.player_package_aliang), "com.aliangmaker.media.PlayVideoActivity");
                intent.putExtra("name", playerData.title).putExtra("danmaku", playerData.danmakuUrl).putExtra("live_mode", playerData.isLive());
                intent.setData(Uri.parse(playerData.videoUrl));
                if (!playerData.isLocal()) {
                    Map<String, String> headers = new HashMap<>(); headers.put("Cookie", SharedPreferencesUtil.getString("cookies", "")); headers.put("Referer", "https://www.bilibili.com/");
                    intent.putExtra("cookie", (Serializable) headers).putExtra("agent", NetWorkUtil.USER_AGENT_WEB).putExtra("progress", playerData.progress * 1000L);
                }
                intent.setAction(Intent.ACTION_VIEW);
                break;
            default: intent.setClass(context, SettingPlayerChooseActivity.class); break;
        }
        return intent;
    }

    public static Uri getVideoUri(Context context, String path) {
        return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", new File(path));
    }

    public static SubtitleLink[] getSubtitleLinks(File folder) {
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        SubtitleLink[] links = new SubtitleLink[files != null ? files.length + 1 : 1];
        if (files != null) for (int i = 0; i < files.length; i++) links[i] = new SubtitleLink(i, files[i].getName(), files[i].toString(), false);
        links[links.length - 1] = new SubtitleLink(-1, "不显示字幕", "null", false);
        return links;
    }

    public static SubtitleLink[] getSubtitleLinks(long aid, long cid) throws IOException, JSONException {
        String json = NetWorkUtil.getJson(ConfInfoApi.signWBI("https://api.bilibili.com/x/player/wbi/v2?aid=" + aid + "&cid=" + cid)).toString();
        SubtitleLinkData data = GsonUtil.fromJson(json, SubtitleLinkData.class);
        if (data == null || data.data == null || data.data.subtitle == null || data.data.subtitle.subtitles == null)
            return new SubtitleLink[]{new SubtitleLink(-1, "不显示字幕", "null", false)};
        List<SubtitleItem> subs = data.data.subtitle.subtitles;
        SubtitleLink[] links = new SubtitleLink[subs.size() + 1];
        for (int i = 0; i < subs.size(); i++) {
            SubtitleItem s = subs.get(i);
            links[i] = new SubtitleLink(s.id, s.lan_doc, "https:" + s.subtitle_url, s.type == 1);
        }
        links[subs.size()] = new SubtitleLink(-1, "不显示字幕", "null", false);
        return links;
    }

    public static java.util.List<com.RobinNotBad.BiliClient.model.ViewPoint> getViewPoints(long aid, long cid) throws IOException, JSONException {
        String json = NetWorkUtil.getJson(ConfInfoApi.signWBI("https://api.bilibili.com/x/player/wbi/v2?aid=" + aid + "&cid=" + cid)).toString();
        SubtitleLinkData data = GsonUtil.fromJson(json, SubtitleLinkData.class);
        java.util.List<com.RobinNotBad.BiliClient.model.ViewPoint> viewPoints = new ArrayList<>();
        if (data == null || data.data == null || data.data.view_points == null) return viewPoints;
        for (ViewPointData vp : data.data.view_points) {
            if (vp != null) viewPoints.add(new com.RobinNotBad.BiliClient.model.ViewPoint(vp.content, vp.from, vp.to, vp.type, vp.imgUrl, vp.logoUrl));
        }
        return viewPoints;
    }

    public static long getInteractionGraphVersion(long aid, long cid) throws IOException, JSONException {
        String json = NetWorkUtil.getJson(ConfInfoApi.signWBI("https://api.bilibili.com/x/player/wbi/v2?aid=" + aid + "&cid=" + cid)).toString();
        SubtitleLinkData data = GsonUtil.fromJson(json, SubtitleLinkData.class);
        return (data != null && data.data != null && data.data.interaction != null) ? data.data.interaction.graph_version : 0;
    }

    public static Subtitle[] getSubtitle(String url) throws IOException, JSONException {
        String json = NetWorkUtil.getJson(url).toString();
        SubtitleBody body = GsonUtil.fromJson(json, SubtitleBody.class);
        if (body == null || body.body == null) return new Subtitle[0];
        Subtitle[] subtitles = new Subtitle[body.body.size()];
        for (int i = 0; i < body.body.size(); i++) {
            SubtitleEntry e = body.body.get(i);
            subtitles[i] = e != null ? new Subtitle(e.content, e.from, e.to) : new Subtitle("", 0, 0);
        }
        return subtitles;
    }

    public static Subtitle[] getSubtitle(File file) {
        String str = FileUtil.readString(file);
        if (str == null) return null;
        SubtitleBody body = GsonUtil.fromJson(str, SubtitleBody.class);
        if (body == null || body.body == null) return new Subtitle[0];
        Subtitle[] subtitles = new Subtitle[body.body.size()];
        for (int i = 0; i < body.body.size(); i++) {
            SubtitleEntry e = body.body.get(i);
            subtitles[i] = e != null ? new Subtitle(e.content, e.from, e.to) : new Subtitle("", 0, 0);
        }
        return subtitles;
    }

    public static HighEnergyData getHighEnergyData(long cid, long aid) {
        try {
            String url = "https://bvc.bilivideo.com/pbp/data?cid=" + cid + (aid > 0 ? "&aid=" + aid : "");
            JSONObject response = NetWorkUtil.getJson(url, NetWorkUtil.webHeaders);
            if (response == null) return null;
            int code = response.optInt("code", -1);
            if (code != 0 && code != -1) return null;
            HighEnergyData data = new HighEnergyData();
            data.stepSec = response.optInt("step_sec", 10); data.tagStr = response.optString("tagstr", ""); data.debug = response.optString("debug", "");
            JSONObject events = response.optJSONObject("events");
            if (events != null) {
                JSONArray defaultArray = events.optJSONArray("default");
                if (defaultArray != null && defaultArray.length() > 0) {
                    float[] eventData = new float[defaultArray.length()];
                    for (int i = 0; i < defaultArray.length(); i++) eventData[i] = (float) defaultArray.optDouble(i, 0.0);
                    data.events = eventData;
                } else data.events = new float[0];
            } else data.events = new float[0];
            return data;
        } catch (Exception e) { return null; }
    }
}
