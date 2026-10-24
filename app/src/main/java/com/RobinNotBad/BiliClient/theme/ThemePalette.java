package com.RobinNotBad.BiliClient.theme;

/**
 * 预解析的 ARGB 令牌集。
 * 默认深色 = 装饰色中性化、语义色（danger/selected/link/success）保留历史彩色；
 * 浅色为最佳努力反演（新观感）。所有字段不可变，生成主题由 SchemeEngine 产出。
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
    /** 警示语义色（删除等破坏性操作），不随装饰强调色褪掉 */
    public final int danger;
    /** 选中/已反馈状态色（已赞、当前项），与未选中态靠色相而非亮度区分 */
    public final int selected;

    /** 语义令牌的兜底值 = 主题系统之前的历史原值 */
    public static final int DEFAULT_DANGER = 0xFFFF5722;
    public static final int DEFAULT_SELECTED = 0xFFFB7299;

    public ThemePalette(int textPrimary, int textTransparent, int ripple, int windowBackground,
                        int accent, int gray, int accentLow, int success, int link,
                        int surfaceCard, int snackBg, int highEnergyLine, int highEnergyFill,
                        int buttonTint, int selectedText, int textSecondary) {
        this(textPrimary, textTransparent, ripple, windowBackground, accent, gray, accentLow,
                success, link, surfaceCard, snackBg, highEnergyLine, highEnergyFill,
                buttonTint, selectedText, textSecondary, DEFAULT_DANGER, DEFAULT_SELECTED);
    }

    public ThemePalette(int textPrimary, int textTransparent, int ripple, int windowBackground,
                        int accent, int gray, int accentLow, int success, int link,
                        int surfaceCard, int snackBg, int highEnergyLine, int highEnergyFill,
                        int buttonTint, int selectedText, int textSecondary,
                        int danger, int selected) {
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
        this.danger = danger;
        this.selected = selected;
    }

    /** 复制法写回语义令牌（其余令牌原样带走） */
    public ThemePalette withSemantic(int danger, int selected) {
        return new ThemePalette(textPrimary, textTransparent, ripple, windowBackground,
                accent, gray, accentLow, success, link, surfaceCard, snackBg,
                highEnergyLine, highEnergyFill, buttonTint, selectedText, textSecondary,
                danger, selected);
    }

    /**
     * 内置默认深色：装饰强调色为单色中性（无粉色）。
     * 文字/表面沿用现状色值；danger/selected 保留历史彩色原值，
     * 因为它们承载警示与选中状态语义，中性化会让反馈读不出来。
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
                0xFFA3A3A3, // textSecondary（中灰说明文字）
                DEFAULT_DANGER,   // danger   <- 历史警示橙
                DEFAULT_SELECTED  // selected <- 历史 @color/pink 选中粉
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
                0xFF5C5C5C, // textSecondary
                0xFFC62828, // danger   （白底可读的警示红）
                0xFFC2185B  // selected （白底可读的选中粉）
        );
    }

    /** 按 alpha 重写（保留 RGB） */
    public static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | (alpha << 24);
    }
}
