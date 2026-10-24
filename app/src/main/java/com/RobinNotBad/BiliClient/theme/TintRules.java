package com.RobinNotBad.BiliClient.theme;

/**
 * 染色判定规则：只依赖 ARGB 整数的纯函数集合，不 import android，可靠 JVM 单测覆盖。
 * 规则本身保守——只认"当前等于历史默认值"的颜色，业务色靠彩度/通道差排除。
 */
public final class TintRules {

    private TintRules() {
    }

    /** 默认深色主题的精确值（染色守卫用） */
    public static final int DEFAULT_TEXT = 0xFFEBE0E2;
    public static final int DEFAULT_TEXT_TRANSPARENT = 0x50FEFEFE;
    public static final int DEFAULT_BUTTON_BG = 0xCC262626;
    /** 用户页关注/私信等按钮的另一档默认灰 */
    public static final int DEFAULT_BUTTON_BG_ALT = 0xDD262626;

    /** 采样分类结果：0=跳过 1=近白中性 2=有彩度 3=灰阶中性 */
    public static final int ICON_SKIP = 0, ICON_NEUTRAL = 1, ICON_SATURATED = 2, ICON_GRAYSCALE = 3;

    /**
     * hint 的取色：只换 RGB、保住布局自己写的 alpha 档。
     * 全仓 hint 声明实测是「6 处布局 + 1 处全局 style」：`styles.xml:27` 的 EditTextStyle
     * 写了 `#80ffffff`，经 `themes.xml` 的 editTextStyle 作用于**所有**未显式声明 hint 的 EditText，
     * 所以它才是主要来源；布局里另有 5 处 `#80ffffff` 与 1 处 `#88ffffff`
     * （后者在 activity_player.xml:631，属强制暗色树）。一律压成 `textTransparent`
     * 自带的档位会和布局不一致：深色侧 0x50 比布局的 0x80 暗 37%，浅色侧 0x4D 又比 0x80 淡。
     * 这里选择统一保住布局档，代价是浅色 hint 比 Material 惯用的 38% 略深一档，观感待真机判。
     */
    public static int hintTextColor(int hint, ThemePalette p) {
        return ThemePalette.withAlpha(p.textTransparent, hint >>> 24);
    }

    /**
     * 代码显式拥有的语义色。RecyclerView 的 attach 钩子跑在 bind 之后，
     * 而默认深色的 selected 就是历史装饰粉 #FB7299 本身——不记下来，
     * 树级染色的「装饰粉→accent」规则会把刚设好的选中色又收成 accent。
     * tag 高 4 位是文字令牌、低 4 位是复合图标/图形令牌。
     */
    public static final int SEM_NONE = 0, SEM_SELECTED = 1, SEM_TEXT_PRIMARY = 2,
            SEM_TEXT_TRANSPARENT = 3;

    public static int semanticColor(int token, ThemePalette p) {
        if (token == SEM_SELECTED) return p.selected;
        if (token == SEM_TEXT_PRIMARY) return p.textPrimary;
        if (token == SEM_TEXT_TRANSPARENT) return p.textTransparent;
        return 0;
    }

    /** 这个颜色是当前色板的语义令牌吗？是则返回令牌，否则 SEM_NONE（交给历史字面量启发式） */
    public static int semanticToken(int color, ThemePalette p) {
        if (sameRgb(color, p.selected)) return SEM_SELECTED;
        if (sameRgb(color, p.textPrimary)) return SEM_TEXT_PRIMARY;
        if (sameRgb(color, p.textTransparent)) return SEM_TEXT_TRANSPARENT;
        return SEM_NONE;
    }

    public static int semanticWithText(int tag, int token) {
        return (tag & 0x0F) | (token << 4);
    }

    public static int semanticWithIcon(int tag, int token) {
        return (tag & 0xF0) | token;
    }

    public static int semanticTextToken(int tag) {
        return (tag >> 4) & 0x0F;
    }

    public static int semanticIconToken(int tag) {
        return tag & 0x0F;
    }

    /** 背景染色档位 */
    public static final int BG_NONE = 0, BG_SATURATED = 1, BG_SURFACE = 2, BG_DIVIDER = 3;

