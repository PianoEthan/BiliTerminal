package com.RobinNotBad.BiliClient.api;

import android.util.Log;

import com.RobinNotBad.BiliClient.model.Opus;
import com.RobinNotBad.BiliClient.model.OpusParagraph;
import com.RobinNotBad.BiliClient.model.Stats;
import com.RobinNotBad.BiliClient.model.UserInfo;
import com.RobinNotBad.BiliClient.util.JsonUtil;
import com.RobinNotBad.BiliClient.util.Logu;
import com.RobinNotBad.BiliClient.util.MsgUtil;
import com.RobinNotBad.BiliClient.util.NetWorkUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import androidx.core.util.ObjectsCompat;

import okhttp3.Response;
import okhttp3.ResponseBody;

public class OpusApi {

    public static Opus getOpus(long id) throws IOException, JSONException {
        Opus opus = new Opus();
        opus.type = Opus.TYPE_DYNAMIC;
        opus.id = id;

        String url;
        if (id > 100000000)
            url = "https://www.bilibili.com/opus/" + id; // 别问，问就是动态和专栏都被统一了，判断不了类型，只能判断id长度了。能用。
        else url = "https://www.bilibili.com/read/cv" + id;
        try {
            Response response = NetWorkUtil.get(url);
            // /read/cv{id} 有多层301重定向（加斜杠、跳转到/opus/），循环跟随直到拿到最终页面
            for (int i = 0; i < 5; i++) {
                String location = response.header("Location");
                if (location == null || location.isEmpty()) break;
                response.close();
                response = NetWorkUtil.get(location);
            }
            ResponseBody responseBody = response.body();
            if (responseBody == null) return opus;

            String html = responseBody.string();

            String detailStr = JsonUtil.search(html, "detail", "");
            if (detailStr.isEmpty()) return opus;
            JSONObject detail = new JSONObject(detailStr);

            JSONObject basic = detail.optJSONObject("basic");
            if (basic != null) {
                opus.commentId = Long.parseLong(basic.optString("comment_id_str", "0"));
                opus.commentType = basic.optInt("comment_type", 0);
            }

            if (opus.commentId == 0) opus.commentId = id;
            if (opus.commentType == 0) opus.commentType = 17;

            if (detail.isNull("modules")) return opus;    //isNull其实涵盖了!has的情况，之前都是咋想的判断两次，我简直是sb
            JSONArray modules = detail.getJSONArray("modules");

            for (int i = 0; i < modules.length(); i++) {
                JSONObject module = modules.getJSONObject(i);
                switch (module.optString("module_type")) {
                    case "MODULE_TYPE_TITLE":
                        JSONObject moduleTitle = module.optJSONObject("module_title");
                        if (moduleTitle != null) opus.title = moduleTitle.optString("text", "");
                        break;
                    case "MODULE_TYPE_TOP":
                        ArrayList<String> topImages = new ArrayList<>();
                        JSONObject module_top = module.optJSONObject("module_top");
                        if (module_top != null) {
                            JSONObject display = module_top.optJSONObject("display");
                            if (display != null) {
                                int displayType = display.optInt("type");
                                if (displayType == 1) {
                                    JSONObject album = display.optJSONObject("album");
                                    if (album != null) {
                                        JSONArray pics = album.optJSONArray("pics");
                                        if (pics != null) {
                                            for (int j = 0; j < pics.length(); j++) {
                                                JSONObject pic = pics.optJSONObject(j);
                                                if (pic != null) topImages.add(pic.optString("url", ""));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        opus.topImages = topImages;
                        Logu.d("yes");
                        break;
                    case "MODULE_TYPE_AUTHOR":
                        JSONObject module_author = module.optJSONObject("module_author");    //我感觉b站也是一个巨大的草台班子，用户信息格式都好几种，头像有avatar有face有head的，他们自己的程序员不累吗……
                        if (module_author == null) break;
                        UserInfo author = new UserInfo();
                        author.mid = module_author.optLong("mid", 0);
                        author.name = module_author.optString("name", "");
                        author.followed = module_author.optBoolean("following", false);
                        author.avatar = module_author.optString("face", module_author.optString("avatar", ""));
                        if (!module_author.isNull("vip"))
                            author.vip_nickname_color = module_author.optJSONObject("vip").optString("nickname_color", "");

                        opus.pubTime = module_author.optString("pub_time", "");
                        opus.upInfo = author;
                        break;
                    case "MODULE_TYPE_CONTENT":
                        JSONObject moduleContent = module.optJSONObject("module_content");
                        if (moduleContent != null) {
                            JSONArray paragraphs = moduleContent.optJSONArray("paragraphs");
                            if (paragraphs != null) opus.paragraphs = analyzeParagraphs(paragraphs);
                        }
                        break;
                    case "MODULE_TYPE_STAT":
                        opus.stats = Stats.fromOpus(module.optJSONObject("module_stat"));
                        break;
                }
            }

            if (opus.upInfo == null) opus.upInfo = new UserInfo();
            if (opus.stats == null) opus.stats = new Stats();
        } catch (IllegalArgumentException e) { // 取不出来的时候，会重定向，但重定向的域名是//开头的，会报错
            //这里给opus设置一个参数，让OpusInfoActivity跳转到旧版的DynamicInfoActivity，从而无需重写解析
            //判断方式很简单粗暴，看报错信息里有没有URL这个关键字，有就是跳转错误
            String errMsg = e.getMessage();
            if (errMsg != null && errMsg.contains("URL")) opus.type = Opus.TYPE_DYNAMIC_OLD_STYLE;
            else MsgUtil.err(e);
            return opus;

            /*
            url = "https://api.bilibili.com/x/polymer/web-dynamic/v1/detail?";
            url += "timezone_offset=-480&platform=web&gaia_source=main_web&id=" + id + "&features=itemOpusStyle,opusBigCover,onlyfansVote,endFooterHidden,decorationCard,onlyfansAssetsV2,ugcDelete,onlyfansQaCard,editable,opusPrivateVisible,avatarAutoTheme&web_location=333.1368&x-bili-device-req-json=%7B%22platform%22:%22web%22,%22device%22:%22pc%22%7D&x-bili-web-req-json=%7B%22spm_id%22:%22333.1368%22%7D";
            Response response = NetWorkUtil.get(ConfInfoApi.signWBI(url));
            ResponseBody responseBody = response.body();
            if(responseBody == null) return opus;

            JSONObject json = new JSONObject(responseBody.string());
            JSONObject item = json.getJSONObject("data").getJSONObject("item");

            analyzeOldStyleDynamic(opus, item);
             */
        }
        // B站是会做图文的

        opus.cover = "";
        return opus;
    }

    public static OpusParagraph[] analyzeParagraphs(JSONArray jsonArray) throws JSONException {
        OpusParagraph[] paragraphs = new OpusParagraph[jsonArray.length()];
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject paragraphJson = jsonArray.getJSONObject(i);
            OpusParagraph paragraph = new OpusParagraph(paragraphJson);
            paragraphs[i] = paragraph;
        }
        return paragraphs;
    }

    /**
     * Opus/动态点赞
     *
     * @param dynId 动态id
     * @param up    true=点赞，false=取消赞
     * @return resultCode
     */
    public static int likeOpus(long dynId, boolean up) throws IOException {
        String csrf = SharedPreferencesUtil.getString("csrf", "");
        String url = "https://api.bilibili.com/x/dynamic/feed/dyn/thumb?csrf=" + csrf;
        JSONObject payload = new JSONObject();
        try {
            payload.put("dyn_id_str", String.valueOf(dynId));
            payload.put("up", up ? 1 : 2);
            payload.put("csrf", csrf);
        } catch (JSONException ignored) {
            return -1;
        }
        Response resp = ObjectsCompat.requireNonNull(NetWorkUtil.postJson(url, payload.toString(), NetWorkUtil.webHeaders));
        try {
            assert resp.body() != null;
            JSONObject respBody = new JSONObject(resp.body().string());
            return respBody.getInt("code");
        } catch (JSONException ignored) {
            return -1;
        }
    }

    public static void analyzeOldStyleDynamic(Opus opus, JSONObject item) throws JSONException {
        JSONObject basic = item.getJSONObject("basic");
        opus.commentId = Long.parseLong(basic.optString("comment_id_str", "0"));
        opus.commentType = basic.optInt("comment_type");

        String dynamicType = item.getString("type");

        if (item.isNull("modules")) return;
        JSONObject modules = item.getJSONObject("modules");

        //up主信息
        UserInfo author = new UserInfo();
        if (!modules.isNull("module_author")) {
            JSONObject module_author = modules.getJSONObject("module_author");
            author.mid = module_author.getLong("mid");
            author.name = module_author.getString("name");
            author.followed = module_author.optBoolean("following", false);
            author.avatar = module_author.getString("face");
            if (!module_author.isNull("vip"))
                author.vip_nickname_color = module_author.getJSONObject("vip").optString("nickname_color", "");
            opus.pubTime = module_author.getString("pub_time");
        }
        opus.upInfo = author;

        if (dynamicType.equals("DYNAMIC_TYPE_NONE")) {
            opus.content = "[动态不存在]";
            return;
        }

        //动态主体内容
        JSONObject module_dynamic = modules.getJSONObject("module_dynamic");

        ArrayList<OpusParagraph> paragraphList = new ArrayList<>();

        if (!module_dynamic.isNull("desc")) {
            JSONObject object = new JSONObject();
            object.put("para_type", OpusParagraph.TYPE_TEXT_OPUS);
            object.put("data", module_dynamic.getJSONObject("desc").getJSONArray("rich_text_nodes"));
            paragraphList.add(new OpusParagraph(object));
        }

        if (!module_dynamic.isNull("major")) {
            JSONObject major = module_dynamic.getJSONObject("major");

            if (!major.isNull("opus")) {
                JSONObject dynamic_opus = major.getJSONObject("opus");
                JSONArray opus_pics = dynamic_opus.getJSONArray("pics");

                // 为了排版正常，这里必须把列表完整传递给OpusParagraph，让OpusParagraph那边解析
                // 这么干主要是为了适配这神秘的代码结构，我研究OpusParagraph的使用方法就研究了半天
                // by Moye

                JSONObject object = new JSONObject();
                object.put("para_type", OpusParagraph.TYPE_TEXT_OPUS);
                object.put("data", dynamic_opus.getJSONObject("summary").getJSONArray("rich_text_nodes"));
                paragraphList.add(new OpusParagraph(object));

                object = new JSONObject();
                object.put("para_type", OpusParagraph.TYPE_PIC);
                object.put("pic", new JSONObject().put("pics", opus_pics));
                paragraphList.add(new OpusParagraph(object));
            }

            if (!major.isNull("archive")) {
                // 这里是视频卡片
            }

        }

        opus.paragraphs = paragraphList.toArray(new OpusParagraph[0]);

        JSONObject module_stat = modules.getJSONObject("module_stat");
        Stats stats = new Stats();
        stats.reply = module_stat.getJSONObject("comment").getInt("count");
        stats.like = module_stat.getJSONObject("like").getInt("count");

        opus.stats = stats;
    }
}
