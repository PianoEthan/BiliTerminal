package com.RobinNotBad.BiliClient.theme;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * TintRules 判定表。这批用例锁的是「把谓词从 ThemeApplier 搬到 TintRules 前后
 * 逐值一致」，因此断言写的是搬运时的既有行为；批 2/4 之后有两条改成锁定新规则
 * （historicPinkListSplitsIntoDecorAndDanger、sameRgbDifferentAlphaShareOneTier）。
 */
public class TintRulesTest {

    /** 历史 8 值：7 个装饰粉归 accent，警示橙单列 danger（批 2 的拆分） */
    @Test
    public void historicPinkListSplitsIntoDecorAndDanger() {
        int[] decorPinks = {0xFBFB8787, 0xFFFE679A, 0xFFFB7299, 0xFFFF6699,
                0xFEF05D8E, 0xFFF05D8E, 0xFBFB8799};
        for (int c : decorPinks) {
            assertTrue("应归装饰粉: " + Integer.toHexString(c), TintRules.isDecorPink(c));
            assertFalse("装饰粉不该被判成警示: " + Integer.toHexString(c), TintRules.isDanger(c));
        }
        assertTrue(TintRules.isDanger(0xFFFF5722));
        assertFalse(TintRules.isDecorPink(0xFFFF5722));
        assertFalse(TintRules.isDanger(0xFFEBE0E2));
        assertFalse(TintRules.isDanger(0xFF66CCFF));
        assertFalse(TintRules.isDanger(0xFFBBFFBB));
    }

    /**
     * tintButton 那条路径不认语义、只认彩度：警示橙从 isDecorPink 摘出去之后，
     * 必须仍被 isSaturatedColor 兜住，否则橙红按钮会从"染成 accent"变成"完全不染"。
     */
    @Test
    public void dangerOrangeIsStillCaughtBySaturationFallback() {
        assertTrue(TintRules.isSaturatedColor(0xFFFF5722));
    }

    /**
     * 卡片描边的守卫：只收历史默认灰，色板令牌（选中态刚设的 selected/accentLow）不收。
     * 原先 setStrokeColor(p.gray) 是无条件的，遍历一到就把适配器 bind 里设的选中描边打回灰。
     */
    @Test
    public void strokeGuardKeepsPaletteTokens() {
        ThemePalette p = ThemePalette.builtinDark();
        assertTrue(TintRules.isDefaultStroke(0xDD383838));      // background_card 原描边
        assertTrue(TintRules.isDefaultStroke(0xFFFFFFFF));
        assertTrue(TintRules.isDefaultStroke(0xCC262626));
        assertFalse(TintRules.isDefaultStroke(0xFFFF5722));     // 业务警示橙描边

        assertFalse(TintRules.isPaletteToken(0xDD383838, p));   // 默认灰不是令牌 -> 可染
        assertTrue(TintRules.isPaletteToken(p.selected, p));
        assertTrue(TintRules.isPaletteToken(ThemePalette.withAlpha(p.accentLow, 0xFF), p));
        assertTrue(TintRules.isPaletteToken(p.gray, p));        // 幂等：染过之后不再重染
        // 组合起来才是要防的回归：选中描边既是"近白中性"又不是令牌之外的色
        assertFalse(TintRules.isDefaultStroke(p.danger));
    }

    /**
     * 按压态：原先 9 个调用点全写成 pressedStateList(x, x)，两态同色等于没反馈。
     * 暗底要提亮、亮底要压暗，alpha 不动，且不越界。
     */
    @Test
    public void pressedVariantMovesOppositeToBackground() {
        int dark = TintRules.pressedVariant(0xCC262626);
        assertEquals(0xCC, dark >>> 24);
        org.junit.Assert.assertTrue("暗底按压应更亮", ((dark >> 16) & 0xFF) > 0x26);
        int white = TintRules.pressedVariant(0xFFFFFFFF);
        assertEquals(0xFF, white >>> 24);
        org.junit.Assert.assertTrue("白底按压应更暗", ((white >> 16) & 0xFF) < 0xFF);
        // 选中粉亮度 0.57 > 0.5，走压暗档
        int pink = TintRules.pressedVariant(0xFFFB7299);
        org.junit.Assert.assertTrue(((pink >> 16) & 0xFF) < 0xFB);
        // 极值不溢出
        assertEquals(0xFF262626, TintRules.pressedVariant(0xFF000000));
        assertEquals(0xFFD9D9D9, TintRules.pressedVariant(0xFFFFFFFF));
    }

