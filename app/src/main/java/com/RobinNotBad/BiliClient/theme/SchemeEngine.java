package com.RobinNotBad.BiliClient.theme;

import com.RobinNotBad.BiliClient.theme.model.ThemeManifest;
import com.google.material.color.blend.Blend;
import com.google.material.color.dynamiccolor.DynamicScheme;
import com.google.material.color.dynamiccolor.Variant;
import com.google.material.color.hct.Hct;
import com.google.material.color.palettes.TonalPalette;
import com.google.material.color.scheme.SchemeContent;

/**
 * material-color-utilities 封装：
 * 种子色 → SchemeContent（M3 动态色板）→ 应用令牌；
 * 中性色按 blend 强度向强调色 hue 混合染色（TonalPalette.fromHueAndChroma）。
 * 纯计算，无 IO，主线程可调用（单次 &lt;10ms）。
 */
public class SchemeEngine {

    private static final String TAG = "SchemeEngine";

    public static ThemePalette generate(int seedArgb, boolean dark, int blend, ThemeManifest manifest) {
        double contrast = 0.0;
        if (manifest != null && manifest.colors != null) {
            contrast = manifest.colors.contrast;
            if (contrast < -1.0 || contrast > 1.0) contrast = 0.0;
            if (manifest.colors.blend >= 0) blend = Math.min(100, manifest.colors.blend);
        }
        if (blend < 0) blend = 0;
        if (blend > 100) blend = 100;

        Hct seedHct = Hct.fromInt(seedArgb);
        SchemeContent content = new SchemeContent(seedHct, dark, contrast);

        // 中性色染色：chroma 按 blend% 缩放，hue 保持种子 hue
        double neutralChroma = seedHct.getChroma() * blend / 100.0;
        TonalPalette neutral = TonalPalette.fromHueAndChroma(seedHct.getHue(), neutralChroma);
        DynamicScheme scheme = new DynamicScheme(
                seedHct, Variant.CONTENT, dark, contrast,
                content.primaryPalette, content.secondaryPalette, content.tertiaryPalette,
                neutral, neutral);

        int textPrimary = scheme.getOnSurface();
        int textSecondary = scheme.getOnSurfaceVariant();
        int surfaceCard = ThemePalette.withAlpha(scheme.getSurfaceContainer(), 0xCC);
        int accent = scheme.getPrimary();
        int accentLow = ThemePalette.withAlpha(scheme.getTertiary(), 0x99);
        int gray = ThemePalette.withAlpha(scheme.getOutline(), 0x70);
        int link = functionalTone(0xFF66CCFF, seedArgb, dark);
        int success = functionalTone(0xFFBBFFBB, seedArgb, dark);
        int danger = functionalTone(ThemePalette.DEFAULT_DANGER, seedArgb, dark);
        int selected = functionalTone(ThemePalette.DEFAULT_SELECTED, seedArgb, dark);

        ThemePalette palette = new ThemePalette(
                textPrimary,
                ThemePalette.withAlpha(textPrimary, 0x50),
                ThemePalette.withAlpha(scheme.getOutline(), 0x78),
                dark ? 0xFF000000 : 0xFFFFFFFF, // 按模式 tone 0/100
                accent,
                gray,
                accentLow,
                success,
                link,
                surfaceCard,
                ThemePalette.withAlpha(scheme.getSurfaceContainerHigh(), 0x85),
                ThemePalette.withAlpha(accent, 0xA8),
                ThemePalette.withAlpha(accent, 0x33),
                surfaceCard,
                textPrimary,
                textSecondary,
                danger,
                selected);

        // 令牌覆盖
        if (manifest != null && manifest.colors != null) {
            palette = applyOverrides(palette, manifest);
        }
        return palette;
    }