    /** 近白中性色（亮度高且通道差小）；文字守卫与图标单色判定共用 */
    public static boolean nearWhiteNeutral(int color) {
        if ((color >>> 24) < 0x40) return false;
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        return min >= 0xB8 && (max - min) <= 0x2F;
    }

    /**
     * 判断文字色是否属于"默认文字色族"（可随主题重染）：
     * 精确等于现状默认值，或近似白色的中性色（布局里大量硬编码 #fff/#FFFFFFFF/
     * #DDDDDD/#99ffffff 等）。业务色（VIP 粉 #fb7299、链接蓝 #66ccff、浅绿 #bfb）
     * 通道差大，不会被误染。
     */
    public static boolean isThemedTextColor(int color) {
        return color == DEFAULT_TEXT || nearWhiteNeutral(color);
    }

    /** 中灰中性文字（说明/计数/时间戳层级） */
    public static boolean isNeutralGrayText(int color) {
        if ((color >>> 24) < 0xA0) return false;
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        return max - min <= 0x2F && min >= 0x50 && max < 0xC0;
    }

    public static boolean sameRgb(int a, int b) {
        return (a & 0x00FFFFFF) == (b & 0x00FFFFFF);
    }

    /** 默认卡片/按钮深灰底（#cc262626 / #dd262626 及同族） */
    public static boolean isDefaultSurface(int color) {
        if (color == DEFAULT_BUTTON_BG || color == DEFAULT_BUTTON_BG_ALT) return true;
        int alpha = color >>> 24;
        if (alpha < 0x60) return false;
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        return max - min <= 0x2F && max <= 0x40;
    }

