package com.RobinNotBad.BiliClient.theme;

/**
 * 预解析的 ARGB 令牌集。
 * 默认深色 = 现状精确色值（逐像素不变）；浅色为最佳努力反演（新观感）。
 * 所有字段不可变，生成主题由 SchemeEngine 产出。
 */
public class ThemePalette {

    public final int textPrimary;
    public final int textTransparent;
    public final int ripple;
    public final int windowBackground;
    public final int accent;
    public final int gray;
    public final int accentLow;
    public final int success;
    public final int link;
    public final int surfaceCard;
    public final int snackBg;
    public final int highEnergyLine;
    public final int highEnergyFill;
    public final int buttonTint;
    public final int selectedText;
    /** 次级文字（说明/计数/时间戳等中灰文字） */
    public final int textSecondary;

    public ThemePalette(int textPrimary, int textTransparent, int ripple, int windowBackground,
                        int accent, int gray, int accentLow, int success, int link,
                        int surfaceCard, int snackBg, int highEnergyLine, int highEnergyFill,
                        int buttonTint, int selectedText, int textSecondary) {
        this.textPrimary = textPrimary;
        this.textTransparent = textTransparent;
        this.ripple = ripple;
        this.windowBackground = windowBackground;
        this.accent = accent;
        this.gray = gray;
        this.accentLow = accentLow;
        this.success = success;
        this.link = link;
        this.surfaceCard = surfaceCard;
        this.snackBg = snackBg;
        this.highEnergyLine = highEnergyLine;
        this.highEnergyFill = highEnergyFill;
        this.buttonTint = buttonTint;
        this.selectedText = selectedText;
        this.textSecondary = textSecondary;
    }

    /**
     * 内置默认深色：单色中性设计（无粉色）。
     * 文字/表面沿用现状色值；强调色族为中性近白，不携带彩色倾向。
     */
    public static ThemePalette builtinDark() {
        return new ThemePalette(
                0xFFEBE0E2, // textPrimary   <- textwhite
                0x50FEFEFE, // textTransparent
                0x78FEFEFE, // ripple        <- color_ripple
                0xFF000000, // windowBackground <- bgblack
                0xFFEAEAEA, // accent        单色中性
                0x70707070, // gray
                0x99EAEAEA, // accentLow
                0xFFBBFFBB, // success       <- light_green（功能色保留）
                0xFF66CCFF, // link          （功能色保留）
                0xCC262626, // surfaceCard   <- Card/ButtonStyle
                0x85808080, // snackBg       <- MsgUtil
                0xA8EAEAEA, // highEnergyLine
                0x33EAEAEA, // highEnergyFill
                0xCC262626, // buttonTint
                0xFFEBE0E2, // selectedText
                0xFFA3A3A3  // textSecondary（中灰说明文字）
        );
    }

    /** 浅色主题：最佳努力反演（新观感，非现状值）；强调色 = 墨色，单色中性 */
    public static ThemePalette builtinLight() {
        return new ThemePalette(
                0xFF1F1B1B, // textPrimary
                0x4D000000, // textTransparent
                0x33000000, // ripple
                0xFFFFFFFF, // windowBackground
                0xFF1F1B1B, // accent（墨色，白底可读）
                0x73000000, // gray
                0x661F1B1B, // accentLow
                0xFF2E7D32, // success
                0xFF1565C0, // link
                0xFFFFFFFF, // surfaceCard
                0xCCDDDDDD, // snackBg
                0xB31F1B1B, // highEnergyLine
                0x331F1B1B, // highEnergyFill
                0xFFFFFFFF, // buttonTint
                0xFF1F1B1B, // selectedText
                0xFF5C5C5C  // textSecondary
        );
    }

    /** 按 alpha 重写（保留 RGB） */
    public static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | (alpha << 24);
    }
}
