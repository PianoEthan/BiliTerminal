package com.RobinNotBad.BiliClient.activity.settings;

import android.annotation.SuppressLint;
import android.os.Bundle;

import com.RobinNotBad.BiliClient.BiliTerminal;
import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.base.RefreshListActivity;
import com.RobinNotBad.BiliClient.adapter.SettingsAdapter;
import com.RobinNotBad.BiliClient.model.SettingSection;
import com.RobinNotBad.BiliClient.util.FileUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;

import java.util.ArrayList;
import java.util.List;

public class SettingLaboratoryActivity extends RefreshListActivity {

    @SuppressLint({"MissingInflatedId", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPageName("实验室");

        boolean debugBuild = BiliTerminal.isDebugBuild();

        final List<SettingSection> sectionList = new ArrayList<>() {
            {
                add(new SettingSection("title", "可用性", "", "", ""));
                add(new SettingSection("switch", "新版弹幕获取方式", "new_danmaku_api",
                        getString(R.string.desc_new_danmaku_api), "true"));
                add(new SettingSection("switch", "私信未读标记", SharedPreferencesUtil.PRIVATE_MSG_UNREAD_BADGE_ENABLE,
                        getString(R.string.desc_private_msg_unread_badge_enable), "true"));
                add(new SettingSection("switch", "图文投稿图片上传", SharedPreferencesUtil.LAB_OPUS_IMAGE_UPLOAD,
                        "在发布动态时允许选择本地图片作为配图，关闭则仅发布纯文本动态", "false"));

                add(new SettingSection("title", "下载", "", "", ""));
                add(new SettingSection("switch", "使用旧版下载器", "dev_download_old",
                        getString(R.string.setting_lab_download_old), "false"));
                add(new SettingSection("input_string", "缓存路径", "save_path_video",
                        getString(R.string.setting_lab_path_video), FileUtil.getVideoDownloadPath().toString()));
                add(new SettingSection("input_string", "图片下载路径", "save_path_pictures",
                        getString(R.string.setting_lab_path_pictures), FileUtil.getPicturePath().toString()));

                add(new SettingSection("title", "UI", "", "", ""));
                add(new SettingSection("switch", "横屏模式", "ui_landscape", getString(R.string.setting_lab_ui_landscape),
                        "false"));
                add(new SettingSection("input_string", "开屏文字", "ui_splashtext",
                        getString(R.string.setting_lab_splashtext), "欢迎使用\n哔哩终端"));
                add(new SettingSection("switch", "文字跑马灯", "marquee_enable", getString(R.string.setting_lab_marquee),
                        "true"));

                add(new SettingSection("title", "播放器", "", "", ""));
                add(new SettingSection("switch", "播放器旋屏兼容方案", "dev_player_rotate_software",
                        "在极少数手表上（如小米手表），系统旋屏存在显示不全的问题。打开此开关，播放器将会使用软件旋屏方法。", "false"));
                add(new SettingSection("switch", "显示视频分段", "player_show_viewpoints",
                        "显示视频的章节看点信息，可快速跳转到指定章节", "false"));
                add(new SettingSection("switch", "系统媒体控件", SharedPreferencesUtil.PLAYER_MEDIA_SESSION_ENABLE,
                        getString(R.string.setting_lab_media_session), "false"));
                add(new SettingSection("switch", "互动视频调试", "player_interaction_debug",
                        "在互动视频播放时，在左侧倍速按钮上方显示调试按钮，可以查看和修改互动视频的变量", "false"));
                add(new SettingSection("switch", "互动视频", SharedPreferencesUtil.PLAYER_INTERACTION_ENABLE,
                        "启用互动视频功能，包括选项交互、进度回溯等", "true"));

                add(new SettingSection("title", "网络请求", "", "", ""));
                add(new SettingSection("input_float", "接口重试间隔（秒）", SharedPreferencesUtil.API_RETRY_INTERVAL_SECONDS,
                        getString(R.string.setting_lab_api_retry_interval), "0.1"));
                add(new SettingSection("input_int", "接口重试次数", SharedPreferencesUtil.API_RETRY_MAX_TIMES,
                        getString(R.string.setting_lab_api_retry_max_times), "5"));

                add(new SettingSection("title", "调试", "", "", ""));
                add(new SettingSection("switch", "允许Logu.v", "dev_logv", getString(R.string.setting_lab_logv),
                        String.valueOf(debugBuild)));
                add(new SettingSection("switch", "允许Logu.d", "dev_logd", "", String.valueOf(debugBuild)));
                add(new SettingSection("switch", "允许Logu.i", "dev_logi", "", String.valueOf(debugBuild)));
                add(new SettingSection("switch", "详细显示数据解析报错", "dev_jsonerr_detailed",
                        getString(R.string.setting_lab_jsonerr_detailed), String.valueOf(debugBuild)));
                add(new SettingSection("switch", "详细显示列表报错", "dev_recyclererr_detailed",
                        getString(R.string.setting_lab_recyclererr_detailed), String.valueOf(debugBuild)));
            }
        };

        recyclerView.setHasFixedSize(true);

        SettingsAdapter adapter = new SettingsAdapter(this, sectionList);
        setAdapter(adapter);

        setRefreshing(false);
    }

}