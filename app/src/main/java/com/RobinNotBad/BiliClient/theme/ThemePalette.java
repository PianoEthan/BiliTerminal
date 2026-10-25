package com.RobinNotBad.BiliClient.theme;

/**
 * 预解析的 ARGB 令牌集。
 * 默认深色保留原资源和各消费点的历史颜色；浅色和生成主题使用色板。
 * 所有字段不可变，生成主题由 SchemeEngine 产出。
 */
public class ThemePalette {

    public final boolean originalColors;
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
        this(textPrimary, textTransparent, ripple, windowBackground, accent, gray, accentLow,
                success, link, surfaceCard, snackBg, highEnergyLine, highEnergyFill,
                buttonTint, selectedText, textSecondary, danger, selected, false);
    }

    private ThemePalette(int textPrimary, int textTransparent, int ripple, int windowBackground,
                         int accent, int gray, int accentLow, int success, int link,
                         int surfaceCard, int snackBg, int highEnergyLine, int highEnergyFill,
                         int buttonTint, int selectedText, int textSecondary,
                         int danger, int selected, boolean originalColors) {
        this.originalColors = originalColors;
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
                danger, selected, originalColors);
    }

    public int color(int original, int themed) {
        return originalColors ? original : themed;
    }

    /** 默认深色沿用资源原色；浅色和生成主题按令牌染色。 */
    public static ThemePalette builtinDark() {
        return builtinDark(true);
    }

    static ThemePalette builtinDark(boolean originalColors) {
        return new ThemePalette(
                0xFFEBE0E2, // textPrimary   <- textwhite
                0x50FEFEFE, // textTransparent
                0x78FEFEFE, // ripple        <- color_ripple
                0xFF000000, // windowBackground <- bgblack
                originalColors ? 0xFBFB8787 : 0xFFEAEAEA, // accent
                0x70707070, // gray
                originalColors ? 0x99FB8799 : 0x99EAEAEA, // accentLow
                0xFFBBFFBB, // success       <- light_green
                0xFF66CCFF, // link
                0xCC262626, // surfaceCard   <- Card/ButtonStyle
                0x85808080, // snackBg       <- MsgUtil
                originalColors ? 0xA8FB7299 : 0xA8EAEAEA, // highEnergyLine
                originalColors ? 0x33FB7299 : 0x33EAEAEA, // highEnergyFill
                0xCC262626, // buttonTint
                0xFFEBE0E2, // selectedText
                0xFFA3A3A3, // textSecondary
                DEFAULT_DANGER,
                DEFAULT_SELECTED,
                originalColors
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