    public static boolean isSaturatedColor(int color) {
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        return Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) >= 0x30;
    }

    /** 历史写死的各档装饰粉，收编为强调色（警示橙不在内，见 isDanger） */
    public static boolean isDecorPink(int color) {
        return color == 0xFBFB8787 || color == 0xFFFE679A || color == 0xFFFB7299
                || color == 0xFFFF6699
                || color == 0xFEF05D8E || color == 0xFFF05D8E
                || color == 0xFBFB8799;
    }

    /**
     * 历史警示橙：承载「破坏性操作」语义，中性化会让删除按钮读不出危险，
     * 所以它不进装饰粉名单，单独染成 danger 令牌。
     */
    public static boolean isDanger(int color) {
        return color == 0xFFFF5722;
    }

    /**
     * 按压态派生：暗底提亮、亮底压暗，步长 0x26（38/255，肉眼可辨但不跳色）。
     * 此前所有调用点都写成 pressedStateList(x, x)，两态同色等于没有按压反馈。
     * alpha 原样保留。
     */
    public static int pressedVariant(int color) {
        int alpha = color >>> 24;
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        double lum = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0;
        int delta = lum < 0.5 ? 0x26 : -0x26;
        r = Math.min(255, Math.max(0, r + delta));
        g = Math.min(255, Math.max(0, g + delta));
        b = Math.min(255, Math.max(0, b + delta));
        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }

    /** 历史默认灰描边（#383838 族、默认深灰面、近白），只有这些才可以安全重染 */
    public static boolean isDefaultStroke(int color) {
        return isDefaultSurface(color) || nearWhiteNeutral(color) || sameRgb(color, 0xFF383838);
    }

    /**
     * 已经等于色板里的某个令牌 —— 说明是主题或业务显式写进去的（选中态描边等），
     * 遍历染色不该再把它覆盖掉。
     */
    public static boolean isPaletteToken(int color, ThemePalette p) {
        return sameRgb(color, p.accent) || sameRgb(color, p.accentLow)
                || sameRgb(color, p.selected) || sameRgb(color, p.danger)
                || sameRgb(color, p.gray) || sameRgb(color, p.link)
                || sameRgb(color, p.success);
    }

    /**
     * 单色背景跟随色板的档位：有彩度 → accent；近白与纯黑半透明压暗蒙层保持原样；
     * 其余灰阶按**源亮度**分档——实心深灰 → surfaceCard、中灰弱化/分隔 → gray。
     * 不按 alpha 分档：同一个 #8C8C8C 写成 #318C8C8C 和 #818C8C8C 会被分到不同档，
     * 一个变浅灰一个变近黑，分隔线粗细不一的观感就是这么来的。
     */
    public static int backgroundTier(int color) {
        int alpha = color >>> 24;
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        int hi = Math.max(r, Math.max(g, b));
        int lo = Math.min(r, Math.min(g, b));
        if (hi - lo >= 0x30) return BG_SATURATED;
        if (r >= 0xB8 && g >= 0xB8 && b >= 0xB8 && hi - lo <= 0x2F) return BG_NONE;
        if (hi <= 0x18 && alpha < 0xF0) return BG_NONE;
        if (hi <= 0x30) return BG_SURFACE;
        return hi < 0xB8 ? BG_DIVIDER : BG_NONE;
    }

    /**
     * 16px 采样结果的分类判定（采样本身在 ThemeApplier 里做，这里只出结论）：
     * 多色 → 跳过；近白中性 → ICON_NEUTRAL；有彩度单色 → ICON_SATURATED；
     * 灰阶中性 → ICON_GRAYSCALE；纯黑半透明压暗蒙层 → 跳过。
     * coreSpan 是**实心像素（alpha>=CORE_ALPHA）的亮度跨度**，用它判两档而不是数直方图跑数：
     * Canvas 往透明 ARGB_8888 上画时抗锯齿只改 alpha 不改 RGB，实心区的 core 采样
     * 因此是干净的；shape 的 solid+stroke 只差 0x12 时跨度分类判不出来，但整张被套单一色
     * 会把描边压平——所以背景路径要求 coreSpan 足够小才敢染。
     * 结构性失效面：整只 alpha 都低于 CORE_ALPHA 的弱背景（如半透明按钮底）core 为空、
     * 恒判单档，两档闸门对它无法保护，只能靠主导档保证「压回深色而不是压亮」。全仓 14 个
     * 灰阶 shape 里没有这种组合。
     * 返回 int[]{kind, 代表alpha, 档数, 主导亮度档}。
     * bodyAlpha 是**主体** alpha（同 alpha 像素的众数），不是抗锯齿边缘的最小值：
     * 圆角 shape 的边缘会把 aMin 一路拖到采样下限，整块底被当成半透明蒙层，
     * 深灰卡片叠黑后只剩 #0c0c0c（热评卡就是这么消失的）。
     */
    public static int[] classifySample(boolean any, int rMin, int gMin, int bMin,
                                       int rMax, int gMax, int bMax, int bodyAlpha,
                                       int coreSpan, int dominantLevel) {
        int tiers = coreSpan > CORE_TIER_SPAN ? 2 : 1;
        if (!any) return new int[]{ICON_SKIP, 0, 0, 0};
        int hi = Math.max(rMax, Math.max(gMax, bMax));
        int lo = Math.min(rMin, Math.min(gMin, bMin));
        boolean mono = rMax - rMin <= 0x30 && gMax - gMin <= 0x30 && bMax - bMin <= 0x30;
        if (!mono) return new int[]{ICON_SKIP, bodyAlpha, tiers, dominantLevel};
        if (rMin >= 0xB8 && gMin >= 0xB8 && bMin >= 0xB8 && hi - lo <= 0x2F)
            return new int[]{ICON_NEUTRAL, bodyAlpha, tiers, dominantLevel};
        if (hi - lo >= 0x30) return new int[]{ICON_SATURATED, bodyAlpha, tiers, dominantLevel};
        if (hi <= 0x18 && bodyAlpha < 0xF0) return new int[]{ICON_SKIP, bodyAlpha, tiers, dominantLevel};
        return new int[]{ICON_GRAYSCALE, bodyAlpha, tiers, dominantLevel};
    }

    /** 画满像素的亮度跨度超过这个值就算两档（0x06 以内的差值肉眼分不出） */
    public static final int CORE_TIER_SPAN = 6;

    /**
     * 采样期间的亮度值。写成 (r+g+b)/2 一样编译、一样不越界，但 #A0 的中灰会落进
     * 更高的格子被判成近白，深色主题下中灰分隔线就染成文字色——所以和 histIndex
     * 一起放在能测的地方。
     */
    public static int levelOf(int r, int g, int b) {
        return (r + g + b) / 3;
    }

    /** 采样直方图的格数，必须与 histIndex 的分格方式一致（256 / 8） */
    public static final int HIST_BUCKETS = 32;

    /**
     * 亮度值 → 32 级直方图的格号。单独列出来只为一件事：写成 >>4 一样编译通过、
     * 一样不越界，但 #70 的中灰会落进 0x38 那格被判成深底，全线分隔线塌掉，而单测全绿。
     * dominantOf 取的是格中点，两者必须同一套分格。
     */
    public static int histIndex(int level) {
        return level >> 3;
    }

    /** alpha 到此以上才算「实心」像素：抗锯齿边缘与整只半透明的 shape 都被排除 */
    public static final int CORE_ALPHA = 0xB0;

    /**
     * 采样下限：低于此的像素当作完全透明丢弃。它决定 any/aMin/直方图三件事，
     * 和内联字面量相比最容易被人「顺手调一下」，所以与 CORE_ALPHA 成对放这里。
     */
    public static final int SAMPLE_ALPHA_FLOOR = 0x40;

    /**
     * 采样期间累积「实心像素的亮度跨度」。必须是个类型而不是 ThemeApplier 里的两个局部变量：
     * 极值初值写错（coreMin 起 0）会让任何带亮色实心像素的 shape 恒判两档，深色主题灰阶背景
     * 从此永不跟随色板，而调用点看不出任何异常——这种失效只有这里能测。
     */
    public static final class CoreSpan {
        private int min = 255, max = 0, count;

        public void add(int level, int alpha) {
            if (alpha < CORE_ALPHA) return;
            if (level < min) min = level;
            if (level > max) max = level;
            count++;
        }

        /** 没有实心像素（整只半透明、或只有抗锯齿边缘）时返回 0，交给主导档兜底 */
        public int span() {
            return count == 0 ? 0 : max - min;
        }
    }

    /** 采样 alpha 的分档数：alpha 直接参与合成，按 256 档精确取众数，不量化 */
    public static final int ALPHA_LEVELS = 256;

    /**
     * 采样 alpha 的众数（主体 alpha）。并列取更不透明的一档：
     * 两档 shape 面积打平时宁可保留原透明度，也不要让整块底变淡。
     */
    public static int dominantAlphaOf(int[] histogram) {
        if (histogram == null) return 0;
        int best = 0, alpha = 0;
        for (int i = 0; i < histogram.length; i++) {
            if (histogram[i] > 0 && histogram[i] >= best) {
                best = histogram[i];
                alpha = i;
            }
        }
        return alpha;
    }

    /**
     * 16×16 采样的逐像素统计，纯 Java 以便在主机上钉住「代表 alpha 取谁」。
     * 它曾经取 aMin：圆角 shape 的抗锯齿边缘 alpha 一路低到采样下限，
     * #dd262626 的卡片因此被压成约 50% 的半透明底，叠黑后只剩 #0c0c0c。
     */
    public static final class SampleStats {
        private final int[] levelHist = new int[HIST_BUCKETS];
        private final int[] alphaHist = new int[ALPHA_LEVELS];
        private final CoreSpan core = new CoreSpan();
        private int rMin = 255, gMin = 255, bMin = 255, rMax, gMax, bMax;
        private boolean any;

        public void add(int argb) {
            int a = argb >>> 24;
            if (a < SAMPLE_ALPHA_FLOOR) return;
            any = true;
            int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
            int level = levelOf(r, g, b);
            levelHist[histIndex(level)]++;
            alphaHist[a]++;
            core.add(level, a);
            if (r < rMin) rMin = r;
            if (g < gMin) gMin = g;
            if (b < bMin) bMin = b;
            if (r > rMax) rMax = r;
            if (g > gMax) gMax = g;
            if (b > bMax) bMax = b;
        }

        public int[] classify() {
            return classifySample(any, rMin, gMin, bMin, rMax, gMax, bMax,
                    dominantAlphaOf(alphaHist), core.span(), dominantOf(levelHist));
        }
    }

    /**
     * 多档灰阶 shape 是否值得保住原样。
     * 色板的表面色本身也是深灰时，压平成一档几乎看不出差别，跳过染色正好保住描边；
     * 而浅色主题的表面色是亮的，不跟随就会在白页上留下一个深灰框——那时宁可压平也要跟随。
     */
    public static boolean preserveMultiTierGrayscale(ThemePalette p) {
        return maxChannel(p.surfaceCard) <= 0x30;
    }

    public static int maxChannel(int color) {
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        return Math.max(r, Math.max(g, b));
    }

    /**
     * 把源 alpha 落到令牌上：只能往下压，不能把令牌自己带的 alpha 抬成不透明。
     * 浅色主题的 gray 是 #73000000（半透明黑，叠在白底上才是 #8C8C8C 灰），
     * 一旦 withAlpha(token, 0xFF) 就变成纯黑实心底。
     */
    public static int applyAlpha(int token, int srcAlpha) {
        return ThemePalette.withAlpha(token, Math.min(token >>> 24, srcAlpha));
    }

    /**
     * 交给 Drawable tint（SRC_IN）的 alpha。tint 最终合成 alpha 是
     * tintAlpha × 源alpha / 255，而 applyAlpha 已经把目标定成 min(令牌, 源)：
     * 再把带着目标 alpha 的颜色直接塞给 tint，会被源 alpha 乘第二遍。
     * 半透明卡片（#dd262626）因此比同页不透明 surfaceCard 的卡片暗一档（#1a 对 #1e），
     * 半透明分隔线（alpha 0x31）更是只剩 0x09 几乎看不见。这里反解出应传的 tint alpha。
     */
    public static int tintAlphaFor(int tokenAlpha, int srcAlpha) {
        int target = Math.min(tokenAlpha, srcAlpha);
        if (srcAlpha <= 0) return target;
        return Math.min(0xFF, (target * 0xFF + srcAlpha / 2) / srcAlpha);
    }

    /** 灰阶背景走 tint 时要用的颜色：RGB 取令牌，alpha 按 tintAlphaFor 补偿 */
    public static int tintColor(int token, int srcAlpha) {
        return ThemePalette.withAlpha(token, tintAlphaFor(token >>> 24, srcAlpha));
    }

    /**
     * 从 32 级均值直方图取主导亮度档：返回占比最大的那一格的**中点**（b*8+4），
     * 平票取更暗的一格。不能用格数下沿当亮度值——那会系统性低估 0..7，
     * 让 grayScaleSurface 的深色上限实际变成 0x48；也不能在 ThemeApplier 里就地写，
     * 那是这段逻辑唯一能被 JVM 测到的地方。
     */
    public static int dominantOf(int[] histogram) {
        if (histogram == null) return 0;
        int best = 0, bestBucket = -1;
        for (int i = 0; i < histogram.length; i++) {
            if (histogram[i] > best) { best = histogram[i]; bestBucket = i; }
        }
        return bestBucket < 0 ? 0 : bestBucket * 8 + 4;
    }

    /**
     * 灰阶背景该落哪一档（返回令牌本身，alpha 交给 applyAlpha 决定）。
     * 入参是**占比最大的那一档**（TintRules.dominantOf）而不是采样上限：shape 被画进
     * 16px 盒子时 stroke 是 dp*density 的绝对像素，density 越高越主导，拿上限选档会把
     * 一整张深色卡片判成 divider 档、直接压亮。按主导档最坏也只是压回深色表面。
     * 深色上限 0x40 比 backgroundTier 的 0x30 宽：backgroundTier 的入参是布局里的精确色值，
     * 这里的是采样档，边界处有 ±4 的不确定，余量只给采样侧。
     */
    public static int grayScaleSurface(int dominantLevel, ThemePalette p) {
        if (dominantLevel <= 0x40) return p.surfaceCard;
        return dominantLevel < 0xB8 ? p.gray : p.textSecondary;
    }
}
