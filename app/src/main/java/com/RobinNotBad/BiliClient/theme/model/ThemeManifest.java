package com.RobinNotBad.BiliClient.theme.model;

import android.graphics.Color;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * theme.json 的 Gson 模型（format 1）。
 * 未知键由 Gson 直接忽略（前向兼容）；坏色值在解析阶段丢弃并记日志，绝不崩溃。
 */
public class ThemeManifest {
    public static final int FORMAT = 1;

    public int format;
    public String id;
    public Meta meta;
    public String preview;
    public Colors colors;
    public Background background;

    public static class Meta {
        public String name;
        public String author;
        public int version;
        public String description;
    }

    public static class Colors {
        /** auto | dark | light */
        public String mode;
        /** 显式种子色；存在则忽略 source */
        public String seed;
        public Source source;
        /** -1.0 … 1.0（MCU 对比度） */
        public double contrast;
        /** 0-100 中性色染色强度；缺省取用户设置 */
        public int blend = -1;
        /** 令牌覆盖表：如 {"primary": "#...", "surfaceCard": "#..."} */
        public Map<String, String> override;
    }

    public static class Source {
        /** 背景图相对路径（解压后位于主题目录内） */
        public String image;
        /** 提取失败时的兜底种子色 */
        public String fallback;
    }

    public static class Background {
        /** centerCrop | centerInside | fitXY | tile */
        public String fit;
        /** 0.0-0.9 遮罩透明度 */
        public double scrim = 0.55;
        public String scrimColor;
        public String image;
        public String image_dark;
        public String image_light;
    }

    public boolean isValid() {
        return format == FORMAT && id != null && !id.isEmpty();
    }

    public String getDisplayName() {
        if (meta != null && !TextUtils.isEmpty(meta.name)) return meta.name;
        return id;
    }

    /** 解析 ARGB 色值；失败返回 null */
    public static Integer parseColor(String hex) {
        if (TextUtils.isEmpty(hex)) return null;
        try {
            String s = hex.trim();
            if (s.startsWith("#")) s = s.substring(1);
            if (s.length() == 3) { // #abc → #aabbcc
                s = "" + s.charAt(0) + s.charAt(0) + s.charAt(1) + s.charAt(1) + s.charAt(2) + s.charAt(2);
            }
            if (s.length() == 6) s = "FF" + s;
            if (s.length() != 8) return null;
            return (int) Long.parseLong(s, 16);
        } catch (Exception e) {
            return null;
        }
    }

    /** 取覆盖表中的色值，并同时支持 3/6/8 位 hex */
    public Integer getOverrideColor(String token) {
        if (colors == null || colors.override == null) return null;
        String hex = colors.override.get(token);
        return parseColor(hex);
    }
}