    /**
     * 功能/语义色派生：色相向种子 harmonize，但 tone 必须按明暗给。
     * Blend.harmonize 只动色相不动亮度，历史上直接拿深色档字面量（link t78、success t94）
     * 做基底，浅色主题下白底对比度只剩 1.2~1.8，等于看不见。深色侧维持原样。
     */
    private static final double LIGHT_FUNCTIONAL_TONE = 40.0;

    private static int functionalTone(int baseArgb, int seedArgb, boolean dark) {
        int harmonized = Blend.harmonize(baseArgb, seedArgb);
        if (dark) return harmonized;
        Hct h = Hct.fromInt(harmonized);
        return Hct.from(h.getHue(), h.getChroma(), LIGHT_FUNCTIONAL_TONE).toInt();
    }

    private static ThemePalette applyOverrides(ThemePalette base, ThemeManifest manifest) {
        int textPrimary = base.textPrimary;
        int textTransparent = base.textTransparent;
        int ripple = base.ripple;
        int windowBackground = base.windowBackground;
        int accent = base.accent;
        int gray = base.gray;
        int accentLow = base.accentLow;
        int success = base.success;
        int link = base.link;
        int surfaceCard = base.surfaceCard;
        int snackBg = base.snackBg;
        int highEnergyLine = base.highEnergyLine;
        int highEnergyFill = base.highEnergyFill;
        int buttonTint = base.buttonTint;
        int selectedText = base.selectedText;
        int textSecondary = base.textSecondary;
        int danger = base.danger;
        int selected = base.selected;

        java.util.Map<String, String> over = manifest.colors.override;
        if (over != null) {
            Integer v;
            if ((v = ThemeManifest.parseColor(over.get("textPrimary"))) != null) textPrimary = v;
            if ((v = ThemeManifest.parseColor(over.get("textSecondary"))) != null) textSecondary = v;
            if ((v = ThemeManifest.parseColor(over.get("textTransparent"))) != null) textTransparent = v;
            if ((v = ThemeManifest.parseColor(over.get("ripple"))) != null) ripple = v;
            if ((v = ThemeManifest.parseColor(over.get("windowBackground"))) != null) windowBackground = v;
            if ((v = ThemeManifest.parseColor(over.get("primary"))) != null) accent = v;
            if ((v = ThemeManifest.parseColor(over.get("accent"))) != null) accent = v;
            if ((v = ThemeManifest.parseColor(over.get("gray"))) != null) gray = v;
            if ((v = ThemeManifest.parseColor(over.get("accentLow"))) != null) accentLow = v;
            if ((v = ThemeManifest.parseColor(over.get("success"))) != null) success = v;
            if ((v = ThemeManifest.parseColor(over.get("link"))) != null) link = v;
            if ((v = ThemeManifest.parseColor(over.get("surfaceCard"))) != null) surfaceCard = v;
            if ((v = ThemeManifest.parseColor(over.get("snackBg"))) != null) snackBg = v;
            if ((v = ThemeManifest.parseColor(over.get("highEnergyLine"))) != null) highEnergyLine = v;
            if ((v = ThemeManifest.parseColor(over.get("highEnergyFill"))) != null) highEnergyFill = v;
            if ((v = ThemeManifest.parseColor(over.get("buttonTint"))) != null) buttonTint = v;
            if ((v = ThemeManifest.parseColor(over.get("selectedText"))) != null) selectedText = v;
            if ((v = ThemeManifest.parseColor(over.get("danger"))) != null) danger = v;
            if ((v = ThemeManifest.parseColor(over.get("selected"))) != null) selected = v;
        }
        // 语义令牌必须显式写回：16 参构造只给历史兜底值，直接从 over 读到的 danger/selected 否则会被丢掉
        return new ThemePalette(textPrimary, textTransparent, ripple, windowBackground,
                accent, gray, accentLow, success, link, surfaceCard, snackBg,
                highEnergyLine, highEnergyFill, buttonTint, selectedText, textSecondary)
                .withSemantic(danger, selected);
    }

}
