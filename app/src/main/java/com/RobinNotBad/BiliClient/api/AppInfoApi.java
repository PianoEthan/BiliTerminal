package com.RobinNotBad.BiliClient.api;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.RobinNotBad.BiliClient.BiliTerminal;
import com.RobinNotBad.BiliClient.BuildConfig;
import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.util.MsgUtil;
import com.RobinNotBad.BiliClient.util.NetWorkUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;

public class AppInfoApi {
    public static void check(Context context) {
        // 讲真这免责声明没啥卵用，写这个也就是半开玩笑的，难道免责声明能挡住律师函吗
        // 而且"fuck_uncle"过分了嗷，咱做第三方软件的真不能这么干……
        if (!SharedPreferencesUtil.getBoolean("disclaimer_shown", false)) {
            MsgUtil.showDialog("免责声明", "使用前请先阅读：\n" + context.getString(R.string.about_to_uncle), 3);
            SharedPreferencesUtil.putBoolean("disclaimer_shown", true);
        }

        if (SharedPreferencesUtil.getBoolean(SharedPreferencesUtil.NIGHT_REMINDER_ENABLE, true)) {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            if (hour == 23 || hour <= 3) {
                MsgUtil.showDialog("温馨提醒", "夜深了，要注意休息呐~", 3);
            }
        }

        try {
            int version = BiliTerminal.getVersion();
            int curr = ConfInfoApi.getDateCurr();
            int last_ver = SharedPreferencesUtil.getInt("app_version_last", 0);
            if (last_ver < version) {
                String update_apk = SharedPreferencesUtil.getString("terminal_update_pkg", "");
                if (!TextUtils.isEmpty(update_apk)) {
                    File file = new File(update_apk);
                    if (file.exists()) {
                        if (file.delete()) {
                            SharedPreferencesUtil.putString("terminal_update_pkg", "");
                            MsgUtil.showMsg("更新包已删除");
                        } else MsgUtil.showMsg("更新包删除失败");
                    } else SharedPreferencesUtil.putString("terminal_update_pkg", "");
                }
                SharedPreferencesUtil.putInt("app_version_last", version);
            }


        } catch (Exception e) {
            Log.e("debug-terminal", e.toString());
            MsgUtil.err("终端接口出现问题（不影响软件内容）", e);
        }
    }

    public static final ArrayList<String> customHeaders = new ArrayList<>() {{
        add("User-Agent");
        add(NetWorkUtil.USER_AGENT_WEB);    //防止携带b站cookies导致可能存在的开发者盗号问题（
        add("App-Info");
        try {
            add(new JSONObject()
                    .put("versionName", BuildConfig.VERSION_NAME)
                    .put("versionCode", BuildConfig.VERSION_CODE)
                    .put("isBeta", BuildConfig.BETA)
                    .put("applicationId", BuildConfig.APPLICATION_ID)
                    .put("buildType", BuildConfig.BUILD_TYPE)
                    .put("debugEnabled", BuildConfig.DEBUG)
                    .toString());
        } catch (JSONException e) {
            MsgUtil.showMsg("版本信息json生成出错\n无影响，正常情况下你应该不会遇到");
        }
        add("Device-Info");
        try {
            add(new JSONObject()
                    .put("sdk", Build.VERSION.SDK_INT)
                    .put("release", Build.VERSION.RELEASE)
                    .put("product", Build.PRODUCT)
                    .put("brand", Build.BRAND)
                    .put("device", Build.DEVICE)
                    .put("type", Build.TYPE)
                    .put("id", Build.ID)
                    .toString());
        } catch (JSONException e) {
            MsgUtil.showMsg("设备信息json生成出错\n无影响，正常情况下你应该不会遇到");
        }
    }};
}