    @Test
    public void nearWhiteNeutralBoundaries() {
        assertTrue(TintRules.nearWhiteNeutral(0xFFEBE0E2)); // 现状正文色
        assertTrue(TintRules.nearWhiteNeutral(0xFFEAEAEA)); // 内置深色 accent
        assertTrue(TintRules.nearWhiteNeutral(0xFFFFFFFF));
        assertTrue(TintRules.nearWhiteNeutral(0x99FFFFFF)); // #99ffffff 也算（alpha 门槛 0x40）
        assertFalse(TintRules.nearWhiteNeutral(0x30FFFFFF)); // alpha < 0x40
        assertTrue(TintRules.nearWhiteNeutral(0xFFDDDDDD));  // 0xDD >= 0xB8
        assertFalse(TintRules.nearWhiteNeutral(0xFF999999)); // 0x99 < 0xB8
        assertFalse(TintRules.nearWhiteNeutral(0xFFFB7299)); // 通道差过大
    }

    @Test
    public void themedTextColorAcceptsDefaultAndNearWhite() {
        assertTrue(TintRules.isThemedTextColor(TintRules.DEFAULT_TEXT));
        assertTrue(TintRules.isThemedTextColor(0xFFDDDDDD));
        assertTrue(TintRules.isThemedTextColor(0x99FFFFFF));
        // 0x50FEFEFE 的 alpha 是 0x50，仍过 0x40 门槛 → 它也满足"近白"。
        // 所以 tintView 里必须把"半透明默认值"分支排在 isThemedTextColor 之前，否则会被染成正文色。
        assertTrue(TintRules.isThemedTextColor(TintRules.DEFAULT_TEXT_TRANSPARENT));
        assertFalse(TintRules.isThemedTextColor(0xFF66CCFF));
    }

    @Test
    public void neutralGrayTextBand() {
        assertTrue(TintRules.isNeutralGrayText(0xFF999999));
        assertTrue(TintRules.isNeutralGrayText(0xFFAAAAAA));
        assertTrue(TintRules.isNeutralGrayText(0xFFA2A2A2));
        assertTrue(TintRules.isNeutralGrayText(0xFF8C8C8C));
        assertFalse(TintRules.isNeutralGrayText(0xFFDDDDDD)); // 归近白族
        assertFalse(TintRules.isNeutralGrayText(0xFF444444)); // 低于 0x50
        assertFalse(TintRules.isNeutralGrayText(0x80999999)); // alpha < 0xA0
    }

    @Test
    public void defaultSurfaceFamily() {
        assertTrue(TintRules.isDefaultSurface(TintRules.DEFAULT_BUTTON_BG));
        assertTrue(TintRules.isDefaultSurface(TintRules.DEFAULT_BUTTON_BG_ALT));
        assertTrue(TintRules.isDefaultSurface(0xFF1E1E1E));
        assertFalse(TintRules.isDefaultSurface(0xFF808080)); // 亮灰不是默认底
        assertFalse(TintRules.isDefaultSurface(0x318C8C8C)); // alpha < 0x60
        assertFalse(TintRules.isDefaultSurface(0xFF66CCFF));
    }

    @Test
    public void saturatedColor() {
        assertTrue(TintRules.isSaturatedColor(0xFF66CCFF));
        assertTrue(TintRules.isSaturatedColor(0xFF74A864)); // 本地视频绿标
        assertFalse(TintRules.isSaturatedColor(0xFFEBE0E2));
        assertFalse(TintRules.isSaturatedColor(0xFF262626));
    }

