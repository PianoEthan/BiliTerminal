package com.RobinNotBad.BiliClient.theme;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;

import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.google.material.color.hct.Hct;

import java.util.concurrent.Future;

/**
 * 按内容动态取色：封面 URL → Glide 64px 位图 → QuantizerCelebi 提取种子 →
 * Hct 派生临时强调色小色板（accent/onAccent/container/stroke）。
 * 仅染播放器进度条、SeekBar、选择芯片、详情页操作按钮；pref 门控。
 */
public class ContentTintHelper {

    /** 临时强调色小色板 */
    public static class ContentTint {
        public final int accent;
        public final int onAccent;
        public final int container;
        public final int stroke;

        public ContentTint(int accent, int onAccent, int container, int stroke) {
            this.accent = accent;
            this.onAccent = onAccent;
            this.container = container;
            this.stroke = stroke;
        }
    }

    public interface Callback {
        void onTint(ContentTint tint);
    }

    private static final LruCache<String, Integer> SEED_CACHE = new LruCache<>(64);
    /** key = seed 与明暗位组合：同一种子在深/浅模式下派生的色板不同，不可共用 */
    private static final LruCache<Long, ContentTint> TINT_CACHE = new LruCache<>(32);
    private static volatile ContentTint activeTint;

    private static long tintKey(int seed, boolean dark) {
        return (((long) seed) << 1) | (dark ? 1L : 0L);
    }

    public static boolean isEnabled() {
        return SharedPreferencesUtil.getBoolean(SharedPreferencesUtil.THEME_CONTENT_TINT, true);
    }

    /** 最近一次成功计算的强调色（主线程读） */
    public static ContentTint activeTint() {
        return activeTint;
    }

    /** 异步按 URL 取色；明暗跟随全局设置 */
    public static void requestTint(final Context context, final String url, final Callback callback) {
        requestTint(context, url, false, callback);
    }

    /**
     * 异步按 URL 取色；回调在主线程，isDestroyed/generation 守卫由调用方负责。
     *
     * @param forceDark 强制按深色派生（播放器等永久深色场景用）
     */
    public static void requestTint(final Context context, final String url, final boolean forceDark, final Callback callback) {
        if (!isEnabled() || url == null || url.isEmpty()) return;
        ThemeManager tm = ThemeManager.getInstance();
        final boolean dark = forceDark || tm == null || tm.isDark();
        Integer cachedSeed = SEED_CACHE.get(url);
        if (cachedSeed != null) {
            ContentTint tint = buildTint(cachedSeed, dark);
            if (callback != null) callback.onTint(tint);
            return;
        }
        CenterThreadPool.run(() -> {
            int seed = 0;
            Bitmap bitmap = null;
            try {
                Future<Bitmap> future = Glide.with(context.getApplicationContext())
                        .asBitmap()
                        .load(url)
                        .override(64, 64)
                        .format(DecodeFormat.PREFER_RGB_565)
                        .submit();
                bitmap = future.get(8, java.util.concurrent.TimeUnit.SECONDS);
                if (bitmap != null) seed = ColorExtractor.extractFromBitmap(bitmap);
            } catch (Exception e) {
                seed = 0;
            } finally {
                if (bitmap != null) bitmap.recycle();
            }
            if (seed == 0) return;
            final int finalSeed = seed;
            SEED_CACHE.put(url, seed);
            final ContentTint tint = buildTint(seed, dark);
            activeTint = tint;
            CenterThreadPool.runOnUiThread(() -> {
                if (callback != null) callback.onTint(tint);
            });
        });
    }

    /** 由种子派生小色板（深浅两套；按 seed+明暗联合缓存） */
    public static ContentTint buildTint(int seed, boolean dark) {
        ContentTint cached = TINT_CACHE.get(tintKey(seed, dark));
        if (cached != null) return cached;
        try {
            Hct hct = Hct.fromInt(seed);
            double chroma = Math.max(32.0, Math.min(96.0, hct.getChroma() * 1.2));
            int accent = Hct.from(hct.getHue(), chroma, dark ? 80 : 40).toInt();
            int onAccent = Hct.from(hct.getHue(), chroma, dark ? 20 : 100).toInt();
            int container = Hct.from(hct.getHue(), chroma, dark ? 30 : 90).toInt();
            int stroke = Hct.from(hct.getHue(), chroma, dark ? 60 : 50).toInt();
            ContentTint tint = new ContentTint(accent, onAccent, container, stroke);
            TINT_CACHE.put(tintKey(seed, dark), tint);
            return tint;
        } catch (Exception e) {
            com.RobinNotBad.BiliClient.theme.ThemePalette fb = com.RobinNotBad.BiliClient.theme.ThemeManager.paletteDark();
            return new ContentTint(fb.accent, fb.selectedText, fb.surfaceCard, fb.accent);
        }
    }

    /** 清空缓存（主题切换后颜色体系变了） */
    public static void clearCache() {
        SEED_CACHE.evictAll();
        TINT_CACHE.evictAll();
        activeTint = null;
    }

    /** 详情页操作按钮临时强调色（兜底 ThemeManager 色板） */
    public static int buttonAccent(Context context) {
        ContentTint tint = activeTint();
        return tint != null ? tint.accent : com.RobinNotBad.BiliClient.theme.ThemeManager.palette().accent;
    }

    public static int buttonContainer(Context context) {
        ContentTint tint = activeTint();
        return tint != null ? tint.container : com.RobinNotBad.BiliClient.theme.ThemeManager.palette().surfaceCard;
    }

    public static int buttonOnAccent(Context context) {
        ContentTint tint = activeTint();
        return tint != null ? tint.onAccent : com.RobinNotBad.BiliClient.theme.ThemeManager.palette().selectedText;
    }
}
