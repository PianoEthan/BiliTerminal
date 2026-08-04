package com.RobinNotBad.BiliClient.theme;

import android.content.Context;
import android.text.TextUtils;

import com.RobinNotBad.BiliClient.event.ThemeChangedEvent;
import com.RobinNotBad.BiliClient.theme.model.InstalledTheme;
import com.RobinNotBad.BiliClient.theme.model.ThemeManifest;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;

import org.greenrobot.eventbus.EventBus;

import java.io.File;

/**
 * 主题管理器（单例）。BiliTerminal.onCreate 同步 init（&lt;10ms，失败全兜底内置主题）。
 * 深/浅双套色板由同一种子生成，手动切换（不接 DayNight）。
 * 每次变更 generation++，持久化后 post 非粘性 ThemeChangedEvent。
 */
public class ThemeManager {

    private static final String PREFS_SEED_PREFIX = "theme_extracted_seed_";
    /** 内置预设 id 前缀（不进 filesDir，无背景图） */
    public static final String BUILTIN_PREFIX = "builtin.";

    /** 内置预设主题：种子色 → M3 生成（深浅两套） */
    public static class BuiltinPreset {
        public final String id;
        public final String name;
        public final int seed;

        public BuiltinPreset(String id, String name, int seed) {
            this.id = id;
            this.name = name;
            this.seed = seed;
        }
    }

    public static final BuiltinPreset PRESET_SUMMER_GREEN =
            new BuiltinPreset(BUILTIN_PREFIX + "summer_green", "夏天绿", 0xFF00C853);
    public static final BuiltinPreset PRESET_ICE_BLUE =
            new BuiltinPreset(BUILTIN_PREFIX + "ice_blue", "冰雪蓝", 0xFF4FC3F7);

    public static java.util.List<BuiltinPreset> getBuiltinPresets() {
        java.util.List<BuiltinPreset> list = new java.util.ArrayList<>();
        list.add(PRESET_SUMMER_GREEN);
        list.add(PRESET_ICE_BLUE);
        return list;
    }

    public static BuiltinPreset findPreset(String id) {
        if (id == null || !id.startsWith(BUILTIN_PREFIX)) return null;
        for (BuiltinPreset preset : getBuiltinPresets()) {
            if (preset.id.equals(id)) return preset;
        }
        return null;
    }

    public static boolean isPresetId(String id) {
        return findPreset(id) != null;
    }

    private static ThemeManager instance;