    @Test
    public void backgroundTierMapping() {
        assertEquals(TintRules.BG_SATURATED, TintRules.backgroundTier(0xFF66CCFF));
        assertEquals(TintRules.BG_SATURATED, TintRules.backgroundTier(0xFF74A864));
        assertEquals(TintRules.BG_SURFACE, TintRules.backgroundTier(0xCC262626));
        assertEquals(TintRules.BG_NONE, TintRules.backgroundTier(0xFFFFFFFF));   // 近白不动
        assertEquals(TintRules.BG_NONE, TintRules.backgroundTier(0xB2000000));   // 压暗蒙层不动
    }

    @Test
    /**
     * 批 4 改的就是这条：原先按 alpha 分档，同一个 #8C8C8C 写成 #318C8C8C 走
     * divider、写成 #818C8C8C 走 surface，一个变浅灰一个变近黑。现按源亮度分档。
     */
    public void sameRgbDifferentAlphaShareOneTier() {
        assertEquals(TintRules.BG_DIVIDER, TintRules.backgroundTier(0x318C8C8C));
        assertEquals(TintRules.BG_DIVIDER, TintRules.backgroundTier(0x818C8C8C));
        assertEquals(TintRules.BG_DIVIDER, TintRules.backgroundTier(0xFF8C8C8C));
        assertEquals(TintRules.BG_DIVIDER, TintRules.backgroundTier(0xFFA0A0A0));
        // 深灰实心仍是 surface 档，浅灰实心按分隔档处理，近白不动
        assertEquals(TintRules.BG_SURFACE, TintRules.backgroundTier(0xDD262626));
        assertEquals(TintRules.BG_SURFACE, TintRules.backgroundTier(0x30303030));
        assertEquals(TintRules.BG_NONE, TintRules.backgroundTier(0xFFB8B8B8));
    }

    /** 不透明纯黑当前会被判成 BG_SURFACE（会被染成 #262626），属既有行为，记录待批 4 复核。 */
    @Test
    public void opaqueBlackBackgroundIsSurfaceTier() {
        assertEquals(TintRules.BG_SURFACE, TintRules.backgroundTier(0xFF000000));
    }

