package com.RobinNotBad.BiliClient.theme;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
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
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
 * 保护 VIP 粉、业务色等；topbar id 恒染。颜色判定本身在 TintRules（纯函数，可单测）。
 */
@android.annotation.SuppressLint("RestrictedApi")
public class ThemeApplier {

    /** 背景图解码缓存：key=文件路径+采样，SoftReference 防内存压力 */
    private static final Map<String, SoftReference<Bitmap>> BG_CACHE = new HashMap<>();

    // ---------------------------------------------------------------- 窗口背景

    /** 设置窗口背景：主题背景图 + scrim，或纯色窗口背景 */
    public static void setupWindow(Activity activity) {
        ThemeManager tm = ThemeManager.getInstance();
        if (tm == null || !tm.isReady() || activity == null || activity.getWindow() == null) return;
        ThemePalette palette = tm.getPalette();
        // 原始色模式：窗口背景与系统栏保持 XML 原样，也不启用主题层的 edge-to-edge
        if (palette == null || palette.originalColors) return;

        Drawable windowBg = null;
        File bgFile = tm.getActiveBackgroundFile();
        if (bgFile != null) {
            // onCreate 阶段 decor 尚未布局，用屏幕真实尺寸做解码目标
            int dw = 0;
            int dh = 0;
            try {
                // use new api to get props we need
                if (Build.VERSION.SDK_INT >= 30) {
                    WindowMetrics windowMetrics = activity.getWindowManager().getCurrentWindowMetrics();
                    Insets insets = windowMetrics.getWindowInsets().getInsets(
                            WindowInsets.Type.navigationBars() | WindowInsets.Type.displayCutout()
                    );
                    dw = windowMetrics.getBounds().width() - insets.left - insets.right;
                    dh = windowMetrics.getBounds().height() - insets.top - insets.bottom;
                } else {
                    android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
                    //noinspection deprecation
                    activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
                    dw = metrics.widthPixels;
                    dh = metrics.heightPixels;
                }

            } catch (Exception ignored) {
            }
            dw = Math.max(1, dw);
            dh = Math.max(1, dh);
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
        setupEdgeToEdge(activity, tm.isDark());

        // 状态栏跟随强调色
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            try {
                activity.getWindow().setStatusBarColor(ThemePalette.withAlpha(palette.accent, 0xFF));
            } catch (Exception ignored) {
            }
        }
    }

