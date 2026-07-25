package com.RobinNotBad.BiliClient.activity.settings;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;

import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.base.RefreshListActivity;
import com.RobinNotBad.BiliClient.adapter.SettingsAdapter;
import com.RobinNotBad.BiliClient.model.SettingSection;
import com.RobinNotBad.BiliClient.util.GlideUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;

import java.util.ArrayList;
import java.util.List;

public class SettingPrefActivity extends RefreshListActivity {

    @SuppressLint({"MissingInflatedId", "SetTextI18n", "InflateParams"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPageName("偏好设置");
        Log.e("debug", "进入偏好设置");

        final List<SettingSection> sectionList = new ArrayList<>() {
            {
                add(new SettingSection("title", "功能", "", "", ""));
                add(new SettingSection("switch", "隐私模式", SharedPreferencesUtil.PRIVACY_MODE, getString(R.string.desc_privacy_mode), "false"));
                add(new SettingSection("switch", "长按复制", "copy_enable", getString(R.string.desc_copy_enable), "true"));
                add(new SettingSection("switch", "创作中心", "creative_enable", getString(R.string.desc_creative_enable),
                        "true"));
                add(new SettingSection("switch", "搜索建议", "search_suggestions_enable",
                        getString(R.string.desc_search_suggestions_enable), "true"));
                add(new SettingSection("switch", "默认搜索内容", SharedPreferencesUtil.SEARCH_DEFAULT_CONTENT_ENABLE,
                        getString(R.string.desc_search_default_content_enable), "false"));
                add(new SettingSection("switch", "识别链接", "link_enable", getString(R.string.desc_link_enable), "true"));
                add(new SettingSection("switch", "新动态数量检查", SharedPreferencesUtil.DYNAMIC_UPDATE_CHECK_ENABLE,
                        getString(R.string.desc_dynamic_update_check_enable), "true"));
                add(new SettingSection("switch", "消息数量检查", SharedPreferencesUtil.MESSAGE_UPDATE_CHECK_ENABLE,
                        getString(R.string.desc_message_update_check_enable), "true"));
                add(new SettingSection("switch", "最近更新的UP主", SharedPreferencesUtil.RECENT_UP_DISPLAY_ENABLE,
                        getString(R.string.desc_recent_up_display_enable), "true"));
                add(new SettingSection("switch", "私信自动已读", SharedPreferencesUtil.PRIVATE_MSG_AUTO_READ_ENABLE,
                        getString(R.string.desc_private_msg_auto_read_enable), "true"));
                add(new SettingSection("switch", "我的关注列表分组", SharedPreferencesUtil.FOLLOW_GROUP_MODE,
                        getString(R.string.desc_follow_group_mode), "false"));
                add(new SettingSection("switch", "夜深了", SharedPreferencesUtil.NIGHT_REMINDER_ENABLE,
                        getString(R.string.desc_night_reminder), "true"));


                add(new SettingSection("title", "优化", "", "", ""));
                add(new SettingSection("switch", "禁用返回键", "back_disable", getString(R.string.desc_back_disable),
                        "false"));
                add(new SettingSection("switch", "禁止视频在相册中显示", "save_ban_gallery", getString(R.string.desc_ban_gallery),
                        "true"));
                add(new SettingSection("switch", "请求JPG格式图片", "image_request_jpg",
                        getString(R.string.desc_img_request_jpg), String.valueOf(GlideUtil.defaultRequestJpg())));

                add(new SettingSection("title", "视觉", "", "", ""));
                add(new SettingSection("switch", "加载渐入渐出动画", SharedPreferencesUtil.LOAD_TRANSITION,
                        getString(R.string.desc_load_transition), "true"));
                add(new SettingSection("switch", "翻动时不加载图片", "image_no_load_onscroll",
                        getString(R.string.desc_img_no_load_onscroll), "false"));
                add(new SettingSection("switch", "异步加载布局", SharedPreferencesUtil.ASYNC_INFLATE_ENABLE,
                        getString(R.string.desc_async_inflate_enable), "true"));
                add(new SettingSection("switch", "新提示信息显示方式", SharedPreferencesUtil.SNACKBAR_ENABLE,
                        "打开此选项，会启用新提示信息显示方式", "true"));

                add(new SettingSection("title", "表冠", "", "", ""));
                add(new SettingSection("switch", "启用表冠适配", "ui_rotatory_enable",
                        getString(R.string.setting_lab_ui_rotatory), "false"));
                add(new SettingSection("input_float", "表冠适配灵敏度（Recycler）", "ui_rotatory_recycler", "", "0"));
                add(new SettingSection("input_float", "表冠适配灵敏度（Scroll）", "ui_rotatory_scroll", "", "0"));

            }
        };

        recyclerView.setHasFixedSize(true);

        SettingsAdapter adapter = new SettingsAdapter(this, sectionList);
        setAdapter(adapter);

        setRefreshing(false);

    }

}