    @Test
    public void classifySampleCases() {
        assertArrayEquals(new int[]{TintRules.ICON_SKIP, 0, 0, 0},
                TintRules.classifySample(false, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        // 纯白单色图标 -> 文字色
        assertArrayEquals(new int[]{TintRules.ICON_NEUTRAL, 0xFF, 1, 0xFF},
                TintRules.classifySample(true, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0, 0xFF));
        // 历史粉图标（整只单色 #FB7299）-> 强调色
        assertArrayEquals(new int[]{TintRules.ICON_SATURATED, 0xFB, 1, 0xAC},
                TintRules.classifySample(true, 0xFB, 0x72, 0x99, 0xFB, 0x72, 0x99, 0xFB, 0, 0xAC));
        // 灰阶圆底 -> 表面色
        assertArrayEquals(new int[]{TintRules.ICON_GRAYSCALE, 0x90, 1, 0x70},
                TintRules.classifySample(true, 0x70, 0x70, 0x70, 0x70, 0x70, 0x70, 0x90, 0, 0x70));
        // 半透明纯黑蒙层 -> 跳过
        assertArrayEquals(new int[]{TintRules.ICON_SKIP, 0x80, 1, 0},
                TintRules.classifySample(true, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80, 0, 0));
        // 多色（照片）-> 跳过
        assertArrayEquals(new int[]{TintRules.ICON_SKIP, 0xFF, 2, 0x80},
                TintRules.classifySample(true, 0x10, 0x20, 0x30, 0xFF, 0xEE, 0xDD, 0xFF, 0xEF, 0x80));
        // 播放键（#90707070 圆底 + #fff 笔画）：明度跨度 0x8F > 0x30 -> 判多色跳过，
        // 所以它不会被通用遍历染色，状态图标必须显式指定颜色。
        assertArrayEquals(new int[]{TintRules.ICON_SKIP, 0x90, 2, 0xB8},
                TintRules.classifySample(true, 0x70, 0x70, 0x70, 0xFF, 0xFF, 0xFF, 0x90, 0x8F, 0xB8));
    }

    /**
     * background_card：solid #dd262626 + stroke #dd383838，跨度只有 0x12。
     * 跨度分类（0x30 那档，判单色/多色）会把它当单色灰阶，整张套一色就把描边抹进填充里；
     * 但「画满像素的 RGB 跨度」coreSpan=0x12 > 6 -> c[2]=2，背景路径的闸门拒绝染色。
     * 两个跨度量的是不同的东西：前者含半透明边缘所以必须粗，后者只看实心底所以可以细。
     */
    @Test
    public void twoTierGrayscaleIsFlaggedSoStrokeSurvives() {
        int[] c = TintRules.classifySample(true, 0x26, 0x26, 0x26, 0x38, 0x38, 0x38, 0xDD, 0x12, 0x24);
        assertEquals(TintRules.ICON_GRAYSCALE, c[0]);
        assertEquals(0xDD, c[1]);
        assertEquals(2, c[2]); // solid+stroke 跨度 0x12 -> 两档
        // 档位来源是「主导档」而不是最大采样值：stroke 在 16px 盒子里占比极高，
        // 按最大值选档会把深灰卡片染成中灰（变亮），按主导值才压回 surfaceCard
        assertEquals(0x24, c[3]);
        assertEquals(ThemePalette.builtinDark().surfaceCard,
                TintRules.grayScaleSurface(c[3], ThemePalette.builtinDark()));
        // 闸门：深色主题（表面色本身是深灰）跳过染色保住描边；浅色主题表面色是亮的，
        // 不跟随会留下深灰框，所以照常压平
        assertTrue(TintRules.preserveMultiTierGrayscale(ThemePalette.builtinDark()));
        assertFalse(TintRules.preserveMultiTierGrayscale(ThemePalette.builtinLight()));
        assertTrue(TintRules.preserveMultiTierGrayscale(SchemeEngine.generate(0xFF1565C0, true, 0, null)));
        assertFalse(TintRules.preserveMultiTierGrayscale(SchemeEngine.generate(0xFF1565C0, false, 0, null)));
        assertEquals(1, TintRules.classifySample(true, 0x70, 0x70, 0x70,
                0x70, 0x70, 0x70, 0x90, 0, 0x70)[2]); // 单档灰阶照旧可染
    }

    /**
     * 两档信号用「画满像素（alpha>=0xB0）的 RGB 跨度」，不用直方图占比数档：
     * Canvas 往透明 ARGB_8888 上画时抗锯齿只改 alpha 不改 RGB，单色 shape 的 core 跨度
     * 必然贴近 0，而 solid #26 + stroke #38 的 0x12 会如实张开。
     * 占比数档那套在 16px 盒子里对采样细节太敏感（同一张 shape 只换超采样倍率就从 1 档
     * 跳到 2 档），描边闸门不能建在上面。
     */
    @Test
    public void multiTierComesFromCoreSpanNotBucketRuns() {
        assertEquals(1, tiersOfCoreSpan(0));                // 纯色：边缘只是变淡
        assertEquals(1, tiersOfCoreSpan(TintRules.CORE_TIER_SPAN));
        assertEquals(2, tiersOfCoreSpan(TintRules.CORE_TIER_SPAN + 1));
        assertEquals(2, tiersOfCoreSpan(0x12));             // background_card 的真实跨度
        assertEquals(TintRules.ICON_GRAYSCALE,
                TintRules.classifySample(true, 0x26, 0x26, 0x26, 0x38, 0x38, 0x38,
                        0xDD, 0x12, 0x20)[0]);
    }

    private static int tiersOfCoreSpan(int coreSpan) {
        int v = 0x26 + coreSpan;
        return TintRules.classifySample(true, 0x26, 0x26, 0x26, v, v, v,
                0xDD, coreSpan, 0x20)[2];
    }

    /**
     * 「哪些像素算实心」这一步原先内联在 ThemeApplier 的采样循环里，JVM 测不到：
     * 把 coreMin 初值写成 0 会让任何带亮色实心像素的 shape 恒判两档，深色主题的灰阶背景
     * 从此永不跟随色板，而调用点看不出任何异常。抽成 TintRules.CoreSpan 后这里能点死它。
     */
    @Test
    public void coreSpanOnlyCountsSolidPixels() {
        TintRules.CoreSpan c = new TintRules.CoreSpan();
        assertEquals(0, c.span());                    // 一个像素都没喂
        c.add(0x26, 0xDD);
        assertEquals(0, c.span());                    // 单色：min 不能被初值顶开
        c.add(0x38, 0xDD);
        assertEquals(0x12, c.span());                 // background_card 的 solid+stroke
        c.add(0xFF, TintRules.CORE_ALPHA - 1);        // 边缘像素不算实心
        assertEquals(0x12, c.span());
        TintRules.CoreSpan weak = new TintRules.CoreSpan();
        weak.add(0x26, 0x75);
        weak.add(0xF0, 0x75);                         // 整只半透明的 shape
        assertEquals(0, weak.span());                 // core 为空 -> 判单档，由主导档兜底
    }

    /**
     * 圆角 shape 的代表 alpha 必须是主体众数，不是抗锯齿边缘的最小值：
     * 16×16 采样里四角各有一撮 alpha 0x40..0x60 的像素，旧实现取 aMin 后
     * #dd262626 的热评卡被当成约 50% 的蒙层，叠黑只剩 #0c0c0c，卡片看起来消失。
     */
    @Test
    public void roundedShapeUsesBodyAlphaNotCornerEdges() {
        TintRules.SampleStats stats = new TintRules.SampleStats();
        for (int i = 0; i < 180; i++) stats.add(0xDD262626);
        for (int a = 0x40; a <= 0x60; a++) stats.add((a << 24) | 0x262626);
        int[] c = stats.classify();
        assertEquals(TintRules.ICON_GRAYSCALE, c[0]);
        assertEquals(0xDD, c[1]);
        assertEquals(1, c[2]);
        assertEquals(0x24, c[3]);
        // 令牌 0xCC、源 0xDD：tint 的 SRC_IN 还会乘一次源 alpha，所以传入的 tint alpha
        // 要先反解成 0xEB，合成后才是令牌自己的 0xCC，叠黑约 #1e1e1e。
        // 按旧的最小值 0x40 直接传，合成后只剩 0x09，叠黑就是 #0a0a0a。
        int tinted = TintRules.tintColor(ThemePalette.builtinDark().surfaceCard, c[1]);
        assertEquals(0xEB, tinted >>> 24);
        int composedAlpha = (0xEB * 0xDD + 127) / 0xFF;
        assertEquals(0xCC, composedAlpha);
        assertEquals(0x1E, (composedAlpha * 0x26 + 127) / 0xFF);
    }

    /**
     * tint alpha 反解：最终合成等于 min(令牌 alpha, 源 alpha)，
     * 源不透明时原样传令牌 alpha，源半透明时按比例抬到上限。
     */
    @Test
    public void tintAlphaCompensatesSourceAlpha() {
        assertEquals(0xEB, TintRules.tintAlphaFor(0xCC, 0xDD)); // 半透明卡片
        assertEquals(0x73, TintRules.tintAlphaFor(0x73, 0xFF)); // 浅色 gray 叠白底
        assertEquals(0xFF, TintRules.tintAlphaFor(0xFF, 0x80)); // 不透明令牌、半透明源
        assertEquals(0xFF, TintRules.tintAlphaFor(0x70, 0x31)); // 源已比令牌透明：tint 拉满，合成仍是源的 0x31
        assertEquals(0xFF, TintRules.tintAlphaFor(0xFF, 0xFF)); // 不越界
        assertEquals(0, TintRules.tintAlphaFor(0xCC, 0));        // 零 alpha 不除零
        int lightGray = TintRules.tintColor(ThemePalette.builtinLight().gray, 0xFF);
        assertEquals(0x73, lightGray >>> 24);
        assertEquals(0x000000, lightGray & 0x00FFFFFF);
    }

    @Test
    public void dominantAlphaPrefersBodyAndTieBreaksOpaque() {
        int[] h = new int[TintRules.ALPHA_LEVELS];
        h[0xDD] = 180;
        for (int a = 0x40; a <= 0x60; a++) h[a] = 1;
        assertEquals(0xDD, TintRules.dominantAlphaOf(h));
        int[] tie = new int[TintRules.ALPHA_LEVELS];
        tie[0x80] = 5;
        tie[0xC0] = 5;
        assertEquals(0xC0, TintRules.dominantAlphaOf(tie));
        assertEquals(0, TintRules.dominantAlphaOf(new int[TintRules.ALPHA_LEVELS]));
        assertEquals(0, TintRules.dominantAlphaOf(null));
        assertArrayEquals(new int[]{TintRules.ICON_SKIP, 0, 0, 0},
                new TintRules.SampleStats().classify());
    }

    /**
     * 令牌自带的 alpha 不能被抬成不透明：浅色主题的 gray 写成 #73000000，
     * 叠在白底上才是 #8C8C8C；一旦 withAlpha(gray, 0xFF) 就成了纯黑实心底。
     */
    @Test
    public void applyAlphaNeverMakesTheTokenMoreOpaqueThanItIs() {
        ThemePalette light = ThemePalette.builtinLight();
        int divider = TintRules.applyAlpha(light.gray, 0xFF);  // 源是不透明中灰
        assertEquals(0x73, divider >>> 24);          // 绝不抬到 FF，否则就是黑底
        assertTrue((divider & 0x00FFFFFF) == 0);
        int surface = TintRules.applyAlpha(light.surfaceCard, 0xDD); // 白底 0xFF
        assertEquals(0xDD, surface >>> 24);
        assertEquals(0xFFFFFF, surface & 0x00FFFFFF);
        // 深色主题的 gray 本就带 alpha，取小后仍落在它自己之下
        ThemePalette dark = ThemePalette.builtinDark();
        assertEquals(0x70, TintRules.applyAlpha(dark.gray, 0xDD) >>> 24);
        assertEquals(0x31, TintRules.applyAlpha(dark.gray, 0x31) >>> 24);
    }

    /**
     * 档位按**主导档**选，不是按采样上限：16px 盒子里高 density 的 stroke 会主导面积，
     * 拿上限选档会把深色卡片判成 divider 压亮。最坏情况也得是"压回深色"。
     */
    @Test
    public void grayScaleSurfaceUsesDominantLevel() {
        ThemePalette p = ThemePalette.builtinDark();
        assertEquals(p.surfaceCard, TintRules.grayScaleSurface(0x20, p));
        assertEquals(p.surfaceCard, TintRules.grayScaleSurface(0x40, p));  // 边界仍算深色
        assertEquals(p.gray, TintRules.grayScaleSurface(0x41, p));
        assertEquals(p.gray, TintRules.grayScaleSurface(0x70, p));
        assertEquals(p.textSecondary, TintRules.grayScaleSurface(0xC8, p));
        // background_card：即便两档闸门失灵（被判 1 档），主导档是深灰 -> 仍压回 surfaceCard
        int[] card = TintRules.classifySample(true, 0x26, 0x26, 0x26, 0x38, 0x38, 0x38, 0xDD, 0, 0x24);
        assertEquals(p.surfaceCard, TintRules.grayScaleSurface(card[3], p));
        // density>=3.5 时 2dp 描边环的占比反超填充，主导档变成 stroke 的 #38
        assertEquals(p.surfaceCard, TintRules.grayScaleSurface(0x3C, p));
    }

    /**
     * 主导档取**桶中点**：取格下沿会把亮度系统性低估 0..7，grayScaleSurface 的深色上限
     * 就悄悄从 0x40 变成 0x48。这段逻辑原先内联在 ThemeApplier 的采样循环里，
     * JVM 完全测不到（写错成 b 也全绿），所以抽到这里。
     */
    @Test
    public void dominantOfUsesBucketMidpoint() {
        int[] hist = new int[TintRules.HIST_BUCKETS];
        hist[4] = 100; hist[7] = 30;                      // 填充占多数
        assertEquals(0x24, TintRules.dominantOf(hist));
        hist[4] = 30; hist[7] = 100;                       // 描边环占多数
        assertEquals(0x3C, TintRules.dominantOf(hist));
        int[] tie = new int[TintRules.HIST_BUCKETS];
        tie[4] = 50; tie[9] = 50;                          // 平票取更暗的一格
        assertEquals(0x24, TintRules.dominantOf(tie));
        assertEquals(0, TintRules.dominantOf(new int[TintRules.HIST_BUCKETS])); // 没采到像素
        assertEquals(0, TintRules.dominantOf(null));
    }

    /**
     * 采样链最后两寸也不可依赖「只在 Android 里写一遍」：
     * 格号写成 >>4 一样编译、一样不越界，但 #70 中灰会落进 0x38 那格被判成深底；
     * 门槛写成绝对值漂移一点，background_card 的 0xDD 就会掉出实心集、描边被压平。
     */
    @Test
    public void samplingThresholdsArePinned() {
        assertEquals(0x26, TintRules.levelOf(0x26, 0x26, 0x26));   // 写成 /2 会翻倍落错格
        assertEquals(0xA0, TintRules.levelOf(0xA0, 0xA0, 0xA0));
        assertEquals(4, TintRules.histIndex(TintRules.levelOf(0x26, 0x26, 0x26)));
        assertEquals(20, TintRules.histIndex(TintRules.levelOf(0xA0, 0xA0, 0xA0)));
        assertEquals(7, TintRules.histIndex(0x38));      // 它的 stroke
        assertEquals(14, TintRules.histIndex(0x70));     // colors.xml 的 gray，必须落中灰档
        assertEquals(0x24, TintRules.dominantOf(singletonHist(0x26)));   // 格下沿 0x20 + 4
        assertEquals(0x74, TintRules.dominantOf(singletonHist(0x70)));
        assertEquals(0, TintRules.histIndex(0));
        assertEquals(TintRules.HIST_BUCKETS - 1, TintRules.histIndex(255));  // 数组长度与分格方式必须同一套
        assertEquals(32, TintRules.HIST_BUCKETS);
        assertTrue(0xDD >= TintRules.CORE_ALPHA);        // 卡片的 solid/stroke 都得算实心
        assertTrue(0x80 < TintRules.CORE_ALPHA);         // edittext 那族低 alpha 实心被排除是有意的
        // 采样下限与实心门槛必须成对：背景要能被采到（进 any/aMin/直方图），但不能算实心
        assertEquals(0x40, TintRules.SAMPLE_ALPHA_FLOOR);
        assertTrue(TintRules.SAMPLE_ALPHA_FLOOR < TintRules.CORE_ALPHA);
        assertTrue(0x80 >= TintRules.SAMPLE_ALPHA_FLOOR && 0x80 < TintRules.CORE_ALPHA);
        // 门槛只能被"故意改代码"移动，不能被"顺手调一下"移动：两条都取绝对值
        assertEquals(0xB0, TintRules.CORE_ALPHA);
        TintRules.CoreSpan band = new TintRules.CoreSpan();
        band.add(0x26, 0xB0);                       // 门槛本身算实心
        band.add(0x38, 0xCC);                       // #cc262626 / #cc808080 / #cd000000 都在这带里
        assertEquals(0x12, band.span());            // 门槛若被抬到 0xD0，这里归 0、描边被压平
    }

    private static int[] singletonHist(int level) {
        int[] h = new int[TintRules.HIST_BUCKETS];
        h[TintRules.histIndex(level)] = 1;
        return h;
    }

    /**
     * 批 4 的 hint 规则原先整条内联在 ThemeApplier 里，是这轮最后一个「测不到的门槛」。
     * 布局实测 6 处 hint：5 处 #80ffffff + 1 处 #88ffffff，两族都得进这条分支、
     * 且只换 RGB 不动 alpha（改前被一律压成 textTransparent 自带的 0x50，肉眼可见变暗）。
     */
    @Test
    public void hintKeepsItsOwnAlphaBand() {
        assertTrue(TintRules.isThemedTextColor(0x80FFFFFF));
        assertTrue(TintRules.isThemedTextColor(0x88FFFFFF));
        assertFalse(TintRules.isThemedTextColor(0xFF808080));   // 写死的中灰 hint 不拉进色板
        ThemePalette dark = ThemePalette.builtinDark(), light = ThemePalette.builtinLight();
        assertEquals(0x80, TintRules.hintTextColor(0x80FFFFFF, dark) >>> 24);
        assertEquals(0x88, TintRules.hintTextColor(0x88FFFFFF, dark) >>> 24);
        assertEquals(dark.textTransparent & 0xFFFFFF, TintRules.hintTextColor(0x80FFFFFF, dark) & 0xFFFFFF);
        // 浅色侧是「刻意跟随布局档」：0x80 黑比令牌自带的 0x4D 更重。布局的 #80ffffff 是
        // 深色时代写的值，把它的不透明度一起搬进浅色会让 hint 偏深一档 —— 观感要真机判，
        // 但规则本身先固定住，日后想改成按明暗分流必须显式改这两条断言
        assertEquals(0x80, TintRules.hintTextColor(0x80FFFFFF, light) >>> 24);
        assertTrue(0x80 > light.textTransparent >>> 24);
        assertEquals(light.textTransparent & 0xFFFFFF,
                TintRules.hintTextColor(0x80FFFFFF, light) & 0xFFFFFF);
        assertTrue(0x80 > dark.textTransparent >>> 24);          // 保住的那档确实比令牌亮
    }

    @Test
    public void sameRgbIgnoresAlpha() {
        assertTrue(TintRules.sameRgb(0xCC262626, 0xFF262626));
        assertFalse(TintRules.sameRgb(0xFF262626, 0xFF262627));
    }

    @Test
    public void selectedTokenCollidesWithDecorPinkSoOwnershipIsRequired() {
        ThemePalette p = ThemePalette.builtinDark();
        // 默认深色的 selected 就是历史装饰粉本身：树级染色在 bind 之后跑时，
        // 「装饰粉->accent」规则会原地吃掉代码设的选中色，所以必须记语义令牌
        assertTrue(TintRules.isDecorPink(p.selected));
        assertEquals(TintRules.SEM_SELECTED, TintRules.semanticToken(p.selected, p));
        assertEquals(TintRules.SEM_TEXT_PRIMARY, TintRules.semanticToken(p.textPrimary, p));
        assertEquals(TintRules.SEM_TEXT_TRANSPARENT, TintRules.semanticToken(p.textTransparent, p));
        // @color/pink（#FB8787）不是任何令牌 -> 仍交给字面量启发式，去粉意图不变
        assertEquals(TintRules.SEM_NONE, TintRules.semanticToken(0xFFFB8787, p));
    }

    @Test
    public void semanticTagKeepsTextAndIconNibblesIndependent() {
        int tag = TintRules.semanticWithText(0, TintRules.SEM_SELECTED);
        tag = TintRules.semanticWithIcon(tag, TintRules.SEM_TEXT_PRIMARY);
        assertEquals(TintRules.SEM_SELECTED, TintRules.semanticTextToken(tag));
        assertEquals(TintRules.SEM_TEXT_PRIMARY, TintRules.semanticIconToken(tag));

        // 复用 holder 后状态翻转：只改文字档，图标档要留着
        tag = TintRules.semanticWithText(tag, TintRules.SEM_TEXT_TRANSPARENT);
        assertEquals(TintRules.SEM_TEXT_TRANSPARENT, TintRules.semanticTextToken(tag));
        assertEquals(TintRules.SEM_TEXT_PRIMARY, TintRules.semanticIconToken(tag));

        ThemePalette p = ThemePalette.builtinDark();
        assertEquals(p.selected, TintRules.semanticColor(TintRules.SEM_SELECTED, p));
        assertEquals(p.textPrimary, TintRules.semanticColor(TintRules.SEM_TEXT_PRIMARY, p));
        assertEquals(0, TintRules.semanticColor(TintRules.SEM_NONE, p));
        assertEquals(0, TintRules.semanticTextToken(0));
        assertEquals(0, TintRules.semanticIconToken(0));
    }
}