    private final Context appContext;
    private int generation = 0;
    private boolean dark = true;
    private int blend = 35;
    private boolean bgEnabled = true;
    private boolean extractBg = true;
    private boolean contentTint = true;
    private String themeId = "";
    private ThemeManifest manifest;
    private BuiltinPreset preset;
    private ThemePalette palette = ThemePalette.builtinDark();
    private ThemePalette paletteDark;
    private boolean ready = false;
    private volatile int extractedSeed = 0;
    private long lastPostTime = 0;
    private boolean pendingPost = false;

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new ThemeManager(context.getApplicationContext());
            instance.load();
        }
    }

    public static ThemeManager getInstance() {
        return instance;
    }

    /** 静态取当前色板；未初始化兜底内置深色（冷启动安全） */
    public static ThemePalette palette() {
        ThemeManager tm = instance;
        return tm != null && tm.palette != null ? tm.palette : ThemePalette.builtinDark();
    }

    /** 静态取深色套色板（播放器等永久深色场景用） */
    public static ThemePalette paletteDark() {
        ThemeManager tm = instance;
        return tm != null ? tm.getPaletteDark() : ThemePalette.builtinDark();
    }

    private ThemeManager(Context context) {
        this.appContext = context;
    }

    // ------------------------------------------------------------ 初始化

    private void load() {
        themeId = SharedPreferencesUtil.getString(SharedPreferencesUtil.THEME_ID, "");
        dark = SharedPreferencesUtil.getBoolean(SharedPreferencesUtil.THEME_DARK, true);
        blend = SharedPreferencesUtil.getInt(SharedPreferencesUtil.THEME_BLEND, 35);
        bgEnabled = SharedPreferencesUtil.getBoolean(SharedPreferencesUtil.THEME_BG_ENABLE, true);
        extractBg = SharedPreferencesUtil.getBoolean(SharedPreferencesUtil.THEME_EXTRACT_BG, true);
        contentTint = SharedPreferencesUtil.getBoolean(SharedPreferencesUtil.THEME_CONTENT_TINT, true);

        if (!TextUtils.isEmpty(themeId)) {
            preset = findPreset(themeId);
            if (preset == null) {
                InstalledTheme installed = BThemeInstaller.loadInstalled(appContext, themeId);
                if (installed == null) {
                    themeId = "";
                    SharedPreferencesUtil.putString(SharedPreferencesUtil.THEME_ID, "");
                } else {
                    manifest = installed.manifest;
                }
            }
            // 恢复上次提取的种子（若存在）
            if (!TextUtils.isEmpty(themeId)) {
                extractedSeed = SharedPreferencesUtil.getInt(PREFS_SEED_PREFIX + themeId, 0);
            }
        }
        regenerate();
        ready = true;

        // P5：背景图取色种子（异步，冷启动先用 fallback）
        if (extractBg && manifest != null && manifest.colors != null
                && manifest.colors.source != null
                && TextUtils.isEmpty(manifest.colors.seed)
                && !TextUtils.isEmpty(manifest.colors.source.image)) {
            // 捕获当时状态，避免用户中途换主题后把种子写错主题
            final ThemeManifest m = manifest;
            final String tid = themeId;
            final boolean darkNow = dark;
            CenterThreadPool.run(() -> {
                File bg = getBackgroundFile(m, darkNow);
                int seed = bg == null ? 0 : ColorExtractor.extractFromFile(bg);
                if (seed != 0) {
                    // 若与本主题已持久化的提取种子一致则跳过
                    int saved = SharedPreferencesUtil.getInt(PREFS_SEED_PREFIX + tid, 0);
                    if (saved == 0) {
                        extractedSeed = seed;
                        SharedPreferencesUtil.putInt(PREFS_SEED_PREFIX + tid, seed);
                        regenerate();
                        postChanged();
                    }
                }
            });
        }
    }

    // ------------------------------------------------------------ 状态

    public boolean isReady() {
        return ready;
    }

    public int getGeneration() {
        return generation;
    }

    public boolean isDark() {
        return dark;
    }

    public int getBlend() {
        return blend;
    }

    public String getThemeId() {
        return themeId;
    }

    public ThemeManifest getManifest() {
        return manifest;
    }

    public ThemePalette getPalette() {
        return palette;
    }

    /** 是否自定义主题 */
    public boolean isCustom() {
        return !TextUtils.isEmpty(themeId);
    }

    // ------------------------------------------------------------ 变更

    public synchronized void setTheme(String id) {
        if (TextUtils.isEmpty(id)) {
            themeId = "";
            manifest = null;
            preset = null;
        } else if (isPresetId(id)) {
            preset = findPreset(id);
            themeId = id;
            manifest = null;
        } else {
            InstalledTheme installed = BThemeInstaller.loadInstalled(appContext, id);
            if (installed == null) return;
            themeId = id;
            manifest = installed.manifest;
            preset = null;
        }
        SharedPreferencesUtil.putString(SharedPreferencesUtil.THEME_ID, themeId);
        regenerate();
        postChanged();
    }

    public synchronized void setDark(boolean dark) {
        this.dark = dark;
        SharedPreferencesUtil.putBoolean(SharedPreferencesUtil.THEME_DARK, dark);
        regenerate();
        postChanged();
    }

    public synchronized void setBlend(int blend) {
        if (blend < 0) blend = 0;
        if (blend > 100) blend = 100;
        this.blend = blend;
        SharedPreferencesUtil.putInt(SharedPreferencesUtil.THEME_BLEND, blend);
        regenerate();
        postChanged();
    }

    public void setBgEnabled(boolean enabled) {
        bgEnabled = enabled;
        SharedPreferencesUtil.putBoolean(SharedPreferencesUtil.THEME_BG_ENABLE, enabled);
        postChanged();
    }

    public boolean isBgEnabled() {
        return bgEnabled;
    }

    public boolean isExtractBgEnabled() {
        return extractBg;
    }

    public boolean isContentTintEnabled() {
        return contentTint;
    }

    private synchronized void regenerate() {
        paletteDark = null; // 使深色套缓存失效
        palette = computePalette(dark);
        ContentTintHelper.clearCache(); // 明暗位/色板变了，内容取色缓存一并失效
    }

    /** 按指定明暗计算色板（纯计算；深浅两套始终可独立取得） */
    private ThemePalette computePalette(boolean darkMode) {
        if (TextUtils.isEmpty(themeId) || (preset == null && manifest == null)) {
            return darkMode ? ThemePalette.builtinDark() : ThemePalette.builtinLight();
        }
        try {
            if (preset != null) {
                return SchemeEngine.generate(preset.seed, darkMode, blend, null);
            }
            return SchemeEngine.generate(resolveSeed(), darkMode, blend, manifest);
        } catch (Throwable t) {
            // material-color-utilities 在个别旧系统上可能加载失败，兜底内置主题保证不崩
            return darkMode ? ThemePalette.builtinDark() : ThemePalette.builtinLight();
        }
    }

    /**
     * 深色套色板（无论当前全局明暗）。永久深色场景（播放器/闪屏）染色用，
     * 避免浅色模式的暗文字落到黑背景上。自定义主题取其深色套。
     */
    public synchronized ThemePalette getPaletteDark() {
        if (dark) return palette;
        if (paletteDark == null) paletteDark = computePalette(true);
        return paletteDark;
    }

    /** 种子优先级：显式 seed > 背景图提取种子 > source.fallback > 默认中性灰 */
    private int resolveSeed() {
        if (manifest != null && manifest.colors != null) {
            Integer explicit = ThemeManifest.parseColor(manifest.colors.seed);
            if (explicit != null) return explicit;
            if (extractedSeed != 0) return extractedSeed;
            Integer fallback = ThemeManifest.parseColor(
                    manifest.colors.source == null ? null : manifest.colors.source.fallback);
            if (fallback != null) return fallback;
        }
        return 0xFF9E9E9E;
    }

    /** 变更 → 事件（2s 防抖，避免冷启动提取+recreate 双重触发；延后窗口内仍会补发） */
    private synchronized void postChanged() {
        generation++;
        long now = System.currentTimeMillis();
        if (now - lastPostTime < 2000) {
            if (!pendingPost) {
                pendingPost = true;
                CenterThreadPool.runOnUIThreadAfter(2100, this::flushPendingPost);
            }
            return;
        }
        flushPendingPost();
    }

    private synchronized void flushPendingPost() {
        pendingPost = false;
        long now = System.currentTimeMillis();
        if (now - lastPostTime < 500) return; // 刚发过，防重复 recreate
        lastPostTime = now;
        EventBus.getDefault().post(new ThemeChangedEvent(generation));
    }

    // ------------------------------------------------------------ 背景

    /** 当前生效的背景图（受 THEME_BG_ENABLE 门控） */
    public File getActiveBackgroundFile() {
        if (!bgEnabled || manifest == null) return null;
        return getBackgroundFile(manifest, dark);
    }

    public String getBackgroundFit() {
        if (manifest == null || manifest.background == null || TextUtils.isEmpty(manifest.background.fit)) {
            return "centerCrop";
        }
        return manifest.background.fit;
    }

    private static File getBackgroundFile(ThemeManifest manifest, boolean dark) {
        if (manifest == null || manifest.background == null) return null;
        String name = dark ? manifest.background.image_dark : manifest.background.image_light;
        if (TextUtils.isEmpty(name)) name = manifest.background.image;
        if (TextUtils.isEmpty(name)) return null;
        File dir = BThemeInstaller.getThemeDir(instance.appContext, manifest.id);
        if (dir == null) return null;
        File f = new File(dir, name);
        return f.isFile() ? f : null;
    }
}
