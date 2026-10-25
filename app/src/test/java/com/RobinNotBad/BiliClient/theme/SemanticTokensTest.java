package com.RobinNotBad.BiliClient.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

/**
 * 语义令牌（danger/selected）的取值与写回规则。
 * 这两个令牌承载「警示」与「已选中」语义，不允许被装饰强调色中性化吞掉。
 */
public class SemanticTokensTest {

    private static int alpha(int c) { return c >>> 24; }

    /** 通道极差：0=纯灰，越大越有彩度。语义色必须读得出"是彩色" */
    private static int chromaSpread(int c) {
        int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
        return Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b));
    }

    @Test
    public void builtinDarkKeepsHistoricSemanticValues() {
        ThemePalette p = ThemePalette.builtinDark();
        assertEquals(0xFFFF5722, p.danger);   // 警示橙：主题系统之前的原值
        assertEquals(0xFFFB7299, p.selected);
        assertEquals(0xFBFB8787, p.accent);
        assertEquals(0x99FB8799, p.accentLow);
        org.junit.Assert.assertTrue(p.originalColors);
        assertEquals(0xFFCF4B5F, p.color(0xFFCF4B5F, p.selected));
        assertEquals(0xFFFFFFFF, p.color(0xFFFFFFFF, p.windowBackground));
    }

    @Test
    public void builtinLightSemanticValuesAreNotInk() {
        ThemePalette p = ThemePalette.builtinLight();
        assertEquals(0xFFC62828, p.danger);
        assertEquals(0xFFC2185B, p.selected);
        assertNotEquals(p.accent, p.danger);
        assertNotEquals(p.accent, p.selected);
        assertNotEquals(p.danger, p.selected);
    }

    /** 16 参构造的兜底必须是历史值，不是 0（0 = 全透明黑，会直接看不见） */
    @Test
    public void sixteenArgCtorFallsBackToHistoricDefaults() {
        ThemePalette p = new ThemePalette(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
        assertEquals(ThemePalette.DEFAULT_DANGER, p.danger);
        assertEquals(ThemePalette.DEFAULT_SELECTED, p.selected);
        assertEquals(16, p.textSecondary); // 其余参数顺序不能被新字段带偏
        assertEquals(1, p.textPrimary);
    }

    @Test
    public void withSemanticReplacesOnlyTheTwoTokens() {
        ThemePalette base = ThemePalette.builtinDark();
        ThemePalette p = base.withSemantic(0xFF112233, 0xFF445566);
        assertEquals(0xFF112233, p.danger);
        assertEquals(0xFF445566, p.selected);
        // 原始色标志必须跟着复制，否则 SchemeEngine 写回语义令牌后原始色会静默失效
        org.junit.Assert.assertTrue(p.originalColors);
        org.junit.Assert.assertFalse(ThemePalette.builtinDark(false)
                .withSemantic(0xFF112233, 0xFF445566).originalColors);
        assertEquals(base.accent, p.accent);
        assertEquals(base.textPrimary, p.textPrimary);
        assertEquals(base.surfaceCard, p.surfaceCard);
        assertEquals(base.buttonTint, p.buttonTint);
        assertEquals(base.textSecondary, p.textSecondary);
    }

    /** 生成主题：语义令牌由种子派生，但不得退化成 accent，也不得变透明 */
    @Test
    public void generatedThemeDerivesDistinctSemanticTokens() {
        int seed = 0xFF1565C0; // 蓝种子：与 danger/selected 的历史 hue 差得很远
        ThemePalette dark = SchemeEngine.generate(seed, true, 0, null);
        ThemePalette light = SchemeEngine.generate(seed, false, 0, null);
        for (ThemePalette p : new ThemePalette[]{dark, light}) {
            assertEquals(0xFF, alpha(p.danger));
            assertEquals(0xFF, alpha(p.selected));
            assertNotEquals("danger 被 accent 吞掉", p.accent, p.danger);
            assertNotEquals("selected 与 danger 撞色", p.danger, p.selected);
            org.junit.Assert.assertTrue("danger 褪成灰", chromaSpread(p.danger) >= 0x10);
            org.junit.Assert.assertTrue("selected 褪成灰", chromaSpread(p.selected) >= 0x10);
        }
    }

    /**
     * Blend.harmonize 只混色相、不改亮度，而 link/success 的基底用的是深色档字面量。
     * 不压 tone 的话浅色主题在白底上只剩 1.2~1.8 对比度（等于看不见），
     * 但深色侧必须维持原样——这条同时钉住两侧。
     */
    @Test
    public void functionalTokensRespectModeTone() {
        int seed = 0xFF1565C0;
        ThemePalette light = SchemeEngine.generate(seed, false, 0, null);
        for (int c : new int[]{light.link, light.success, light.danger, light.selected}) {
            org.junit.Assert.assertTrue("浅色功能色 tone 过高，白底读不出: " + Integer.toHexString(c),
                    tone(c) <= 55.0);
        }
        ThemePalette dark = SchemeEngine.generate(seed, true, 0, null);
        org.junit.Assert.assertTrue("深色侧被改动", tone(dark.link) >= 70.0);
        org.junit.Assert.assertTrue("深色侧被改动", tone(dark.success) >= 90.0);
    }

    private static double tone(int argb) {
        return com.google.material.color.hct.Hct.fromInt(argb).getTone();
    }

    /**
     * manifest 的 danger/selected 必须真的落进色板。
     * applyOverrides 末尾原来是用 16 参构造重建 palette 的，那两个键会被
     * 委托构造的历史兜底值静默覆盖——主题作者写了却不生效，最难查的那种。
     */
    @Test
    public void manifestOverridesReachTheSemanticTokens() {
        com.RobinNotBad.BiliClient.theme.model.ThemeManifest m =
                new com.RobinNotBad.BiliClient.theme.model.ThemeManifest();
        m.format = com.RobinNotBad.BiliClient.theme.model.ThemeManifest.FORMAT;
        m.id = "test.override";
        com.RobinNotBad.BiliClient.theme.model.ThemeManifest.Colors c =
                new com.RobinNotBad.BiliClient.theme.model.ThemeManifest.Colors();
        java.util.Map<String, String> over = new java.util.HashMap<>();
        over.put("danger", "#FF123456");
        over.put("selected", "#FF654321");
        over.put("someFutureKey", "#FFAABBCC"); // 未知键只忽略，不能报错
        c.override = over;
        m.colors = c;

        ThemePalette p = SchemeEngine.generate(0xFF1565C0, true, 0, m);
        assertEquals(0xFF123456, p.danger);
        assertEquals(0xFF654321, p.selected);
        assertNotEquals(ThemePalette.DEFAULT_DANGER, p.danger);
    }

    /** parseColor 去掉 TextUtils 依赖后，各写法与坏值的行为必须原样 */
    @Test
    public void parseColorHandlesFormsAndBadValues() {
        assertEquals(Integer.valueOf(0xFFAABBCC),
                com.RobinNotBad.BiliClient.theme.model.ThemeManifest.parseColor("#abc"));
        assertEquals(Integer.valueOf(0xFF1565C0),
                com.RobinNotBad.BiliClient.theme.model.ThemeManifest.parseColor("#1565C0"));
        assertEquals(Integer.valueOf(0x80FF5722),
                com.RobinNotBad.BiliClient.theme.model.ThemeManifest.parseColor("#80FF5722"));
        assertEquals(null, com.RobinNotBad.BiliClient.theme.model.ThemeManifest.parseColor(null));
        assertEquals(null, com.RobinNotBad.BiliClient.theme.model.ThemeManifest.parseColor(""));
        assertEquals(null, com.RobinNotBad.BiliClient.theme.model.ThemeManifest.parseColor("zzzzzz"));
    }

    /** 同种子两次生成必须一致（派生不能带隐藏状态） */
    @Test
    public void derivationIsDeterministic() {
        int seed = 0xFF9C27B0;
        ThemePalette a = SchemeEngine.generate(seed, true, 40, null);
        ThemePalette b = SchemeEngine.generate(seed, true, 40, null);
        assertEquals(a.danger, b.danger);
        assertEquals(a.selected, b.selected);
    }

    @Test
    public void paletteBadgesKeepContrastInBothModes() {
        for (ThemePalette p : new ThemePalette[]{ThemePalette.builtinDark(false), ThemePalette.builtinLight(),
                SchemeEngine.generate(0xFF1565C0, true, 35, null),
                SchemeEngine.generate(0xFF1565C0, false, 35, null)}) {
            org.junit.Assert.assertFalse(p.originalColors);
            double ratio = contrast(p.windowBackground, p.selected);
            org.junit.Assert.assertTrue("徽章文字对比不足: " + ratio, ratio >= 4.5);
            assertEquals(p.selected, p.color(0xFFCF4B5F, p.selected));
        }
    }

    @Test
    public void medalBadgeKeepsOriginalOrReadablePaletteColors() {
        ThemePalette original = ThemePalette.builtinDark();
        assertEquals(0xFFFFFFFF, original.color(0xFFFFFFFF, original.windowBackground));
        assertEquals(0x8C9EBAE8, original.color(0x8C9EBAE8, original.link));
        ThemePalette dark = ThemePalette.builtinDark(false);
        for (ThemePalette p : new ThemePalette[]{dark, ThemePalette.builtinLight(),
                SchemeEngine.generate(0xFF1565C0, true, 35, null),
                SchemeEngine.generate(0xFF1565C0, false, 35, null)}) {
            for (int surface : new int[]{p.windowBackground, p.surfaceCard}) {
                int bg = overSurface(p.link, surface);
                double ratio = contrast(p.windowBackground, bg);
                org.junit.Assert.assertTrue("勋章字对比不足: " + ratio, ratio >= 4.5);
            }
        }
        int darkBgOnCard = overSurface(dark.accentLow, dark.surfaceCard);
        org.junit.Assert.assertTrue(contrast(dark.selectedText, darkBgOnCard) < 4.5);
    }

    /** 半透明令牌叠到某个不透明表面上，得到实际看到的颜色 */
    private static int overSurface(int token, int surface) {
        double a = (token >>> 24) / 255.0;
        int r = (int) (((token >> 16) & 0xFF) * a + ((surface >> 16) & 0xFF) * (1 - a) + 0.5);
        int g = (int) (((token >> 8) & 0xFF) * a + ((surface >> 8) & 0xFF) * (1 - a) + 0.5);
        int b = (int) ((token & 0xFF) * a + (surface & 0xFF) * (1 - a) + 0.5);
        return (r << 16) | (g << 8) | b;
    }

    private static double contrast(int a, int b) {
        double la = luminance(a), lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminance(int c) {
        double[] v = {((c >> 16) & 0xFF) / 255.0, ((c >> 8) & 0xFF) / 255.0, (c & 0xFF) / 255.0};
        for (int i = 0; i < 3; i++)
            v[i] = v[i] <= 0.04045 ? v[i] / 12.92 : Math.pow((v[i] + 0.055) / 1.055, 2.4);
        return 0.2126 * v[0] + 0.7152 * v[1] + 0.0722 * v[2];
    }
}