    /** edge-to-edge：decorFitsSystemWindows=false + 透明系统栏 + insets 让位（幂等） */
    @SuppressWarnings("deprecation")
    private static void setupEdgeToEdge(final Activity activity, final boolean dark) {
        if (activity == null || activity.getWindow() == null) return;
        // 全屏/无状态栏页面跳过（Player/Splash/圆屏）
        if (isStatusBarHidden(activity)) return;
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            activity.getWindow().setDecorFitsSystemWindows(false);
        } else if (android.os.Build.VERSION.SDK_INT >= 21) {
            activity.getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
            activity.getWindow().getDecorView().setSystemUiVisibility(
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            if (dark && Build.VERSION.SDK_INT >= 23) {
                activity.getWindow().getDecorView().setSystemUiVisibility(
                        activity.getWindow().getDecorView().getSystemUiVisibility()
                                | android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
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
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop() + top,
                        v.getPaddingRight(), v.getPaddingBottom() + bottom);
                return insets;
            });
            content.requestApplyInsets();
        } else {
            // API21 以下：手动读状态栏高度让位（无 insets API）
            try {
                @SuppressLint({"InternalInsetResource", "DiscouragedApi"})
                int resId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
                int top = resId > 0 ? activity.getResources().getDimensionPixelSize(resId) : 0;
                content.setPadding(content.getPaddingLeft(), content.getPaddingTop() + top,
                        content.getPaddingRight(), content.getPaddingBottom());
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 状态栏是否隐藏。主题的 android:windowFullscreen 由 generateLayout 合进窗口 flags，
     * 而 setupWindow 跑在 setContentView 之前，所以必须先取 decor 逼出这次生成；
     * 漏判时窗口已被框架让出状态栏，再按 insets 加一次 padding 就双重下移。
     */
    @SuppressWarnings("deprecation")
    private static boolean isStatusBarHidden(Activity activity) {
        Window window = activity.getWindow();
        View decor = window.getDecorView();
        if (Build.VERSION.SDK_INT >= 21) {
            WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(decor);
            if (insets != null) {
                return !insets.isVisible(WindowInsetsCompat.Type.statusBars());
            }
        }
        if ((window.getAttributes().flags
                & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0) {
            return true;
        }
        if (Build.VERSION.SDK_INT >= 16) {
            return (decor.getSystemUiVisibility()
                    & View.SYSTEM_UI_FLAG_FULLSCREEN) != 0;
        }
        return false;
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

        switch (mode) {
            case "tile":
                BitmapDrawable tile = new BitmapDrawable(res, src);
                tile.setTileModeXY(android.graphics.Shader.TileMode.REPEAT, android.graphics.Shader.TileMode.REPEAT);
                tile.setGravity(Gravity.FILL);
                return tile;
            case "fitXY": {
                Bitmap scaled = Bitmap.createScaledBitmap(src, dw, dh, true);
                return new BitmapDrawable(res, scaled);
            }
            case "centerInside": {
                float scale = Math.min((float) dw / bw, (float) dh / bh);
                int nw = Math.max(1, Math.round(bw * scale));
                int nh = Math.max(1, Math.round(bh * scale));
                Bitmap scaled = Bitmap.createScaledBitmap(src, nw, nh, true);
                BitmapDrawable bd = new BitmapDrawable(res, scaled);
                bd.setGravity(Gravity.CENTER);
                return new LayerDrawable(new Drawable[]{
                        new ColorDrawable(fallbackColor), bd});
            }
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

    /** 应用内容染色（已打标节点跳过自身，仍走进未打标的后加子视图） */
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

    /** 适配器 attach/bind：已打标则不重染自身，仍递归未打标孩子 */
    public static void refreshIfStale(View root) {
        ThemeManager tm = ThemeManager.getInstance();
        if (tm == null || root == null) return;
        apply(root, tm.getGeneration(), tm.getPalette(), false);
    }

    private static void apply(View view, int generation, ThemePalette p, boolean forceDark) {
        if (view == null) return;
        if (view.getTag(R.id.theme_no_tint) != null) return; // 跳过标记子树
        if (view instanceof SurfaceView || view instanceof TextureView) return;
        if (view.getClass().getName().contains("Danmaku")) return; // 弹幕渲染面

        hookRecyclerView(view, forceDark);
        Object tag = view.getTag(R.id.theme_applied_gen);
        boolean already = tag instanceof Integer && ((Integer) tag) == generation;
        if (!already) tintView(view, p);

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

    @SuppressWarnings("ConstantValue")
    private static void tintView(View view, ThemePalette p) {
        if (p.originalColors) return;

        // 单色 shape 背景跟随色板（含布局内联 hex 的 ColorDrawable）：
        // 有彩度 → accent；灰阶实心 → surfaceCard、弱化/分隔 → gray；
        // 近白/纯黑蒙层保持原样。逻辑见 retintBackground。
        retintBackground(view, p);

        if (view instanceof MaterialCardView) {
            MaterialCardView card = (MaterialCardView) view;
            int cardBg = card.getCardBackgroundColor().getDefaultColor();
            // bind 已写成强调色/当前表面色（如私信气泡）则不覆盖，只收默认灰/近白
            if (!TintRules.sameRgb(cardBg, p.accent) && !TintRules.sameRgb(cardBg, p.surfaceCard)
                    && (TintRules.isDefaultSurface(cardBg) || TintRules.nearWhiteNeutral(cardBg))) {
                card.setCardBackgroundColor(p.surfaceCard);
            }
            // 描边只收"历史默认灰"：适配器/bind 显式写好的选中态（selected、accentLow 等）
            // 已是色板令牌，再覆盖一次就会把刚 setChecked 的颜色打回 gray
            int stroke = card.getStrokeColor();
            if (TintRules.isDefaultStroke(stroke) && !TintRules.isPaletteToken(stroke, p))
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
        // 分支必须按「子类在前」排列：SeekBar 是 ProgressBar、Switch/RadioButton 是 Button，
        // 放在后面会永远被父类分支截走（历史顺序就是这两处被截，选中态色板分支整段不执行）
        if (view instanceof SeekBar) {
            ThemeCompat.tintSeekBar((SeekBar) view, p.accent);
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
        // 复合按钮（SwitchCompat/Switch/RadioButton 都是 Button 的子类）必须在
        // instanceof Button 之前判定，否则会被父类分支截走
        if (view instanceof SwitchCompat) {
            SwitchCompat sw = (SwitchCompat) view;
            // checked 档必须走选中令牌：accent 与 unchecked 的 textPrimary 在默认深色下同档，
            // 开关会看起来"开和关一个样"
            sw.setThumbTintList(ThemeCompat.checkedStateList(p.textPrimary, p.selected));
            sw.setTrackTintList(ThemeCompat.checkedStateList(p.ripple,
                    ThemePalette.withAlpha(p.selected, 0x99)));
            if (TintRules.isThemedTextColor(sw.getCurrentTextColor())) sw.setTextColor(p.textPrimary);
            return;
        }
        if (view instanceof Switch) {
            @SuppressLint("UseSwitchCompatOrMaterialCode")
            Switch sw = (Switch) view;
            // setThumbTintList/setTrackTintList 为 API23+（getThumbDrawable 其实 API16 就有，
            // 但拿它整体套一色会让开/关两个状态同色，与下面 RadioButton 分支同一个坑），
            // 所以与 SwitchCompat 分支走同一条 checkedStateList 语义
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                sw.setThumbTintList(ThemeCompat.checkedStateList(p.textPrimary, p.selected));
                sw.setTrackTintList(ThemeCompat.checkedStateList(p.ripple,
                        ThemePalette.withAlpha(p.selected, 0x99)));
            }
            if (TintRules.isThemedTextColor(sw.getCurrentTextColor())) sw.setTextColor(p.textPrimary);
            return;
        }
        if (view instanceof AppCompatRadioButton) {
            AppCompatRadioButton radio = (AppCompatRadioButton) view;
            radio.setSupportButtonTintList(ThemeCompat.checkedStateList(p.ripple, p.selected));
            if (TintRules.isThemedTextColor(radio.getCurrentTextColor())) radio.setTextColor(p.textPrimary);
            return;
        }
        if (view instanceof RadioButton) {
            RadioButton radio = (RadioButton) view;
            // setButtonTintList 为 API21+；与上面的 AppCompatRadioButton 分支同一条语义
            // （未选 ripple / 选中 selected）。这里不能退回 tintDrawable 整体套色：
            // button drawable 同时含未选环与选中点，套一色会让两个状态同色。
            // 布局里的 <RadioButton> 在 AppCompat 宿主下会被换成 AppCompatRadioButton，
            // 本分支只覆盖代码里 new RadioButton 的路径（当前全仓没有）。
            if (android.os.Build.VERSION.SDK_INT >= 21)
                radio.setButtonTintList(ThemeCompat.checkedStateList(p.ripple, p.selected));
            if (TintRules.isThemedTextColor(radio.getCurrentTextColor())) radio.setTextColor(p.textPrimary);
            return;
        }
        if (view instanceof Button) {
            tintButton((Button) view, p);
            return;
        }
        if (view instanceof EditText) {
            EditText editText = (EditText) view;
            android.content.res.ColorStateList hintColors = editText.getHintTextColors();
            if (hintColors != null) {
                int hint = hintColors.getDefaultColor();
                // 只染默认文字色族的 hint，并保留原 alpha 档（规则在 TintRules.hintTextColor）
                if (TintRules.isThemedTextColor(hint))
                    editText.setHintTextColor(TintRules.hintTextColor(hint, p));
            }
            if (TintRules.isThemedTextColor(editText.getCurrentTextColor())) editText.setTextColor(p.textPrimary);
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
        if (view instanceof ImageView) {
            tintIconIfMonochrome((ImageView) view, p);
            return;
        }
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            int color = textView.getCurrentTextColor();
            int id = textView.getId();
            int semantic = semanticTag(textView);
            int textToken = TintRules.semanticTextToken(semantic);
            int iconToken = TintRules.semanticIconToken(semantic);
            if (iconToken != TintRules.SEM_NONE) {
                ThemeCompat.tintCompoundDrawables(textView, TintRules.semanticColor(iconToken, p));
            }
            if (textToken != TintRules.SEM_NONE) {
                // 文字色由代码写定（已赞、当前分P 这类），按当前色板重放，不再走字面量启发式
                textView.setTextColor(TintRules.semanticColor(textToken, p));
                return;
            }
            if (id == R.id.pageName || id == R.id.timeText) {
                textView.setTextColor(p.textPrimary);
                ThemeCompat.tintCompoundDrawables(textView, p.textPrimary);
            } else if (color == TintRules.DEFAULT_TEXT_TRANSPARENT) {
                // 半透明默认值须先于启发式判断（0x50FEFEFE 也满足"近白"）
                textView.setTextColor(p.textTransparent);
            } else if (color == 0xFF66CCFF) { // @color/link
                textView.setTextColor(p.link);
            } else if (TintRules.isDanger(color)) {
                // 历史写死的警示橙（删除收藏夹等）→ 警示令牌，不随强调色中性化褪掉
                textView.setTextColor(p.danger);
                ThemeCompat.tintCompoundDrawables(textView, p.danger);
            } else if (TintRules.isDecorPink(color)) {
                // 历史写死的各档装饰粉（@color/pink、点赞数等）统一收编为强调色
                textView.setTextColor(p.accent);
                ThemeCompat.tintCompoundDrawables(textView, p.accent);
            } else if (TintRules.isThemedTextColor(color)) {
                textView.setTextColor(p.textPrimary);
                ThemeCompat.tintCompoundDrawables(textView, p.textPrimary);
            } else if (TintRules.isNeutralGrayText(color)) {
                // 中灰说明文字（#999/#AAA/#a2a2a2 族）→ 次级文字令牌
                textView.setTextColor(p.textSecondary);
            }
        }
    }

    /** 采样分类缓存：int[]{kind, 代表alpha, 档数, 主导亮度档}，形状见 TintRules.classifySample */
    private static final android.util.LruCache<Drawable.ConstantState, int[]> MONO_ICON_CACHE =
            new android.util.LruCache<>(128);

    /** 采样失败只报第一次（见 classifyDrawable 的 catch） */
    private static boolean SAMPLING_FAILURE_LOGGED = false;

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

    /** setImageResource + 立刻按色板重染，避免换图后露出旧粉 */
    public static void setImage(ImageView iv, int resId) {
        if (iv == null) return;
        iv.setImageResource(resId);
        retintImage(iv);
    }

    /**
     * 播放器控制栏的 mipmap PNG 状态图标专用：位图被 tintIconIfMonochrome 的
     * BitmapDrawable 守卫挡在外面（Glide 头像/封面也走那条守卫，不能删），
     * 所以这里按调用点已知的语义显式染色。实测这 8 张图都是单色字形
     * （黑像素只是 alpha&lt;128 的抗锯齿边缘），SRC_IN 不会糊掉细节。
     * active=true 取选中令牌（原本是写死的蓝 #4A90E2），false 取文字色。
     * 播放器恒为深色场景，取深色套色板，与分 P 列表、高能曲线一致。
     */
    public static void setStateImage(ImageView iv, int resId, boolean active) {
        if (iv == null) return;
        iv.setImageResource(resId);
        ThemePalette p = ThemeManager.paletteDark();
        if (p.originalColors) {
            iv.clearColorFilter();
            iv.setTag(R.id.theme_semantic, null);
            return;
        }
        int token = active ? TintRules.SEM_SELECTED : TintRules.SEM_TEXT_PRIMARY;
        iv.setColorFilter(TintRules.semanticColor(token, p),
                android.graphics.PorterDuff.Mode.SRC_IN);
        iv.setTag(R.id.theme_semantic, TintRules.semanticWithIcon(0, token));
    }

    /**
     * 已赞/已币/已收藏这类**状态图标**必须点名染色（激活=选中令牌，未激活=文字色）：
     * 它们的历史 fillColor 是饱和粉 #fb8799，通用彩度分支会把饱和色一律收成 accent，
     * 默认深色主题下就压成 #EAEAEA，和未选中态的 #EBE0E2 同一个档，等于看不出三连过没有。
     */
    public static void setStateIcon(ImageView iv, int resId, boolean active) {
        if (iv == null) return;
        iv.setImageResource(resId);
        ThemePalette p = ThemeManager.palette();
        if (p.originalColors) {
            iv.clearColorFilter();
            iv.setTag(R.id.theme_semantic, null);
            return;
        }
        int token = active ? TintRules.SEM_SELECTED : TintRules.SEM_TEXT_PRIMARY;
        iv.setColorFilter(TintRules.semanticColor(token, p),
                android.graphics.PorterDuff.Mode.SRC_IN);
        iv.setTag(R.id.theme_semantic, TintRules.semanticWithIcon(0, token));
    }

    /** TextView 换复合图标后按指定色重染（评论/动态点赞态） */
    public static void retintCompound(TextView tv, int color) {
        if (tv == null || ThemeManager.palette().originalColors) return;
        ThemeCompat.tintCompoundDrawables(tv, color);
        int token = TintRules.semanticToken(color, ThemeManager.palette());
        tv.setTag(R.id.theme_semantic, TintRules.semanticWithIcon(semanticTag(tv), token));
    }

    /**
     * 代码写定的文字语义色（已赞计数、当前分P 等）。必须记令牌而不是只 setTextColor：
     * RecyclerView 先 bind 后 attach，attach 钩子跑树级染色时，默认深色的 selected
     * 与历史装饰粉 #FB7299 同值，会被「装饰粉→accent」规则原地吃掉。
     */
    public static void setSemanticText(TextView tv, int color, ThemePalette p) {
        if (tv == null) return;
        tv.setTextColor(color);
        int token = TintRules.semanticToken(color, p);
        tv.setTag(R.id.theme_semantic, TintRules.semanticWithText(semanticTag(tv), token));
    }

    private static int semanticTag(View view) {
        Object tag = view.getTag(R.id.theme_semantic);
        return tag instanceof Integer ? (Integer) tag : 0;
    }

    /**
     * 播放器专用：换图后按**深色套**补染。播放器是永久深色场景（树级入口就是
     * `applyContentForcedDark`），补染若跟随浅色套，浅色模式下 textPrimary=#1F1B1B
     * 的近黑图标会画在黑底控制栏上——批 5 补上这批调用点时就是把这条漏了。
     */
    public static void retintImageForcedDark(ImageView iv) {
        if (iv == null) return;
        ThemeManager tm = ThemeManager.getInstance();
        if (tm == null) return;
        tintIconIfMonochrome(iv, tm.getPaletteDark());
    }

    /** 换背景后按**深色套**重染（播放器分 P 行的选中底），理由同 retintImageForcedDark */
    public static void retintBackgroundForcedDark(View v) {
        if (v == null) return;
        ThemeManager tm = ThemeManager.getInstance();
        if (tm == null) return;
        retintBackground(v, tm.getPaletteDark());
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
        if (p.originalColors) return;
        // 单色背景跟随色板：
        // - 有彩度 → accent
        // - 灰阶按源亮度分档（见 TintRules.backgroundTier）：实心深灰 → surfaceCard 的 RGB、
        //          中灰弱化/分隔 → gray 令牌的 RGB，两者都只换 RGB、保留原 alpha
        //          （内置深色 gray = #70707070，与 colors.xml 一致）；
        // - 近白与纯黑半透明压暗蒙层（照片 scrim）功能性强，保持原样。
        Drawable bg = view.getBackground();
        if (bg instanceof android.graphics.drawable.ColorDrawable) {
            // 布局里内联 hex 背景（分隔线/评论框/动态图底/用户页等）
            int c = ((android.graphics.drawable.ColorDrawable) bg).getColor();
            int alpha = c >>> 24;
            int tier = TintRules.backgroundTier(c);
            if (tier == TintRules.BG_SATURATED) { // 有彩度单色（本地视频绿徽等）
                view.setBackgroundColor(p.accent);
            } else if (tier == TintRules.BG_SURFACE || tier == TintRules.BG_DIVIDER) {
                int token = tier == TintRules.BG_DIVIDER ? p.gray : p.surfaceCard;
                view.setBackgroundColor(TintRules.applyAlpha(token, alpha));
            }
        } else if (bg instanceof android.graphics.drawable.StateListDrawable) {
            // 状态选择器背景（如互动投票芯片：默认态灰阶、按压态粉色）
            // 逐状态原地 mutate 染色：彩色 → accent；灰阶 → surfaceCard/gray 分档。
            // getStateCount/getStateDrawable 为 API29+；<29 保持原样。
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                android.graphics.drawable.StateListDrawable sld = (android.graphics.drawable.StateListDrawable) bg;
                int count = sld.getStateCount();
                for (int i = 0; i < count; i++) {
                    Drawable state = sld.getStateDrawable(i);
                    if (state == null) continue;
                    int[] c = classifyCached(state);
                    if (c == null) continue;
                    if (c[0] == TintRules.ICON_SATURATED) {
                        applyTintedBackgroundState(state, p.accent);
                    } else if (c[0] == TintRules.ICON_GRAYSCALE
                            && !(c[2] > 1 && TintRules.preserveMultiTierGrayscale(p))) {
                        applyTintedBackgroundState(state,
                                TintRules.tintColor(TintRules.grayScaleSurface(c[3], p), c[1]));
                    }
                }
            }
        } else if (bg instanceof android.graphics.drawable.GradientDrawable
                || bg instanceof android.graphics.drawable.ShapeDrawable) {
            Drawable.ConstantState cs = bg.getConstantState();
            if (cs != null) {
                int[] c = classifyCached(bg);
                if (c == null) return;
                if (c[0] == TintRules.ICON_SATURATED) {
                    setTintedBackground(view, bg, p.accent);
                } else if (c[0] == TintRules.ICON_GRAYSCALE
                        && !(c[2] > 1 && TintRules.preserveMultiTierGrayscale(p))) {
                    // 深色主题下两档灰阶（solid+stroke）保持原样：GradientDrawable 没有公开的
                    // 描边取色口，整张套一色就等于把描边抹进填充里。浅色主题表面色是亮的，
                    // 不跟随会留下深灰框，所以那里照常压平染色。
                    setTintedBackground(view, bg,
                            TintRules.tintColor(TintRules.grayScaleSurface(c[3], p), c[1]));
                }
            }
        }
    }

    private static void tintIconIfMonochrome(ImageView iv, ThemePalette p) {
        if (p.originalColors) return;
        int iconToken = TintRules.semanticIconToken(semanticTag(iv));
        if (iconToken != TintRules.SEM_NONE) {
            // setStateIcon/setStateImage 写定的状态色：按当前色板重放，别压成 accent
            iv.setColorFilter(TintRules.semanticColor(iconToken, p),
                    android.graphics.PorterDuff.Mode.SRC_IN);
            return;
        }
        Drawable d = iv.getDrawable();
        if (d == null || d instanceof BitmapDrawable) return;
        int w = d.getIntrinsicWidth();
        int h = d.getIntrinsicHeight();
        if (w <= 0 || h <= 0 || w > 192 || h > 192) return; // 过大者不是图标
        Drawable.ConstantState cs = d.getConstantState();
        if (cs == null) return;
        int[] c = classifyCached(d);
        if (c == null) return;
        if (c[0] == TintRules.ICON_NEUTRAL) {
            iv.setColorFilter(p.textPrimary, android.graphics.PorterDuff.Mode.SRC_IN);
        } else if (c[0] == TintRules.ICON_SATURATED) {
            iv.setColorFilter(p.accent, android.graphics.PorterDuff.Mode.SRC_IN);
        }
    }

    /**
     * 缩至 16px 绘制采样分类 drawable：
     * 多色 → 跳过；近白中性 → 1（文字色）；有彩度单色 → 2（强调色）；
     * 灰阶中性 → 3（表面色，保留代表 alpha）；纯黑半透明压暗蒙层 → 跳过。
     * 结果 int[]{kind, 代表alpha, 档数, 主导亮度档}。
     */
    private static int[] classifyDrawable(Drawable d) {
        Bitmap bmp = null;
        Rect orig = null;
        try {
            bmp = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
            orig = d.copyBounds();
            Canvas canvas = new Canvas(bmp);
            d.setBounds(0, 0, 16, 16);
            d.draw(canvas);
            // 统计口在 TintRules.SampleStats：代表 alpha 取主体众数而不是抗锯齿边缘的最小值，
            // 否则 #dd262626 这类圆角 shape 会被当成半透明蒙层压黑（热评卡 #0c0c0c）。
            TintRules.SampleStats stats = new TintRules.SampleStats();
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    stats.add(bmp.getPixel(x, y));
                }
            }
            return stats.classify();
        } catch (Exception e) {
            // 采样失败 = 这只 drawable 这一次不染色。吞掉本身可以（个别 ROM 在 canvas/位图上会抛），
            // 但一次都不留痕的话，格数写错这类失效会永远查不到：只报第一次，避免逐 drawable 刷屏。
            // 返回 null 而不是 SKIP：失败结果若进缓存，会被当成合法的「这图本来就不染」永久记住
            if (!SAMPLING_FAILURE_LOGGED) {
                SAMPLING_FAILURE_LOGGED = true;
                android.util.Log.w("ThemeApplier",
                        "drawable 采样失败，该图本次保持原色：" + d.getClass().getName(), e);
            }
            return null;
        } finally {
            if (orig != null) d.setBounds(orig); // 还原现场 bounds，避免 ImageView 按 16px 绘制
            if (bmp != null) bmp.recycle();
        }
    }

    /** 带缓存的分类入口：null 表示这次采样失败（不缓存，下次还会再试） */
    private static int[] classifyCached(Drawable d) {
        if (d == null) return null;
        Drawable.ConstantState cs = d.getConstantState();
        if (cs != null) {
            int[] hit = MONO_ICON_CACHE.get(cs);
            if (hit != null) return hit;
        }
        int[] c = classifyDrawable(d);
        if (c != null && cs != null) MONO_ICON_CACHE.put(cs, c);
        return c;
    }

    /** API&lt;21 wrap 会产生新实例，必须设回视图 */
    private static void setTintedBackground(View view, Drawable original, int color) {
        Drawable tinted = ThemeCompat.tintDrawable(original, color);
        if (tinted != null && tinted != original)
            ViewCompat.setBackground(view, tinted);
    }

    private static void applyTintedBackgroundState(Drawable state, int color) {
        ThemeCompat.tintDrawable(state, color);
    }

    private static void tintButton(Button button, ThemePalette p) {
        android.content.res.ColorStateList tint = null;
        if (button instanceof MaterialButton) {
            //noinspection RedundantCast
            tint = ((MaterialButton) button).getBackgroundTintList();
        }
        if (tint == null && button instanceof AppCompatButton) {
            tint = ((AppCompatButton) button).getSupportBackgroundTintList();
        }
        if (tint == null) {
            tint = androidx.core.view.ViewCompat.getBackgroundTintList(button);
        }
        // tint==null：自定义 background（私信发送粉底等）。饱和单色收为 accent。
        if (tint == null) {
            Drawable bg = button.getBackground();
            if (bg == null) return;
            int[] c = classifyCached(bg);
            if (c == null) return;
            if (c[0] == TintRules.ICON_SATURATED) {
                setTintedBackground(button, bg, p.accent);
                if (TintRules.isThemedTextColor(button.getCurrentTextColor())) {
                    button.setTextColor(p.selectedText);
                }
            }
            return;
        }
        int def = tint.getDefaultColor();
        int fill;
        if (TintRules.sameRgb(def, p.buttonTint) || TintRules.isDefaultSurface(def) || def == TintRules.DEFAULT_TEXT || TintRules.nearWhiteNeutral(def)) {
            fill = p.buttonTint;
        } else if (TintRules.sameRgb(def, p.accent) || TintRules.isDecorPink(def) || TintRules.isSaturatedColor(def)) {
            fill = p.accent;
        } else {
            return;
        }
        ThemeCompat.setBackgroundTintList(button, ThemeCompat.pressedStateList(fill));
        if (TintRules.isThemedTextColor(button.getCurrentTextColor())) {
            button.setTextColor(p.selectedText);
        }
    }
}
