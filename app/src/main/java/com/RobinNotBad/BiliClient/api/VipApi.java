package com.RobinNotBad.BiliClient.api;

import com.RobinNotBad.BiliClient.model.VipInfo;
import com.RobinNotBad.BiliClient.util.Cookies;
import com.RobinNotBad.BiliClient.util.NetWorkUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Response;
import okhttp3.ResponseBody;

public class VipApi {
    public static VipInfo getVipInfo() throws JSONException, IOException {
        String url = "https://api.bilibili.com/x/vip/privilege/my";
        JSONObject all = NetWorkUtil.getJson(url);
        if (all.getInt("code") != 0) throw new JSONException(all.getString("message"));

        JSONObject data = all.getJSONObject("data");
        VipInfo vipInfo = new VipInfo();

        vipInfo.isShortVip = data.optBoolean("is_short_vip", false);
        vipInfo.isFreightOpen = data.optBoolean("is_freight_open", false);
        vipInfo.level = data.optInt("level", 0);
        vipInfo.curExp = data.optLong("cur_exp", 0);
        vipInfo.nextExp = data.optLong("next_exp", 0);
        vipInfo.isVip = data.optBoolean("is_vip", false);
        vipInfo.isSeniorMember = data.optInt("is_senior_member", 0);
        vipInfo.format060102 = data.optInt("format060102", 0);
        vipInfo.isOverdueVip = data.optBoolean("is_overdue_vip", false);
        vipInfo.vipStatus = data.optInt("vip_status", 0);
        vipInfo.vipType = data.optInt("vip_type", 0);
        vipInfo.keeptimeEnd = data.optLong("keeptime_end", 0);
        vipInfo.vipDueDate = data.optLong("vip_due_date", 0);
        vipInfo.vipIsAnnual = data.optBoolean("vip_is_annual", false);
        vipInfo.vipIsMonth = data.optBoolean("vip_is_month", false);
        vipInfo.vipIsNewUser = data.optBoolean("vip_is_new_user", false);
        vipInfo.bindPhone = data.optString("bind_phone", "");

        JSONObject taobaoAccount = data.optJSONObject("taobao_account");
        if (taobaoAccount != null) {
            vipInfo.taobaoAccount = taobaoAccount.toString();
        }

        JSONArray list = data.optJSONArray("list");
        if (list != null) {
            vipInfo.privilegeList = new ArrayList<>();
            for (int i = 0; i < list.length(); i++) {
                JSONObject privilege = list.getJSONObject(i);
                VipInfo.Privilege privilegeItem = new VipInfo.Privilege();
                privilegeItem.type = privilege.optInt("type", 0);
                privilegeItem.state = privilege.optInt("state", 0);
                privilegeItem.expireTime = privilege.optLong("expire_time", 0);
                privilegeItem.vipType = privilege.optInt("vip_type", 0);
                privilegeItem.nextReceiveDays = privilege.optInt("next_receive_days", 0);
                privilegeItem.periodEndUnix = privilege.optLong("period_end_unix", 0);
                vipInfo.privilegeList.add(privilegeItem);
            }
        }

        return vipInfo;
    }

    public static JSONObject addExperience() throws IOException, JSONException {
        String url = "https://api.bilibili.com/x/vip/experience/add";
        Cookies cookies = NetWorkUtil.getCookies();
        String csrf = cookies.get("bili_jct");
        NetWorkUtil.FormData formData = new NetWorkUtil.FormData();
        formData.put("csrf", csrf);
        try (Response response = NetWorkUtil.post(url, formData.toString())) {
            try (ResponseBody body = response.body()) {
                if (body != null) return new JSONObject(body.string());
                else throw new JSONException("在访问" + url + "时返回数据为空");
            }
        }
    }
}

