package com.RobinNotBad.BiliClient.theme;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.appcompat.widget.SwitchCompat;

import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.theme.model.ThemeManifest;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

/**
 * 视图树染色器：挂在 BaseActivity.setContentView / BaseFragment.onViewCreated /
 * AbstractAdapter 三个扼点上。规则保守——只染"当前等于默认值"的视图，
 * 保护 VIP 粉、业务色等；topbar id 恒染。
 * 内置深色主题下强调色类染一律跳过（保证与现状逐像素一致）。
 */
public class ThemeApplier {

    /** 默认深色主题的精确值（染色守卫用） */
    private static final int DEFAULT_TEXT = 0xFFEBE0E2;
    private static final int DEFAULT_TEXT_TRANSPARENT = 0x50FEFEFE;
    private static final int DEFAULT_BUTTON_BG = 0xCC262626;

    /** 背景图解码缓存：key=文件路径+采样，SoftReference 防内存压力 */
    private static final Map<String, SoftReference<Bitmap>> BG_CACHE = new HashMap<>();

    // ---------------------------------------------------------------- 窗口背景

    /** 设置窗口背景：主题背景图 + scrim，或纯色窗口背景 */
    public static void setupWindow(Activity activity) {
        ThemeManager tm = ThemeManager.getInstance();
        if (tm == null || !tm.isReady() || activity == null || activity.getWindow() == null) return;
        ThemePalette palette = tm.getPalette();
        if (palette == null) return;

        Drawable windowBg = null;
        File bgFile = tm.getActiveBackgroundFile();
        if (bgFile != null) {
            // onCreate 阶段 decor 尚未布局，用屏幕真实尺寸做解码目标
            android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
            try {
                activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
            } catch (Exception ignored) {
            }
            int dw = Math.max(1, metrics.widthPixels);
            int dh = Math.max(1, metrics.heightPixels);
            Bitmap bitmap = decodeBackground(bgFile, dw, dh);
            if (bitmap != null) {
                Drawable bg = buildBackgroundDrawable(bitmap, dw, dh, tm.getBackgroundFit(), palette.windowBackground);
                int scrimColor = Color.BLACK;
                int scrimAlpha = (int) Math.round(0.55 * 255);
                if (tm.getManifest() != null && tm.getManifest().background != null) {
                    if (tm.getManifest().background.scrimColor != null) {
                        Integer c = ThemeManifest.parseColor(tm.getManifest().background.scrimColor);
                        if (c != null) scrimColor = c;
                    }
                    double s = tm.getManifest().background.scrim;
                    if (s >= 0.0 && s <= 0.9) scrimAlpha = (int) Math.round(s * 255);
                }
                ColorDrawable scrim = new ColorDrawable(
                        (scrimColor & 0x00FFFFFF) | (scrimAlpha << 24));
                windowBg = new LayerDrawable(new Drawable[]{bg, scrim});
            }
        }
        if (windowBg == null) windowBg = new ColorDrawable(palette.windowBackground);
        activity.getWindow().setBackgroundDrawable(windowBg);

        // edge-to-edge：内容延伸到系统栏，系统栏透明，insets 让位
        // （Player/Splash 全屏本就无系统栏，不受影响）
        setupEdgeToEdge(activity, palette, tm.isDark());

        // 状态栏跟随强调色
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            try {
                activity.getWindow().setStatusBarColor(ThemePalette.withAlpha(palette.accent, 0xFF));
            } catch (Exception ignored) {
            }
        }
    }

    /** edge-to-edge：decorFitsSystemWindows=false + 透明系统栏 + insets 让位（幂等） */
    private static void setupEdgeToEdge(final Activity activity, final ThemePalette p, final boolean dark) {
        if (activity == null || activity.getWindow() == null) return;
        // 全屏/无状态栏页面跳过（Player/Splash/圆屏）
        if (android.os.Build.VERSION.SDK_INT >= 16) {
            int flags = activity.getWindow().getDecorView().getSystemUiVisibility();
            if ((flags & android.view.View.SYSTEM_UI_FLAG_FULLSCREEN) != 0) return;
        }
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            activity.getWindow().setDecorFitsSystemWindows(false);
        } else if (android.os.Build.VERSION.SDK_INT >= 21) {
            activity.getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
            activity.getWindow().getDecorView().setSystemUiVisibility(
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            if (dark) activity.getWindow().getDecorView().setSystemUiVisibility(
                    activity.getWindow().getDecorView().getSystemUiVisibility()
                            | android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        // insets 让位：内容顶部让出状态栏高度，底部让出导航栏（仅 BaseActivity 布局）
        final View content = activity.findViewById(android.R.id.content);
        if (content == null) return;
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            content.setOnApplyWindowInsetsListener((v, insets) -> {
                if (v.getTag(R.id.theme_insets_applied) != null) return insets;
                v.setTag(R.id.theme_insets_applied, Boolean.TRUE);
                int top = insets.getSystemWindowInsetTop();
                int bottom = insets.getSystemWindowInsetBottom();
                v.setPadding(0, top, 0, bottom);
                return insets;
            });
            content.requestApplyInsets();
        } else {
            // API21 以下：手动读状态栏高度让位（无 insets API）
            try {
                int resId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
                int top = resId > 0 ? activity.getResources().getDimensionPixelSize(resId) : 0;
                content.setPadding(0, top, 0, 0);
            } catch (Exception ignored) {
            }
        }
    }

    /** 解码（≤2x 屏幕尺寸采样；RGB_565；SoftReference 缓存） */
    private static Bitmap decodeBackground(File file, int targetW, int targetH) {
        String key = file.getPath() + "|" + targetW + "x" + targetH;
        SoftReference<Bitmap> ref = BG_CACHE.get(key);
        if (ref != null) {
            Bitmap cached = ref.get();
            if (cached != null && !cached.isRecycled()) return cached;
        }
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getPath(), opts);
        int sample = 1;
        int w = Math.max(1, opts.outWidth);
        int h = Math.max(1, opts.outHeight);
        while (w / sample > targetW * 2 || h / sample > targetH * 2) sample <<= 1;
        opts.inJustDecodeBounds = false;
        opts.inSampleSize = sample;
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bitmap = BitmapFactory.decodeFile(file.getPath(), opts);
        if (bitmap != null) BG_CACHE.put(key, new SoftReference<>(bitmap));
        return bitmap;
    }

    /** 按 fit 模式把解码位图适配到窗口尺寸 */
    private static Drawable buildBackgroundDrawable(Bitmap src, int dw, int dh, String fit, int fallbackColor) {
        Resources res = Resources.getSystem();
        String mode = fit == null ? "centerCrop" : fit;
        int bw = src.getWidth();
        int bh = src.getHeight();

        if ("tile".equals(mode)) {
            BitmapDrawable tile = new BitmapDrawable(res, src);
            tile.setTileModeXY(android.graphics.Shader.TileMode.REPEAT, android.graphics.Shader.TileMode.REPEAT);
            tile.setGravity(Gravity.FILL);
            return tile;
        }
        if ("fitXY".equals(mode)) {
            Bitmap scaled = Bitmap.createScaledBitmap(src, dw, dh, true);
            return new BitmapDrawable(res, scaled);
        }
        if ("centerInside".equals(mode)) {
            float scale = Math.min((float) dw / bw, (float) dh / bh);
            int nw = Math.max(1, Math.round(bw * scale));
            int nh = Math.max(1, Math.round(bh * scale));
            Bitmap scaled = Bitmap.createScaledBitmap(src, nw, nh, true);
            BitmapDrawable bd = new BitmapDrawable(res, scaled);
            bd.setGravity(Gravity.CENTER);
            return new LayerDrawable(new Drawable[]{
                    new ColorDrawable(fallbackColor), bd});
        }
        // centerCrop（默认）
        float scale = Math.max((float) dw / bw, (float) dh / bh);
        int nw = Math.max(1, Math.round(bw * scale));
        int nh = Math.max(1, Math.round(bh * scale));
        Bitmap scaled = Bitmap.createScaledBitmap(src, nw, nh, true);
        Bitmap crop = Bitmap.createBitmap(scaled,
                (nw - dw) / 2, (nh - dh) / 2, Math.min(dw, nw), Math.min(dh, nh));
        if (crop != scaled) scaled.recycle();
        return new BitmapDrawable(res, crop);
    }

    // ---------------------------------------------------------------- 内容染色

    /** 应用内容染色（幂等：generation 一致则跳过） */
    public static void applyContent(View root) {
        ThemeManager tm = ThemeManager.getInstance();
        if (tm == null) return;
        apply(root, tm.getGeneration(), tm.getPalette(), false);
    }

    /**
     * 强制按深色色板染色：用于永久深色场景（播放器视频面、闪屏深色背景图），
     * 不跟随全局浅色模式——否则浅色套的暗文字会落在黑背景上不可见。
     * 自定义主题仍取其深色套（深浅两套始终都生成）。
     */
    public static void applyContentForcedDark(View root) {
        ThemeManager tm = ThemeManager.getInstance();
        if (tm == null) return;
        apply(root, tm.getGeneration(), tm.getPaletteDark(), true);
    }

    /** 适配器 bind 时调用：generation 过期才重染 */
    public static void refreshIfStale(View root) {
        ThemeManager tm = ThemeManager.getInstance();
        if (tm == null || root == null) return;
        int generation = tm.getGeneration();
        Object tag = root.getTag(R.id.theme_applied_gen);
        if (tag instanceof Integer && ((Integer) tag) == generation) return;
        apply(root, generation, tm.getPalette(), false);
    }

    private static void apply(View view, int generation, ThemePalette p, boolean forceDark) {
        if (view == null) return;
        if (view.getTag(R.id.theme_no_tint) != null) return; // 跳过标记子树
        if (view instanceof SurfaceView || view instanceof TextureView) return;
        if (view.getClass().getName().contains("Danmaku")) return; // 弹幕渲染面

        hookRecyclerView(view, forceDark);
        tintView(view, p);

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                apply(group.getChildAt(i), generation, p, forceDark);
            }
        }
        view.setTag(R.id.theme_applied_gen, generation);
    }

    /**
     * 通用列表扼点：本项目绝大多数业务适配器直接继承 RecyclerView.Adapter
     * （继承 AbstractAdapter 的只有一个），cell 染色统一走"子视图附着后染色"——
     * RV 先 bind 后 attach，此处拿到的是 bind 阶段的最终颜色；generation 标签幂等。
     * forceDark 子树（播放器）的 cell 永远按深色套渲染，不跟随全局明暗。
     */
    private static void hookRecyclerView(View view, final boolean forceDark) {
        if (!(view instanceof androidx.recyclerview.widget.RecyclerView)) return;
        androidx.recyclerview.widget.RecyclerView rv = (androidx.recyclerview.widget.RecyclerView) view;
        if (rv.getTag(R.id.theme_rv_hooked) != null) return;
        rv.setTag(R.id.theme_rv_hooked, Boolean.TRUE);
        rv.addOnChildAttachStateChangeListener(new androidx.recyclerview.widget.RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@androidx.annotation.NonNull View child) {
                if (forceDark) {
                    ThemeManager tm = ThemeManager.getInstance();
                    if (tm != null) apply(child, tm.getGeneration(), tm.getPaletteDark(), true);
                } else {
                    refreshIfStale(child);
                }
            }

            @Override
            public void onChildViewDetachedFromWindow(@androidx.annotation.NonNull View child) {
            }
        });
    }

    /** 近白中性色（亮度高且通道差小）；文字守卫与图标单色判定共用 */
    private static boolean nearWhiteNeutral(int color) {
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
    private static boolean isThemedTextColor(int color) {
        return color == DEFAULT_TEXT || nearWhiteNeutral(color);
    }

    private static void tintView(View view, ThemePalette p) {

        // 单色 shape 背景跟随色板（含布局内联 hex 的 ColorDrawable）：
        // 有彩度 → accent；灰阶实心 → surfaceCard、弱化/分隔 → gray；
        // 近白/纯黑蒙层保持原样。逻辑见 retintBackground。
        retintBackground(view, p);

        if (view instanceof MaterialCardView) {
            MaterialCardView card = (MaterialCardView) view;
            card.setCardBackgroundColor(p.surfaceCard);
            card.setStrokeColor(p.gray);
            return;
        }
        if (view instanceof androidx.swiperefreshlayout.widget.SwipeRefreshLayout) {
            androidx.swiperefreshlayout.widget.SwipeRefreshLayout refresh =
                    (androidx.swiperefreshlayout.widget.SwipeRefreshLayout) view;
            refresh.setColorSchemeColors(p.accent);
            refresh.setProgressBackgroundColorSchemeColor(p.surfaceCard);
            return;
        }
        if (view instanceof CircularProgressIndicator) {
            ((CircularProgressIndicator) view).setIndicatorColor(p.accent);
            return;
        }
        if (view instanceof ProgressBar) {
            ProgressBar bar = (ProgressBar) view;
            if (bar.getIndeterminateDrawable() != null)
                ThemeCompat.tintDrawable(bar.getIndeterminateDrawable(), p.accent);
            if (bar.getProgressDrawable() != null)
                ThemeCompat.tintDrawable(bar.getProgressDrawable(), p.accent);
            return;
        }
        if (view instanceof MaterialButton) {
            tintButton((MaterialButton) view, p);
            return;
        }
        if (view instanceof AppCompatButton) {
            tintButton((AppCompatButton) view, p);
            return;
        }
        if (view instanceof Button) {
            tintButton((Button) view, p);
            return;
        }
        if (view instanceof EditText) {
            EditText editText = (EditText) view;
            if (editText.getHintTextColors() != null) editText.setHintTextColor(p.textTransparent);
            if (isThemedTextColor(editText.getCurrentTextColor())) editText.setTextColor(p.textPrimary);
            // drawableEnd 等复合图标（搜索放大镜等）随主题文字色
            ThemeCompat.tintCompoundDrawables(editText, p.textPrimary);
            // 光标跟随强调色（API29+ 才有公开 setter）
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                try {
                    android.graphics.drawable.GradientDrawable cursor = new android.graphics.drawable.GradientDrawable();
                    cursor.setColor(p.accent);
                    cursor.setSize(Math.max(1, (int) (2 * editText.getResources().getDisplayMetrics().density)),
                            Math.max(1, editText.getLineHeight()));
                    editText.setTextCursorDrawable(cursor);
                } catch (Exception ignored) {
                }
            }
            return;
        }
        if (view instanceof SwitchCompat) {
            SwitchCompat sw = (SwitchCompat) view;
            sw.setThumbTintList(ThemeCompat.checkedStateList(p.textPrimary, p.accent));
            sw.setTrackTintList(ThemeCompat.checkedStateList(p.ripple, p.accentLow));
            if (isThemedTextColor(sw.getCurrentTextColor())) sw.setTextColor(p.textPrimary);
            return;
        }
        if (view instanceof Switch) {
            Switch sw = (Switch) view;
            // getThumbDrawable 为 API23+；<23 跳过拇指染色（布局里的 <Switch>
            // 通常已被 AppCompat 替换为 SwitchCompat 走上面的分支）
            if (android.os.Build.VERSION.SDK_INT >= 23)
                ThemeCompat.tintDrawable(sw.getThumbDrawable(), p.accent);
            if (isThemedTextColor(sw.getCurrentTextColor())) sw.setTextColor(p.textPrimary);
            return;
        }
        if (view instanceof AppCompatRadioButton) {
            AppCompatRadioButton radio = (AppCompatRadioButton) view;
            radio.setSupportButtonTintList(ThemeCompat.checkedStateList(p.ripple, p.accent));
            if (isThemedTextColor(radio.getCurrentTextColor())) radio.setTextColor(p.textPrimary);
            return;
        }
        if (view instanceof RadioButton) {
            RadioButton radio = (RadioButton) view;
            // getButtonDrawable 为 API23+；AppCompat 注入的 RadioButton 走上面的
            // AppCompatRadioButton 分支（setSupportButtonTintList，全版本安全）
            if (android.os.Build.VERSION.SDK_INT >= 23)
                ThemeCompat.tintDrawable(radio.getButtonDrawable(), p.accent);
            if (isThemedTextColor(radio.getCurrentTextColor())) radio.setTextColor(p.textPrimary);
            return;
        }
        if (view instanceof SeekBar) {
            ThemeCompat.tintSeekBar((SeekBar) view, p.accent);
            return;
        }
        if (view instanceof ImageView) {
            tintIconIfMonochrome((ImageView) view, p);
            return;
        }
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            int color = textView.getCurrentTextColor();
            int id = textView.getId();
            if (id == R.id.pageName || id == R.id.timeText) {
                textView.setTextColor(p.textPrimary);
                ThemeCompat.tintCompoundDrawables(textView, p.textPrimary);
            } else if (color == DEFAULT_TEXT_TRANSPARENT) {
                // 半透明默认值须先于启发式判断（0x50FEFEFE 也满足"近白"）
                textView.setTextColor(p.textTransparent);
            } else if (color == 0xFF66CCFF) { // @color/link
                textView.setTextColor(p.link);
            } else if (color == 0xFBFB8787 || color == 0xFFFE679A || color == 0xFFFB7299 || color == 0xFFFF6699
                    || color == 0xFFFF5722) {
                // 历史写死的各档粉色/警示橙（@color/pink、点赞数、删除收藏夹等）统一收编为强调色
                textView.setTextColor(p.accent);
                ThemeCompat.tintCompoundDrawables(textView, p.accent);
            } else if (isThemedTextColor(color)) {
                textView.setTextColor(p.textPrimary);
                ThemeCompat.tintCompoundDrawables(textView, p.textPrimary);
            } else if (isNeutralGrayText(color)) {
                // 中灰说明文字（#999/#AAA/#a2a2a2 族）→ 次级文字令牌
                textView.setTextColor(p.textSecondary);
            }
        }
    }

    /** 中灰中性文字（说明/计数/时间戳层级） */
    private static boolean isNeutralGrayText(int color) {
        if ((color >>> 24) < 0xA0) return false;
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        return max - min <= 0x2F && min >= 0x50 && max < 0xC0;
    }

    /** 染色判定缓存：int[]{kind, 代表alpha}；kind：0=跳过 1=近白中性 2=有彩度 3=灰阶中性 */
    private static final android.util.LruCache<Drawable.ConstantState, int[]> MONO_ICON_CACHE =
            new android.util.LruCache<>(128);

    private static final int ICON_SKIP = 0, ICON_NEUTRAL = 1, ICON_SATURATED = 2, ICON_GRAYSCALE = 3;

    /**
     * 单色图标随主题：
     * - 近白中性色（菜单/设置/BV/箭头等 #ebe0e2 图标集）→ textPrimary；
     * - 单色有彩度（历史粉色箭头/点亮态、蓝色开关态等）→ accent（全应用去粉后
     *   默认主题里这些图标随中性强调色渲染）；
     * - 多色图标（播放器灰白控制钮、黑白会员标、粉白文章角标）与照片一律跳过。
     */
    /**
     * 给 ImageView 换图后按色板重染（状态切换专用，如点赞/收藏/投币/播放控制）。
     * setImageResource 换出的新 drawable 不带之前染色；此处直接按当前色板补染，
     * 图标色分类（近白→textPrimary / 有彩度→accent / 其他不动）与遍历染色一致。
     */
    public static void retintImage(ImageView iv) {
        if (iv == null) return;
        ThemeManager tm = ThemeManager.getInstance();
        if (tm == null) return;
        tintIconIfMonochrome(iv, tm.getPalette());
    }

    /** 换背景后按色板重染（状态切换专用，如分 P 选中底） */
    public static void retintBackground(View v) {
        if (v == null) return;
        ThemeManager tm = ThemeManager.getInstance();
        if (tm == null) return;
        retintBackground(v, tm.getPalette());
    }

    /**
     * 对话框内容染色（AlertDialog 等不经扼点的树）。
     * show 后布局完成即染一次（幂等 tag 保证）。
     */
    public static void tintDialog(final android.app.Dialog dialog) {
        if (dialog == null || dialog.getWindow() == null) return;
        dialog.getWindow().getDecorView().addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (v.getTag(R.id.theme_applied_gen) == null) applyContent(v);
        });
    }

    private static void retintBackground(View view, ThemePalette p) {
        // 单色背景跟随色板：
        // - 有彩度 → accent
        // - 灰阶：实心（alpha≥0x60）→ surfaceCard 的 RGB + 原透明度；
        //          弱化/分隔（alpha<0x60）→ gray 令牌的 RGB（M3 outline 色，
        //          默认值 #8C8C8C 与现状一致）——避免深色主题下分隔线隐形；
        // - 纯黑半透明压暗蒙层（照片 scrim）功能性强，保持原样。
        Drawable bg = view.getBackground();
        if (bg instanceof android.graphics.drawable.ColorDrawable) {
            // 布局里内联 hex 背景（分隔线/评论框/动态图底/用户页等）
            int c = ((android.graphics.drawable.ColorDrawable) bg).getColor();
            int alpha = c >>> 24;
            int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
            int hi = Math.max(r, Math.max(g, b));
            int lo = Math.min(r, Math.min(g, b));
            if (hi - lo >= 0x30) { // 有彩度单色（本地视频绿徽等）
                view.setBackgroundColor(p.accent);
            } else if (!(r >= 0xB8 && g >= 0xB8 && b >= 0xB8 && hi - lo <= 0x2F)
                    && !(hi <= 0x18 && alpha < 0xF0)) {
                // 灰阶中性表面：透明度<0x60 属分隔/弱化层，用 outline 色保持可见
                int rgb = alpha < 0x60 ? ThemePalette.withAlpha(p.gray, 0xFF) : ThemePalette.withAlpha(p.surfaceCard, 0xFF);
                view.setBackgroundColor(ThemePalette.withAlpha(rgb, alpha));
            }
        } else if (bg instanceof android.graphics.drawable.StateListDrawable) {
            // 状态选择器背景（如互动投票芯片：默认态灰阶、按压态粉色）
            // 逐状态原地 mutate 染色：彩色 → accent；灰阶 → surfaceCard/gray 分档。
            // getStateCount/getStateDrawable 为 API21+；<21 保持原样。
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                android.graphics.drawable.StateListDrawable sld = (android.graphics.drawable.StateListDrawable) bg;
                int count = sld.getStateCount();
                for (int i = 0; i < count; i++) {
                    Drawable state = sld.getStateDrawable(i);
                    if (state == null) continue;
                    Drawable.ConstantState cs = state.getConstantState();
                    int[] c = cs == null ? null : MONO_ICON_CACHE.get(cs);
                    if (c == null) {
                        c = classifyDrawable(state);
                        if (cs != null) MONO_ICON_CACHE.put(cs, c);
                    }
                    if (c[0] == ICON_SATURATED) {
                        ThemeCompat.tintDrawable(state, p.accent);
                    } else if (c[0] == ICON_GRAYSCALE) {
                        int rgb = c[1] < 0x60 ? ThemePalette.withAlpha(p.gray, 0xFF) : ThemePalette.withAlpha(p.surfaceCard, 0xFF);
                        ThemeCompat.tintDrawable(state, ThemePalette.withAlpha(rgb, c[1]));
                    }
                }
            }
        } else if (bg instanceof android.graphics.drawable.GradientDrawable
                || bg instanceof android.graphics.drawable.ShapeDrawable) {
            Drawable.ConstantState cs = bg.getConstantState();
            if (cs != null) {
                int[] c = MONO_ICON_CACHE.get(cs);
                if (c == null) {
                    c = classifyDrawable(bg);
                    MONO_ICON_CACHE.put(cs, c);
                }
                if (c[0] == ICON_SATURATED) {
                    ThemeCompat.tintDrawable(bg, p.accent);
                } else if (c[0] == ICON_GRAYSCALE) {
                    int rgb = c[1] < 0x60 ? ThemePalette.withAlpha(p.gray, 0xFF) : ThemePalette.withAlpha(p.surfaceCard, 0xFF);
                    ThemeCompat.tintDrawable(bg, ThemePalette.withAlpha(rgb, c[1]));
                }
            }
        }
    }

    private static void tintIconIfMonochrome(ImageView iv, ThemePalette p) {
        Drawable d = iv.getDrawable();
        if (d == null || d instanceof BitmapDrawable) return;
        int w = d.getIntrinsicWidth();
        int h = d.getIntrinsicHeight();
        if (w <= 0 || h <= 0 || w > 192 || h > 192) return; // 过大者不是图标
        Drawable.ConstantState cs = d.getConstantState();
        if (cs == null) return;
        int[] c = MONO_ICON_CACHE.get(cs);
        if (c == null) {
            c = classifyDrawable(d);
            MONO_ICON_CACHE.put(cs, c);
        }
        if (c[0] == ICON_NEUTRAL) {
            iv.setColorFilter(p.textPrimary, android.graphics.PorterDuff.Mode.SRC_IN);
        } else if (c[0] == ICON_SATURATED) {
            iv.setColorFilter(p.accent, android.graphics.PorterDuff.Mode.SRC_IN);
        }
    }

    /**
     * 缩至 16px 绘制采样分类 drawable：
     * 多色 → 跳过；近白中性 → 1（文字色）；有彩度单色 → 2（强调色）；
     * 灰阶中性 → 3（表面色，保留代表 alpha）；纯黑半透明压暗蒙层 → 跳过。
     */
    private static int[] classifyDrawable(Drawable d) {
        Bitmap bmp = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
        Rect orig = d.copyBounds();
        try {
            Canvas canvas = new Canvas(bmp);
            d.setBounds(0, 0, 16, 16);
            d.draw(canvas);
            int rMin = 255, gMin = 255, bMin = 255, rMax = 0, gMax = 0, bMax = 0, aMin = 255;
            boolean any = false;
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int px = bmp.getPixel(x, y);
                    int a = px >>> 24;
                    if (a < 0x40) continue;
                    any = true;
                    if (a < aMin) aMin = a;
                    int r = (px >> 16) & 0xFF, g = (px >> 8) & 0xFF, b = px & 0xFF;
                    if (r < rMin) rMin = r;
                    if (g < gMin) gMin = g;
                    if (b < bMin) bMin = b;
                    if (r > rMax) rMax = r;
                    if (g > gMax) gMax = g;
                    if (b > bMax) bMax = b;
                }
            }
            if (!any) return new int[]{ICON_SKIP, 0};
            int hi = Math.max(rMax, Math.max(gMax, bMax));
            int lo = Math.min(rMin, Math.min(gMin, bMin));
            boolean mono = rMax - rMin <= 0x30 && gMax - gMin <= 0x30 && bMax - bMin <= 0x30;
            if (!mono) return new int[]{ICON_SKIP, aMin}; // 多色不碰
            if (rMin >= 0xB8 && gMin >= 0xB8 && bMin >= 0xB8 && hi - lo <= 0x2F)
                return new int[]{ICON_NEUTRAL, aMin};
            if (hi - lo >= 0x30) return new int[]{ICON_SATURATED, aMin}; // 有彩度的单色
            if (hi <= 0x18 && aMin < 0xF0) return new int[]{ICON_SKIP, aMin}; // 压暗蒙层
            return new int[]{ICON_GRAYSCALE, aMin};
        } catch (Exception e) {
            return new int[]{ICON_SKIP, 0};
        } finally {
            d.setBounds(orig); // 还原现场 bounds，避免 ImageView 按 16px 绘制
            bmp.recycle();
        }
    }

    private static void tintButton(Button button, ThemePalette p) {
        android.content.res.ColorStateList tint = null;
        if (button instanceof AppCompatButton) {
            tint = ((AppCompatButton) button).getSupportBackgroundTintList();
        }
        // 接管两类按钮：
        // - ButtonStyle 体系（backgroundTint=#cc262626）
        // - 代码动态 new MaterialButton()（backgroundTint=?attr/colorPrimary=#ebe0e2，
        //   无样式时走主题默认，若不接管会在任何主题下保持近白底色+主题文字）
        // tint==null 说明有自定义 background drawable（弹幕发送/字幕芯片等），自成设计不动。
        if (tint == null) return;
        int def = tint.getDefaultColor();
        if (def != DEFAULT_BUTTON_BG && def != DEFAULT_TEXT) return;
        ThemeCompat.setBackgroundTintList(button, ThemeCompat.pressedStateList(p.buttonTint, p.buttonTint));
        if (isThemedTextColor(button.getCurrentTextColor())) {
            button.setTextColor(p.selectedText);
        }
    }
}
