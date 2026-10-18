package com.RobinNotBad.BiliClient.util;

import android.util.Log;

import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class CookieGenerator {
    private static final String TAG = "CookieGenerator";
    private static final String CHARSET = "0123456789ABCDEF";
    private static final int[] PCK = {8, 4, 4, 4, 12};
    private static final String[] MP = {"1","2","3","4","5","6","7","8","9","A","B","C","D","E","F","10"};
    private static final String[] PLAYER_COOKIE_KEYS = {"SESSDATA", "buvid3", "buvid4", "buvid_fp", "b_nut", "bili_ticket", "LIVE_BUVID"};

    public static void ensureCookies() {
        try {
            if (SharedPreferencesUtil.getString("buvid3", "").isEmpty()) {
                generateBuvids();
            }
            if (SharedPreferencesUtil.getString("bili_ticket", "").isEmpty()) {
                generateBiliTicket();
            }
            if (SharedPreferencesUtil.getString("_uuid", "").isEmpty()) {
                SharedPreferencesUtil.putString("_uuid", genUuidInfoc());
            }
            if (SharedPreferencesUtil.getString("b_lsid", "").isEmpty()) {
                SharedPreferencesUtil.putString("b_lsid", genBlsid());
            }
            if (SharedPreferencesUtil.getString("buvid_fp", "").isEmpty()) {
                SharedPreferencesUtil.putString("buvid_fp", genBuvidFp());
            }
            if (SharedPreferencesUtil.getString("b_nut", "").isEmpty()) {
                SharedPreferencesUtil.putString("b_nut", String.valueOf(System.currentTimeMillis() / 1000));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to ensure cookies", e);
        }
    }

    public static String getCookieString() {
        return getCookieString(false);
    }

    public static String getCookieString(boolean forVideoQuality) {
        StringBuilder sb = new StringBuilder();

        boolean privacyMode = SharedPreferencesUtil.getBoolean(SharedPreferencesUtil.PRIVACY_MODE, false);
        if (!privacyMode || forVideoQuality) {
            String loggedCookie = SharedPreferencesUtil.getString(SharedPreferencesUtil.cookies, "");
            if (!loggedCookie.isEmpty()) {
                sb.append(loggedCookie);
            }
        }

        appendCookie(sb, "buvid3", SharedPreferencesUtil.getString("buvid3", ""));
        appendCookie(sb, "buvid4", SharedPreferencesUtil.getString("buvid4", ""));
        appendCookie(sb, "bili_ticket", SharedPreferencesUtil.getString("bili_ticket", ""));
        appendCookie(sb, "_uuid", SharedPreferencesUtil.getString("_uuid", ""));
        appendCookie(sb, "b_lsid", SharedPreferencesUtil.getString("b_lsid", ""));
        appendCookie(sb, "buvid_fp", SharedPreferencesUtil.getString("buvid_fp", ""));
        appendCookie(sb, "b_nut", SharedPreferencesUtil.getString("b_nut", ""));
        appendCookie(sb, "bili_ticket_expires", SharedPreferencesUtil.getString("bili_ticket_expires", ""));

        return sb.toString();
    }

    public static String getCookieForPlayer() {
        if (SharedPreferencesUtil.getBoolean(SharedPreferencesUtil.PRIVACY_MODE, false)) return "";

        StringBuilder sb = new StringBuilder();
        Set<String> added = new HashSet<>();
        for (String part : getCookieString().split(";")) {
            String item = part.trim();
            int separator = item.indexOf('=');
            if (separator <= 0) continue;
            String name = item.substring(0, separator).trim();
            String value = item.substring(separator + 1).trim();
            if (value.isEmpty() || !isPlayerCookie(name) || !added.add(name)) continue;
            appendCookie(sb, name, value);
        }
        return sb.toString();
    }

    private static boolean isPlayerCookie(String name) {
        for (String key : PLAYER_COOKIE_KEYS) {
            if (key.equals(name)) return true;
        }
        return false;
    }

    private static void appendCookie(StringBuilder sb, String name, String value) {
        if (value.isEmpty()) return;
        if (sb.length() > 0 && !sb.toString().endsWith("; ")) {
            sb.append("; ");
        }
        sb.append(name).append("=").append(value);
    }

    private static void generateBuvids() {
        try {
            String json = NetWorkUtil.getJsonNoCookie("https://api.bilibili.com/x/frontend/finger/spi").toString();
            JSONObject data = new JSONObject(json).optJSONObject("data");
            if (data != null) {
                String b3 = data.optString("b_3", "");
                String b4 = data.optString("b_4", "");
                if (!b3.isEmpty()) SharedPreferencesUtil.putString("buvid3", b3);
                if (!b4.isEmpty()) SharedPreferencesUtil.putString("buvid4", b4);
                Log.d(TAG, "Generated buvid3/buvid4");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate buvids", e);
        }
    }

    private static void generateBiliTicket() {
        try {
            int ts = (int) (System.currentTimeMillis() / 1000);
            String hexsign = hmacSha256("XgwSnGZ1p", "ts" + ts);
            String url = "https://api.bilibili.com/bapis/bilibili.api.ticket.v1.Ticket/GenWebTicket?key_id=ec02&hexsign=" + hexsign + "&context[ts]=" + ts;
            String json = NetWorkUtil.post(url, "", NetWorkUtil.webHeaders).body().string();
            JSONObject resp = new JSONObject(json);
            JSONObject data = resp.optJSONObject("data");
            if (data != null) {
                String ticket = data.optString("ticket", "");
                long createTime = data.optLong("created_at", 0);
                if (!ticket.isEmpty()) {
                    SharedPreferencesUtil.putString("bili_ticket", ticket);
                    SharedPreferencesUtil.putString("bili_ticket_expires", String.valueOf(createTime + 3 * 24 * 60 * 60));
                    Log.d(TAG, "Generated bili_ticket");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate bili_ticket", e);
        }
    }

    private static String hmacSha256(String key, String message) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hashBytes = sha256Hmac.doFinal(message.getBytes());
            StringBuilder hexHash = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexHash.append('0');
                hexHash.append(hex);
            }
            return hexHash.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String genBlsid() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(CHARSET.charAt(random.nextInt(CHARSET.length())));
        }
        return sb.toString() + "_" + Long.toHexString(System.currentTimeMillis()).toUpperCase(Locale.getDefault());
    }

    private static String genUuidInfoc() {
        long t = System.currentTimeMillis() % 100000;
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int len : PCK) {
            for (int i = 0; i < len; i++) {
                sb.append(MP[random.nextInt(16)]);
            }
            sb.append("-");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append(String.format(Locale.getDefault(), "%05d", t)).append("infoc");
        return sb.toString();
    }

    private static String genBuvidFp() {
        return String.format("%016x%016x",
                System.currentTimeMillis() ^ 0x52DCE729L,
                System.nanoTime() ^ 0x38495AB5L);
    }
}
