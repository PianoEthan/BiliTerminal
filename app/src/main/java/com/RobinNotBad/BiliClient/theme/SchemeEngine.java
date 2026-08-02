package com.RobinNotBad.BiliClient.theme;

import android.util.Log;

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
        int link = Blend.harmonize(0xFF66CCFF, seedArgb);
        int success = Blend.harmonize(0xFFBBFFBB, seedArgb);

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
                textSecondary);

        // 令牌覆盖
        if (manifest != null && manifest.colors != null) {
            palette = applyOverrides(palette, manifest);
        }
        return palette;
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
        }
        return new ThemePalette(textPrimary, textTransparent, ripple, windowBackground,
                accent, gray, accentLow, success, link, surfaceCard, snackBg,
                highEnergyLine, highEnergyFill, buttonTint, selectedText, textSecondary);
    }

    /** 从色值取 HCT hue/chroma（日志兜底） */
    public static void logSeed(int seed) {
        try {
            Hct hct = Hct.fromInt(seed);
            Log.d(TAG, "seed=" + Integer.toHexString(seed) + " hct=" + hct.getHue() + "," + hct.getChroma() + "," + hct.getTone());
        } catch (Exception ignored) {
        }
    }
}
